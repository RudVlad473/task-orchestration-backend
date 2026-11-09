#!/usr/bin/env bash
# Report-only Trivy scans of the built image and of the rendered k8s manifests, run the same locally and in CI.
#
# Usage: bash scripts/trivy-scan.sh image | config
# image writes .trivy-reports/image.json and image-packages.txt; config writes config.json and
# config-expected.txt. Each prints the guard command that judges whether the scan covered what it
# was given (scripts/verify-trivy-report.py). Findings leave the exit status 0 on purpose; only a
# scan that could not run, or a k8s root nobody classified, exits non-zero.
#
# Decisions:
# - Trivy runs from one container image pinned by digest, read from ghcr.io and Docker Hub and
#   required equal on 2026-11-09 for a release GitHub reports as immutable. It is never run through
#   trivy-action or setup-trivy: in March 2026 most trivy-action tags and every setup-trivy tag were
#   force-pushed to credential-stealing code (GHSA-69fq-xp46-6x23), and that advisory says images
#   referenced by digest were not affected.
# - The pin is above every patched version in the advisory list (0.72.0 and later), so the Helm
#   tar bomb, the crafted-database path traversal and the plugin path traversal are all fixed.
# - Operating-system packages only (--pkg-types os). Java libraries are already reported by the
#   weekly dependency-check run and by Dependabot, and reporting them a third time here would triple
#   the triage without a new finding.
# - The image goes to the scanner as a docker save tar through --input, so no Docker socket is ever
#   mounted into the scanner container.
# - The scanner runs as the invoking uid and gid with TRIVY_CACHE_DIR in /tmp, so the report files
#   it writes into the bind-mounted checkout are not root-owned and need no cleanup with sudo.
# - docker build --pull, so a cached base layer on this machine cannot make a stale scan look
#   current.
# - --timeout 30m, because the default 5m made the first local run fail with "context deadline
#   exceeded" while the vulnerability database downloaded and the image was analysed on 2026-11-09.
#   It is a runaway guard, not a forecast; the workflow's own step timeout is the outer bound.
# - Report-only (no --exit-code), because no triaged baseline exists yet. Moving to a gate is a
#   separate decision about severity and exit code.
# - The denominator for the guard is dpkg inside the same built image (status ii), measured to
#   match Trivy's own package count 143 to 143 on the eclipse-temurin:21-jre-jammy base on 2026-11-09.
# - config scans four roots (the three overlays and data/postgres) in rendered form, plus both
#   Dockerfiles. Raw patch files would be scanned as partial manifests and report findings on
#   fragments, and the bases render inside the overlays, so scanning them alone double-counts.
# - config passes --skip-check-update, so only the checks embedded in the pinned binary run. The
#   verdict is then a pure function of the manifests and the pin, and the checks age with the pin.
# - Both modes share this script and run_trivy, so the CI command and the local command are the same.
#
# Known holes:
# - config does not scan workloads installed by a HelmRelease: their charts render in the cluster.
# - The embedded config checks are as old as the pinned binary.
# - config reads rendered output only, so a base edited without a rendering root is invisible until
#   a root that renders it is listed in SCAN_ROOTS.
# - The vulnerability database is not pinned, by design: a pinned database is a scanner that stops
#   finding new CVEs. The script never passes --db-repository or --java-db-repository.
# - A fresh build shows what a rebuild would ship, not the operating-system layer running in prod,
#   which was built at its own deploy time.
# - Anonymous Docker Hub pulls of the Gradle and Temurin base images may be rate limited on a shared
#   runner. That was not measured.
# - Library and secret scanning are covered by other gates (dependency-check, Dependabot, gitleaks,
#   TruffleHog), so this scan does not repeat them.
# - A base image without dpkg gives an empty denominator and the guard fails loudly, so a non-Debian
#   base needs a new denominator source.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

TRIVY_IMAGE="ghcr.io/aquasecurity/trivy:0.76.2@sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb"

TRIVY_TIMEOUT="30m"

REPORT_DIR=".trivy-reports"
SCAN_TAG="kanban-trivy-scan:local"

log() {
  echo "trivy-scan: $*" >&2
}

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

usage() {
  echo "usage: bash scripts/trivy-scan.sh image | config" >&2
  exit 2
}

# Repo-authored roots that render pod-bearing kinds, so their manifests are scanned.
SCAN_ROOTS=(
  k8s/overlays/prod
  k8s/overlays/nonprod
  k8s/overlays/architecture
  k8s/data/postgres
)

# Roots that are deliberately not scanned. Every kustomization root under k8s/ must be in exactly
# one of the two lists, and the drift check below fails on any that is in neither.
#
# Decisions:
# - base/app, base/architecture-site, base/redpanda: rendered inside the overlays, so scanning them
#   alone double-counts.
# - flux-system: Flux's own unmodified export, skipped like the CustomResourceDefinition kind in
#   verify-k8s-manifests.sh.
# - data/postgres-bridge, monitoring/configs, monitoring/controllers, platform, platform/cert-manager
#   and platform/edge: they render no pod-bearing kind. The workloads their HelmReleases install
#   come from upstream charts that are rendered in the cluster.
SKIP_ROOTS=(
  k8s/base/app
  k8s/base/architecture-site
  k8s/base/redpanda
  k8s/flux-system
  k8s/data/postgres-bridge
  k8s/monitoring/configs
  k8s/monitoring/controllers
  k8s/platform
  k8s/platform/cert-manager
  k8s/platform/edge
)

run_trivy() {
  docker run --rm \
    --user "$(id -u):$(id -g)" \
    -e TRIVY_CACHE_DIR=/tmp/trivy-cache \
    -v "${REPO_ROOT}:/repo" -w /repo \
    "${TRIVY_IMAGE}" "$@"
}

