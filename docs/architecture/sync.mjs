// The seam between the codebase and the LikeC4 model.
//
//   node docs/architecture/sync.mjs generate [--root DIR] [--model DIR]   rewrite generated.c4 from the code
//   node docs/architecture/sync.mjs check    [--root DIR] [--model DIR]   exit 1 when the model and the code disagree
//
// Exit codes: 0 clean, 1 drift, 2 usage, 3 tooling missing (likec4 or yaml not installed),
// 4 a k8s fact source is missing or unreadable.
//
// --root points at a directory laid out like the repo (used by the drift drills, which
// run against a scratch copy so the real code is never edited).
// --model points at the folder holding the .c4 files and generated.c4. The default is the
// model/ folder beside this script. The drills pass a scratch copy, so a drill that edits a
// view never edits the real model.

import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join, posix, relative, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const repo = resolve(here, '../..')
const args = process.argv.slice(2)
const mode = args[0]
const rootIdx = args.indexOf('--root')
const root = rootIdx >= 0 ? resolve(args[rootIdx + 1]) : repo
const modelIdx = args.indexOf('--model')
const modelDir = modelIdx >= 0 ? resolve(args[modelIdx + 1] ?? '') : join(here, 'model')
const generatedFile = join(modelDir, 'generated.c4')

// Both modules load by dynamic import inside the same guard. A static import would fail before
// this guard runs, and a missing install would surface as a stack trace with exit 1, which the
// pre-commit hook reports as model drift instead of missing tooling.
async function loadTooling() {
  let LikeC4
  let parseAllDocuments
  try {
    ;({ LikeC4 } = await import('likec4'))
    ;({ parseAllDocuments } = await import('yaml'))
  } catch (err) {
    if (err?.code !== 'ERR_MODULE_NOT_FOUND') throw err
    const missing = err.message.match(/Cannot find (?:package|module) '([^']+)'/)?.[1] ?? 'a required module'
    console.error(`${missing} is not installed for docs/architecture. Run: pnpm --dir docs/architecture install`)
    process.exit(3)
  }
  return { LikeC4, parseAllDocuments }
}

// The five Kruchten viewpoints. This list and the `tag` lines in model/spec.c4 are the same
// set, and the check below is what keeps a view from sitting outside it.
const VIEWPOINTS = ['logical', 'process', 'development', 'physical', 'scenarios']

// The metadata keys the checks read. The DSL cannot declare metadata keys, so this list is the
// vocabulary: a key outside it is reported, and adding a key means editing it here.
const METADATA_KEYS = ['source', 'workload']

// --- facts read from the code ---------------------------------------------------------

function tables() {
  const dir = 'src/main/resources/db/migration'
  const found = new Map()
  const files = readdirSync(join(root, dir)).filter((f) => f.endsWith('.sql'))
  files.sort((a, b) => Number(a.match(/^V(\d+)/)[1]) - Number(b.match(/^V(\d+)/)[1]))
  for (const f of files) {
    const sql = readFileSync(join(root, dir, f), 'utf8')
    for (const m of sql.matchAll(/^\s*CREATE TABLE\s+(?:IF NOT EXISTS\s+)?"?(\w+)"?/gim)) {
      found.set(m[1], `${dir}/${f}`)
    }
    for (const m of sql.matchAll(/^\s*DROP TABLE\s+(?:IF EXISTS\s+)?"?(\w+)"?/gim)) {
      found.delete(m[1])
    }
  }
  return found
}

// Foreign keys, read statement by statement from the same migrations, in the same numeric order.
// Each CREATE TABLE or ALTER TABLE statement names its child table, and every REFERENCES clause
// inside it is one edge to a parent. The label is the column list of a FOREIGN KEY (...) clause,
// else the column the clause is attached to. A CONSTRAINT name is the edge's key, so a later
// DROP CONSTRAINT removes it, and a later DROP COLUMN removes the edges labelled with that column.
//
// The parser does not follow:
// - an FK made inside a DO block or a function;
// - table or column renames;
// - DROP TABLE. No migration drops a table today. A dropped table's FKs would still be emitted
//   and would point at a missing element, so a DROP TABLE is expected to fail through the model's
//   own validation, not observed. This is a recorded limitation, not a claimed behaviour.
// The name of the column whose definition ends at the end of `text`, a CREATE TABLE prefix: the
// first word after the last comma that sits directly inside the table's own parentheses.
function lastColumnName(text) {
  let depth = 0
  let start = text.indexOf('(') + 1
  for (let i = text.indexOf('('); i < text.length; i++) {
    if (text[i] === '(') depth++
    else if (text[i] === ')') depth--
    else if (text[i] === ',' && depth === 1) start = i + 1
  }
  return text.slice(start).match(/^\s*"?(\w+)"?/)?.[1] ?? ''
}

