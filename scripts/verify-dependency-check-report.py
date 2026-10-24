#!/usr/bin/env python3
"""Fail unless a dependency-check JSON report scanned the runtime classpath it was meant to scan.

Reads the report written by dependencyCheckAnalyze and the module tree printed by
`./gradlew dependencies --configuration runtimeClasspath`, and passes only when the report lists
at least 95 percent as many non-virtual dependencies as the tree has unique modules. It always
prints a markdown table (expected modules, dependencies scanned, vulnerabilities by severity) and
appends it to --summary when given, so the job summary carries the counts.
Exit codes: 0 pass, 1 guard failure, 2 usage error. Stdlib only, so CI needs no pip step.

Decisions:
  * Count-based, because the failure this guards against was a count: the 2026-08-19 scan went
    green with 0 dependencies scanned, since the plugin treated runtimeClasspath as a test
    configuration and skipped it. A report that lists nothing is indistinguishable from a clean
    report unless something states how many entries to expect.
  * The denominator comes from the tree of the same commit, not a fixed minimum, so adding or
    dropping a dependency moves the expectation with it and nobody edits a number.
  * 95 percent, not 100: dependency-check may merge or drop an entry (a repackaged jar, a module
    that resolves to a POM with no jar). The figure is a chosen margin, not a measured one, and
    --tolerance changes it for one run. Loosening it to make a run pass hides the failure the
    guard exists for: diagnose the scan instead.
  * Virtual entries do not count toward the floor. A virtual dependency is one the engine
    synthesised from evidence, not a jar it opened, so a report padded with them scanned nothing.
  * A tree with no parsable module line, or with a FAILED resolution line, fails: a guard without
    a denominator has stopped checking.
  * Severity names are upper-cased and otherwise reported as the engine wrote them, so a new
    severity word shows up in the table instead of being folded into another.

Known holes:
  * Counts are compared, not identities: a scan of the right number of the wrong jars passes.
  * POM-only modules in the tree count toward the expected number even if the engine has no jar
    to open for them.
  * The report's scanInfo (engine version, data source timestamps) is printed and never judged,
    so a stale NVD database does not fail this guard.
  * Suppressed vulnerabilities are not counted in the severity breakdown.
"""

import argparse
import json
import math
import re
import sys

DEFAULT_TOLERANCE = 0.05
SEVERITY_ORDER = ("CRITICAL", "HIGH", "MEDIUM", "MODERATE", "LOW")

_MODULE_LINE = re.compile(r"^[| ]*[+\\]--- (?P<body>.+?)\s*$")
_MARKER_SUFFIX = re.compile(r"\s+\((?P<mark>\*|c|n)\)$")


class GuardError(Exception):
    """A problem with an input that makes the verdict a failure rather than a usage error."""


def parse_tree(text):
    """Return (modules, failed_lines): the unique group:artifact:version triples and FAILED lines.

    A module is counted at its resolved (right-hand) version. Constraint (c) lines, platform (n)
    lines and project lines are not modules. A (*) line repeats a subtree printed elsewhere, so it
    is counted once through the dedupe.
    """
    modules = set()
    failed = []
    for line in text.splitlines():
        match = _MODULE_LINE.match(line)
        if not match:
            continue
        body = match.group("body")
        if re.search(r"\bFAILED$", body):
            failed.append(line.strip())
            continue
        marker = _MARKER_SUFFIX.search(body)
        if marker:
            body = body[: marker.start()]
            if marker.group("mark") in ("c", "n"):
                continue
        if body.startswith("project "):
            continue
        if " -> " in body:
            left, right = body.split(" -> ", 1)
            version = right.split()[0] if right.split() else ""
            if right.startswith("project "):
                continue
            coordinates = left.split(":")
        else:
            coordinates = body.split(":")
            version = coordinates[2] if len(coordinates) >= 3 else ""
        if len(coordinates) < 2 or not coordinates[0] or not coordinates[1] or not version:
            continue
        modules.add((coordinates[0], coordinates[1], version))
    return modules, failed


def load_report(path):
    """Return the parsed report as a dict, or raise GuardError naming why it cannot be used."""
    try:
        with open(path, encoding="utf-8") as handle:
            report = json.load(handle)
    except OSError as err:
        raise GuardError("cannot read the report %s: %s" % (path, err))
    except ValueError as err:
        raise GuardError("the report %s is not valid JSON: %s" % (path, err))
    if not isinstance(report, dict) or not isinstance(report.get("dependencies"), list):
        raise GuardError("the report %s has no top-level dependencies array" % path)
    return report


