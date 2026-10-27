#!/usr/bin/env fish
# Helper for the verification experiments: run the pinned likec4 in docs/architecture against a workspace under verify/.
# Usage: lc4.fish <workspace-dir-name> <likec4 args before the path...>
# Output is stripped of colour codes and stack frames.

set here (dirname (status filename))
set bin (realpath $here/../../../../docs/architecture/node_modules/.bin/likec4)
set ws $argv[1]
set rest $argv[2..-1]

$bin $rest[1] $rest[2..-1] $here/$ws 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | rg -v '^\s+at '
