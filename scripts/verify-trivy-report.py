#!/usr/bin/env python3
"""Fail unless a Trivy JSON report covered the image packages or the staged files it was meant to cover.

Reads the report written by scripts/trivy-scan.sh and a text file listing what the scan was given:
dpkg package names for --kind image, staged file names for --kind config. Image passes when the
report lists at least 95 percent as many unique OS packages as the list holds; config passes when
every listed file is the Target of a config Result with at least one check run. It always prints a
markdown table and appends it to --summary when given, on pass and on fail.
Exit codes: 0 pass, 1 guard failure, 2 usage or input error. Stdlib only, so CI needs no pip step.

Decisions:
  * Count-based, because the failure this guards against was a count: a dependency scan went green
    on 2026-08-19 after covering nothing. A report that lists nothing is indistinguishable from a
    clean report unless something states how many entries to expect.
  * The denominator comes from the same run (dpkg inside the scanned image, the files staged for
    the scan), never a fixed number, so a changed base image or a new manifest moves the
    expectation with it and nobody edits a number.
  * 95 percent is a chosen margin. The measured parity on the eclipse-temurin:21-jre-jammy base
    image was 143 of 143 on 2026-11-09, so a shortfall is a signal to diagnose the scan, not to
    loosen --tolerance. The tolerance applies to the image kind only: a staged file is either in
    the report or not.
  * A config file counts as scanned when its Result has Successes plus Failures above zero. A
    clean file has no Misconfigurations key and still appears with Failures 0, so absence of the
    key means no failures, not no scan.
  * Severity names are upper-cased and otherwise reported as the scanner wrote them, and a missing
    severity is UNKNOWN, so a new severity word shows up in the table instead of being folded in.
  * Findings never fail the guard. Whether a finding should fail a job is a policy choice for
    when a triaged baseline exists.

Known holes:
  * Counts are compared, not identities: a scan of the right number of the wrong packages passes.
  * The report carries no database date, so a stale vulnerability database does not fail this guard.
  * Findings removed by an ignore file (.trivyignore.yaml for config, .trivyignore for image) are
    absent from the report and from the table.
  * Library packages are out of scope by the scan's own flag, so this guard never counts them.
  * An image without dpkg gives an empty expected list and fails loudly, so a non-Debian base
    needs a new denominator source.
"""

import argparse
import json
import math
import sys

DEFAULT_TOLERANCE = 0.05
SEVERITY_ORDER = ("CRITICAL", "HIGH", "MEDIUM", "LOW", "UNKNOWN")


class InputError(Exception):
    """An input that cannot be read or parsed: a usage-level problem, exit 2."""


def load_report(path):
    """Return the parsed report as a dict, or raise InputError naming why it cannot be used."""
    try:
        with open(path, encoding="utf-8") as handle:
            data = json.load(handle)
    except OSError as err:
        raise InputError("cannot read the report %s: %s" % (path, err))
    except ValueError as err:
        raise InputError("the report %s is not valid JSON: %s" % (path, err))
    if not isinstance(data, dict):
        raise InputError("the report %s is not a JSON object" % path)
    return data


def load_expected(path):
    """Return the unique non-blank lines of the expected-names file, in sorted order."""
    try:
        with open(path, encoding="utf-8") as handle:
            lines = handle.read().splitlines()
    except OSError as err:
        raise InputError("cannot read the expected list %s: %s" % (path, err))
    return sorted({line.strip() for line in lines if line.strip()})


def results_of(report):
    results = report.get("Results")
    return [result for result in results if isinstance(result, dict)] if isinstance(results, list) else []


def severity_of(entry):
    severity = entry.get("Severity")
    return str(severity).upper() if severity else "UNKNOWN"


def ordered_severities(severities):
    known = [name for name in SEVERITY_ORDER if name in severities]
    rest = sorted(name for name in severities if name not in SEVERITY_ORDER)
    return known + rest


def floor_for(expected, tolerance):
    """Return the smallest scanned count that passes. The round() absorbs float noise."""
    return int(math.ceil(round(expected * (1 - tolerance), 6)))


def os_family(report):
    metadata = report.get("Metadata")
    operating_system = metadata.get("OS") if isinstance(metadata, dict) else None
    if not isinstance(operating_system, dict) or not operating_system.get("Family"):
        return None, None
    return str(operating_system["Family"]), str(operating_system.get("Name", ""))


