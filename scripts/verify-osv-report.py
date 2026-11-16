#!/usr/bin/env python3
"""Fail unless an osv-scanner report covered the SBOM it was given and the SBOM covered the runtime jars.

Reads the report written by scripts/osv-scan.sh, the CycloneDX SBOM Trivy made from the boot jar,
the BOOT-INF/lib listing and the `./gradlew dependencies --configuration runtimeClasspath` tree.
Passes when osv-scanner resolved at least 95 percent of the SBOM's Maven components and the SBOM
holds at least 95 percent of the runtime modules that ship a jar. It always prints a markdown
table with the counts and one row per finding, and appends it to --summary when given.
Exit codes: 0 pass, 1 guard failure, 2 usage or input error. Stdlib only, so CI needs no pip step.

Decisions:
  * Count-based, because the failure this guards against was a count: a dependency scan went green
    on 2026-08-19 after covering nothing. A report that lists nothing is indistinguishable from a
    clean report unless something states how many entries to expect.
  * Two denominators, each from the same run. osv-scanner is compared with the SBOM it read, and the
    SBOM with the runtime modules that ship a jar, so a scan that read a thin SBOM cannot pass on
    the strength of a complete osv report.
  * A runtime module ships a jar when artifact-version.jar is in the BOOT-INF/lib listing. Modules
    that fail that test (BOMs, POM-only modules, constraints) are counted in their own row and stay
    out of the denominator, because no SBOM could name a jar that is not in the file.
  * 95 percent is a chosen margin. On 2026-11-16 the measured parity was 144 of 144 SBOM components
    resolved, and 142 of 157 runtime modules matched by exact coordinate, so a shortfall is a signal
    to diagnose the scan, not to loosen --tolerance.
  * parse_tree is loaded from verify-dependency-check-report.py in this directory, so both guards
    count runtime modules identically. The hyphens in that filename rule out a plain import, and a
    missing file is an input error rather than a silent zero.
  * Findings never fail the guard. Whether a finding should fail a job is a policy choice for when
    a triaged baseline exists. One row is printed per package and advisory group, with the ids,
    the aliases and the highest severity osv-scanner reported.
  * A vulnerable package that carries no groups array still yields a row per vulnerability, so a
    changed report layout shows up in the table instead of hiding findings.

Known holes:
  * The counts compare identities only as far as coordinates go: a scan that resolved the right
    number of the wrong packages passes only if their coordinates are in the SBOM.
  * A module whose jar carries a classifier counts as shipping no jar, so it leaves the denominator.
  * osv-scanner cannot see advisories OSV holds only as git ranges, or JavaScript bundled inside a
    jar, so a clean report never clears dependency-check.
  * The report carries no OSV database date, so stale data does not fail this guard.
"""

import argparse
import importlib.util
import json
import math
import os
import sys
from urllib.parse import unquote

DEFAULT_TOLERANCE = 0.05
MAX_UNMATCHED_SHOWN = 10
NO_SEVERITY = "none reported"
TREE_GUARD = "verify-dependency-check-report.py"


class InputError(Exception):
    """An input that cannot be read or parsed: a usage-level problem, exit 2."""


def load_parse_tree():
    """Return parse_tree from the dependency-check guard beside this file, or raise InputError."""
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), TREE_GUARD)
    if not os.path.isfile(path):
        raise InputError("cannot find %s beside this guard: it supplies parse_tree" % TREE_GUARD)
    try:
        spec = importlib.util.spec_from_file_location("verify_dependency_check_report", path)
        loaded = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(loaded)
        return loaded.parse_tree
    except (OSError, SyntaxError, AttributeError) as err:
        raise InputError("cannot load parse_tree from %s: %s" % (path, err))


def load_json_object(path, label):
    """Return the parsed JSON object at path, or raise InputError naming why it cannot be used."""
    try:
        with open(path, encoding="utf-8") as handle:
            data = json.load(handle)
    except OSError as err:
        raise InputError("cannot read the %s %s: %s" % (label, path, err))
    except ValueError as err:
        raise InputError("the %s %s is not valid JSON: %s" % (label, path, err))
    if not isinstance(data, dict):
        raise InputError("the %s %s is not a JSON object" % (label, path))
    return data


def load_text(path, label):
    try:
        with open(path, encoding="utf-8") as handle:
            return handle.read()
    except OSError as err:
        raise InputError("cannot read the %s %s: %s" % (label, path, err))


def floor_for(expected, tolerance):
    """Return the smallest count that passes. The round() absorbs float noise."""
    return int(math.ceil(round(expected * (1 - tolerance), 6)))