cleanup_image() {
  rm -f "${REPO_ROOT}/${REPORT_DIR}/image.tar"
  docker rmi -f "${SCAN_TAG}" >/dev/null 2>&1 || true
}

scan_image() {
  mkdir -p "${REPO_ROOT}/${REPORT_DIR}"
  rm -f "${REPO_ROOT}/${REPORT_DIR}/image.json" "${REPO_ROOT}/${REPORT_DIR}/image-packages.txt" "${REPO_ROOT}/${REPORT_DIR}/image.tar"
  trap cleanup_image EXIT

  log "building ${SCAN_TAG} from ./Dockerfile"
  SECONDS=0
  docker build --pull -t "${SCAN_TAG}" "${REPO_ROOT}"
  log "build took ${SECONDS}s"

  docker save -o "${REPO_ROOT}/${REPORT_DIR}/image.tar" "${SCAN_TAG}"

  docker run --rm --entrypoint dpkg-query "${SCAN_TAG}" -W -f '${db:Status-Abbrev}${binary:Package}\n' \
    | awk '$1 == "ii" { sub(/:.*/, "", $2); print $2 }' \
    | sort -u > "${REPO_ROOT}/${REPORT_DIR}/image-packages.txt"
  log "dpkg lists $(wc -l < "${REPO_ROOT}/${REPORT_DIR}/image-packages.txt") installed packages"

  log "scanning the image's operating-system packages with ${TRIVY_IMAGE}"
  SECONDS=0
  run_trivy image \
    --input "${REPORT_DIR}/image.tar" \
    --pkg-types os \
    --scanners vuln \
    --list-all-pkgs \
    --quiet \
    --skip-version-check \
    --timeout "${TRIVY_TIMEOUT}" \
    -f json -o "${REPORT_DIR}/image.json"
  log "scan took ${SECONDS}s"

  [ -s "${REPO_ROOT}/${REPORT_DIR}/image.json" ] || fail "Trivy wrote no report to ${REPORT_DIR}/image.json"
  log "next: python3 -I scripts/verify-trivy-report.py --kind image --report ${REPORT_DIR}/image.json --expected ${REPORT_DIR}/image-packages.txt"
}

check_roots() {
  local discovered classified unclassified stale
  discovered="$(cd "${REPO_ROOT}" && find k8s -type f -name kustomization.yaml -exec dirname {} \; | LC_ALL=C sort)"
  classified="$(printf '%s\n' "${SCAN_ROOTS[@]}" "${SKIP_ROOTS[@]}" | LC_ALL=C sort)"
  unclassified="$(LC_ALL=C comm -23 <(echo "${discovered}") <(echo "${classified}"))"
  stale="$(LC_ALL=C comm -13 <(echo "${discovered}") <(echo "${classified}"))"
  if [ -n "${unclassified}" ]; then
    echo "FAIL: kustomization root(s) under k8s/ in neither SCAN_ROOTS nor SKIP_ROOTS: $(echo "${unclassified}" | tr '\n' ' ')" >&2
  fi
  if [ -n "${stale}" ]; then
    echo "FAIL: SCAN_ROOTS or SKIP_ROOTS name root(s) that hold no kustomization.yaml: $(echo "${stale}" | tr '\n' ' ')" >&2
  fi
  if [ -n "${unclassified}${stale}" ]; then
    fail "add each new root to SCAN_ROOTS or SKIP_ROOTS in scripts/trivy-scan.sh with a reason, and drop each stale one"
  fi
}

scan_config() {
  local input="${REPORT_DIR}/config-input" kubectl root name
  check_roots

  mkdir -p "${REPO_ROOT}/${REPORT_DIR}"
  rm -rf "${REPO_ROOT:?}/${REPORT_DIR}/config-input"
  rm -f "${REPO_ROOT}/${REPORT_DIR}/config.json" "${REPO_ROOT}/${REPORT_DIR}/config-expected.txt"
  mkdir -p "${REPO_ROOT}/${input}"

  kubectl="$(bash "${SCRIPT_DIR}/verify-k8s-manifests.sh" --tool kubectl | tail -n 1)"
  for root in "${SCAN_ROOTS[@]}"; do
    name="${root//\//-}.yaml"
    log "rendering ${root} to ${name}"
    "${kubectl}" kustomize "${REPO_ROOT}/${root}" > "${REPO_ROOT}/${input}/${name}"
  done
  cp "${REPO_ROOT}/Dockerfile" "${REPO_ROOT}/${input}/Dockerfile"
  cp "${REPO_ROOT}/k8s/data/postgres/backup/Dockerfile" "${REPO_ROOT}/${input}/backup.Dockerfile"
  ls -1 "${REPO_ROOT}/${input}" | LC_ALL=C sort > "${REPO_ROOT}/${REPORT_DIR}/config-expected.txt"

  log "scanning $(wc -l < "${REPO_ROOT}/${REPORT_DIR}/config-expected.txt") staged files with ${TRIVY_IMAGE}"
  SECONDS=0
  run_trivy config \
    --skip-check-update \
    --quiet \
    --skip-version-check \
    --timeout "${TRIVY_TIMEOUT}" \
    -f json -o "${REPORT_DIR}/config.json" \
    "${input}"
  log "scan took ${SECONDS}s"

  [ -s "${REPO_ROOT}/${REPORT_DIR}/config.json" ] || fail "Trivy wrote no report to ${REPORT_DIR}/config.json"
  log "next: python3 -I scripts/verify-trivy-report.py --kind config --report ${REPORT_DIR}/config.json --expected ${REPORT_DIR}/config-expected.txt"
}

[ "$#" -eq 1 ] || usage
case "$1" in
  image) scan_image ;;
  config) scan_config ;;
  *) usage ;;
esac
