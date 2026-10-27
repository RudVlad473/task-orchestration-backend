#!/usr/bin/env fish
# Usage: likec4.fish start|validate|png|site|check|sync
#   start     live-reloading viewer on http://localhost:61000 (edit a .c4 file, the diagram moves)
#   validate  parse and type-check the model
#   png       render every view to out/*.png
#   site      build a static site to out/site
#   check     fail when the model and the code disagree
#   sync      regenerate model/generated.c4 from the code

set here (dirname (status filename))
set spikes (realpath $here/..)
set bin $spikes/005-likec4-toolchain-and-views/node_modules/.bin/likec4
set sync $spikes/007-likec4-code-sync-seam/sync.mjs

switch $argv[1]
    case start
        $bin start $here/model
    case validate
        $bin validate $here/model
    case png
        $bin export png -o $here/out $here/model
    case site
        $bin build -o $here/out/site $here/model
    case check
        node $sync check
    case sync
        node $sync generate
    case '*'
        echo "usage: likec4.fish start|validate|png|site|check|sync" >&2
        exit 2
end
