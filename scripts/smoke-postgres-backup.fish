#!/usr/bin/env fish
#
# Local smoke test for k8s/data/postgres/backup/postgres-backup.sh. Not run in CI: it needs Docker and
# network access to pull Alpine packages.
#
# It starts a scratch postgres:16 holding a seeded table, generates a throwaway age identity, puts a stub
# `aws` first on PATH (it logs its arguments and copies the --body file out), and runs the real script in
# an alpine:3.22 container for five cases:
#
#   1. a non-Sunday run uploads exactly one object under daily/, naming a checksum algorithm
#   2. a Sunday run uploads the same bytes under daily/ and weekly/
#   3. the uploaded object is age ciphertext that decrypts to exactly the dump size the script logged
#   4. an unreachable database exits non-zero with zero uploads
#   5. a missing required variable exits non-zero, names the variable, and prints no secret value
#
# Prints PASS or FAIL per case, removes every container, network and temp file it created even on
# failure, and exits non-zero if any case failed.
#
# SMOKE_BACKUP_SCRIPT overrides the script under test (used to prove the cases fail against a broken script).

set -g repo (path resolve (status dirname)/..)
set -g BACKUP_SCRIPT $repo/k8s/data/postgres/backup/postgres-backup.sh
if set -q SMOKE_BACKUP_SCRIPT
    set BACKUP_SCRIPT $SMOKE_BACKUP_SCRIPT
end

set -g image_alpine alpine:3.22@sha256:5291449c3df73caf6ed85e649dec1b9e818b39a5d8c871e97afc13e9cd5e8fa8
set -g image_pg postgres:16

set -g suffix (random)
set -g net smoke-pgbackup-net-$suffix
set -g pg smoke-pgbackup-pg-$suffix
set -g tools smoke-pgbackup-tools-$suffix
set -g tmp (mktemp -d)

# Distinctive values, so a leak into the script's output is detectable by a plain substring search.
set -g pw_sentinel pw-sentinel-7f3a91
set -g key_id_sentinel keyid-sentinel-4b2e60
set -g key_secret_sentinel keysecret-sentinel-9c1d38

set -g failures 0
set -g reasons
set -g recipient ""
set -g last_rc 0

function cleanup --on-event fish_exit
    # Files written by the container are owned by root, so empty the mount from inside it first.
    docker exec $tools sh -c 'rm -rf /smoke/*' >/dev/null 2>&1
    docker rm -f $tools $pg >/dev/null 2>&1
    docker network rm $net >/dev/null 2>&1
    rm -rf $tmp
end

# Run a shell snippet inside the tools container and print its stdout.
function cx
    docker exec $tools sh -c "$argv"
end

function finish_case --argument-names n desc
    if test (count $reasons) -eq 0
        echo "PASS case $n: $desc"
    else
        echo "FAIL case $n: $desc"
        for r in $reasons
            echo "  - $r"
        end
        set -g failures (math $failures + 1)
    end
    set -g reasons
end

# run_backup <case number> <PGHOST> <weekday override or ""> <1 to set AGE_RECIPIENT, 0 to leave it unset>
function run_backup --argument-names n pghost dow with_age
    set -l d /smoke/case$n
    docker exec $tools mkdir -p $d/work $d/out
    set -l envs -e PGHOST=$pghost -e PGPORT=5432 -e PGUSER=postgres -e PGPASSWORD=$pw_sentinel \
        -e PGDATABASE=kanban_prod -e B2_ENDPOINT=https://s3.invalid.example -e B2_BUCKET=smoke-bucket \
        -e AWS_ACCESS_KEY_ID=$key_id_sentinel -e AWS_SECRET_ACCESS_KEY=$key_secret_sentinel \
        -e AWS_DEFAULT_REGION=smoke-region -e WORK_DIR=$d/work -e STUB_LOG=$d/aws.log -e STUB_OUT=$d/out \
        -e PATH=/smoke/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
    if test -n "$dow"
        set -a envs -e BACKUP_DAY_OF_WEEK_OVERRIDE=$dow
    end
    if test "$with_age" = 1
        set -a envs -e AGE_RECIPIENT=$recipient
    end
    docker exec $envs $tools sh /backup/(basename $BACKUP_SCRIPT) >$tmp/case$n.out 2>&1
    set -g last_rc $status