def summarize_report(report):
    """Return (all_entries, non_virtual, vulnerable_dependencies, severity_counts)."""
    entries = [dep for dep in report["dependencies"] if isinstance(dep, dict)]
    non_virtual = [dep for dep in entries if dep.get("isVirtual") is not True]
    severities = {}
    vulnerable = 0
    for dep in entries:
        vulnerabilities = dep.get("vulnerabilities")
        if not isinstance(vulnerabilities, list) or not vulnerabilities:
            continue
        vulnerable += 1
        for vulnerability in vulnerabilities:
            severity = "UNKNOWN"
            if isinstance(vulnerability, dict) and vulnerability.get("severity"):
                severity = str(vulnerability["severity"]).upper()
            severities[severity] = severities.get(severity, 0) + 1
    return len(entries), len(non_virtual), vulnerable, severities


def floor_for(expected, tolerance):
    """Return the smallest non-virtual count that passes. The round() absorbs float noise."""
    return int(math.ceil(round(expected * (1 - tolerance), 6)))


def ordered_severities(severities):
    known = [name for name in SEVERITY_ORDER if name in severities]
    rest = sorted(name for name in severities if name not in SEVERITY_ORDER)
    return known + rest


def scan_info_rows(report):
    rows = []
    info = report.get("scanInfo")
    if not isinstance(info, dict):
        return rows
    if info.get("engineVersion"):
        rows.append(("Engine version", str(info["engineVersion"])))
    sources = info.get("dataSource")
    if isinstance(sources, list):
        for source in sources:
            if isinstance(source, dict) and source.get("name"):
                rows.append((str(source["name"]), str(source.get("timestamp", ""))))
    return rows


def render_table(rows):
    lines = ["| Check | Value |", "| --- | --- |"]
    lines.extend("| %s | %s |" % row for row in rows)
    return "\n".join(lines) + "\n"


def evaluate(report, tree_text, tolerance):
    """Return (ok, failures, rows): the verdict, why it failed, and the table rows."""
    modules, failed = parse_tree(tree_text)
    expected = len(modules)
    entries, non_virtual, vulnerable, severities = summarize_report(report)
    floor = floor_for(expected, tolerance)
    failures = []
    if failed:
        failures.append(
            "the runtime tree has %d unresolved line(s), e.g. %s: the denominator cannot be trusted"
            % (len(failed), failed[0])
        )
    if expected == 0:
        failures.append(
            "the runtime tree has no parsable module line: without a denominator the guard "
            "cannot tell a scan of the classpath from a scan of nothing"
        )
    if entries == 0:
        failures.append(
            "0 dependencies scanned: the report lists no dependency at all, so the scan skipped "
            "the classpath it was given or stopped before analysis"
        )
    elif expected > 0 and non_virtual < floor:
        failures.append(
            "%d non-virtual dependencies scanned against a floor of %d (%.0f%% of %d runtime modules)"
            % (non_virtual, floor, (1 - tolerance) * 100, expected)
        )
    ok = not failures
    rows = [
        ("Expected runtime modules (runtimeClasspath tree)", str(expected)),
        ("Dependencies scanned (all entries)", str(entries)),
        ("Dependencies scanned (non-virtual)", str(non_virtual)),
        ("Floor (%.0f%% of expected)" % ((1 - tolerance) * 100), str(floor)),
        ("Vulnerable dependencies", str(vulnerable)),
    ]
    for name in ordered_severities(severities):
        rows.append(("Vulnerabilities, %s" % name, str(severities[name])))
    if not severities:
        rows.append(("Vulnerabilities", "0"))
    rows.extend(scan_info_rows(report))
    rows.append(("Verdict", "PASS" if ok else "FAIL"))
    return ok, failures, rows


def read_tree(path):
    try:
        with open(path, encoding="utf-8") as handle:
            return handle.read()
    except OSError as err:
        raise GuardError("cannot read the runtime tree %s: %s" % (path, err))


def main(argv=None):
    parser = argparse.ArgumentParser(description="Fail unless the dependency-check report scanned the runtime classpath.")
    parser.add_argument("--report", required=True, help="dependency-check JSON report")
    parser.add_argument("--tree", required=True, help="output of dependencies --configuration runtimeClasspath")
    parser.add_argument("--tolerance", type=float, default=DEFAULT_TOLERANCE, help="allowed shortfall, 0 to below 1")
    parser.add_argument("--summary", default=None, help="file to append the markdown table to")
    try:
        args = parser.parse_args(argv)
    except SystemExit as exit_request:
        return 2 if exit_request.code else 0
    if not 0 <= args.tolerance < 1:
        print("usage error: --tolerance %s is outside 0 to below 1." % args.tolerance, file=sys.stderr)
        return 2
    try:
        report = load_report(args.report)
        tree_text = read_tree(args.tree)
    except GuardError as err:
        print("FAIL: %s" % err)
        return 1
    ok, failures, rows = evaluate(report, tree_text, args.tolerance)
    table = render_table(rows)
    print(table, end="")
    for failure in failures:
        print("FAIL: %s" % failure)
    if args.summary:
        try:
            with open(args.summary, "a", encoding="utf-8") as handle:
                handle.write("### Dependency scan coverage\n\n" + table + "\n")
        except OSError as err:
            print("error: cannot append to %s: %s" % (args.summary, err), file=sys.stderr)
            return 2
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
