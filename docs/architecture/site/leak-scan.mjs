// Scans a built directory for internal details that must not reach the public architecture site.
//
//   node docs/architecture/site/leak-scan.mjs <dir>   scan every file under <dir>, JS and HTML included
//
// Exit codes: 0 clean, 1 a target hit or robots.txt lacks the disallow line, 2 usage,
// 4 the directory is missing, unreadable or holds no files.
//
// Report lines, in this order: TARGET <name> hits=<n> for every target (zero rows included),
// HIT <name> <file> count=<n> first=<matched text> for every target and file with hits,
// HOSTS <hosts>, ROBOTS disallow=<yes|no>, FILES <n> BYTES <n>, and a last line that reads
// OK targets=<n> or LEAK targets=<names>. A clean report goes to stdout, a leak to stderr.
// When GITHUB_STEP_SUMMARY is set the script appends the same lines there, so the workflow
// step needs no pipe.
//
// Decisions:
// - Every file is read as bytes and decoded as latin1, so a binary file cannot throw and no file is skipped.
// - The report prints the matched text, the file and a count, never the line around a match. A bundle is one
//   line of hundreds of KB and the CI log of a public repository is public.
// - The patterns are strict because loose ones hit vendored code: a bare ssh matches a shell word list, a
//   dotted quad without boundaries matches SVG numbers, and a generic host-and-namespace rule matches the
//   deployment ids LikeC4 generates. The test file holds one passing fixture per measured trap, so nobody
//   loosens a pattern or adds an exception without a red test.
// - The four hostnames in ALLOWED_HOSTS are the only exception list. Any addition needs its reason beside it.

import { readdirSync, readFileSync, statSync, appendFileSync } from 'node:fs'
import { join, relative } from 'node:path'

const ALLOWED_HOSTS = new Set([
  'kanban-board-rud-vlad-473.duckdns.org',
  'kanban-board-rud-vlad-473-nonprod.duckdns.org',
  'kanban-board-rud-vlad-473-monitoring.duckdns.org',
  'kanban-board-rud-vlad-473-architecture.duckdns.org',
])

const IPV4_OCTET = '(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])'
const IPV6_LONG = '(?<![0-9A-Fa-f:])(?:[0-9A-Fa-f]{1,4}:){3,7}[0-9A-Fa-f]{1,4}(?![0-9A-Fa-f:])'
const IPV6_SHORT = '(?<![0-9A-Za-z:])(?:[0-9A-Fa-f]{1,4}:){1,6}:(?:[0-9A-Fa-f]{1,4})?(?![0-9A-Za-z:])'

// Secrets the manifests reference by name and the model never draws. Four more names are Secrets too and are
// left out on purpose, because the model draws them as components: postgres-backup, letsencrypt-staging,
// letsencrypt-production and flux-system.
const SECRET_NAMES = ['app-tls', 'grafana-tls', 'architecture-tls', 'postgres-init', 'app-env', 'grafana-admin']

// Account and login forms the repository states. Six names are left out on purpose, one reason each:
// - vrudenko is the Java package com.vrudenko.kanban_board, which every source path in the site names.
// - rud-vlad-473 sits inside the four public hostnames, which the hostname target passes.
// - root as a bare word is CSS :root and JS .root in vendored code, so only the login form root@ is listed.
// - deploy as a bare word is deploy.yml in model text, so only the login form deploy@ is listed.
// - runner is a common word, and the GitHub-hosted runner is not this system's.
// - monitoring is the namespace the model draws on purpose.
const USER_NAME = /\b(?:rudenkovladimir|rudvlad473|andre|deploy-nonprod)\b|\b(?:root|deploy)@/gi

