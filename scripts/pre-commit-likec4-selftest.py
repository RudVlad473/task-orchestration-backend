#!/usr/bin/env python3
"""Self-test for .githooks/pre-commit's LikeC4 drift step: the real hook runs in scratch repos against every commit shape that touches, or avoids, the model's fact sources.

The cases cover six refusals for drift, three matching commits that pass the check, two refusals
for missing tooling and one commit of an unrelated path that must not run the step at all.

Exit codes: 0 when every case passes, 1 when any fails, 2 when the environment is unusable (no hook,
no node or git, no installed likec4, or likec4 resolvable from outside a scratch repo), so "could
not run" never reads as a verdict. --hook PATH tests another copy of the hook, which is how the
unwired and filter-mutant directions are checked.

Decisions:
Each scratch repo copies the tracked files under src/main, k8s and docs/architecture, with the later
gates (comment lint, instruction budget, gradlew, the secret scan's docker call) stubbed to succeed,
so the only gate that can refuse a commit is the one under test.
A repo built without tooling is first probed by running its own sync.mjs, which must exit 3 because
likec4 is not resolvable from any ancestor folder. Otherwise the no-tooling cases could not tell a
missing install from a present one, and the run stops as an environment problem.
"""

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_HOOK = os.path.join(REPO_ROOT, ".githooks", "pre-commit")
COMMAND_TIMEOUT_SECONDS = 120

BANNER = "Checking the LikeC4 model against the code it describes..."
DRIFT_REFUSED_TEXT = "LikeC4 model drift"
NOT_RUN_TEXT = "LikeC4 drift check did not run to completion"
NO_NODE_TEXT = "Node is not on PATH"
INSTALL_COMMAND = "pnpm --dir docs/architecture install"
STALE_TEXT = "generated.c4 is stale"

ANSI_ESCAPE = re.compile(r"\x1b\[[0-9;]*[A-Za-z]")
OK_TALLY = re.compile(r"^OK \(\d+ elements(, \d+ [a-z ]+)+\)$", re.MULTILINE)

KAFKA_TOPICS = "src/main/java/com/vrudenko/kanban_board/constant/KafkaTopics.java"
MIGRATION = "src/main/resources/db/migration/V9999__selftest.sql"
MIGRATION_SQL = "CREATE TABLE selftest_drift (id TEXT PRIMARY KEY);\n"
WORKER_MANIFEST = "k8s/base/selftest-worker.yaml"
WORKER_YAML = "apiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: selftest-worker\n"
CRONJOB = "k8s/data/postgres/backup-cronjob.yaml"
MODEL = "docs/architecture/model/model.c4"
VIEWS = "docs/architecture/model/views.c4"

GIT = shutil.which("git") or "git"
HOOK = DEFAULT_HOOK
WORKDIR = ""


class EnvironmentProblem(Exception):
    """The machine cannot run the cases; reported as exit 2, never as a failed case."""


def clean_env(extra=None):
    """A copy of the environment with every git variable removed, so a run launched from inside a hook or under user config cannot touch the real repo."""
    env = {k: v for k, v in os.environ.items() if not k.startswith("GIT_")}
    env["GIT_CONFIG_GLOBAL"] = os.devnull
    env["GIT_CONFIG_NOSYSTEM"] = "1"
    if WORKDIR:
        # Git stops climbing at this folder, so a scratch folder missing its own .git cannot reach the real repo.
        env["GIT_CEILING_DIRECTORIES"] = WORKDIR
    env.update(extra or {})
    return env


def run(cmd, cwd, extra_env=None):
    """Run cmd with stdout and stderr merged; a timeout is reported as a failure naming the command."""
    try:
        done = subprocess.run(
            cmd,
            cwd=cwd,
            env=clean_env(extra_env),
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=COMMAND_TIMEOUT_SECONDS,
        )
    except subprocess.TimeoutExpired:
        raise AssertionError("timed out after %ds: %s" % (COMMAND_TIMEOUT_SECONDS, " ".join(cmd)))
    return done.returncode, done.stdout


