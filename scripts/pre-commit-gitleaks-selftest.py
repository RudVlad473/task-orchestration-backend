#!/usr/bin/env python3
"""Self-test for .githooks/pre-commit's secret scan: the real hook runs in throwaway repos against every commit shape that records its own index.

A scan of the wrong index prints "no leaks found" while reading about 0 bytes, so a clean verdict
alone proves nothing and the clean case also asserts bytes scanned above 0. Needs Docker and the
gitleaks image the hook pins, whose reference is read from the hook itself.

Exit codes: 0 when every case passes, 1 when any fails, 2 when the environment is unusable (no hook,
Docker or image), so "could not run" never reads as a verdict. --hook PATH tests another copy of the
hook, which is how the pre-fix direction is checked.
"""

import argparse
import os
import re
import secrets
import shutil
import string
import subprocess
import sys
import tempfile

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_HOOK = os.path.join(REPO_ROOT, ".githooks", "pre-commit")
COMMAND_TIMEOUT_SECONDS = 300

REFUSED_TEXT = "Secret scan found a likely credential"
NOT_RUN_TEXT = "Secret scan did not run to completion"
CLEAN_TEXT = "No secrets found in staged diff."

ANSI_ESCAPE = re.compile(r"\x1b\[[0-9;]*[A-Za-z]")
SCANNED_BYTES = re.compile(r"scanned ~(\d+) bytes")
IMAGE_LINE = re.compile(r"^GITLEAKS_IMAGE='([^']+)'$", re.MULTILINE)

HOOK = DEFAULT_HOOK
WORKDIR = ""


def clean_env(extra=None):
    """A copy of the environment with every git variable removed, so a run launched from inside a hook or under user config cannot touch the real repo."""
    env = {k: v for k, v in os.environ.items() if not k.startswith("GIT_")}
    env["GIT_CONFIG_GLOBAL"] = os.devnull
    env["GIT_CONFIG_NOSYSTEM"] = "1"
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


def git(repo, *args, extra_env=None):
    code, out = run(["git", *args], repo, extra_env)
    assert code == 0, "git %s failed (%d): %s" % (" ".join(args), code, out)
    return out


def head_of(repo):
    return git(repo, "rev-parse", "HEAD").strip()


def canary():
    alphabet = string.ascii_letters + string.digits
    return "ghp_" + "".join(secrets.choice(alphabet) for _ in range(36))


def append_line(repo, name, line):
    with open(os.path.join(repo, name), "a", encoding="utf-8") as handle:
        handle.write(line + "\n")


def append_canary(repo):
    append_line(repo, "leak.txt", "token=" + canary())


def strip_ansi(text):
    return ANSI_ESCAPE.sub("", text)


def scanned_bytes(output):
    found = SCANNED_BYTES.search(strip_ansi(output))
    return int(found.group(1)) if found else None


def make_repo(case_name):
    """A fresh repo at <case dir>/main whose commits run the hook under test; returns (case dir, repo)."""
    case_dir = os.path.join(WORKDIR, case_name)
    repo = os.path.join(case_dir, "main")
    os.makedirs(repo)
    git(repo, "init", "--quiet")
    git(repo, "config", "user.name", "Selftest")
    git(repo, "config", "user.email", "selftest@example.invalid")
    git(repo, "config", "commit.gpgsign", "false")

    # The hook's later gates read the real repo's tree and would refuse a throwaway one. The secret scan
    # runs first and is the only gate under test, so every later gate is stubbed to succeed.
    os.makedirs(os.path.join(repo, "scripts"))
    for stub in ("verify-comments.py", "verify-instruction-budget.py"):
        with open(os.path.join(repo, "scripts", stub), "w", encoding="utf-8") as handle:
            handle.write("raise SystemExit(0)\n")
    gradlew = os.path.join(repo, "gradlew")
    with open(gradlew, "w", encoding="utf-8") as handle:
        handle.write("#!/bin/sh\nexit 0\n")
    os.chmod(gradlew, 0o755)
    append_line(repo, "clean.txt", "a plain line")
    append_line(repo, "leak.txt", "another plain line")

    # The initial commit happens before core.hooksPath is set, so setup never runs the hook.
    git(repo, "add", "--all")
    git(repo, "commit", "--quiet", "-m", "initial")

    hooks_dir = os.path.join(case_dir, "hooks")
    os.makedirs(hooks_dir)
    installed = os.path.join(hooks_dir, "pre-commit")
    shutil.copyfile(HOOK, installed)
    os.chmod(installed, 0o755)
    git(repo, "config", "core.hooksPath", hooks_dir)
    return case_dir, repo


def refused(code, out, repo, head_before):
    if NOT_RUN_TEXT in out:
        raise AssertionError("the scanner did not run (environment problem, not the bug): " + strip_ansi(out)[-400:])
    assert code != 0, "the commit succeeded; expected a refusal. Output: " + strip_ansi(out)[-400:]
    assert REFUSED_TEXT in out, "the commit failed but not at the secret scan: " + strip_ansi(out)[-400:]
    assert head_of(repo) == head_before, "HEAD moved although the commit was refused"


