#!/usr/bin/env python3
"""Self-test for scripts/verify-dependency-check-report.py: the guard fires on a report that scanned too little.

A guard that never fires looks identical to a scan that covered the classpath, so each case plants
one report and one module tree in a temporary directory, runs the guard as a subprocess, and asserts
its exit code and a fragment of its message. Run with --guard to point the same cases at another copy
of the guard: a copy with its floor comparison removed must make this script exit 1.
Reads nothing outside its temporary directory and the guard path. Exit codes: 0 all cases hold, 1 any fails.
"""

import argparse
import json
import math
import os
import subprocess
import sys
import tempfile

DEFAULT_GUARD = os.path.join(os.path.dirname(os.path.abspath(__file__)), "verify-dependency-check-report.py")

MODULES = 160
FLOOR = int(math.ceil(MODULES * 0.95))

MIXED_TREE = """\
runtimeClasspath - Runtime classpath of source set 'main'.
+--- org.a:one:1.0
|    \\--- org.a:x:1.0 -> 2.0
+--- org.a:two -> 2.0
|    \\--- org.a:three:1.0 -> 3.0
+--- org.a:x:2.0
+--- org.a:one:1.0 (*)
+--- org.a:four:1.0 (c)
+--- org.a:five:1.0 (n)
\\--- project :sub

(c) - A dependency constraint, not a dependency. The dependency affected by the constraint occurs elsewhere in the tree.
(*) - Indicates repeated occurrences of a transitive dependency subtree. Gradle expands transitive dependency subtrees only once per project; repeat occurrences only display the root of the subtree, followed by this indicator.
"""
MIXED_MODULES = 4


def tree_text(count):
    lines = ["runtimeClasspath - Runtime classpath of source set 'main'."]
    for index in range(count):
        lines.append("+--- org.example:module-%d:1.%d" % (index, index))
    lines.append("")
    return "\n".join(lines)


def report(count, virtual=False, vulnerabilities=None):
    entry = {"fileName": "module.jar", "isVirtual": virtual}
    if vulnerabilities:
        entry["vulnerabilities"] = vulnerabilities
    return {
        "reportSchema": "1.1",
        "scanInfo": {"engineVersion": "12.2.2", "dataSource": []},
        "projectInfo": {"name": "root project 'kanban-board'"},
        "dependencies": [dict(entry, fileName="module-%d.jar" % index) for index in range(count)],
    }


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

    def case(self, name, report_obj, tree, expect_code, fragments=(), extra=()):
        report_path = self.write(json.dumps(report_obj), ".json")
        tree_path = self.write(tree, ".txt")
        self.check(name, ["--report", report_path, "--tree", tree_path] + list(extra), expect_code, fragments)


def run_cases(harness):
    harness.case("zero-dependency report fails and names the count", report(0), tree_text(MODULES), 1, ["0 dependencies scanned"])
    harness.case("exactly the floor passes", report(FLOOR), tree_text(MODULES), 0)
    harness.case(
        "one below the floor fails and names both counts and the floor",
        report(FLOOR - 1),
        tree_text(MODULES),
        1,
        [str(FLOOR - 1), str(FLOOR), str(MODULES)],
    )
    harness.case("every module scanned passes", report(MODULES), tree_text(MODULES), 0)
    harness.case("virtual entries do not count toward the floor", report(MODULES, virtual=True), tree_text(MODULES), 1)
    harness.case("a tree with no module line fails", report(MODULES), "runtimeClasspath - Runtime classpath of source set 'main'.\nNo dependencies\n", 1, ["no parsable module line"])
    harness.case("an empty tree fails", report(MODULES), "", 1, ["no parsable module line"])
    harness.case("a FAILED resolution line fails", report(MODULES), tree_text(MODULES) + "+--- org.example:gone:1.0 FAILED\n", 1, ["unresolved"])
    harness.case("a tolerance of zero demands every module", report(MODULES - 1), tree_text(MODULES), 1, extra=["--tolerance", "0"])
    harness.case("two-segment arrow lines, dedupe and skipped lines give four modules: four passes", report(MIXED_MODULES), MIXED_TREE, 0)
    harness.case("two-segment arrow lines, dedupe and skipped lines give four modules: three fails", report(MIXED_MODULES - 1), MIXED_TREE, 1)

    tree_path = harness.write(tree_text(MODULES), ".txt")
    missing = os.path.join(harness.workdir, "no-such-report.json")
    harness.check("a missing report fails without a traceback", ["--report", missing, "--tree", tree_path], 1, ["cannot read the report"])
    garbage = harness.write("{not json", ".json")
    harness.check("an unparseable report fails without a traceback", ["--report", garbage, "--tree", tree_path], 1, ["not valid JSON"])
    no_array = harness.write(json.dumps({"scanInfo": {}}), ".json")
    harness.check("a report with no dependencies array fails", ["--report", no_array, "--tree", tree_path], 1, ["no top-level dependencies array"])
    good = harness.write(json.dumps(report(MODULES)), ".json")
    harness.check("a missing tree fails without a traceback", ["--report", good, "--tree", os.path.join(harness.workdir, "no-tree.txt")], 1, ["cannot read the runtime tree"])
    harness.check("no arguments is a usage error", [], 2)
    harness.check("a tolerance of one is a usage error", ["--report", good, "--tree", tree_path, "--tolerance", "1"], 2)

    severities = [{"severity": "high"}, {"severity": "High"}, {"severity": "CRITICAL"}, {}]
    harness.case(
        "severities are upper-cased and counted",
        report(MODULES, vulnerabilities=severities),
        tree_text(MODULES),
        0,
        ["| Vulnerabilities, CRITICAL | %d |" % MODULES, "| Vulnerabilities, HIGH | %d |" % (2 * MODULES), "| Vulnerabilities, UNKNOWN | %d |" % MODULES],
    )

    summary = harness.path(".md")
    report_path = harness.write(json.dumps(report(MODULES)), ".json")
    harness.check("the summary file gains the table", ["--report", report_path, "--tree", tree_path, "--summary", summary], 0, ["| Verdict | PASS |"])
    try:
        with open(summary, encoding="utf-8") as handle:
            written = handle.read()
    except OSError:
        written = ""
    if "| Verdict | PASS |" not in written:
        harness.failures.append("summary file content")
        print("FAIL  the summary file holds the table: %r" % written)
    else:
        print("ok    the summary file holds the table")


def main(argv=None):
    parser = argparse.ArgumentParser(description="Prove the dependency-check report guard fires.")
    parser.add_argument("--guard", default=DEFAULT_GUARD, help="guard script to test (default: the repository copy)")
    args = parser.parse_args(argv)
    guard = os.path.abspath(args.guard)
    if not os.path.isfile(guard):
        print("error: no guard at %s" % guard, file=sys.stderr)
        return 1
    with tempfile.TemporaryDirectory() as workdir:
        harness = Harness(guard, workdir)
        run_cases(harness)
    if harness.failures:
        print("%d case(s) failed: %s" % (len(harness.failures), "; ".join(harness.failures)))
        return 1
    print("all cases hold")
    return 0


if __name__ == "__main__":
    sys.exit(main())
