#!/usr/bin/env python3
"""Self-test for scripts/verify-trivy-report.py: the guard fires on a scan that covered too little.

A guard that never fires looks identical to a scan that covered everything, so each case plants
one report and one expected-names file in a temporary directory, runs the guard as a subprocess,
and asserts its exit code and a fragment of its message. Run with --guard to point the same cases
at another copy of the guard: a copy with its floor comparison removed must make this script exit 1.
Reads nothing outside its temporary directory and the guard path. Exit codes: 0 all cases hold, 1 any fails.
"""

import argparse
import json
import math
import os
import subprocess
import sys
import tempfile

DEFAULT_GUARD = os.path.join(os.path.dirname(os.path.abspath(__file__)), "verify-trivy-report.py")

PACKAGES = 143
FLOOR = int(math.ceil(PACKAGES * 0.95))


def names(count):
    return ["pkg-%d" % index for index in range(count)]


def expected_text(entries):
    return "\n".join(entries) + "\n"


def image_report(count, family="ubuntu", vulnerabilities=None, with_results=True, extra_results=()):
    """An image report whose single os-pkgs Result lists `count` packages."""
    report = {"SchemaVersion": 2, "ArtifactName": "image.tar", "Metadata": {}}
    if family:
        report["Metadata"]["OS"] = {"Family": family, "Name": "22.04"}
    if with_results:
        result = {
            "Target": "image.tar (ubuntu 22.04)",
            "Class": "os-pkgs",
            "Type": "ubuntu",
            "Packages": [{"Name": name} for name in names(count)],
        }
        if vulnerabilities:
            result["Vulnerabilities"] = vulnerabilities
        report["Results"] = [result] + list(extra_results)
    return report


def config_result(target, kind="kubernetes", successes=10, failures=0, severities=()):
    result = {
        "Target": target,
        "Class": "config",
        "Type": kind,
        "MisconfSummary": {"Successes": successes, "Failures": failures},
    }
    if severities:
        result["Misconfigurations"] = [{"ID": "KSV%03d" % index, "Severity": severity} for index, severity in enumerate(severities)]
    return result


def config_report(results):
    return {"SchemaVersion": 2, "ArtifactName": "config-input", "Results": list(results)}


class Harness:
    def __init__(self, guard, workdir):
        self.guard = guard
        self.workdir = workdir
        self.counter = 0
        self.failures = []

    def path(self, suffix):
        self.counter += 1
        return os.path.join(self.workdir, "fixture-%d%s" % (self.counter, suffix))

    def write(self, text, suffix):
        path = self.path(suffix)
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(text)
        return path

    def run(self, args):
        result = subprocess.run(
            [sys.executable, "-I", self.guard] + args,
            cwd=self.workdir,
            capture_output=True,
            text=True,
            timeout=60,
        )
        return result.returncode, result.stdout + result.stderr

    def check(self, name, args, expect_code, fragments=()):
        code, output = self.run(args)
        problems = []
        if code != expect_code:
            problems.append("exit %d, expected %d" % (code, expect_code))
        for fragment in fragments:
            if fragment not in output:
                problems.append("output lacks %r" % fragment)
        if "Traceback" in output:
            problems.append("output holds a traceback")
        if problems:
            self.failures.append(name)
            print("FAIL  %s: %s" % (name, "; ".join(problems)))
            print("      output: %s" % output.strip().replace("\n", "\n              "))
        else:
            print("ok    %s" % name)

    def case(self, name, kind, report_obj, expected, expect_code, fragments=(), extra=()):
        report_path = self.write(json.dumps(report_obj), ".json")
        expected_path = self.write(expected, ".txt")
        self.check(name, ["--kind", kind, "--report", report_path, "--expected", expected_path] + list(extra), expect_code, fragments)

    def image(self, name, report_obj, expected, expect_code, fragments=(), extra=()):
        self.case(name, "image", report_obj, expected, expect_code, fragments, extra)

    def config(self, name, report_obj, expected, expect_code, fragments=(), extra=()):
        self.case(name, "config", report_obj, expected, expect_code, fragments, extra)