def evaluate_image(report, expected, tolerance):
    """Return (ok, failures, rows) for an image report against the dpkg package list."""
    scanned = set()
    severities = {}
    fixed = 0
    for result in results_of(report):
        if result.get("Class") != "os-pkgs":
            continue
        for package in result.get("Packages") or []:
            if isinstance(package, dict) and package.get("Name"):
                scanned.add(str(package["Name"]))
        for vulnerability in result.get("Vulnerabilities") or []:
            if not isinstance(vulnerability, dict):
                continue
            severity = severity_of(vulnerability)
            severities[severity] = severities.get(severity, 0) + 1
            if vulnerability.get("FixedVersion"):
                fixed += 1
    family, name = os_family(report)
    floor = floor_for(len(expected), tolerance)
    failures = []
    if family is None:
        failures.append("the report names no OS family (Metadata.OS.Family): the scan did not recognise the image as an operating system")
    if not expected:
        failures.append("the expected list is empty: without a denominator the guard cannot tell a scan of the image from a scan of nothing")
    if not scanned:
        failures.append("0 OS packages scanned: the report lists no package at all, so the scan stopped before analysis or the image held nothing it understood")
    elif expected and len(scanned) < floor:
        failures.append(
            "%d OS packages scanned against a floor of %d (%.0f%% of %d expected)"
            % (len(scanned), floor, (1 - tolerance) * 100, len(expected))
        )
    rows = [
        ("OS", ("%s %s" % (family, name)).strip() if family else "none reported"),
        ("Expected OS packages (dpkg in the image)", str(len(expected))),
        ("OS packages scanned", str(len(scanned))),
        ("Floor (%.0f%% of expected)" % ((1 - tolerance) * 100), str(floor)),
    ]
    for severity in ordered_severities(severities):
        rows.append(("Vulnerabilities, %s" % severity, str(severities[severity])))
    if not severities:
        rows.append(("Vulnerabilities", "0"))
    rows.append(("With a fix", str(fixed)))
    return not failures, failures, rows


def evaluate_config(report, expected):
    """Return (ok, failures, rows) for a config report against the staged file names."""
    by_target = {}
    for result in results_of(report):
        if result.get("Class") == "config" and result.get("Target"):
            by_target[str(result["Target"])] = result
    failures = []
    if not expected:
        failures.append("the expected list is empty: without a denominator the guard cannot tell a scan of the files from a scan of nothing")
    missing = [name for name in expected if name not in by_target]
    for name in missing:
        failures.append("not scanned: %s has no config Result in the report" % name)
    for name in expected:
        result = by_target.get(name)
        if result is None:
            continue
        summary = result.get("MisconfSummary")
        summary = summary if isinstance(summary, dict) else {}
        if int(summary.get("Successes") or 0) + int(summary.get("Failures") or 0) == 0:
            failures.append("no checks ran on %s: its Successes and Failures are both zero" % name)
    files_by_type = {}
    failures_by_type = {}
    per_target = []
    for name in expected:
        result = by_target.get(name)
        if result is None:
            continue
        kind = str(result.get("Type") or "unknown")
        files_by_type[kind] = files_by_type.get(kind, 0) + 1
        findings = [entry for entry in result.get("Misconfigurations") or [] if isinstance(entry, dict)]
        per_target.append((name, len(findings)))
        for entry in findings:
            counts = failures_by_type.setdefault(kind, {})
            severity = severity_of(entry)
            counts[severity] = counts.get(severity, 0) + 1
    rows = [
        ("Expected files (staged for the scan)", str(len(expected))),
        ("Files scanned", str(len(expected) - len(missing))),
    ]
    for kind in sorted(files_by_type):
        rows.append(("Files, %s" % kind, str(files_by_type[kind])))
    for kind in sorted(failures_by_type):
        for severity in ordered_severities(failures_by_type[kind]):
            rows.append(("Failures, %s, %s" % (kind, severity), str(failures_by_type[kind][severity])))
    for name, count in per_target:
        rows.append(("Failures in %s" % name, str(count)))
    return not failures, failures, rows


def render_table(rows):
    lines = ["| Check | Value |", "| --- | --- |"]
    lines.extend("| %s | %s |" % row for row in rows)
    return "\n".join(lines) + "\n"


def main(argv=None):
    parser = argparse.ArgumentParser(description="Fail unless the Trivy report covered what it was meant to cover.")
    parser.add_argument("--kind", required=True, choices=("image", "config"), help="which report shape to expect")
    parser.add_argument("--report", required=True, help="Trivy JSON report")
    parser.add_argument("--expected", required=True, help="text file, one expected name per line")
    parser.add_argument("--tolerance", type=float, default=DEFAULT_TOLERANCE, help="allowed shortfall for the image kind, 0 to below 1")
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
        expected = load_expected(args.expected)
    except InputError as err:
        print("error: %s" % err, file=sys.stderr)
        return 2
    if args.kind == "image":
        ok, failures, rows = evaluate_image(report, expected, args.tolerance)
    else:
        ok, failures, rows = evaluate_config(report, expected)
    rows.append(("Verdict", "PASS" if ok else "FAIL"))
    table = render_table(rows)
    print(table, end="")
    for failure in failures:
        print("FAIL: %s" % failure)
    if args.summary:
        try:
            with open(args.summary, "a", encoding="utf-8") as handle:
                handle.write("### Trivy %s scan coverage\n\n%s\n" % (args.kind, table))
        except OSError as err:
            print("error: cannot append to %s: %s" % (args.summary, err), file=sys.stderr)
            return 2
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
