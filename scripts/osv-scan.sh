#!/usr/bin/env bash
# Report-only osv-scanner scan of a CycloneDX SBOM of the Spring Boot jar, run the same locally and in CI.
#
# Usage: bash scripts/osv-scan.sh
# Writes .osv-reports/runtime-classpath-tree.txt, boot-inf-lib.txt, bootjar.cdx.json and osv.json,
# then prints the guard command that judges whether the scan covered what it was given
# (scripts/verify-osv-report.py). Findings leave the exit status 0 on purpose; an osv-scanner exit
# other than 0 or 1, a digest mismatch, or any step that could not run exits non-zero.
#
# Decisions:
# - osv-scanner is a release binary pinned by version and SHA-256 below, downloaded into a mktemp
#   directory outside the checkout, checked before chmod and before the slow steps, and never put on
#   PATH. The release is mutable (isImmutable false on 2026-11-16), so the version tag alone proves
#   nothing. On that date the pinned digest equalled both GitHub's asset digest and the
#   osv-scanner_SHA256SUMS line, which share GitHub as their root of trust.
# - No third-party action or setup script installs it: in March 2026 most trivy-action tags were
#   force-pushed to credential-stealing code (GHSA-69fq-xp46-6x23), which is why this repo runs
#   scanners from pinned artifacts.
# - The SBOM comes from the Trivy image pinned in scripts/trivy-scan.sh, read through its image-ref
#   mode so the digest exists once. A Trivy pin bump therefore changes this scan's input too.
# - The Trivy container mounts only .osv-reports, which is narrower than trivy-scan.sh's whole
#   checkout mount, so the scanner never sees .git or the sources.
# - Gradle runs with no flag that relaxes dependency verification. Strict verification is the
#   default here and the Dockerfile's cold bootJar passes it on every deploy.
# - Findings never fail the script. Whether a finding should fail a job is a policy choice for when
#   a triaged baseline exists.
# - The runtime tree and the BOOT-INF/lib listing come from the same build as the jar, so the guard's
#   denominators move with the dependencies and nobody edits a number.
# - --timeout 30m for Trivy is a runaway guard, not a forecast. On 2026-11-16 the SBOM step took about
#   5m45s locally, almost all of it a 954 MiB Java DB download into the container's /tmp.
# - The driver is bash, not fish: the runner has no fish, the sibling scanner script is bash, and
#   the linters here (shellcheck, the comment lint) read .sh but not .fish.
#
# Known holes:
# - SLSA provenance of the osv-scanner binary is not verified, because slsa-verifier is not installed.
# - osv-scanner matches exact Maven coordinates, so a clean report never clears dependency-check: on
#   2026-11-16 it could not see 151 Spring advisories OSV holds only as git ranges, or JavaScript
#   bundled inside swagger-ui.
# - The OSV database is not pinned, by design, and the report carries no database date.
# - Public Maven coordinates go to api.osv.dev on every run.
# - Only the linux amd64 asset is pinned, so any other platform fails at the first step.
# - The Java DB is downloaded on every run and is not cached.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

OSV_SCANNER_VERSION="2.7.1"
OSV_SCANNER_SHA256="fb7dab19a8bd9bbe5c3189f09d4b7ee1820c9553a23bac202648a56efd2f6ee8"

REPORT_DIR=".osv-reports"
STAGE_DIR="${REPORT_DIR}/sbom-input"
TRIVY_TIMEOUT="30m"

TOOLS_DIR=""
TIMINGS=""

log() {
  echo "osv-scan: $*" >&2
}

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

cleanup() {
  [ -z "${TOOLS_DIR}" ] || rm -rf "${TOOLS_DIR}"
  rm -rf "${REPO_ROOT:?}/${STAGE_DIR}"
}

# Record the seconds since the last SECONDS=0 under a step name.
lap() {
  TIMINGS="${TIMINGS}${TIMINGS:+ }$1=${SECONDS}s"
  log "$1 took ${SECONDS}s"
}

require_platform() {
  [ "$(uname -s)" = "Linux" ] && [ "$(uname -m)" = "x86_64" ] \
    || fail "only the linux amd64 osv-scanner asset is pinned, and this machine is $(uname -s) $(uname -m)"
}

download_osv_scanner() {
  local url="https://github.com/google/osv-scanner/releases/download/v${OSV_SCANNER_VERSION}/osv-scanner_linux_amd64"
  log "downloading osv-scanner v${OSV_SCANNER_VERSION}"
  curl -fsSL --retry 3 -o "${TOOLS_DIR}/osv-scanner" "${url}" || fail "could not download ${url}"
  if ! (cd "${TOOLS_DIR}" && echo "${OSV_SCANNER_SHA256}  osv-scanner" | sha256sum -c - >/dev/null 2>&1); then
    fail "osv-scanner SHA-256 is $(sha256sum "${TOOLS_DIR}/osv-scanner" | cut -d' ' -f1), expected ${OSV_SCANNER_SHA256}"
  fi
  chmod +x "${TOOLS_DIR}/osv-scanner"
  log "digest matches the pin; $("${TOOLS_DIR}/osv-scanner" --version 2>&1 | head -n 1)"
}