end

# The --key argument of each recorded put-object call, one per line.
function uploaded_keys --argument-names n
    cx "cat /smoke/case$n/aws.log 2>/dev/null" | string replace -rf '.*--key (\S+).*' '$1'
end

# ---------------------------------------------------------------- setup

docker info >/dev/null 2>&1
or begin
    echo "SETUP FAILED: Docker is not reachable"
    exit 2
end

docker network create $net >/dev/null
or begin
    echo "SETUP FAILED: cannot create network"
    exit 2
end

docker run -d --name $pg --network $net -e POSTGRES_PASSWORD=$pw_sentinel -e POSTGRES_DB=kanban_prod $image_pg >/dev/null
or begin
    echo "SETUP FAILED: cannot start postgres"
    exit 2
end

mkdir -p $tmp/bin
printf '%s\n' \
    '#!/bin/sh' \
    'echo "$*" >> "$STUB_LOG"' \
    'body=""' \
    'key=""' \
    'while [ "$#" -gt 0 ]; do' \
    '  case "$1" in' \
    '    --body) body=$2; shift ;;' \
    '    --key) key=$2; shift ;;' \
    '  esac' \
    '  shift' \
    'done' \
    'if [ -n "$body" ]; then' \
    '  cp "$body" "$STUB_OUT/$(echo "$key" | tr / _)"' \
    'fi' >$tmp/bin/aws
chmod +x $tmp/bin/aws

docker run -d --name $tools --network $net -v $tmp:/smoke -v (path dirname $BACKUP_SCRIPT):/backup:ro $image_alpine sleep 3600 >/dev/null
or begin
    echo "SETUP FAILED: cannot start the tools container"
    exit 2
end

docker exec $tools apk add --no-cache age aws-cli postgresql16-client >/dev/null 2>&1
or begin
    echo "SETUP FAILED: apk add failed"
    exit 2
end

# pg_isready over TCP only succeeds on the final server; the init-time server listens on the socket only.
set -l ready 0
for i in (seq 1 90)
    if docker exec $pg pg_isready -h 127.0.0.1 -U postgres -q
        set ready 1
        break
    end
    sleep 1
end
if test $ready -ne 1
    echo "SETUP FAILED: postgres did not become ready"
    exit 2
end

docker exec $pg psql -v ON_ERROR_STOP=1 -q -U postgres -d kanban_prod -c "CREATE TABLE smoke_backup (id int PRIMARY KEY, note text); INSERT INTO smoke_backup SELECT g, 'row ' || g FROM generate_series(1, 500) g;" >/dev/null
or begin
    echo "SETUP FAILED: cannot seed the table"
    exit 2
end

docker exec $tools age-keygen -o /smoke/identity.txt >/dev/null 2>&1
set recipient (docker exec $tools age-keygen -y /smoke/identity.txt)
if not string match -qr '^age1[0-9a-z]{58}$' -- $recipient
    echo "SETUP FAILED: no usable age recipient"
    exit 2
end

# ---------------------------------------------------------------- case 1: non-Sunday

run_backup 1 $pg 3 1
test $last_rc -eq 0
or set -ga reasons "exit $last_rc, expected 0"
set -l keys1 (uploaded_keys 1)
test (count $keys1) -eq 1
or set -ga reasons "expected exactly 1 put-object call, got "(count $keys1)
if test (count $keys1) -ge 1
    string match -qr '^daily/kanban_prod-[0-9]{8}T[0-9]{6}Z\.dump\.age$' -- $keys1[1]
    or set -ga reasons "unexpected object key: $keys1[1]"
end
# B2 refuses an Object Lock bucket's PUT without a checksum header, and the container's when_required
# mode sends none unless the call names an algorithm.
set -l unchecked (cx "grep -v -e '--checksum-algorithm' /smoke/case1/aws.log" | count)
test $unchecked -eq 0
or set -ga reasons "$unchecked put-object call(s) without --checksum-algorithm"
finish_case 1 "non-Sunday run uploads exactly one object under daily/, naming a checksum algorithm"

# ---------------------------------------------------------------- case 2: Sunday