def maven_coordinate(purl):
    """Return group:artifact@version for a pkg:maven purl, without qualifiers or subpath, or None."""
    if not isinstance(purl, str) or not purl.startswith("pkg:maven/"):
        return None
    body = purl[len("pkg:maven/"):].split("#", 1)[0].split("?", 1)[0]
    if "@" not in body:
        return None
    name, version = body.rsplit("@", 1)
    if "/" not in name:
        return None
    group, artifact = name.split("/", 1)
    if not group or not artifact or not version:
        return None
    return "%s:%s@%s" % (unquote(group), unquote(artifact), unquote(version))


def all_components(sbom):
    """Return every component dict in the SBOM, nested ones included."""
    found = []
    pending = list(sbom.get("components") or []) if isinstance(sbom.get("components"), list) else []
    while pending:
        component = pending.pop()
        if not isinstance(component, dict):
            continue
        found.append(component)
        nested = component.get("components")
        if isinstance(nested, list):
            pending.extend(nested)
    return found


def sbom_coordinates(sbom):
    """Return (the set of Maven coordinates, the count of components with no Maven purl)."""
    coordinates = set()
    without = 0
    for component in all_components(sbom):
        coordinate = maven_coordinate(component.get("purl"))
        if coordinate is None:
            without += 1
        else:
            coordinates.add(coordinate)
    return coordinates, without


def report_packages(report):
    """Return every package entry the report lists, in order."""
    entries = []
    results = report.get("results")
    for result in results if isinstance(results, list) else []:
        packages = result.get("packages") if isinstance(result, dict) else None
        if isinstance(packages, list):
            entries.extend(entry for entry in packages if isinstance(entry, dict))
    return entries


def package_identity(entry):
    """Return (ecosystem, name, version) of a report package entry, with empty strings for gaps."""
    package = entry.get("package")
    package = package if isinstance(package, dict) else {}
    return str(package.get("ecosystem") or ""), str(package.get("name") or ""), str(package.get("version") or "")


def osv_coordinates(entries):
    """Return the set of group:artifact@version for the Maven packages the report lists."""
    found = set()
    for entry in entries:
        ecosystem, name, version = package_identity(entry)
        if ecosystem == "Maven" and name and version:
            found.add("%s@%s" % (name, version))
    return found


def severity_text(value):
    return str(value) if value else NO_SEVERITY


def findings_of(entries):
    """Return sorted (name, version, ids, aliases, severity) tuples, one per package and advisory group."""
    seen = {}
    for entry in entries:
        _, name, version = package_identity(entry)
        groups = [item for item in entry.get("groups") or [] if isinstance(item, dict)]
        if groups:
            candidates = [(g.get("ids"), g.get("aliases"), g.get("max_severity")) for g in groups]
        else:
            candidates = [([v.get("id")], v.get("aliases"), None) for v in entry.get("vulnerabilities") or [] if isinstance(v, dict)]
        for ids, aliases, severity in candidates:
            ids = tuple(sorted(str(item) for item in ids or []))
            aliases = tuple(sorted({str(item) for item in aliases or []}))
            seen[(name, version, ids)] = (name, version, ids, aliases, severity_text(severity))
    return [seen[key] for key in sorted(seen)]


def read_listing(text):
    """Return the unique non-blank lines of the BOOT-INF/lib listing."""
    return {line.strip() for line in text.splitlines() if line.strip()}


def module_name(module):
    return "%s:%s:%s" % module


def empty_input_failures(osv_set, sbom_set, modules, listing, shipping):
    """Return the failures for an input that is empty, where no floor would catch it."""
    failures = []
    if not osv_set:
        failures.append("osv-scanner reported 0 packages (Maven): the scan read nothing from the SBOM")
    if not sbom_set:
        failures.append("the SBOM lists no Maven component: without a denominator the guard cannot tell a scan of the jar from a scan of nothing")
    if not modules:
        failures.append("the runtime tree has no parsable module line: without a denominator the guard cannot tell a scan of the classpath from a scan of nothing")
    if not listing:
        failures.append("the BOOT-INF/lib listing lists no jar")
    if modules and listing and not shipping:
        failures.append("the BOOT-INF/lib listing names no runtime module's jar, so no module ships a jar to match")
    return failures