function foreignKeys() {
  const dir = 'src/main/resources/db/migration'
  const found = []
  const files = readdirSync(join(root, dir)).filter((f) => f.endsWith('.sql'))
  files.sort((a, b) => Number(a.match(/^V(\d+)/)[1]) - Number(b.match(/^V(\d+)/)[1]))
  const ident = '"?(\\w+)"?'
  for (const f of files) {
    const sql = readFileSync(join(root, dir, f), 'utf8').replace(/--[^\n]*/g, '')
    for (const stmt of sql.split(';')) {
      const create = stmt.match(new RegExp(`^\\s*CREATE TABLE\\s+(?:IF NOT EXISTS\\s+)?${ident}`, 'i'))
      const alter = stmt.match(new RegExp(`^\\s*ALTER TABLE\\s+(?:ONLY\\s+)?(?:IF EXISTS\\s+)?${ident}`, 'i'))
      const child = (create ?? alter)?.[1]
      if (!child) continue
      const dropConstraint = stmt.match(new RegExp(`DROP CONSTRAINT\\s+(?:IF EXISTS\\s+)?${ident}`, 'i'))
      if (dropConstraint) {
        for (let i = found.length - 1; i >= 0; i--) {
          if (found[i].child === child && found[i].key === dropConstraint[1]) found.splice(i, 1)
        }
        continue
      }
      const dropColumn = stmt.match(new RegExp(`DROP COLUMN\\s+(?:IF EXISTS\\s+)?${ident}`, 'i'))
      if (dropColumn) {
        for (let i = found.length - 1; i >= 0; i--) {
          if (found[i].child === child && found[i].label === dropColumn[1]) found.splice(i, 1)
        }
        continue
      }
      for (const ref of stmt.matchAll(new RegExp(`REFERENCES\\s+${ident}`, 'gi'))) {
        const before = stmt.slice(0, ref.index)
        const table = before.match(/(?:CONSTRAINT\s+"?(\w+)"?\s+)?FOREIGN\s+KEY\s*\(([^)]*)\)\s*$/i)
        let label
        if (table) label = table[2].split(',').map((c) => c.trim().replace(/"/g, '')).join(', ')
        else if (alter) label = before.match(new RegExp(`ADD\\s+(?:COLUMN\\s+)?(?:IF NOT EXISTS\\s+)?${ident}[^,]*$`, 'i'))?.[1] ?? ''
        else label = lastColumnName(before)
        found.push({ child, parent: ref[1], label, key: table?.[1] })
      }
    }
  }
  const seen = new Set()
  return found
    .filter((e) => (seen.has(`${e.child}\0${e.parent}\0${e.label}`) ? false : seen.add(`${e.child}\0${e.parent}\0${e.label}`)))
    .sort((a, b) => a.child.localeCompare(b.child) || a.parent.localeCompare(b.parent) || a.label.localeCompare(b.label))
}

// The top-level packages and the import edges between them, read the way
// .planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/verify/pkg.fish reads them.
// A package is a folder directly under the application's root package. An edge p to q exists when
// some .java file under p has a line `import com.vrudenko.kanban_board.q...` or the static form,
// and q is not p.
//
// The regex takes a lowercase package segment, so an import of a class in the root package
// (KanbanBoardApplication) gives no edge. No source file imports a root-level class today, so no
// fixture exercises that exclusion.
//
// The parser does not follow a fully qualified class name used in code without an import line.
const APP_PACKAGE_DIR = 'src/main/java/com/vrudenko/kanban_board'
const IMPORT_LINE = /^import (?:static )?com\.vrudenko\.kanban_board\.([a-z_]+)/gm

function javaFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const p = join(dir, e.name)
    if (e.isDirectory()) return javaFiles(p)
    return e.name.endsWith('.java') ? [p] : []
  })
}

