#!/usr/bin/env python3
"""Self-test for scripts/verify-osv-report.py: the guard fires on a scan that covered too little.

A guard that never fires looks identical to a scan that covered everything, so each case plants
an osv-scanner report, a CycloneDX SBOM, a BOOT-INF/lib listing and a Gradle tree in a temporary
directory, runs the guard as a subprocess, and asserts its exit code and fragments of its output.
Run with --guard to point the same cases at another copy of the guard: a copy with a floor
comparison or the empty-input failures removed must make this script exit 1. A copy elsewhere
needs verify-dependency-check-report.py beside it, because the guard loads parse_tree from there.
Reads nothing outside its temporary directory and the guard path. Exit codes: 0 all cases hold, 1 any fails.
"""

import argparse
import json
import math
import os
import shutil
import subprocess
import sys
import tempfile
from urllib.parse import quote

DEFAULT_GUARD = os.path.join(os.path.dirname(os.path.abspath(__file__)), "verify-osv-report.py")

SBOM_SIZE = 144
NO_JAR_MODULES = 4


def module(index):
    return ("org.t%d" % index, "lib%d" % index, "1.%d" % index)


def no_jar_modules():
    return [("org.nojar", "bom%d" % index, "1.0") for index in range(NO_JAR_MODULES)]


class Scenario:
    """The runtime modules that ship a jar and the SBOM coordinates that describe them."""

    def __init__(self, count=SBOM_SIZE):
        self.modules = [module(index) for index in range(count)]
        self.sbom = list(self.modules)

    def legacy(self, count):
        """Rename the SBOM group of the first `count` modules, so the tree no longer matches them."""
        for index in range(count):
            group, artifact, version = self.modules[index]
            self.sbom[index] = ("legacy." + group, artifact, version)
        return self


def purl(coordinate, suffix=""):
    group, artifact, version = coordinate
    return "pkg:maven/%s/%s@%s%s" % (quote(group, safe=""), quote(artifact, safe=""), version, suffix)


def sbom_json(coordinates, extra=(), nested=()):
    components = [{"bom-ref": purl(c), "type": "library", "group": c[0], "name": c[1], "version": c[2], "purl": purl(c)} for c in coordinates]
    components.extend(extra)
    if nested:
        components[0]["components"] = [{"type": "library", "name": c[1], "version": c[2], "purl": purl(c)} for c in nested]
    return {"bomFormat": "CycloneDX", "specVersion": "1.6", "components": components}


def osv_package(coordinate, ecosystem="Maven", groups=None):
    group, artifact, version = coordinate
    package = {"package": {"name": "%s:%s" % (group, artifact), "version": version, "ecosystem": ecosystem, "purl": purl(coordinate)}}
    if groups:
        package["vulnerabilities"] = [{"id": advisory_id, "aliases": []} for entry in groups for advisory_id in entry["ids"]]
        package["groups"] = groups
    return package


def osv_json(coordinates, groups_by_index=None, extra_packages=()):
    groups_by_index = groups_by_index or {}
    packages = [osv_package(c, groups=groups_by_index.get(index)) for index, c in enumerate(coordinates)]
    packages.extend(extra_packages)
    return {"results": [{"source": {"path": "bootjar.cdx.json", "type": "sbom"}, "packages": packages}]}


def group(ids, aliases=(), severity="7.5"):
    entry = {"ids": list(ids), "aliases": list(aliases) or list(ids)}
    if severity is not None:
        entry["max_severity"] = severity
    return entry