run_backup 2 $pg 7 1
test $last_rc -eq 0
or set -ga reasons "exit $last_rc, expected 0"
set -l keys2 (uploaded_keys 2)
test (count $keys2) -eq 2
or set -ga reasons "expected exactly 2 put-object calls, got "(count $keys2)
if test (count $keys2) -eq 2
    string match -qr '^daily/kanban_prod-' -- $keys2[1]
    or set -ga reasons "first key is not under daily/: $keys2[1]"
    string match -qr '^weekly/kanban_prod-' -- $keys2[2]
    or set -ga reasons "second key is not under weekly/: $keys2[2]"
    docker exec $tools sh -c 'cd /smoke/case2/out && cmp daily_* weekly_*' >/dev/null 2>&1
    or set -ga reasons "daily and weekly bodies differ"
end
finish_case 2 "Sunday run uploads the same bytes under daily/ and weekly/"

# ---------------------------------------------------------------- case 3: encryption

set -l obj (cx "ls /smoke/case1/out" | head -n 1)
if test -z "$obj"
    set -ga reasons "no uploaded object captured in case 1"
else
    set -l head5 (cx "head -c5 /smoke/case1/out/$obj")
    test "$head5" != PGDMP
    or set -ga reasons "uploaded body is a plaintext custom-format dump"
    set -l logged (string replace -rf '.*dump_bytes=([0-9]+).*' '$1' <$tmp/case1.out | head -n 1)
    if test -z "$logged"
        set -ga reasons "script did not log dump_bytes="
    end
    cx "age -d -i /smoke/identity.txt /smoke/case1/out/$obj > /smoke/case1/decrypted.dump" >/dev/null 2>&1
    or set -ga reasons "age -d failed with the throwaway identity"
    set -l decrypted (string trim (cx "wc -c < /smoke/case1/decrypted.dump 2>/dev/null"))
    if test -z "$decrypted"; or test "$decrypted" -le 0
        set -ga reasons "decrypted size is 0 or missing (logged dump size: $logged)"
    else if test "$decrypted" != "$logged"
        set -ga reasons "decrypted $decrypted bytes, script logged $logged"
    end
    set -l listed (cx "pg_restore -l /smoke/case1/decrypted.dump 2>/dev/null | grep -c smoke_backup")
    test -n "$listed"; and test "$listed" -ge 1
    or set -ga reasons "pg_restore -l on the decrypted dump does not list smoke_backup"
    cx "ls /smoke/case1/work" | string match -q 'db.dump'
    and set -ga reasons "plaintext db.dump left in WORK_DIR"
end
finish_case 3 "uploaded object is age ciphertext decrypting to the logged dump size"

# ---------------------------------------------------------------- case 4: unreachable database

run_backup 4 unreachable.invalid 3 1
test $last_rc -ne 0
or set -ga reasons "exit 0, expected non-zero"
set -l keys4 (uploaded_keys 4)
test (count $keys4) -eq 0
or set -ga reasons "expected zero put-object calls, got "(count $keys4)
set -l files4 (cx "ls /smoke/case4/out")
test (count $files4) -eq 0
or set -ga reasons "an object was captured despite the failure"
finish_case 4 "unreachable database exits non-zero with zero uploads"

# ---------------------------------------------------------------- case 5: missing variable

run_backup 5 $pg 3 0
test $last_rc -ne 0
or set -ga reasons "exit 0, expected non-zero"
grep -qF 'missing required variable AGE_RECIPIENT' $tmp/case5.out
or set -ga reasons "output does not name the missing variable AGE_RECIPIENT"
for s in $pw_sentinel $key_id_sentinel $key_secret_sentinel
    if grep -qF -- $s $tmp/case1.out $tmp/case2.out $tmp/case4.out $tmp/case5.out
        set -ga reasons "a secret value appeared in the script output"
    end
end
set -l keys5 (uploaded_keys 5)
test (count $keys5) -eq 0
or set -ga reasons "expected zero put-object calls, got "(count $keys5)
finish_case 5 "missing variable exits non-zero, names it and prints no secret"

# ---------------------------------------------------------------- result

if test $failures -gt 0
    echo "SMOKE: $failures of 5 cases failed"
    exit 1
end
echo "SMOKE: all 5 cases passed"
exit 0