def committed_clean(code, out, repo, head_before):
    assert code == 0, "the clean commit was refused (%d): %s" % (code, strip_ansi(out)[-400:])
    assert CLEAN_TEXT in out, "no clean verdict in the hook output: " + strip_ansi(out)[-400:]
    size = scanned_bytes(out)
    assert size is not None and size > 0, "the scan read %s bytes, a false clean" % size
    assert head_of(repo) != head_before, "HEAD did not move although the commit succeeded"


def test_staged_leak_plain_commit_is_refused():
    _, repo = make_repo("staged-plain")
    append_canary(repo)
    git(repo, "add", "leak.txt")
    before = head_of(repo)
    code, out = run(["git", "commit", "-m", "x"], repo)
    refused(code, out, repo, before)


def test_unstaged_leak_pathspec_commit_is_refused():
    _, repo = make_repo("unstaged-pathspec")
    append_canary(repo)
    before = head_of(repo)
    code, out = run(["git", "commit", "-m", "x", "--", "leak.txt"], repo)
    refused(code, out, repo, before)


def test_unstaged_clean_pathspec_commit_succeeds_and_scans_bytes():
    _, repo = make_repo("clean-pathspec")
    append_line(repo, "clean.txt", "one more plain line")
    before = head_of(repo)
    code, out = run(["git", "commit", "-m", "x", "--", "clean.txt"], repo)
    committed_clean(code, out, repo, before)


def test_unstaged_leak_commit_all_is_refused():
    _, repo = make_repo("commit-all")
    append_canary(repo)
    before = head_of(repo)
    code, out = run(["git", "commit", "-a", "-m", "x"], repo)
    refused(code, out, repo, before)


def test_nested_worktree_pathspec_leak_is_refused():
    _, repo = make_repo("nested-worktree")
    nested = os.path.join(repo, ".claude", "worktrees", "wt")
    git(repo, "worktree", "add", "--quiet", nested)
    append_canary(nested)
    before = head_of(nested)
    code, out = run(["git", "commit", "-m", "x", "--", "leak.txt"], nested)
    refused(code, out, nested, before)


def test_out_of_tree_worktree_pathspec_leak_is_refused():
    case_dir, repo = make_repo("out-of-tree-worktree")
    outside = os.path.join(case_dir, "oot")
    git(repo, "worktree", "add", "--quiet", outside)
    append_canary(outside)
    before = head_of(outside)
    code, out = run(["git", "commit", "-m", "x", "--", "leak.txt"], outside)
    refused(code, out, outside, before)


def test_index_outside_repo_leak_is_refused():
    case_dir, repo = make_repo("index-outside-repo")
    outside_index = os.path.join(case_dir, "outside-index")
    shutil.copyfile(os.path.join(repo, ".git", "index"), outside_index)
    append_canary(repo)
    env = {"GIT_INDEX_FILE": outside_index}
    git(repo, "add", "leak.txt", extra_env=env)
    before = head_of(repo)
    code, out = run(["git", "commit", "-m", "x"], repo, env)
    refused(code, out, repo, before)


def preflight(hook):
    """Return an error message when the environment cannot run the cases, else None."""
    if not os.path.isfile(hook):
        return "hook not found: %s" % hook
    with open(hook, encoding="utf-8") as handle:
        found = IMAGE_LINE.search(handle.read())
    if not found:
        return "no GITLEAKS_IMAGE='...' line in %s" % hook
    image = found.group(1)
    try:
        if run(["docker", "info"], REPO_ROOT)[0] != 0:
            return "docker info failed; is the Docker daemon running?"
        if run(["docker", "image", "inspect", image], REPO_ROOT)[0] != 0 and run(["docker", "pull", image], REPO_ROOT)[0] != 0:
            return "image %s is neither present nor pullable" % image
    except (AssertionError, FileNotFoundError) as err:
        return "docker is unusable: %s" % err
    return None


def main(argv=None):
    global HOOK, WORKDIR
    parser = argparse.ArgumentParser(description="Run the pre-commit secret scan against every commit shape.")
    parser.add_argument("--hook", default=DEFAULT_HOOK, help="path of the hook copy to test (default: the repo's own)")
    args = parser.parse_args(argv)
    HOOK = os.path.abspath(args.hook)

    problem = preflight(HOOK)
    if problem:
        print("ENVIRONMENT: " + problem, file=sys.stderr)
        return 2

    tests = [(n, f) for n, f in sorted(globals().items()) if n.startswith("test_") and callable(f)]
    failed = 0
    # The prefix holds a space on purpose: the hook mounts the repo path, and its quoting must survive it.
    with tempfile.TemporaryDirectory(prefix="gitleaks selftest ") as tmp:
        WORKDIR = tmp
        for name, func in tests:
            try:
                func()
                print("PASS: %s" % name)
            except AssertionError as err:
                failed += 1
                print("FAIL: %s: %s" % (name, err))
            except Exception as err:  # a crashing case must read as a failure, not a traceback that hides the rest
                failed += 1
                print("FAIL: %s: %s: %s" % (name, type(err).__name__, err))
    print("%d/%d passed" % (len(tests) - failed, len(tests)))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