def evaluate(report, sbom, listing, tree_text, tolerance, parse_tree):
    """Return (ok, failures, rows): the verdict, why it failed, and the table rows."""
    entries = report_packages(report)
    osv_set = osv_coordinates(entries)
    sbom_set, without_purl = sbom_coordinates(sbom)
    modules, failed = parse_tree(tree_text)
    shipping = sorted(m for m in modules if "%s-%s.jar" % (m[1], m[2]) in listing)
    matched = [m for m in shipping if "%s:%s@%s" % m in sbom_set]
    unmatched = sorted(module_name(m) for m in shipping if "%s:%s@%s" % m not in sbom_set)
    resolved = len(sbom_set & osv_set)
    sbom_floor = floor_for(len(sbom_set), tolerance)
    runtime_floor = floor_for(len(shipping), tolerance)
    findings = findings_of(entries)
    unique_groups = {ids for _, _, ids, _, _ in findings}

    failures = []
    failures.extend(empty_input_failures(osv_set, sbom_set, modules, listing, shipping))
    if failed:
        failures.append("the runtime tree has %d unresolved line(s), e.g. %s: the denominator cannot be trusted" % (len(failed), failed[0]))
    if resolved < sbom_floor:
        failures.append("osv-scanner resolved %d of %d SBOM Maven components, below the floor of %d (%.0f%%)" % (resolved, len(sbom_set), sbom_floor, (1 - tolerance) * 100))
    if len(matched) < runtime_floor:
        failures.append(
            "the SBOM matched %d of %d runtime modules that ship a jar, below the floor of %d (%.0f%%); unmatched: %s"
            % (len(matched), len(shipping), runtime_floor, (1 - tolerance) * 100, unmatched_text(unmatched))
        )

    percent = (1 - tolerance) * 100
    rows = [
        ("osv-scanner packages (Maven)", str(len(osv_set))),
        ("SBOM Maven components", str(len(sbom_set))),
        ("SBOM components without a Maven purl", str(without_purl)),
        ("SBOM components osv-scanner resolved", str(resolved)),
        ("Floor (%.0f%% of SBOM Maven components)" % percent, str(sbom_floor)),
        ("Jars in BOOT-INF/lib", str(len(listing))),
        ("Runtime modules (runtimeClasspath tree)", str(len(modules))),
        ("Runtime modules with no jar of their own", str(len(modules) - len(shipping))),
        ("Runtime modules that ship a jar", str(len(shipping))),
        ("Runtime modules matched in the SBOM", str(len(matched))),
        ("Floor (%.0f%% of runtime modules that ship a jar)" % percent, str(runtime_floor)),
        ("Unmatched runtime modules", unmatched_text(unmatched)),
        ("Findings (package, advisory group)", str(len(findings))),
        ("Unique advisory groups", str(len(unique_groups))),
    ]
    for name, version, ids, aliases, severity in findings:
        value = "ids %s; aliases %s; max severity %s" % (", ".join(ids) or "none", ", ".join(aliases) or "none", severity)
        rows.append(("Finding, %s %s" % (name, version), value))
    return not failures, failures, rows


def unmatched_text(names):
    if not names:
        return "none"
    shown = ", ".join(names[:MAX_UNMATCHED_SHOWN])
    extra = len(names) - MAX_UNMATCHED_SHOWN
    return shown + (" (+%d more)" % extra if extra > 0 else "")


def render_table(rows):
    lines = ["| Check | Value |", "| --- | --- |"]
    lines.extend("| %s | %s |" % row for row in rows)
    return "\n".join(lines) + "\n"


def main(argv=None):
    parser = argparse.ArgumentParser(description="Fail unless the osv-scanner report covered the SBOM and the SBOM covered the runtime jars.")
    parser.add_argument("--report", required=True, help="osv-scanner JSON report")
    parser.add_argument("--sbom", required=True, help="CycloneDX JSON SBOM of the boot jar")
    parser.add_argument("--libs", required=True, help="text file listing the BOOT-INF/lib jar names")
    parser.add_argument("--tree", required=True, help="gradle dependencies --configuration runtimeClasspath output")
    parser.add_argument("--tolerance", type=float, default=DEFAULT_TOLERANCE, help="allowed shortfall for both checks, 0 to below 1")
    parser.add_argument("--summary", default=None, help="file to append the markdown table to")
    try:
        args = parser.parse_args(argv)
    except SystemExit as exit_request:
        return 2 if exit_request.code else 0
    if not 0 <= args.tolerance < 1:
        print("usage error: --tolerance %s is outside 0 to below 1." % args.tolerance, file=sys.stderr)
        return 2
    try:
        parse_tree = load_parse_tree()
        report = load_json_object(args.report, "report")
        sbom = load_json_object(args.sbom, "SBOM")
        listing = read_listing(load_text(args.libs, "BOOT-INF/lib listing"))
        tree_text = load_text(args.tree, "runtime tree")
    except InputError as err:
        print("error: %s" % err, file=sys.stderr)
        return 2
    ok, failures, rows = evaluate(report, sbom, listing, tree_text, args.tolerance, parse_tree)
    rows.append(("Verdict", "PASS" if ok else "FAIL"))
    table = render_table(rows)
    print(table, end="")
    for failure in failures:
        print("FAIL: %s" % failure)
    if args.summary:
        try:
            with open(args.summary, "a", encoding="utf-8") as handle:
                handle.write("### osv-scanner coverage and findings\n\n%s\n" % table)
        except OSError as err:
            print("error: cannot append to %s: %s" % (args.summary, err), file=sys.stderr)
            return 2
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