function packages() {
  const base = join(root, APP_PACKAGE_DIR)
  const names = readdirSync(base, { withFileTypes: true })
    .filter((e) => e.isDirectory())
    .map((e) => e.name)
    .sort()
  const edges = new Set()
  for (const p of names) {
    for (const file of javaFiles(join(base, p))) {
      for (const m of readFileSync(file, 'utf8').matchAll(IMPORT_LINE)) {
        if (m[1] !== p) edges.add(`${p}\0${m[1]}`)
      }
    }
  }
  return { names, edges: [...edges].map((e) => e.split('\0')).sort((a, b) => a[0].localeCompare(b[0]) || a[1].localeCompare(b[1])) }
}

// Strongly connected components of two or more packages (Tarjan). A one-package component with
// no self edge is not a cycle, and packages() never emits a self edge.
function cycles({ names, edges }) {
  const next = new Map(names.map((n) => [n, []]))
  for (const [p, q] of edges) next.get(p)?.push(q)
  const index = new Map()
  const low = new Map()
  const onStack = new Set()
  const stack = []
  const groups = []
  const visit = (v) => {
    index.set(v, index.size)
    low.set(v, index.get(v))
    stack.push(v)
    onStack.add(v)
    for (const w of next.get(v) ?? []) {
      if (!index.has(w)) {
        visit(w)
        low.set(v, Math.min(low.get(v), low.get(w)))
      } else if (onStack.has(w)) {
        low.set(v, Math.min(low.get(v), index.get(w)))
      }
    }
    if (low.get(v) === index.get(v)) {
      const group = []
      let w
      do {
        w = stack.pop()
        onStack.delete(w)
        group.push(w)
      } while (w !== v)
      if (group.length > 1) groups.push(group.sort())
    }
  }
  for (const n of names) if (!index.has(n)) visit(n)
  return groups.sort((a, b) => a[0].localeCompare(b[0]))
}

function cycleLine(pkgs) {
  const groups = cycles(pkgs)
  return `package cycles (reported, not gated): ${groups.length ? groups.map((g) => g.join(', ')).join(' | ') : 'none'}`
}

function topics() {
  const src = readFileSync(join(root, 'src/main/java/com/vrudenko/kanban_board/constant/KafkaTopics.java'), 'utf8')
  return new Set([...src.matchAll(/String\s+\w+\s*=\s*"([^"]+)"/g)].map((m) => m[1]))
}

// The Kind/name of every Deployment, StatefulSet, CronJob and HelmRelease under k8s/base,
// k8s/data, k8s/platform and k8s/monitoring. k8s/overlays holds patches and routes, not
// workloads. From k8s/flux-system exactly one file is read, gotk-components.yaml, because it holds
// the Flux controller Deployments. controller-resources.yaml beside it is not read: it repeats the
// six names as a strategic-merge patch, so reading it would let a rename in the generator file
// pass, the old name still being found in the patch.
const WORKLOAD_DIRS = ['k8s/base/', 'k8s/data/', 'k8s/platform/', 'k8s/monitoring/', 'k8s/flux-system/gotk-components.yaml']
const WORKLOAD_KINDS = new Set(['Deployment', 'StatefulSet', 'CronJob', 'HelmRelease'])

function workloads(docs) {
  const out = new Set()
  for (const { file, o } of docs) {
    if (!WORKLOAD_DIRS.some((d) => file.startsWith(d))) continue
    if (WORKLOAD_KINDS.has(o.kind) && typeof o.metadata?.name === 'string') out.add(`${o.kind}/${o.metadata.name}`)
  }
  return out
}

// The k8s trees the readers depend on. The namespace reader walks the whole k8s tree, so a
// missing subtree does not fail by itself: removing k8s/monitoring or k8s/flux-system changes no
// finding, because k8s/platform alone defines all seven namespaces. A check that reads less than
// it should must not report clean, so each tree is required to exist before any manifest is read.
const K8S_DIRS = ['k8s/base', 'k8s/data', 'k8s/platform', 'k8s/overlays', 'k8s/monitoring', 'k8s/flux-system']

// Every non-empty YAML document under k8s/, parsed once per check, in sorted name order. A
// document with parse errors stops the check with exit 4: the parser reports errors on the
// document instead of throwing, so a reader that skipped them would half-read a broken manifest.
function k8sDocs(parseAllDocuments) {
  const docs = []
  const walk = (dir) => {
    const entries = readdirSync(dir, { withFileTypes: true }).sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))
    for (const e of entries) {
      const p = join(dir, e.name)
      if (e.isDirectory()) walk(p)
      else if (/\.ya?ml$/.test(e.name)) {
        const file = relative(root, p).split(sep).join('/')
        for (const doc of parseAllDocuments(readFileSync(p, 'utf8'))) {
          if (doc.errors.length) {
            const [first] = doc.errors
            console.error(`k8s fact source unreadable: ${file}: ${first.code} ${first.message.split('\n')[0]}`)
            process.exit(4)
          }
          const o = doc.toJS()
          if (o && typeof o === 'object') docs.push({ file, o })
        }
      }
    }
  }
  walk(join(root, 'k8s'))
  return docs
}