def run_image_cases(harness):
    expected = expected_text(names(PACKAGES))
    harness.image("a report with no Results scanned nothing and says so", image_report(0, with_results=False), expected, 1, ["0 OS packages scanned"])
    harness.image("a report with an empty Results array scanned nothing", dict(image_report(0, with_results=False), Results=[]), expected, 1, ["0 OS packages scanned"])
    harness.image("a report with Results but no packages scanned nothing", image_report(0), expected, 1, ["0 OS packages scanned"])
    harness.image("a report with no OS family fails", image_report(PACKAGES, family=None), expected, 1, ["no OS family"])
    harness.image("an empty expected list fails", image_report(PACKAGES), "\n\n", 1, ["expected list is empty"])
    harness.image("exactly the floor passes", image_report(FLOOR), expected, 0, ["scanned", str(FLOOR), str(PACKAGES)])
    harness.image(
        "one below the floor fails and names both counts and the floor",
        image_report(FLOOR - 1),
        expected,
        1,
        [str(FLOOR - 1), str(FLOOR), str(PACKAGES)],
    )
    harness.image("every package scanned passes with zero counts", image_report(PACKAGES), expected, 0, ["| Vulnerabilities | 0 |", "| With a fix | 0 |", "| Verdict | PASS |"])
    harness.image("a tolerance of zero demands every package", image_report(PACKAGES - 1), expected, 1, extra=["--tolerance", "0"])

    doubled = image_report(FLOOR - 1)
    doubled["Results"][0]["Packages"] = doubled["Results"][0]["Packages"] * 2
    harness.image("repeated package names count once", doubled, expected, 1, [str(FLOOR - 1)])

    padded = expected_text(names(PACKAGES) * 2) + "\n\n"
    harness.image("duplicates and blank lines in the expected list collapse", image_report(FLOOR), padded, 0, [str(PACKAGES)])

    library_only = image_report(0, with_results=False)
    library_only["Results"] = [{"Target": "app.jar", "Class": "lang-pkgs", "Type": "jar", "Packages": [{"Name": name} for name in names(PACKAGES)]}]
    harness.image("library packages do not count toward the OS floor", library_only, expected, 1, ["0 OS packages scanned"])

    two_results = image_report(FLOOR - 5)
    two_results["Results"].append({"Target": "second", "Class": "os-pkgs", "Type": "ubuntu", "Packages": [{"Name": name} for name in names(FLOOR)]})
    harness.image("os-pkgs Results are unioned by name", two_results, expected, 0)

    findings = [
        {"VulnerabilityID": "CVE-1", "PkgName": "pkg-1", "Severity": "HIGH", "FixedVersion": "1.2"},
        {"VulnerabilityID": "CVE-2", "PkgName": "pkg-2", "Severity": "high"},
        {"VulnerabilityID": "CVE-3", "PkgName": "pkg-3", "Severity": "CRITICAL", "FixedVersion": ""},
        {"VulnerabilityID": "CVE-4", "PkgName": "pkg-4", "Severity": "LOW", "FixedVersion": "2.0"},
        {"VulnerabilityID": "CVE-5", "PkgName": "pkg-5"},
        {"VulnerabilityID": "CVE-6", "PkgName": "pkg-6", "Severity": "NEGLIGIBLE"},
    ]
    harness.image(
        "severities are upper-cased and counted, and fixes are counted",
        image_report(PACKAGES, vulnerabilities=findings),
        expected,
        0,
        [
            "| Vulnerabilities, CRITICAL | 1 |",
            "| Vulnerabilities, HIGH | 2 |",
            "| Vulnerabilities, LOW | 1 |",
            "| Vulnerabilities, UNKNOWN | 1 |",
            "| Vulnerabilities, NEGLIGIBLE | 1 |",
            "| With a fix | 2 |",
        ],
    )
    harness.image("the OS family and name are in the table", image_report(PACKAGES), expected, 0, ["| OS | ubuntu 22.04 |"])