def git(repo, *args):
    code, out = run([GIT, *args], repo)
    assert code == 0, "git %s failed (%d): %s" % (" ".join(args), code, out)
    return out


def head_of(repo):
    return git(repo, "rev-parse", "HEAD").strip()


def strip_ansi(text):
    return ANSI_ESCAPE.sub("", text)


def tail(text):
    return strip_ansi(text)[-500:].replace("\n", " | ")


def read_bytes(repo, rel):
    with open(os.path.join(repo, rel), "rb") as handle:
        return handle.read()


def write_new(repo, rel, text):
    path = os.path.join(repo, rel)
    assert not os.path.exists(path), "%s already exists" % rel
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(text)


def replace_in(repo, rel, old, new):
    before = read_bytes(repo, rel)
    assert old.encode() in before, "%s does not contain %r" % (rel, old)
    with open(os.path.join(repo, rel), "wb") as handle:
        handle.write(before.replace(old.encode(), new.encode()))
    assert read_bytes(repo, rel) != before, "the edit left %s unchanged" % rel


def append_to(repo, rel, text):
    before = read_bytes(repo, rel)
    with open(os.path.join(repo, rel), "ab") as handle:
        handle.write(text.encode())
    assert read_bytes(repo, rel) != before, "the edit left %s unchanged" % rel


def remove_tree(path):
    """Trash the folder when trash-put exists (local runs), else delete it (CI has no trash-put)."""
    trash = shutil.which("trash-put")
    if trash:
        code, out = run([trash, path], REPO_ROOT)
        if code == 0:
            return
        print("WARNING: trash-put failed (%d), deleting instead: %s" % (code, out.strip()), file=sys.stderr)
    shutil.rmtree(path, ignore_errors=True)


def tracked_files():
    code, out = run([GIT, "ls-files", "-z", "--", "src/main", "k8s", "docs/architecture"], REPO_ROOT)
    assert code == 0, "git ls-files failed in the real repo: " + out
    return [rel for rel in out.split("\0") if rel]


def copy_tracked(repo):
    for rel in tracked_files():
        source = os.path.join(REPO_ROOT, rel)
        if not os.path.lexists(source):
            continue
        target = os.path.join(repo, rel)
        os.makedirs(os.path.dirname(target), exist_ok=True)
        if os.path.islink(source):
            os.symlink(os.readlink(source), target)
        else:
            shutil.copy2(source, target)