// The namespaces the manifests define, each mapped to the first file that names it: the name of
// every Namespace document, plus any string value under a `namespace` or `targetNamespace` key at
// any depth. Only strings count, because CRD schemas hold `namespace` keys whose value is an
// object. targetNamespace is needed because cert-manager appears under no other key.
function manifestNamespaces(docs) {
  const found = new Map()
  const add = (ns, file) => {
    if (!found.has(ns)) found.set(ns, file)
  }
  const visit = (node, file) => {
    if (Array.isArray(node)) {
      for (const v of node) visit(v, file)
    } else if (node && typeof node === 'object') {
      for (const [k, v] of Object.entries(node)) {
        if ((k === 'namespace' || k === 'targetNamespace') && typeof v === 'string') add(v, file)
        visit(v, file)
      }
    }
  }
  for (const { file, o } of docs) {
    if (o.kind === 'Namespace' && typeof o.metadata?.name === 'string') add(o.metadata.name, file)
    visit(o, file)
  }
  return found
}

// Maps (file, document) to the namespaces that document lands in on the cluster.
//
// A kustomization with a top-level `namespace` stamps it on every file it reaches through
// `resources`, directly or through a nested kustomization directory, and the outermost one wins as
// in kustomize. Only a kustomize.config.k8s.io Kustomization is followed. A Flux Kustomization's
// spec.targetNamespace is not modelled, and a HelmRelease is placed by its declared
// spec.targetNamespace, never by the objects its chart renders.
//
// Two Flux kinds break the inherited-then-own order. A source object (HelmRepository and its
// siblings) only tells Flux where to fetch from and sits in flux-system by convention, so it
// lands nowhere. A HelmRelease with spec.targetNamespace lands there, because that is where the
// chart's workloads run and kustomize rewrites metadata.namespace, not spec.targetNamespace.
function placements(docs) {
  const kustomizations = new Map()
  for (const { file, o } of docs) {
    if (typeof o.apiVersion === 'string' && o.apiVersion.startsWith('kustomize.config.k8s.io') && o.kind === 'Kustomization') {
      kustomizations.set(posix.dirname(file), o)
    }
  }
  // Calls onFile for each file a kustomization reaches and onDir for each nested kustomization
  // directory, skipping an entry that is not a string, is absolute or leaves k8s/.
  const walk = (dir, onFile, onDir, visited = new Set()) => {
    if (visited.has(dir)) return
    visited.add(dir)
    const resources = kustomizations.get(dir)?.resources
    if (!Array.isArray(resources)) return
    for (const entry of resources) {
      if (typeof entry !== 'string' || posix.isAbsolute(entry)) continue
      const target = posix.normalize(posix.join(dir, entry))
      if (target !== 'k8s' && !target.startsWith('k8s/')) continue
      if (kustomizations.has(target)) {
        onDir(target)
        walk(target, onFile, onDir, visited)
      } else onFile(target)
    }
  }
  const named = [...kustomizations].filter(([, k]) => typeof k.namespace === 'string')
  const nested = new Set()
  for (const [dir] of named) walk(dir, () => {}, (d) => nested.add(d))
  const inherited = new Map()
  for (const [dir, k] of named) {
    if (nested.has(dir)) continue
    walk(
      dir,
      (file) => inherited.set(file, (inherited.get(file) ?? new Set()).add(k.namespace)),
      () => {},
    )
  }
  return (file, o) => {
    const api = typeof o.apiVersion === 'string' ? o.apiVersion : ''
    if (api.startsWith('source.toolkit.fluxcd.io/')) return []
    if (o.kind === 'HelmRelease' && api.startsWith('helm.toolkit.fluxcd.io/') && typeof o.spec?.targetNamespace === 'string') {
      return [o.spec.targetNamespace]
    }
    if (inherited.has(file)) return [...inherited.get(file)]
    return typeof o.metadata?.namespace === 'string' ? [o.metadata.namespace] : []
  }
}