def tree_text(modules, extra_lines=()):
    """A Gradle runtimeClasspath tree in the real line format, with a resolved-version arrow, a repeat and a constraint."""
    lines = [
        "",
        "------------------------------------------------------------",
        "Root project 'kanban-board'",
        "------------------------------------------------------------",
        "",
        "runtimeClasspath - Runtime classpath of source set 'main'.",
    ]
    for index, (group_id, artifact, version) in enumerate(modules):
        if index % 5 == 0:
            lines.append("+--- %s:%s:0.1 -> %s" % (group_id, artifact, version))
        else:
            lines.append("+--- %s:%s:%s" % (group_id, artifact, version))
        if index % 7 == 0:
            lines.append("|    \\--- %s:%s:%s (*)" % (group_id, artifact, version))
    lines.append("+--- org.constraint:only:9.9 (c)")
    lines.extend(extra_lines)
    lines.append("\\--- org.last:tail:1.0")
    lines.append("")
    lines.append("(c) - A dependency constraint, not a dependency.")
    lines.append("(*) - Indicates repeated occurrences of a transitive dependency subtree.")
    lines.append("")
    return "\n".join(lines) + "\n"


def libs_text(modules):
    return "\n".join("%s-%s.jar" % (artifact, version) for _, artifact, version in modules) + "\n"


def full_inputs(scenario=None):
    """Return (osv, sbom, libs, tree) for a scan that covered everything."""
    scenario = scenario or Scenario()
    tail = ("org.last", "tail", "1.0")
    sbom_coordinates = scenario.sbom + [tail]
    return (
        osv_json(sbom_coordinates),
        sbom_json(sbom_coordinates),
        libs_text(scenario.modules + [tail]),
        tree_text(scenario.modules, extra_lines=["+--- %s:%s:%s" % no_jar for no_jar in no_jar_modules()]),
    )


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

    def run(self, args, guard=None):
        result = subprocess.run(
            [sys.executable, "-I", guard or self.guard] + args,
            cwd=self.workdir,
            capture_output=True,
            text=True,
            timeout=60,
        )
        return result.returncode, result.stdout + result.stderr

    def check(self, name, args, expect_code, fragments=(), absent=(), guard=None):
        code, output = self.run(args, guard)
        problems = []
        if code != expect_code:
            problems.append("exit %d, expected %d" % (code, expect_code))
        for fragment in fragments:
            if fragment not in output:
                problems.append("output lacks %r" % fragment)
        for fragment in absent:
            if fragment in output:
                problems.append("output holds %r" % fragment)
        if "Traceback" in output:
            problems.append("output holds a traceback")
        if problems:
            self.failures.append(name)
            print("FAIL  %s: %s" % (name, "; ".join(problems)))
            print("      output: %s" % output.strip().replace("\n", "\n              "))
        else:
            print("ok    %s" % name)

    def scan(self, name, inputs, expect_code, fragments=(), absent=(), extra=()):
        osv, sbom, libs, tree = inputs
        osv_path = self.write(osv if isinstance(osv, str) else json.dumps(osv), ".json")
        sbom_path = self.write(sbom if isinstance(sbom, str) else json.dumps(sbom), ".json")
        libs_path = self.write(libs, ".txt")
        tree_path = self.write(tree, ".txt")
        args = ["--report", osv_path, "--sbom", sbom_path, "--libs", libs_path, "--tree", tree_path] + list(extra)
        self.check(name, args, expect_code, fragments, absent)


def with_inputs(base, **replacements):
    osv, sbom, libs, tree = base
    values = {"osv": osv, "sbom": sbom, "libs": libs, "tree": tree}
    values.update(replacements)
    return values["osv"], values["sbom"], values["libs"], values["tree"]