def write_executable(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(text)
    os.chmod(path, 0o755)


def probe_no_tooling(case_name, repo):
    """Prove likec4 cannot be resolved from the scratch repo, so a refusal there is caused by the missing install."""
    code, out = run(["node", "docs/architecture/sync.mjs", "check"], repo)
    if code != 3 or INSTALL_COMMAND not in out:
        raise EnvironmentProblem(
            "likec4 is resolvable from outside the scratch repo, so the no-tooling cases cannot discriminate "
            "(probe of %s exited %d): %s" % (case_name, code, tail(out))
        )
    print("PROBE ok %s: likec4 is not resolvable from the scratch repo" % case_name)


def make_repo(case_name, tooling):
    """A fresh repo at <case dir>/main whose commits run the hook under test; returns (case dir, repo)."""
    case_dir = os.path.join(WORKDIR, case_name)
    repo = os.path.join(case_dir, "main")
    os.makedirs(repo)
    copy_tracked(repo)

    # The hook's later gates read the real repo's tree and would refuse a scratch one. The drift step is the
    # only gate under test, so every other gate is stubbed to succeed.
    for stub in ("verify-comments.py", "verify-instruction-budget.py"):
        write_executable(os.path.join(repo, "scripts", stub), "raise SystemExit(0)\n")
    write_executable(os.path.join(repo, "gradlew"), "#!/bin/sh\nexit 0\n")
    write_new(repo, "README.txt", "scratch repo\n")

    git(repo, "init", "--quiet")
    git(repo, "config", "user.name", "Selftest")
    git(repo, "config", "user.email", "selftest@example.invalid")
    git(repo, "config", "commit.gpgsign", "false")
    toplevel = git(repo, "rev-parse", "--show-toplevel").strip()
    assert os.path.realpath(toplevel) == os.path.realpath(repo), "git resolved %s, not the scratch repo %s" % (toplevel, repo)

    # The initial commit happens before core.hooksPath is set, so setup never runs the hook.
    git(repo, "add", "--all")
    git(repo, "commit", "--quiet", "-m", "initial")

    hooks_dir = os.path.join(case_dir, "hooks")
    os.makedirs(hooks_dir)
    installed = os.path.join(hooks_dir, "pre-commit")
    shutil.copyfile(HOOK, installed)
    os.chmod(installed, 0o755)
    git(repo, "config", "core.hooksPath", hooks_dir)

    # The docker stub isolates the step under test; the secret scan has its own self-test.
    write_executable(os.path.join(case_dir, "bin", "docker"), "#!/bin/sh\nexit 0\n")

    if tooling:
        os.symlink(os.path.realpath(os.path.join(REPO_ROOT, "docs", "architecture", "node_modules")), os.path.join(repo, "docs", "architecture", "node_modules"))
        # A directory-only ignore pattern does not match a symlink.
        exclude = os.path.join(repo, ".git", "info", "exclude")
        os.makedirs(os.path.dirname(exclude), exist_ok=True)
        with open(exclude, "a", encoding="utf-8") as handle:
            handle.write("/docs/architecture/node_modules\n")
    else:
        probe_no_tooling(case_name, repo)
    return case_dir, repo


def bin_path(case_dir):
    return os.path.join(case_dir, "bin") + os.pathsep + os.environ.get("PATH", "")


def commit(case_dir, repo, *args):
    return run([GIT, "commit", "-m", "x", *args], repo, {"PATH": bin_path(case_dir)})


def refused_for_drift(code, out, repo, head_before, finding):
    plain = strip_ansi(out)
    assert code != 0, "the commit succeeded; expected a refusal. Output: " + tail(out)
    assert BANNER in plain, "the drift step did not run: " + tail(out)
    assert DRIFT_REFUSED_TEXT in plain, "the commit failed but not as drift: " + tail(out)
    assert finding in plain, "the DRIFT list lacks %r: %s" % (finding, tail(out))
    assert head_of(repo) == head_before, "HEAD moved although the commit was refused"


def refused_for_missing_tooling(code, out, repo, head_before, required, forbidden=()):
    plain = strip_ansi(out)
    assert code != 0, "the commit succeeded; expected a refusal. Output: " + tail(out)
    assert INSTALL_COMMAND in plain, "the install command was not printed: " + tail(out)
    for text in required:
        assert text in plain, "output lacks %r: %s" % (text, tail(out))
    for text in forbidden:
        assert text not in plain, "output contains %r: %s" % (text, tail(out))
    assert head_of(repo) == head_before, "HEAD moved although the commit was refused"


def committed_with_check(code, out, repo, head_before):
    plain = strip_ansi(out)
    assert code == 0, "the commit was refused (%d): %s" % (code, tail(out))
    assert BANNER in plain, "the drift step did not run: " + tail(out)
    assert OK_TALLY.search(plain), "no OK tally line from the check: " + tail(out)
    assert head_of(repo) != head_before, "HEAD did not move although the commit succeeded"


def committed_without_step(code, out, repo, head_before):
    plain = strip_ansi(out)
    assert code == 0, "the commit was refused (%d): %s" % (code, tail(out))
    assert BANNER not in plain, "the drift step ran for an unrelated path: " + tail(out)
    assert head_of(repo) != head_before, "HEAD did not move although the commit succeeded"


def add_migration(repo):
    write_new(repo, MIGRATION, MIGRATION_SQL)


def add_topic_constant(repo):
    replace_in(repo, KAFKA_TOPICS, "}\n", '    public static final String SELFTEST = "kanban.selftest";\n}\n')


def add_worker_manifest(repo):
    write_new(repo, WORKER_MANIFEST, WORKER_YAML)


def test_01_new_migration_without_regenerating_is_refused():
    case_dir, repo = make_repo("migration-stale", True)
    add_migration(repo)
    git(repo, "add", MIGRATION)
    before = head_of(repo)
    code, out = commit(case_dir, repo)
    refused_for_drift(code, out, repo, before, STALE_TEXT)


def test_02_new_topic_constant_pathspec_commit_is_refused():
    case_dir, repo = make_repo("topic-constant", True)
    add_topic_constant(repo)
    before = head_of(repo)
    code, out = commit(case_dir, repo, "--", KAFKA_TOPICS)
    refused_for_drift(code, out, repo, before, "topic kanban.selftest is in KafkaTopics.java but not in the model")


def test_03_new_deployment_commit_all_is_refused():
    case_dir, repo = make_repo("new-deployment", True)
    add_worker_manifest(repo)
    git(repo, "add", WORKER_MANIFEST)
    before = head_of(repo)
    code, out = commit(case_dir, repo, "-a")
    refused_for_drift(code, out, repo, before, "k8s Deployment/selftest-worker has no element")


def test_04_deleting_a_cited_manifest_is_refused():
    case_dir, repo = make_repo("cited-manifest-deleted", True)
    git(repo, "rm", "--quiet", CRONJOB)
    before = head_of(repo)
    code, out = commit(case_dir, repo)
    refused_for_drift(code, out, repo, before, "model claims workload CronJob/postgres-backup")


def test_05_model_only_edit_is_refused():
    case_dir, repo = make_repo("model-only-edit", True)
    replace_in(repo, MODEL, "'kanban.activity.dlt'", "'kanban.activity.deadletter'")
    git(repo, "add", MODEL)
    before = head_of(repo)
    code, out = commit(case_dir, repo)
    refused_for_drift(code, out, repo, before, "topic kanban.activity.dlt is in KafkaTopics.java but not in the model")


def test_06_migration_with_regenerated_model_passes_the_check():
    case_dir, repo = make_repo("migration-regenerated", True)
    add_migration(repo)
    code, out = run(["node", "docs/architecture/sync.mjs", "generate"], repo)
    assert code == 0, "generate failed (%d): %s" % (code, tail(out))
    git(repo, "add", MIGRATION, "docs/architecture/model/generated.c4")
    before = head_of(repo)
    code, out = commit(case_dir, repo)
    committed_with_check(code, out, repo, before)


def test_07_topic_constant_with_model_topic_passes_the_check():
    case_dir, repo = make_repo("topic-modelled", True)
    add_topic_constant(repo)
    write_new(repo, "docs/architecture/model/selftest.c4", "model {\n  extend redpanda {\n    selftest = topic 'kanban.selftest'\n  }\n}\n")
    git(repo, "add", KAFKA_TOPICS, "docs/architecture/model/selftest.c4")
    before = head_of(repo)
    code, out = commit(case_dir, repo)
    committed_with_check(code, out, repo, before)


def test_08_deployment_with_modelled_workload_passes_the_check():
    case_dir, repo = make_repo("deployment-modelled", True)
    add_worker_manifest(repo)
    write_new(
        repo,
        "docs/architecture/model/selftest.c4",
        "model {\n  selftest_worker = system 'Selftest worker' {\n    metadata {\n      workload 'Deployment/selftest-worker'\n    }\n  }\n}\n",
    )
    git(repo, "add", WORKER_MANIFEST, "docs/architecture/model/selftest.c4")
    before = head_of(repo)
    code, out = commit(case_dir, repo)
    committed_with_check(code, out, repo, before)


def test_09_stale_model_without_tooling_prints_the_install_command():
    case_dir, repo = make_repo("stale-no-tooling", False)
    add_migration(repo)
    git(repo, "add", MIGRATION)
    before = head_of(repo)
    code, out = commit(case_dir, repo)
    refused_for_missing_tooling(code, out, repo, before, [NOT_RUN_TEXT], forbidden=[STALE_TEXT])


def test_10_no_node_on_path_is_refused_with_the_install_command():
    case_dir, repo = make_repo("no-node", True)
    append_to(repo, VIEWS, "// selftest\n")
    git(repo, "add", VIEWS)
    bin_dir = os.path.join(case_dir, "bin")
    for tool in ("git", "sh", "dirname", "basename"):
        found = shutil.which(tool)
        if not found:
            raise EnvironmentProblem("%s is not on PATH" % tool)
        os.symlink(os.path.realpath(found), os.path.join(bin_dir, tool))
    if shutil.which("node", path=bin_dir) is not None:
        raise EnvironmentProblem("node is still reachable through %s" % bin_dir)
    before = head_of(repo)
    code, out = run([GIT, "commit", "-m", "x"], repo, {"PATH": bin_dir})
    refused_for_missing_tooling(code, out, repo, before, [NO_NODE_TEXT])


def test_11_unrelated_path_runs_no_drift_step():
    case_dir, repo = make_repo("unrelated-path", False)
    append_to(repo, "README.txt", "one more line\n")
    git(repo, "add", "README.txt")
    before = head_of(repo)
    code, out = commit(case_dir, repo)
    committed_without_step(code, out, repo, before)


def test_12_untagged_view_is_refused():
    case_dir, repo = make_repo("untagged-view", True)
    replace_in(repo, VIEWS, "    #process\n", "")
    git(repo, "add", VIEWS)
    before = head_of(repo)
    code, out = commit(case_dir, repo)
    refused_for_drift(code, out, repo, before, "view process carries no viewpoint tag")


def preflight(hook):
    """Return an error message when the environment cannot run the cases, else None."""
    if not os.path.isfile(hook):
        return "hook not found: %s" % hook
    for tool in ("node", "git", "sh"):
        if not shutil.which(tool):
            return "%s is not on PATH" % tool
    if not os.path.isdir(os.path.join(REPO_ROOT, "docs", "architecture", "node_modules", "likec4")):
        return "likec4 is not installed. Run: %s" % INSTALL_COMMAND
    return None


def main(argv=None):
    global HOOK, WORKDIR
    parser = argparse.ArgumentParser(description="Run the pre-commit LikeC4 drift step against every commit shape.")
    parser.add_argument("--hook", default=DEFAULT_HOOK, help="path of the hook copy to test (default: the repo's own)")
    args = parser.parse_args(argv)
    HOOK = os.path.abspath(args.hook)

    problem = preflight(HOOK)
    if problem:
        print("ENVIRONMENT: " + problem, file=sys.stderr)
        return 2

    tests = [(n, f) for n, f in sorted(globals().items()) if n.startswith("test_") and callable(f)]
    failed = 0
    environment_error = None
    out_dir = os.path.join(REPO_ROOT, "out")
    created_out = not os.path.isdir(out_dir)
    os.makedirs(out_dir, exist_ok=True)
    # The prefix holds a space on purpose, so the hook's quoting is exercised. The folder sits under the
    # repo root's ignored out/, where no ancestor holds node_modules.
    workdir = tempfile.mkdtemp(prefix="likec4-selftest ", dir=out_dir)
    try:
        WORKDIR = os.path.realpath(workdir)
        for name, func in tests:
            try:
                func()
                print("PASS: %s" % name)
            except EnvironmentProblem as err:
                environment_error = str(err)
                break
            except AssertionError as err:
                failed += 1
                print("FAIL: %s: %s" % (name, str(err).replace("\n", " | ")))
            except Exception as err:  # a crashing case must read as a failure, not a traceback that hides the rest
                failed += 1
                print("FAIL: %s: %s: %s" % (name, type(err).__name__, str(err).replace("\n", " | ")))
    finally:
        remove_tree(workdir)
        if created_out:
            try:
                # rmdir refuses a non-empty folder, so other tools' output is never removed.
                os.rmdir(out_dir)
            except OSError:
                pass

    if environment_error:
        print("ENVIRONMENT: " + environment_error, file=sys.stderr)
        return 2
    print("%d/%d passed" % (len(tests) - failed, len(tests)))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