// For each element that has deployment instances, the namespaces its instances sit in must equal
// the namespaces its anchor documents land in. The anchors are the documents named by its
// `workload` metadata (Kind/name), else the documents at or under each `k8s/` path in its
// `source`. An element with no anchor document is not checked.
function checkPlacement(computed, docs, problems) {
  const placeOf = placements(docs)
  const byElement = new Map()
  for (const i of computed.deployment.instances()) {
    const ns = [...i.ancestors()].find((a) => a.kind === 'namespace')?.title
    if (!byElement.has(i.element.id)) byElement.set(i.element.id, { element: i.element, namespaces: new Set() })
    if (ns) byElement.get(i.element.id).namespaces.add(ns)
  }
  for (const id of [...byElement.keys()].sort()) {
    const { element, namespaces } = byElement.get(id)
    const workloads = [element.getMetadata('workload')].flat().filter(Boolean)
    const sources = [element.getMetadata('source')].flat().filter((s) => typeof s === 'string' && s.startsWith('k8s/'))
    const anchors = docs.filter(({ file, o }) =>
      workloads.length ? workloads.includes(`${o.kind}/${o.metadata?.name}`) : sources.some((s) => file === s || file.startsWith(`${s}/`)),
    )
    if (!anchors.length) continue
    const expected = new Set(anchors.flatMap(({ file, o }) => placeOf(file, o)))
    const have = [...namespaces].sort().join(',')
    const want = [...expected].sort().join(',')
    if (have !== want) problems.push(`placement: ${id} is deployed in [${have}] in the model, manifests place it in [${want}]`)
  }
}

// Reports every metadata key outside METADATA_KEYS on elements, deployment nodes, instances and
// relationships, each in id order. A relationship's own id is a hash, so it is written as
// `source -> target`. A misspelled `source` would otherwise skip the source-exists check silently.
function checkMetadataKeys(computed, problems) {
  const surfaces = [
    ['element', [...computed.elements()].map((x) => [x.id, x])],
    ['deployment node', [...computed.deployment.nodes()].map((x) => [x.id, x])],
    ['instance', [...computed.deployment.instances()].map((x) => [x.id, x])],
    ['relationship', [...computed.relationships()].map((x) => [`${x.source.id} -> ${x.target.id}`, x])],
  ]
  for (const [surface, items] of surfaces) {
    for (const [id, item] of items.sort((a, b) => a[0].localeCompare(b[0]))) {
      for (const key of Object.keys(item.getMetadata() ?? {}).sort()) {
        if (!METADATA_KEYS.includes(key)) {
          problems.push(`${surface} ${id} carries metadata key '${key}', not in the allowlist (${METADATA_KEYS.join(', ')})`)
        }
      }
    }
  }
}

// Every deployment instance must be drawn in some deployment view. LikeC4 leaves an instance
// with no relationship out of a view that includes it, and the model still validates. Only
// instances are required: the empty namespace nodes are undrawn by design.
function checkViewMembership(computed, problems) {
  const drawn = new Set()
  for (const view of computed.views()) {
    if (!view.isDeploymentView?.()) continue
    for (const n of view.nodes()) if (n.kind === 'instance') drawn.add(n.id)
  }
  const ids = [...computed.deployment.instances()].map((i) => i.id).sort()
  for (const id of ids) if (!drawn.has(id)) problems.push(`instance ${id} appears in no deployment view`)
}

// A deployment edge merges every logical relationship between one instance pair. LikeC4 prints
// the placeholder [...] as its label when those relationships have different titles. The only
// fix measured in 1.60 is one shared title per pair, so a pair that gains a differently titled
// relationship brings the placeholder back and is reported here.
function checkEdgeLabels(computed, problems) {
  const views = [...computed.views()].filter((v) => v.isDeploymentView?.()).sort((a, b) => a.id.localeCompare(b.id))
  for (const view of views) {
    const edges = [...view.edges()].map((e) => ({ line: `${e.source.id} -> ${e.target.id}`, label: e.label }))
    for (const e of edges.sort((a, b) => a.line.localeCompare(b.line))) {
      if (e.label === '[...]') problems.push(`view ${view.id}: edge ${e.line} has merged label '[...]'`)
    }
  }
}

