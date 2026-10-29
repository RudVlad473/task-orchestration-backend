// Description inventory for the Phase 3.1 audit.
//
// Usage: node 03.1-inventory.mjs [model-dir]
//
// The default model directory is docs/architecture/model, resolved five directories above this
// script. The script prints one tab-separated row per element, deployment node and view, after a
// header line. It exits 1, with the model errors on stderr, when the model does not validate, so an
// invalid model never gives a partial inventory.
//
// Columns:
// - surface: element, deployment node or view;
// - id;
// - generated: Y when an element carries the generated tag, N otherwise, - for nodes and views;
// - words: whitespace tokens of the trimmed text, 0 when the text is empty;
// - period: Y or N for the trailing period, - when the text is empty;
// - text: the description, with tabs and newlines collapsed to one space.

import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const likec4Url = new URL('../../../../../docs/architecture/node_modules/likec4/dist/index.mjs', import.meta.url)
const { LikeC4 } = await import(likec4Url.href)

const modelDir = resolve(process.argv[2] ?? resolve(here, '../../../../../docs/architecture/model'))
const lc = await LikeC4.fromWorkspace(modelDir, { printErrors: false, throwIfInvalid: false, logger: false })
const errors = lc.getErrors()
if (errors.length > 0) {
  for (const e of errors) console.error(`${e.sourceFsPath ?? ''}:${e.line ?? ''} ${e.message}`)
  process.exit(1)
}
const computed = await lc.computedModel()

const byId = (a, b) => a.id.localeCompare(b.id)
const rows = []
const add = (surface, item, generated) => {
  const raw = item.description?.nonEmpty ? item.description.text : ''
  const text = (raw ?? '').replace(/[\t\r\n]+/g, ' ')
  const trimmed = text.trim()
  const words = trimmed ? trimmed.split(/\s+/).length : 0
  const period = trimmed ? (trimmed.endsWith('.') ? 'Y' : 'N') : '-'
  rows.push([surface, item.id, generated, words, period, text].join('\t'))
}

for (const e of [...computed.elements()].sort(byId)) add('element', e, [...e.tags].includes('generated') ? 'Y' : 'N')
for (const n of [...computed.deployment.nodes()].sort(byId)) add('deployment node', n, '-')
for (const v of [...computed.views()].sort(byId)) add('view', v, '-')

console.log(['surface', 'id', 'generated', 'words', 'period', 'text'].join('\t'))
for (const r of rows) console.log(r)
