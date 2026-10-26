#!/bin/sh
# Nightly backup of the production database: dump, check the archive, encrypt with age, upload to B2.
# Runs under busybox sh in the backup container. Every failing step exits non-zero before any upload.
#
# Environment contract (all required; no value is ever printed):
# - PGHOST, PGPORT, PGUSER, PGPASSWORD, PGDATABASE: read by pg_dump itself, never passed as arguments.
# - AGE_RECIPIENT: the age public key. The private key never reaches the cluster.
# - B2_ENDPOINT, B2_BUCKET, AWS_DEFAULT_REGION, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY: the bucket's
#   S3 endpoint, name, region and the write-only key.
# - WORK_DIR: a writable scratch directory.
#
# Decisions:
# The VM key can write but not read, list or delete, so this script never reads back from B2. The
# integrity signals are the archive check below and the exit status of put-object.
# The weekly copy is an if block, never a trailing && list: under set -e a false test as the last
# command would exit 1 on every day except Sunday.
# Object keys under daily/ and weekly/ are the prefixes the bucket lifecycle rules expire.
# age needs the plaintext path as an argument; without it age reads an empty stdin and writes a valid
# but empty ciphertext.
set -eu
# shellcheck disable=SC3040
set -o pipefail

log() {
  printf '%s %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"
}

for name in PGHOST PGPORT PGUSER PGPASSWORD PGDATABASE AGE_RECIPIENT B2_ENDPOINT B2_BUCKET \
  AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_DEFAULT_REGION WORK_DIR; do
  eval "value=\${$name:-}"
  if [ -z "$value" ]; then
    echo "missing required variable $name" >&2
    exit 1
  fi
done

# The plaintext dump must not outlive the run, whichever step fails.
trap 'rm -f "$WORK_DIR/db.dump" "$WORK_DIR/db.dump.age"' EXIT

ts=$(date -u +%Y%m%dT%H%M%SZ)

# A freshly started pod is refused by the NetworkPolicy controller for several seconds, until it adds
# the pod to the allowed set. Measured 2026-10-26: the first connection attempt of a new labelled pod
# was refused and the one 10 seconds later succeeded, while a job's own retries all failed.
tries=0
until pg_isready -q -t 3; do
  tries=$((tries + 1))
  if [ "$tries" -ge 18 ]; then
    echo "postgres not accepting connections after $tries attempts" >&2
    exit 1
  fi
  sleep 10
done

log "dump start"
pg_dump -Fc --no-password -f "$WORK_DIR/db.dump"
pg_restore -l "$WORK_DIR/db.dump" >/dev/null
dump_bytes=$(wc -c <"$WORK_DIR/db.dump" | tr -d ' ')
if [ "$dump_bytes" -le 0 ]; then
  echo "dump is empty" >&2
  exit 1
fi
log "dump ok dump_bytes=$dump_bytes"

age -r "$AGE_RECIPIENT" -o "$WORK_DIR/db.dump.age" "$WORK_DIR/db.dump"
rm -f "$WORK_DIR/db.dump"
log "encrypted"

encrypted_bytes=$(wc -c <"$WORK_DIR/db.dump.age" | tr -d ' ')
encrypted_sha256=$(sha256sum "$WORK_DIR/db.dump.age" | cut -d ' ' -f 1)

daily_key="daily/${PGDATABASE}-$ts.dump.age"
aws s3api put-object --endpoint-url "$B2_ENDPOINT" --bucket "$B2_BUCKET" \
  --key "$daily_key" --body "$WORK_DIR/db.dump.age" >/dev/null
log "uploaded $daily_key"
keys="$daily_key"

# BACKUP_DAY_OF_WEEK_OVERRIDE exists so a test can reach the Sunday branch on any weekday.
day_of_week=${BACKUP_DAY_OF_WEEK_OVERRIDE:-$(date -u +%u)}
if [ "$day_of_week" = 7 ]; then
  weekly_key="weekly/${PGDATABASE}-$ts.dump.age"
  aws s3api put-object --endpoint-url "$B2_ENDPOINT" --bucket "$B2_BUCKET" \
    --key "$weekly_key" --body "$WORK_DIR/db.dump.age" >/dev/null
  log "uploaded $weekly_key"
  keys="$keys $weekly_key"
fi

log "done keys=$keys encrypted_bytes=$encrypted_bytes sha256=$encrypted_sha256"