// A scenario view draws at most 5 participants. 5 is the width signin-ok already has, kept as the
// cap while the LikeC4 legibility rule is undefined.
function checkParticipants(computed, problems) {
  const views = [...computed.views()].filter((v) => v.isDynamicView?.()).sort((a, b) => a.id.localeCompare(b.id))
  for (const view of views) {
    const count = [...view.nodes()].length
    if (count > 5) problems.push(`dynamic view ${view.id} has ${count} participants, more than 5`)
  }
}

// An element that claims a workload needs a deployment instance. checkPlacement only sees elements
// that have instances, so deleting the last instanceOf of a workload element used to pass. The
// rule keys on `workload` alone: children such as kanban.api carry sources and no instance by design.
function checkDeployedWorkloads(computed, problems) {
  const deployed = new Set([...computed.deployment.instances()].map((i) => i.element.id))
  const elements = [...computed.elements()].sort((a, b) => a.id.localeCompare(b.id))
  for (const e of elements) {
    const workloads = [e.getMetadata('workload')].flat().filter(Boolean)
    if (workloads.length && !deployed.has(e.id)) {
      problems.push(`element ${e.id} claims workload ${workloads.join(', ')} but has no deployment instance`)
    }
  }
}

// --- generation -----------------------------------------------------------------------

// One function per fact source, each returning the lines it contributes, so a source added
// later extends render() with one more call instead of one more block in a long body.
function renderTables() {
  const lines = []
  for (const [name, file] of [...tables()].sort()) {
    lines.push(
      `    table_${name} = table '${name}' {`,
      '      #generated',
      `      metadata { source '${file}' }`,
      '    }',
    )
  }
  return lines
}

function renderForeignKeys() {
  const lines = []
  for (const fk of foreignKeys()) {
    lines.push(`  postgres.table_${fk.child} -[fk]-> postgres.table_${fk.parent} '${fk.label}' {`, '    #generated', '  }')
  }
  return lines
}

function renderPackages(pkgs) {
  const lines = []
  for (const name of pkgs.names) {
    lines.push(
      `    pkg_${name} = component '${name}' {`,
      '      #generated',
      `      metadata { source '${APP_PACKAGE_DIR}/${name}' }`,
      '    }',
    )
  }
  return lines
}

function renderPackageEdges(pkgs) {
  const lines = []
  for (const [p, q] of pkgs.edges) {
    lines.push(`  kanban.pkg_${p} -[imports]-> kanban.pkg_${q} {`, '    #generated', '  }')
  }
  return lines
}

function render() {
  const pkgs = packages()
  return [
    '// GENERATED by docs/architecture/sync.mjs. Do not edit.',
    '// Source of truth: the CREATE TABLE and REFERENCES statements in src/main/resources/db/migration,',
    '// and the import statements in src/main/java.',
    'model {',
    '  extend postgres {',
    ...renderTables(),
    '  }',
    ...renderForeignKeys(),
    '  extend kanban {',
    ...renderPackages(pkgs),
    '  }',
    ...renderPackageEdges(pkgs),
    '}',
    '',
  ].join('\n')
}

// --- checks ---------------------------------------------------------------------------

