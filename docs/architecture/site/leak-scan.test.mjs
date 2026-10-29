// Fixture tests for leak-scan.mjs. Each case writes a small directory and spawns the script on it, so the
// tests hold the same exit codes and report lines the workflow relies on.
//
//   node --test docs/architecture/site/leak-scan.test.mjs
//
// Test names start with the target or the trap they cover, so a failure names it.
//
// Decisions:
// - A failing case per target proves the pattern fires. A passing case per trap proves a string measured in
//   the real build does not, so a pattern cannot be loosened without a red test.
// - Credential-shaped samples are joined at runtime from pieces, so no complete shape sits in this file for
//   a secret scanner to refuse.

import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { after, test } from 'node:test'
import { fileURLToPath } from 'node:url'

const script = fileURLToPath(new URL('./leak-scan.mjs', import.meta.url))
const TARGET_COUNT = 13
const made = []

after(() => {
  for (const dir of made) rmSync(dir, { recursive: true, force: true })
})

// Builds a directory from a map of relative path to string or Buffer content. The robots file is the valid
// one unless the case replaces it (a string) or removes it (false).
function build(files, robots = 'User-agent: *\nDisallow: /\n') {
  const dir = mkdtempSync(join(tmpdir(), 'leak-scan-'))
  made.push(dir)
  if (robots !== false) writeFileSync(join(dir, 'robots.txt'), robots)
  for (const [name, content] of Object.entries(files)) {
    const path = join(dir, name)
    mkdirSync(dirname(path), { recursive: true })
    writeFileSync(path, content)
  }
  return dir
}

function run(args, env = {}) {
  const result = spawnSync(process.execPath, [script, ...args], {
    encoding: 'utf8',
    env: { ...process.env, GITHUB_STEP_SUMMARY: '', ...env },
  })
  const lines = (result.status === 0 ? result.stdout : result.stderr).trim().split('\n')
  return { status: result.status, stdout: result.stdout, stderr: result.stderr, lines, last: lines.at(-1) }
}

// A case that must fail names its target on the last line, counts it, and prints the matched text.
function expectLeak(target, files, first, robots) {
  const result = run([build(files, robots)])
  assert.equal(result.status, 1, result.stderr)
  assert.equal(result.last, `LEAK targets=${target}`)
  assert.ok(
    result.lines.some((line) => new RegExp(`^TARGET ${target} hits=[1-9]`).test(line)),
    `no TARGET row with hits for ${target}`,
  )
  assert.ok(
    result.lines.some((line) => line.startsWith(`HIT ${target} `) && line.endsWith(`first=${first}`)),
    `no HIT row for ${target} ending in first=${first}\n${result.stderr}`,
  )
}

function expectClean(files, robots) {
  const result = run([build(files, robots)])
  assert.equal(result.status, 0, result.stderr)
  assert.equal(result.last, `OK targets=${TARGET_COUNT}`)
  return result
}

const piece = (...parts) => parts.join('')