def run_coverage_cases(harness):
    base = full_inputs()
    total = SBOM_SIZE + 1
    floor = int(math.ceil(total * 0.95))
    harness.scan(
        "full coverage passes and the table shows every count row",
        base,
        0,
        [
            "| osv-scanner packages (Maven) | %d |" % total,
            "| SBOM Maven components | %d |" % total,
            "| SBOM components without a Maven purl | 0 |",
            "| SBOM components osv-scanner resolved | %d |" % total,
            "| Floor (95%% of SBOM Maven components) | %d |" % floor,
            "| Jars in BOOT-INF/lib | %d |" % total,
            "| Runtime modules (runtimeClasspath tree) | %d |" % (total + NO_JAR_MODULES),
            "| Runtime modules with no jar of their own | %d |" % NO_JAR_MODULES,
            "| Runtime modules that ship a jar | %d |" % total,
            "| Runtime modules matched in the SBOM | %d |" % total,
            "| Floor (95%% of runtime modules that ship a jar) | %d |" % floor,
            "| Unmatched runtime modules | none |",
            "| Findings (package, advisory group) | 0 |",
            "| Unique advisory groups | 0 |",
            "| Verdict | PASS |",
        ],
    )

    harness.scan("an osv report with no results says 0 packages", with_inputs(base, osv={"results": []}), 1, ["0 packages"])
    harness.scan("an osv report with no results key says 0 packages", with_inputs(base, osv={}), 1, ["0 packages"])
    harness.scan("an osv result with no packages says 0 packages", with_inputs(base, osv={"results": [{"source": {}, "packages": []}]}), 1, ["0 packages"])
    harness.scan("an SBOM with no Maven component says so", with_inputs(base, sbom={"components": [{"type": "library", "name": "left-pad", "purl": "pkg:npm/left-pad@1.0.0"}]}), 1, ["the SBOM lists no Maven component"])
    harness.scan("an SBOM with no components says so", with_inputs(base, sbom={"components": []}), 1, ["the SBOM lists no Maven component"])

    sbom_coordinates = Scenario().sbom + [("org.last", "tail", "1.0")]
    at_floor = with_inputs(base, osv=osv_json(sbom_coordinates[:floor]))
    harness.scan("osv resolving exactly the floor passes", at_floor, 0, ["| SBOM components osv-scanner resolved | %d |" % floor])
    below = with_inputs(base, osv=osv_json(sbom_coordinates[: floor - 1]))
    harness.scan("osv resolving one fewer fails and names the count, the floor and the total", below, 1, [str(floor - 1), str(floor), str(total), "osv-scanner resolved"])

    foreign = [osv_package(c, ecosystem="PyPI") for c in sbom_coordinates[floor - 1 :]]
    harness.scan("packages of another ecosystem do not raise the resolved count", with_inputs(base, osv=osv_json(sbom_coordinates[: floor - 1], extra_packages=foreign)), 1, [str(floor - 1)])
    repeated = [osv_package(c) for c in sbom_coordinates[: floor - 1]] * 2
    harness.scan("duplicate osv packages do not raise the resolved count", with_inputs(base, osv={"results": [{"packages": repeated}]}), 1, [str(floor - 1)])

    odd = Scenario()
    odd.modules[1] = ("org.plus", "lib+1", "1.1")
    odd.sbom[1] = odd.modules[1]
    osv_odd, sbom_odd, libs_odd, tree_odd = full_inputs(odd)
    sbom_odd["components"][1]["purl"] = purl(odd.sbom[1], "?type=jar")
    sbom_odd["components"][2]["purl"] = purl(odd.sbom[2], "#sub/path")
    harness.scan("purls with percent-encoding, a qualifier or a subpath still match, even at tolerance 0", (osv_odd, sbom_odd, libs_odd, tree_odd), 0, ["| SBOM components osv-scanner resolved | %d |" % total], extra=["--tolerance", "0"])

    mixed = sbom_json(sbom_coordinates, extra=[{"type": "library", "name": "bundled", "purl": "pkg:npm/bundled@1.0.0"}, {"type": "file", "name": "no-purl"}])
    harness.scan("components without a Maven purl are counted apart and do not count as Maven", with_inputs(base, sbom=mixed), 0, ["| SBOM Maven components | %d |" % total, "| SBOM components without a Maven purl | 2 |"])
    nested = sbom_json(sbom_coordinates[:-1], nested=[("org.last", "tail", "1.0")])
    harness.scan("a Maven purl in a nested component is found", with_inputs(base, sbom=nested, osv=osv_json(sbom_coordinates)), 0, ["| SBOM Maven components | %d |" % total])