// Each row is [target name, pattern, optional filter that keeps only the matches that count].
const TARGETS = [
  ['ipv4', new RegExp(`(?<![0-9.])${IPV4_OCTET}(?:\\.${IPV4_OCTET}){3}(?![0-9])`, 'g')],
  ['ipv6', new RegExp(`${IPV6_LONG}|${IPV6_SHORT}`, 'g')],
  ['ssh-command', /ssh /g],
  ['kubectl', /kubectl/g],
  ['secret-word', /secret/gi],
  ['key-path', /authorized_keys|id_ed25519|id_rsa|~\/\.ssh|\/home\/|\/opt\/deploy|BEGIN [A-Z ]*PRIVATE|pass show/g],
  ['ssh-host', /netcup-prod/g],
  ['credential', /ghp_[A-Za-z0-9]{20}|AKIA[0-9A-Z]{16}|dckr_pat_|-----BEGIN|public-dashboards\//g],
  ['in-cluster-host', /\.svc\b|cluster\.local|postgres\.kanban|PGHOST/g],
  ['secret-name', new RegExp(`\\b(?:${SECRET_NAMES.join('|')})\\b`, 'g')],
  ['user-name', USER_NAME],
  ['hostname', /[A-Za-z0-9.-]*duckdns\.org/gi, (text) => !ALLOWED_HOSTS.has(text.toLowerCase())],
]
const ROBOTS = 'robots'
const TARGET_COUNT = TARGETS.length + 1

const usage = 'usage: node docs/architecture/site/leak-scan.mjs <dir>'
const args = process.argv.slice(2)
if (args.length !== 1) {
  console.error(usage)
  process.exit(2)
}
const dir = args[0]

function* walk(current) {
  const entries = readdirSync(current, { withFileTypes: true }).sort((a, b) => (a.name < b.name ? -1 : 1))
  for (const entry of entries) {
    const path = join(current, entry.name)
    if (entry.isDirectory()) yield* walk(path)
    else yield path
  }
}

// Matched text is cut and stripped of control bytes before it reaches a log.
function printable(text) {
  const clean = text.replace(/[^\x20-\x7e]/g, '?')
  return clean.length > 80 ? `${clean.slice(0, 80)}...` : clean
}

let files
try {
  if (!statSync(dir).isDirectory()) throw new Error('not a directory')
  files = [...walk(dir)]
} catch (err) {
  console.error(`directory ${dir} is missing or unreadable: ${err.message}\n${usage}`)
  process.exit(4)
}
if (files.length === 0) {
  console.error(`directory ${dir} holds no files, so a clean scan would prove nothing`)
  process.exit(4)
}

const totals = new Map(TARGETS.map(([name]) => [name, 0]))
const hitLines = []
const hosts = new Set()
let bytes = 0
let robotsText = null

try {
  for (const path of files) {
    const buffer = readFileSync(path)
    bytes += buffer.length
    const text = buffer.toString('latin1')
    const rel = relative(dir, path).split('\\').join('/')
    if (rel === 'robots.txt') robotsText = text
    for (const [name, pattern, keep] of TARGETS) {
      const found = text.match(pattern) ?? []
      if (name === 'hostname') for (const host of found) hosts.add(host.toLowerCase())
      const counted = keep ? found.filter(keep) : found
      if (counted.length === 0) continue
      totals.set(name, totals.get(name) + counted.length)
      hitLines.push(`HIT ${name} ${rel} count=${counted.length} first=${printable(counted[0])}`)
    }
  }
} catch (err) {
  console.error(`a file under ${dir} is unreadable: ${err.message}`)
  process.exit(4)
}

const disallows = robotsText !== null && robotsText.split(/\r?\n/).some((line) => line.trim() === 'Disallow: /')
const hit = TARGETS.map(([name]) => name).filter((name) => totals.get(name) > 0)
if (!disallows) {
  hit.push(ROBOTS)
  hitLines.push(`HIT ${ROBOTS} robots.txt count=1 first=${robotsText === null ? 'missing' : 'no Disallow: / line'}`)
}

const lines = [
  ...TARGETS.map(([name]) => `TARGET ${name} hits=${totals.get(name)}`),
  `TARGET ${ROBOTS} hits=${disallows ? 0 : 1}`,
  ...hitLines,
  `HOSTS ${hosts.size ? [...hosts].sort().join(',') : 'none'}`,
  `ROBOTS disallow=${disallows ? 'yes' : 'no'}`,
  `FILES ${files.length} BYTES ${bytes}`,
  hit.length ? `LEAK targets=${hit.join(',')}` : `OK targets=${TARGET_COUNT}`,
]
const report = lines.join('\n')

const summaryFile = process.env.GITHUB_STEP_SUMMARY
if (summaryFile) {
  try {
    appendFileSync(summaryFile, `\`\`\`\n${report}\n\`\`\`\n`)
  } catch (err) {
    console.error(`could not write the step summary: ${err.message}`)
  }
}

if (hit.length) {
  console.error(report)
  process.exit(1)
}
console.log(report)