// [target, content, first matched text, optional label]. The label names the test where the content is a
// credential-shaped string that should stay out of test output.
const LEAKS = [
  ['ipv4', 'host 10.20.30.40 here', '10.20.30.40'],
  ['ipv6', '2001:db8:0:0:0:0:0:1', '2001:db8:0:0:0:0:0:1'],
  ['ipv6', 'link fe80::1 here', 'fe80::1'],
  ['ssh-command', 'run ssh bastion now', 'ssh '],
  ['kubectl', 'kubectl get pods', 'kubectl'],
  ['secret-word', 'a Secret value', 'Secret'],
  ['key-path', 'cat authorized_keys', 'authorized_keys'],
  ['key-path', 'the id_ed25519 file', 'id_ed25519'],
  ['key-path', 'the id_rsa file', 'id_rsa'],
  ['key-path', 'in ~/.ssh/config', '~/.ssh'],
  ['key-path', 'under /home/someone', '/home/'],
  ['key-path', 'at /opt/deploy', '/opt/deploy'],
  ['key-path', piece('BEGIN', ' RSA ', 'PRIVATE', ' KEY'), piece('BEGIN', ' RSA ', 'PRIVATE'), 'private key header'],
  ['key-path', 'run pass show token', 'pass show'],
  ['ssh-host', 'the netcup-prod box', 'netcup-prod'],
  ['credential', piece('gh', 'p_', 'a1'.repeat(10)), piece('gh', 'p_', 'a1'.repeat(10)), 'GitHub token shape'],
  ['credential', piece('AK', 'IA', 'ABCDEFGHIJKLMNOP'), piece('AK', 'IA', 'ABCDEFGHIJKLMNOP'), 'AWS key shape'],
  ['credential', piece('dckr_', 'pat_', 'xyz'), piece('dckr_', 'pat_'), 'Docker Hub token prefix'],
  ['credential', piece('-'.repeat(5), 'BEGIN', ' CERTIFICATE'), piece('-'.repeat(5), 'BEGIN'), 'PEM header'],
  ['credential', 'see public-dashboards/example', 'public-dashboards/'],
  ['in-cluster-host', 'postgres.kanban-data.svc.cluster.local', 'postgres.kanban'],
  ['in-cluster-host', 'api.svc', '.svc'],
  ['in-cluster-host', 'node.cluster.local', 'cluster.local'],
  ['in-cluster-host', 'at postgres.kanban', 'postgres.kanban'],
  ['in-cluster-host', 'export PGHOST', 'PGHOST'],
  ['secret-name', 'app-tls', 'app-tls'],
  ['secret-name', 'grafana-tls', 'grafana-tls'],
  ['secret-name', 'architecture-tls', 'architecture-tls'],
  ['secret-name', 'postgres-init', 'postgres-init'],
  ['secret-name', 'app-env', 'app-env'],
  ['secret-name', 'grafana-admin', 'grafana-admin'],
  ['user-name', 'rudenkovladimir/kanban-board-backend', 'rudenkovladimir'],
  ['user-name', 'github.com/RudVlad473', 'RudVlad473'],
  ['user-name', 'the andre account', 'andre'],
  ['user-name', 'user deploy-nonprod', 'deploy-nonprod'],
  ['user-name', 'root@host', 'root@'],
  ['user-name', 'deploy@vps', 'deploy@'],
  ['hostname', 'https://evil-kanban.duckdns.org/', 'evil-kanban.duckdns.org'],
  ['hostname', 'sub.kanban-board-rud-vlad-473.duckdns.org', 'sub.kanban-board-rud-vlad-473.duckdns.org'],
  ['hostname', 'EVIL.DUCKDNS.ORG', 'EVIL.DUCKDNS.ORG'],
]

for (const [target, content, first, label] of LEAKS) {
  test(`${target}: ${label ?? content.slice(0, 40)} fails the scan`, () => {
    expectLeak(target, { 'assets/page.js': content }, first)
  })
}

test('robots: a build with no robots.txt fails the scan', () => {
  expectLeak('robots', { 'index.html': '<html></html>' }, 'missing', false)
})

test('robots: a file without a Disallow line fails the scan', () => {
  expectLeak('robots', { 'index.html': '<html></html>' }, 'no Disallow: / line', 'User-agent: *\n')
})

test('robots: a Disallow for a sub-path only fails the scan', () => {
  expectLeak('robots', { 'index.html': '<html></html>' }, 'no Disallow: / line', 'User-agent: *\nDisallow: /admin\n')
})

test('robots: an empty Disallow value fails the scan', () => {
  expectLeak('robots', { 'index.html': '<html></html>' }, 'no Disallow: / line', 'User-agent: *\nDisallow:\n')
})

test('robots: a robots.txt below the root does not count', () => {
  expectLeak('robots', { 'sub/robots.txt': 'Disallow: /\n' }, 'missing', false)
})

test('robots: a Disallow line with CRLF line endings passes', () => {
  expectClean({ 'index.html': '<html></html>' }, 'User-agent: *\r\nDisallow: /\r\n')
})

// Strings measured in the real build or the model that must never hit.
const TRAPS = [
  ['ssh in a shell word list', '|split|ssh|stat|'],
  ['SVG path numbers that look like a dotted quad', 'M58.06.87.06'],
  ['LikeC4 deployment id k3s.monitoring', 'k3s.monitoring k3s.kube-system k3s.architecture k3s.cert-manager k3s.flux-system'],
  ['namespace title kanban-data', 'kanban-data'],
  ['CSS design token', '--token-color: red; token'],
  ['the word password', 'POST /api/signin {email, password}'],
  ['Java package path with vrudenko', 'src/main/java/com/vrudenko/kanban_board/controller'],
  ['CSS root selector', ':root{color:red}'],
  ['workflow file deploy.yml', 'deploy.yml'],
  ['the word runner', 'runner'],
  ['the word root as a bare word', 'the root element and a.root'],
  ['the word monitoring', 'namespace monitoring'],
  ['uppercase SSH in vendored code and prose', 'the SSH label and an SSH forward with a pinned host key'],
  ['hostname part rud-vlad-473 alone', 'rud-vlad-473'],
  ['Secret name letsencrypt-staging drawn by the model', 'letsencrypt-staging letsencrypt-production'],
  ['Secret name flux-system drawn by the model', 'flux-system'],
  ['Secret name postgres-backup drawn by the model', 'postgres-backup'],
  ['an unrelated word that holds ssh without a space', 'sshd and openssh'],
  ['a version number with three dots', 'v1.2.3'],
  ['a clock time', 'at 10:20 and 10:20:30'],
]

