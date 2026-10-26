#!/usr/bin/env python3
"""Proof harness for quick task 261026-ovp.

Run from the repo root as: python3 -I <this file> --label L --log LOG --tests '*Cls' ...

Exit codes:
- 0: every expectation matched
- 1: the run completed but an expectation was missed
- 2: INVALID, no JUnit XML was produced (compile or context failure)
- 3: refused before Gradle started, src/main state did not fit the run kind

A mutation run (any --expect-fail) needs a dirty src/main. A pass run needs a clean one.
"""

import argparse
import datetime
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RESULTS = Path("build/test-results/test")
GRADLE_LOG = Path("build/261026-ovp-last-gradle.log")

# Awaitility wraps the assertion that never became true, so its timeout counts too.
ASSERTION_TYPES = {
    "java.lang.AssertionError",
    "org.opentest4j.AssertionFailedError",
    "org.awaitility.core.ConditionTimeoutException",
}


def dirty_main():
    out = subprocess.run(
        ["git", "status", "--porcelain", "--", "src/main"],
        capture_output=True,
        text=True,
        check=True,
    ).stdout
    return [line[3:] for line in out.splitlines() if line.strip()]


def collapse(text, limit=200):
    return " ".join((text or "").split())[:limit]


def classify(case):
    failure = case.find("failure")
    if failure is None:
        failure = case.find("error")
    if failure is not None:
        message = failure.get("message") or (failure.text or "").strip()
        return "FAIL", failure.get("type") or "", collapse(message)
    if case.find("skipped") is not None:
        return "SKIP", "", ""
    return "PASS", "", ""


def collect():
    cases = []
    for path in sorted(RESULTS.glob("TEST-*.xml")):
        for case in ET.parse(path).getroot().iter("testcase"):
            actual, ftype, message = classify(case)
            cases.append(
                {
                    "classname": case.get("classname", ""),
                    "method": case.get("name", ""),
                    "actual": actual,
                    "type": ftype,
                    "message": message,
                }
            )
    return cases


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--label", required=True)
    parser.add_argument("--log", required=True)
    parser.add_argument("--tests", action="append", default=[])
    parser.add_argument("--expect-fail", action="append", default=[])
    parser.add_argument("--others-pass", action="store_true")
    args = parser.parse_args()

    dirty = dirty_main()
    mutation_run = bool(args.expect_fail)
    if mutation_run and not dirty:
        print("REFUSED: --expect-fail given but src/main is clean, no mutation applied")
        return 3
    if not mutation_run and dirty:
        print("REFUSED: pass run but src/main is dirty: " + ", ".join(dirty))
        return 3

    shutil.rmtree(RESULTS, ignore_errors=True)
    command = ["./gradlew", "test", "--rerun"]
    for pattern in args.tests:
        command += ["--tests", pattern]
    command += ["-x", "jacocoTestCoverageVerification"]
    GRADLE_LOG.parent.mkdir(parents=True, exist_ok=True)
    with GRADLE_LOG.open("w") as sink:
        subprocess.run(
            command, stdin=subprocess.DEVNULL, stdout=sink, stderr=subprocess.STDOUT
        )

    cases = collect()
    if not cases:
        print("INVALID: no JUnit XML produced. Last 40 lines of " + str(GRADLE_LOG))
        print("\n".join(GRADLE_LOG.read_text().splitlines()[-40:]))
        return 2

    named = {name + "()" for name in args.expect_fail} | set(args.expect_fail)
    ok = True
    rows = []
    seen = set()
    for case in cases:
        is_named = case["method"] in named
        if is_named:
            seen.add(case["method"])
            expected = "FAIL"
            matched = case["actual"] == "FAIL" and case["type"] in ASSERTION_TYPES
        elif not mutation_run or args.others_pass:
            expected = "PASS"
            matched = case["actual"] == "PASS"
        else:
            expected = "any"
            matched = True
        ok = ok and matched
        rows.append((expected, case, matched))

    for name in args.expect_fail:
        if name + "()" not in seen and name not in seen:
            ok = False
            print("MISSING: no testcase named " + name)

    stamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    dirty_text = ",".join(dirty)
    log = Path(args.log)
    with log.open("a") as out:
        for expected, case, _ in rows:
            out.write(
                "\t".join(
                    [
                        stamp,
                        args.label,
                        case["classname"],
                        case["method"],
                        expected,
                        case["actual"],
                        case["type"],
                        case["message"],
                        dirty_text,
                    ]
                )
                + "\n"
            )

    counts = {}
    for _, case, _ in rows:
        counts[case["actual"]] = counts.get(case["actual"], 0) + 1
    mismatched = [
        case["classname"] + "." + case["method"] for _, case, hit in rows if not hit
    ]
    print(
        "%s %s: %d cases %s%s"
        % (
            "MATCH" if ok else "MISMATCH",
            args.label,
            len(rows),
            counts,
            (" mismatched: " + "; ".join(mismatched)) if mismatched else "",
        )
    )
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