def run_runtime_cases(harness):
    total = SBOM_SIZE + 1
    floor = int(math.ceil(total * 0.95))
    allowed = total - floor
    harness.scan("the SBOM matching exactly the runtime floor passes", full_inputs(Scenario().legacy(allowed)), 0, ["| Runtime modules matched in the SBOM | %d |" % floor, "| Verdict | PASS |"])
    harness.scan(
        "one fewer match fails and names the count, the floor and an unmatched module",
        full_inputs(Scenario().legacy(allowed + 1)),
        1,
        [str(floor - 1), str(floor), str(total), "org.t0:lib0:1.0", "SBOM matched"],
    )
    many = Scenario().legacy(12)
    names = sorted("%s:%s:%s" % m for m in many.modules[:12])
    harness.scan(
        "at most 10 unmatched modules are named",
        full_inputs(many),
        1,
        names[:10],
        absent=[names[10]],
    )

    hibernate = Scenario()
    hibernate.modules[0] = ("org.hibernate.orm", "hibernate-core", "6.6.18")
    hibernate.sbom[0] = ("org.hibernate", "hibernate-core", "6.6.18")
    harness.scan(
        "a module the SBOM lists under another group is unmatched and named",
        full_inputs(hibernate),
        0,
        ["| Unmatched runtime modules | org.hibernate.orm:hibernate-core:6.6.18 |", "| Runtime modules matched in the SBOM | %d |" % (total - 1)],
    )

    base = full_inputs()
    harness.scan("a listing that names no runtime module's jar fails", with_inputs(base, libs="unrelated-1.0.jar\n"), 1, ["no runtime module"])
    harness.scan("an empty listing fails", with_inputs(base, libs="\n"), 1, ["lists no jar"])
    harness.scan("an empty tree fails", with_inputs(base, tree=""), 1, ["no parsable module line"])
    harness.scan("a tree with a FAILED line fails", with_inputs(base, tree=tree_text(Scenario().modules, extra_lines=["+--- org.bad:bad:1.0 FAILED"])), 1, ["unresolved", "org.bad:bad:1.0"])


def run_findings_cases(harness):
    scenario = Scenario()
    sbom_coordinates = scenario.sbom + [("org.last", "tail", "1.0")]
    groups = {
        3: [group(["GHSA-aaaa-1111"], ["CVE-2026-0001", "GHSA-aaaa-1111"], "9.8")],
        5: [group(["GHSA-bbbb-2222"], ["CVE-2026-0002"], "5.3"), group(["GHSA-cccc-3333"], ["CVE-2026-0003"], None)],
        7: [group(["GHSA-aaaa-1111"], ["CVE-2026-0001", "GHSA-aaaa-1111"], "9.8")],
    }
    base = full_inputs()
    with_findings = with_inputs(base, osv=osv_json(sbom_coordinates, groups))
    harness.scan(
        "findings never fail the guard and each (package, advisory group) gets a row",
        with_findings,
        0,
        [
            "| Findings (package, advisory group) | 4 |",
            "| Unique advisory groups | 3 |",
            "| Finding, org.t3:lib3 1.3 | ids GHSA-aaaa-1111; aliases CVE-2026-0001, GHSA-aaaa-1111; max severity 9.8 |",
            "| Finding, org.t5:lib5 1.5 | ids GHSA-bbbb-2222; aliases CVE-2026-0002; max severity 5.3 |",
            "| Finding, org.t5:lib5 1.5 | ids GHSA-cccc-3333; aliases CVE-2026-0003; max severity none reported |",
            "| Finding, org.t7:lib7 1.7 | ids GHSA-aaaa-1111;",
            "| Verdict | PASS |",
        ],
    )

    package = osv_package(sbom_coordinates[2])
    package["vulnerabilities"] = [{"id": "GHSA-dddd-4444", "aliases": ["CVE-2026-0004"]}]
    ungrouped = osv_json(sbom_coordinates)
    ungrouped["results"][0]["packages"][2] = package
    harness.scan(
        "a vulnerable package with no groups still yields a finding row",
        with_inputs(base, osv=ungrouped),
        0,
        ["| Findings (package, advisory group) | 1 |", "| Finding, org.t2:lib2 1.2 | ids GHSA-dddd-4444; aliases CVE-2026-0004;"],
    )

    failing = with_inputs(base, osv=osv_json(sbom_coordinates[:1], {0: [group(["GHSA-eeee-5555"])]}))
    harness.scan("a failing scan still prints its findings table", failing, 1, ["| Verdict | FAIL |", "| Finding, org.t0:lib0 1.0 |"])