for (const [trap, content] of TRAPS) {
  test(`trap ${trap}: passes the scan`, () => {
    expectClean({ 'assets/page.js': content })
  })
}

test('hostname: the four public hosts pass and are listed', () => {
  const hosts = [
    'kanban-board-rud-vlad-473.duckdns.org',
    'kanban-board-rud-vlad-473-nonprod.duckdns.org',
    'kanban-board-rud-vlad-473-monitoring.duckdns.org',
    'kanban-board-rud-vlad-473-architecture.duckdns.org',
  ]
  const result = expectClean({ 'index.html': hosts.map((host) => `https://${host}/`).join(' ') })
  assert.ok(result.lines.includes(`HOSTS ${[...hosts].sort().join(',')}`), result.stdout)
})

test('hostname: a public host next to a foreign host still fails', () => {
  expectLeak(
    'hostname',
    { 'index.html': 'https://kanban-board-rud-vlad-473.duckdns.org/ https://evil.duckdns.org/' },
    'evil.duckdns.org',
  )
})

test('walk: a target in a nested subdirectory is caught', () => {
  expectLeak('ipv4', { 'a/b/c/deep.js': 'x 10.20.30.40 y' }, '10.20.30.40')
})

test('walk: a target in a file with NUL bytes is caught', () => {
  const content = Buffer.concat([Buffer.from([0, 0, 255, 254]), Buffer.from('10.20.30.40'), Buffer.from([0])])
  expectLeak('ipv4', { 'a/binary.ico': content }, '10.20.30.40')
})

test('walk: a file with high bytes only passes', () => {
  expectClean({ 'a/binary.ico': Buffer.from([0, 255, 254, 128, 200]) })
})

test('report: the line around a match is never printed', () => {
  const result = run([build({ 'index.js': 'BEFORE-CONTEXT 10.20.30.40 AFTER-CONTEXT' })])
  assert.equal(result.status, 1)
  assert.ok(!result.stderr.includes('BEFORE-CONTEXT'))
  assert.ok(!result.stderr.includes('AFTER-CONTEXT'))
})

test('report: a clean run prints a zero row for every target', () => {
  const result = expectClean({ 'index.html': '<html></html>' })
  const rows = result.lines.filter((line) => line.startsWith('TARGET '))
  assert.equal(rows.length, TARGET_COUNT)
  assert.ok(rows.every((line) => line.endsWith('hits=0')))
})

test('report: a leak run still lists every target', () => {
  const result = run([build({ 'index.js': '10.20.30.40' })])
  assert.equal(result.lines.filter((line) => line.startsWith('TARGET ')).length, TARGET_COUNT)
})

test('report: the step summary file receives the report in a fenced block', () => {
  const summary = join(mkdtempSync(join(tmpdir(), 'leak-scan-')), 'summary.md')
  made.push(dirname(summary))
  const result = run([build({ 'index.html': '<html></html>' })], { GITHUB_STEP_SUMMARY: summary })
  assert.equal(result.status, 0)
  const written = readFileSync(summary, 'utf8')
  assert.ok(written.startsWith('```\n') && written.trimEnd().endsWith('```'))
  assert.ok(written.includes(`OK targets=${TARGET_COUNT}`))
})

test('exit: a directory with no files exits 4', () => {
  const dir = mkdtempSync(join(tmpdir(), 'leak-scan-'))
  made.push(dir)
  assert.equal(run([dir]).status, 4)
})

test('exit: a directory that holds only empty subdirectories exits 4', () => {
  const dir = mkdtempSync(join(tmpdir(), 'leak-scan-'))
  made.push(dir)
  mkdirSync(join(dir, 'empty'))
  assert.equal(run([dir]).status, 4)
})

test('exit: a missing path exits 4', () => {
  assert.equal(run([join(tmpdir(), 'leak-scan-does-not-exist')]).status, 4)
})

test('exit: a path that is a file exits 4', () => {
  const dir = build({ 'a.txt': 'x' })
  assert.equal(run([join(dir, 'a.txt')]).status, 4)
})

test('exit: no argument exits 2', () => {
  assert.equal(run([]).status, 2)
})

test('exit: two arguments exit 2', () => {
  const dir = build({ 'a.txt': 'x' })
  assert.equal(run([dir, dir]).status, 2)
})