async function check() {
  // Load likec4 first, so missing tooling wins over drift and always prints the install command.
  const { LikeC4, parseAllDocuments } = await loadTooling()
  // Exit 4 keeps a missing fact source out of the hook's drift branch, whose advice to update the
  // model would be wrong for a missing directory.
  const missing = K8S_DIRS.filter((dir) => !existsSync(join(root, dir)))
  for (const dir of missing) console.error(`k8s fact source missing: ${dir}`)
  if (missing.length) process.exit(4)
  const docs = k8sDocs(parseAllDocuments)
  const problems = []

  const want = render()
  const have = existsSync(generatedFile) ? readFileSync(generatedFile, 'utf8') : ''
  if (want !== have) problems.push('generated.c4 is stale: run `sync.mjs generate` (the migrations or the package imports changed)')

  const likec4 = await LikeC4.fromWorkspace(modelDir, { printErrors: false, throwIfInvalid: false, logger: false })
  const invalid = likec4.getErrors()
  if (invalid.length) problems.push(`model does not validate: ${invalid.map((e) => e.message).join('; ')}`)
  const computed = await likec4.computedModel()
  const elements = [...computed.elements()]

  // Every view except index belongs to exactly one Kruchten viewpoint. index is the system
  // context that opens the set, so it sits outside the 4+1 and carries no viewpoint tag.
  // views() returns every kind of view, so deployment and dynamic views are held to the rule too.
  for (const view of computed.views()) {
    if (view.id === 'index') continue
    const own = [...view.tags].filter((t) => VIEWPOINTS.includes(t))
    if (own.length === 0) problems.push(`view ${view.id} carries no viewpoint tag`)
    else if (own.length > 1) problems.push(`view ${view.id} carries ${own.length} viewpoint tags`)
  }

  // Each of the five viewpoints must have at least one view, so retagging or deleting the
  // last view of a viewpoint leaves a hole in the 4+1 set that the check reports.
  const covered = new Set([...computed.views()].flatMap((v) => [...v.tags]))
  for (const vp of VIEWPOINTS) if (!covered.has(vp)) problems.push(`viewpoint ${vp} has no view`)

  const modelTopics = new Set(elements.filter((e) => e.kind === 'topic').map((e) => e.title))
  for (const t of topics()) if (!modelTopics.has(t)) problems.push(`topic ${t} is in KafkaTopics.java but not in the model`)
  for (const t of modelTopics) if (!topics().has(t)) problems.push(`topic ${t} is in the model but not in KafkaTopics.java`)

  const modelWorkloads = new Set(elements.flatMap((e) => [e.getMetadata('workload')].flat().filter(Boolean)))
  const manifestWorkloads = workloads(docs)
  for (const w of manifestWorkloads) if (!modelWorkloads.has(w)) problems.push(`k8s ${w} has no element carrying metadata workload '${w}'`)
  for (const w of modelWorkloads) if (!manifestWorkloads.has(w)) problems.push(`model claims workload ${w} but k8s/ has no such manifest`)

  for (const e of elements) {
    const src = e.getMetadata('source')
    if (src && !existsSync(join(root, src))) problems.push(`${e.id}: metadata source '${src}' does not exist`)
  }

  // A namespace the manifests define needs a namespace node in the deployment model, and a
  // namespace node needs a manifest that defines its title.
  const defined = manifestNamespaces(docs)
  const nsNodes = [...computed.deployment.nodes()].filter((n) => n.kind === 'namespace')
  const nodeTitles = new Set(nsNodes.map((n) => n.title))
  for (const ns of [...defined.keys()].sort()) {
    if (!nodeTitles.has(ns)) problems.push(`namespace ${ns} is defined in ${defined.get(ns)} but has no namespace node in the deployment model`)
  }
  for (const n of [...nsNodes].sort((a, b) => a.title.localeCompare(b.title))) {
    if (!defined.has(n.title)) problems.push(`deployment node ${n.id} names namespace '${n.title}', which no manifest under k8s/ defines`)
  }

  checkMetadataKeys(computed, problems)
  checkPlacement(computed, docs, problems)
  checkViewMembership(computed, problems)
  checkEdgeLabels(computed, problems)
  checkParticipants(computed, problems)
  checkDeployedWorkloads(computed, problems)

  const pkgs = packages()
  const tally =`${elements.length} elements, ${tables().size} tables, ${foreignKeys().length} foreign keys, ${pkgs.names.length} packages, ${pkgs.edges.length} package edges, ${topics().size} topics, ${manifestWorkloads.size} workloads`
  // Cycles are reported on both outcomes and never add a problem, so they cannot change the exit code.
  if (problems.length) {
    console.error(`DRIFT (${tally}):\n- ${problems.join('\n- ')}\n${cycleLine(pkgs)}`)
    process.exit(1)
  }
  console.log(`OK (${tally})\n${cycleLine(pkgs)}`)
}

if ((mode === 'generate' || mode === 'check') && !existsSync(modelDir)) {
  console.error(`model folder ${modelDir} does not exist\nusage: sync.mjs generate|check [--root DIR] [--model DIR]`)
  process.exit(2)
}

if (mode === 'generate') {
  writeFileSync(generatedFile, render())
  console.log(`wrote ${generatedFile} (${tables().size} tables)`)
} else if (mode === 'check') {
  await check()
} else {
  console.error('usage: sync.mjs generate|check [--root DIR] [--model DIR]')
  process.exit(2)
}