# Print the one jar in build/libs that carries BOOT-INF/lib entries, or fail naming the candidates.
find_boot_jar() {
  local jar candidates=() all=()
  for jar in "${REPO_ROOT}"/build/libs/*.jar; do
    [ -e "${jar}" ] || continue
    all+=("${jar}")
    if [ "$(unzip -Z1 "${jar}" | grep -c '^BOOT-INF/lib/.' || true)" -gt 0 ]; then
      candidates+=("${jar}")
    fi
  done
  [ "${#candidates[@]}" -eq 1 ] || fail "expected exactly one boot jar in build/libs, found ${#candidates[@]} of ${#all[@]} jars: ${all[*]:-none}"
  echo "${candidates[0]}"
}

build_and_list() {
  local jar
  log "building the boot jar"
  SECONDS=0
  (cd "${REPO_ROOT}" && ./gradlew --no-daemon bootJar)
  lap bootjar

  jar="$(find_boot_jar)"
  log "boot jar: ${jar#"${REPO_ROOT}"/}"

  unzip -Z1 "${jar}" | sed -n 's#^BOOT-INF/lib/\(.*\.jar\)$#\1#p' | LC_ALL=C sort -u > "${REPO_ROOT}/${REPORT_DIR}/boot-inf-lib.txt"
  [ -s "${REPO_ROOT}/${REPORT_DIR}/boot-inf-lib.txt" ] || fail "the boot jar lists no BOOT-INF/lib jars"
  log "BOOT-INF/lib holds $(wc -l < "${REPO_ROOT}/${REPORT_DIR}/boot-inf-lib.txt") jars"

  SECONDS=0
  (cd "${REPO_ROOT}" && ./gradlew --no-daemon -q dependencies --configuration runtimeClasspath) > "${REPO_ROOT}/${REPORT_DIR}/runtime-classpath-tree.txt"
  [ -s "${REPO_ROOT}/${REPORT_DIR}/runtime-classpath-tree.txt" ] || fail "Gradle printed an empty runtimeClasspath tree"
  lap tree

  mkdir -p "${REPO_ROOT}/${STAGE_DIR}"
  cp "${jar}" "${REPO_ROOT}/${STAGE_DIR}/"
}

make_sbom() {
  local image
  image="$(bash "${SCRIPT_DIR}/trivy-scan.sh" image-ref)"
  [ -n "${image}" ] || fail "scripts/trivy-scan.sh image-ref printed no image"
  log "generating the SBOM with ${image}"
  SECONDS=0
  docker run --rm \
    --user "$(id -u):$(id -g)" \
    -e TRIVY_CACHE_DIR=/tmp/trivy-cache \
    -v "${REPO_ROOT}/${REPORT_DIR}:/work" -w /work \
    "${image}" rootfs --format cyclonedx --pkg-types library --quiet --skip-version-check \
    --timeout "${TRIVY_TIMEOUT}" --output bootjar.cdx.json sbom-input
  lap sbom
  [ -s "${REPO_ROOT}/${REPORT_DIR}/bootjar.cdx.json" ] || fail "Trivy wrote no SBOM to ${REPORT_DIR}/bootjar.cdx.json"
}

run_osv_scanner() {
  local status
  log "scanning the SBOM with osv-scanner"
  SECONDS=0
  set +e
  (cd "${REPO_ROOT}" && "${TOOLS_DIR}/osv-scanner" scan source -L "${REPORT_DIR}/bootjar.cdx.json" \
    --format json --all-packages --all-vulns --output-file "${REPORT_DIR}/osv.json")
  status=$?
  set -e
  lap osv
  case "${status}" in
    0) log "osv-scanner exit 0: no findings" ;;
    1) log "osv-scanner exit 1: findings reported (report-only)" ;;
    127) fail "osv-scanner exit 127: general error" ;;
    128) fail "osv-scanner exit 128: no packages found in the SBOM" ;;
    129) fail "osv-scanner exit 129: the OSV API failed" ;;
    *) fail "osv-scanner exit ${status}: unexpected" ;;
  esac
  [ -s "${REPO_ROOT}/${REPORT_DIR}/osv.json" ] || fail "osv-scanner wrote no report to ${REPORT_DIR}/osv.json"
}

[ "$#" -eq 0 ] || fail "usage: bash scripts/osv-scan.sh"

require_platform
TOOLS_DIR="$(mktemp -d)"
trap cleanup EXIT
download_osv_scanner

mkdir -p "${REPO_ROOT}/${REPORT_DIR}"
rm -f "${REPO_ROOT}/${REPORT_DIR}/runtime-classpath-tree.txt" "${REPO_ROOT}/${REPORT_DIR}/boot-inf-lib.txt" \
  "${REPO_ROOT}/${REPORT_DIR}/bootjar.cdx.json" "${REPO_ROOT}/${REPORT_DIR}/osv.json"
rm -rf "${REPO_ROOT:?}/${STAGE_DIR}"

build_and_list
make_sbom
run_osv_scanner

log "step timings: ${TIMINGS}"
log "next: python3 -I scripts/verify-osv-report.py --report ${REPORT_DIR}/osv.json --sbom ${REPORT_DIR}/bootjar.cdx.json --libs ${REPORT_DIR}/boot-inf-lib.txt --tree ${REPORT_DIR}/runtime-classpath-tree.txt"
