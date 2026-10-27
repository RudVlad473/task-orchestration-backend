// The seam between the codebase and the LikeC4 model.
//
//   node docs/architecture/sync.mjs generate [--root DIR] [--model DIR]   rewrite generated.c4 from the code
//   node docs/architecture/sync.mjs check    [--root DIR] [--model DIR]   exit 1 when the model and the code disagree
//
// Exit codes: 0 clean, 1 drift, 2 usage, 3 tooling missing (likec4 not installed).
//
// --root points at a directory laid out like the repo (used by the drift drills, which
// run against a scratch copy so the real code is never edited).
// --model points at the folder holding the .c4 files and generated.c4. The default is the
// model/ folder beside this script. The drills pass a scratch copy, so a drill that edits a
// view never edits the real model.

import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
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

async function loadLikeC4() {
  try {
    return await import('likec4')
  } catch (err) {
    if (err?.code !== 'ERR_MODULE_NOT_FOUND') throw err
    console.error('likec4 is not installed for docs/architecture. Run: pnpm --dir docs/architecture install')
    process.exit(3)
  }
}

// The five Kruchten viewpoints. This list and the `tag` lines in model/spec.c4 are the same
// set, and the check below is what keeps a view from sitting outside it.
const VIEWPOINTS = ['logical', 'process', 'development', 'physical', 'scenarios']

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

function topics() {
  const src = readFileSync(join(root, 'src/main/java/com/vrudenko/kanban_board/constant/KafkaTopics.java'), 'utf8')
  return new Set([...src.matchAll(/String\s+\w+\s*=\s*"([^"]+)"/g)].map((m) => m[1]))
}

function workloads() {
  const out = new Set()
  const walk = (d) => {
    for (const e of readdirSync(d, { withFileTypes: true })) {
      const p = join(d, e.name)
      if (e.isDirectory()) walk(p)
      else if (/\.ya?ml$/.test(e.name)) {
        for (const doc of readFileSync(p, 'utf8').split(/^---\s*$/m)) {
          const kind = doc.match(/^kind:\s*(Deployment|StatefulSet|CronJob)\s*$/m)
          const name = doc.match(/^metadata:\s*\n\s+name:\s*(\S+)/m)
          if (kind && name) out.add(`${kind[1]}/${name[1]}`)
        }
      }
    }
  }
  for (const d of ['k8s/base', 'k8s/data']) walk(join(root, d))
  return out
}

// --- generation -----------------------------------------------------------------------

function render() {
  const lines = [
    '// GENERATED by docs/architecture/sync.mjs. Do not edit.',
    '// Source of truth: the CREATE TABLE and REFERENCES statements in src/main/resources/db/migration.',
    'model {',
    '  extend postgres {',
  ]
  for (const [name, file] of [...tables()].sort()) {
    lines.push(
      `    table_${name} = table '${name}' {`,
      '      #generated',
      `      metadata { source '${file}' }`,
      '    }',
    )
  }
  lines.push('  }')
  for (const fk of foreignKeys()) {
    lines.push(`  postgres.table_${fk.child} -[fk]-> postgres.table_${fk.parent} '${fk.label}' {`, '    #generated', '  }')
  }
  lines.push('}', '')
  return lines.join('\n')
}

// --- checks ---------------------------------------------------------------------------

async function check() {
  // Load likec4 first, so missing tooling wins over drift and always prints the install command.
  const { LikeC4 } = await loadLikeC4()
  const problems = []

  const want = render()
  const have = existsSync(generatedFile) ? readFileSync(generatedFile, 'utf8') : ''
  if (want !== have) problems.push('generated.c4 is stale: run `sync.mjs generate` (the migrations changed)')

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

  const modelTopics = new Set(elements.filter((e) => e.kind === 'topic').map((e) => e.title))
  for (const t of topics()) if (!modelTopics.has(t)) problems.push(`topic ${t} is in KafkaTopics.java but not in the model`)
  for (const t of modelTopics) if (!topics().has(t)) problems.push(`topic ${t} is in the model but not in KafkaTopics.java`)

  const modelWorkloads = new Set(elements.flatMap((e) => [e.getMetadata('workload')].flat().filter(Boolean)))
  for (const w of workloads()) if (!modelWorkloads.has(w)) problems.push(`k8s ${w} has no element carrying metadata workload '${w}'`)
  for (const w of modelWorkloads) if (!workloads().has(w)) problems.push(`model claims workload ${w} but k8s/ has no such manifest`)

  for (const e of elements) {
    const src = e.getMetadata('source')
    if (src && !existsSync(join(root, src))) problems.push(`${e.id}: metadata source '${src}' does not exist`)
  }

  const tally = `${elements.length} elements, ${tables().size} tables, ${foreignKeys().length} foreign keys, ${topics().size} topics, ${workloads().size} workloads`
  if (problems.length) {
    console.error(`DRIFT (${tally}):\n- ${problems.join('\n- ')}`)
    process.exit(1)
  }
  console.log(`OK (${tally})`)
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