def run_io_cases(harness):
    base = full_inputs()
    osv, sbom, libs, tree = base
    osv_path = harness.write(json.dumps(osv), ".json")
    sbom_path = harness.write(json.dumps(sbom), ".json")
    libs_path = harness.write(libs, ".txt")
    tree_path = harness.write(tree, ".txt")
    garbage = harness.write("{not json", ".json")
    missing = os.path.join(harness.workdir, "no-such-file")
    good = ["--report", osv_path, "--sbom", sbom_path, "--libs", libs_path, "--tree", tree_path]

    def replaced(flag, value):
        args = list(good)
        args[args.index(flag) + 1] = value
        return args

    harness.check("a missing report is an input error", replaced("--report", missing), 2, ["cannot read"])
    harness.check("an unparseable report is an input error", replaced("--report", garbage), 2, ["not valid JSON"])
    harness.check("an unparseable SBOM is an input error", replaced("--sbom", garbage), 2, ["not valid JSON"])
    harness.check("a missing listing is an input error", replaced("--libs", missing), 2, ["cannot read"])
    harness.check("a missing tree is an input error", replaced("--tree", missing), 2, ["cannot read"])
    harness.check("a report that is not an object is an input error", replaced("--report", harness.write("[]", ".json")), 2, ["not a JSON object"])
    harness.check("no arguments is a usage error", [], 2)
    harness.check("a tolerance of one is a usage error", good + ["--tolerance", "1"], 2)
    harness.check("a negative tolerance is a usage error", good + ["--tolerance", "-0.1"], 2)

    alone = os.path.join(harness.workdir, "alone")
    os.makedirs(alone)
    lonely_guard = os.path.join(alone, "verify-osv-report.py")
    shutil.copy(harness.guard, lonely_guard)
    harness.check("a guard without verify-dependency-check-report.py beside it is an input error", good, 2, ["verify-dependency-check-report.py"], guard=lonely_guard)

    for label, inputs, code in (("pass", base, 0), ("fail", with_inputs(base, osv={"results": []}), 1)):
        summary = harness.path(".md")
        osv_case = harness.write(inputs[0] if isinstance(inputs[0], str) else json.dumps(inputs[0]), ".json")
        verdict = "| Verdict | %s |" % ("PASS" if code == 0 else "FAIL")
        harness.check("the summary file gains the table on %s" % label, replaced("--report", osv_case) + ["--summary", summary], code, [verdict])
        try:
            with open(summary, encoding="utf-8") as handle:
                written = handle.read()
        except OSError:
            written = ""
        if verdict in written and "### osv-scanner coverage and findings" in written:
            print("ok    the summary file holds the heading and table on %s" % label)
        else:
            harness.failures.append("summary file content on %s" % label)
            print("FAIL  the summary file holds the heading and table on %s: %r" % (label, written))


def main(argv=None):
    parser = argparse.ArgumentParser(description="Prove the osv-scanner report guard fires.")
    parser.add_argument("--guard", default=DEFAULT_GUARD, help="guard script to test (default: the repository copy)")
    args = parser.parse_args(argv)
    guard = os.path.abspath(args.guard)
    if not os.path.isfile(guard):
        print("error: no guard at %s" % guard, file=sys.stderr)
        return 1
    with tempfile.TemporaryDirectory() as workdir:
        harness = Harness(guard, workdir)
        run_coverage_cases(harness)
        run_runtime_cases(harness)
        run_findings_cases(harness)
        run_io_cases(harness)
    if harness.failures:
        print("%d case(s) failed: %s" % (len(harness.failures), "; ".join(harness.failures)))
        return 1
    print("all cases hold")
    return 0


if __name__ == "__main__":
    sys.exit(main())