def run_config_cases(harness):
    targets = ["Dockerfile", "backup.Dockerfile", "k8s-overlays-prod.yaml", "k8s-overlays-nonprod.yaml"]
    expected = expected_text(targets)
    clean = [config_result(target, "dockerfile" if "Dockerfile" in target else "kubernetes") for target in targets]
    harness.config("every staged file scanned passes", config_report(clean), expected, 0, ["| Verdict | PASS |"])
    harness.config("an absent file fails and is named", config_report(clean[:-1]), expected, 1, ["k8s-overlays-nonprod.yaml"])
    harness.config("a report with no Results fails and names the files", dict(config_report([]), Results=None), expected, 1, ["Dockerfile"])
    harness.config("a file with no checks run fails and is named", config_report(clean[:-1] + [config_result(targets[-1], successes=0, failures=0)]), expected, 1, ["k8s-overlays-nonprod.yaml"])
    harness.config("a file with failures only still counts as scanned", config_report(clean[:-1] + [config_result(targets[-1], successes=0, failures=2, severities=["HIGH", "LOW"])]), expected, 0)
    harness.config("a file with no Misconfigurations key counts as scanned", config_report(clean), expected, 0, ["| Failures in Dockerfile | 0 |"])
    wrong_class = [dict(result) for result in clean]
    wrong_class[0]["Class"] = "secret"
    harness.config("a Result of another class does not count as the file's scan", config_report(wrong_class), expected, 1, ["Dockerfile"])
    harness.config("an empty expected list fails", config_report(clean), "\n", 1, ["expected list is empty"])

    findings = [
        config_result("Dockerfile", "dockerfile", failures=2, severities=["HIGH", "LOW"]),
        config_result("backup.Dockerfile", "dockerfile"),
        config_result("k8s-overlays-prod.yaml", failures=3, severities=["high", "MEDIUM", "CRITICAL"]),
        config_result("k8s-overlays-nonprod.yaml"),
    ]
    harness.config(
        "failures are counted by severity per Type and per Target",
        config_report(findings),
        expected,
        0,
        [
            "| Files, dockerfile | 2 |",
            "| Files, kubernetes | 2 |",
            "| Failures, dockerfile, HIGH | 1 |",
            "| Failures, dockerfile, LOW | 1 |",
            "| Failures, kubernetes, CRITICAL | 1 |",
            "| Failures, kubernetes, HIGH | 1 |",
            "| Failures, kubernetes, MEDIUM | 1 |",
            "| Failures in k8s-overlays-prod.yaml | 3 |",
        ],
    )


def run_io_cases(harness):
    good_report = harness.write(json.dumps(image_report(PACKAGES)), ".json")
    good_expected = harness.write(expected_text(names(PACKAGES)), ".txt")
    missing = os.path.join(harness.workdir, "no-such-file")
    garbage = harness.write("{not json", ".json")
    base = ["--kind", "image"]
    harness.check("a missing report is an input error", base + ["--report", missing, "--expected", good_expected], 2, ["cannot read"])
    harness.check("an unparseable report is an input error", base + ["--report", garbage, "--expected", good_expected], 2, ["not valid JSON"])
    harness.check("a missing expected list is an input error", base + ["--report", good_report, "--expected", missing], 2, ["cannot read"])
    harness.check("a report that is not an object is an input error", base + ["--report", harness.write("[]", ".json"), "--expected", good_expected], 2, ["not a JSON object"])
    harness.check("no arguments is a usage error", [], 2)
    harness.check("an unknown kind is a usage error", ["--kind", "fs", "--report", good_report, "--expected", good_expected], 2)
    harness.check("a tolerance of one is a usage error", base + ["--report", good_report, "--expected", good_expected, "--tolerance", "1"], 2)

    for label, report_obj, code in (("pass", image_report(PACKAGES), 0), ("fail", image_report(0, with_results=False), 1)):
        summary = harness.path(".md")
        report_path = harness.write(json.dumps(report_obj), ".json")
        verdict = "| Verdict | %s |" % ("PASS" if code == 0 else "FAIL")
        harness.check("the summary file gains the table on %s" % label, base + ["--report", report_path, "--expected", good_expected, "--summary", summary], code, [verdict])
        try:
            with open(summary, encoding="utf-8") as handle:
                written = handle.read()
        except OSError:
            written = ""
        if verdict not in written:
            harness.failures.append("summary file content on %s" % label)
            print("FAIL  the summary file holds the table on %s: %r" % (label, written))
        else:
            print("ok    the summary file holds the table on %s" % label)


def main(argv=None):
    parser = argparse.ArgumentParser(description="Prove the Trivy report guard fires.")
    parser.add_argument("--guard", default=DEFAULT_GUARD, help="guard script to test (default: the repository copy)")
    args = parser.parse_args(argv)
    guard = os.path.abspath(args.guard)
    if not os.path.isfile(guard):
        print("error: no guard at %s" % guard, file=sys.stderr)
        return 1
    with tempfile.TemporaryDirectory() as workdir:
        harness = Harness(guard, workdir)
        run_image_cases(harness)
        run_config_cases(harness)
        run_io_cases(harness)
    if harness.failures:
        print("%d case(s) failed: %s" % (len(harness.failures), "; ".join(harness.failures)))
        return 1
    print("all cases hold")
    return 0


if __name__ == "__main__":
    sys.exit(main())
