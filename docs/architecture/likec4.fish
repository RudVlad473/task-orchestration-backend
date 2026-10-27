#!/usr/bin/env fish
# Usage: likec4.fish start|validate|png|site|check|sync
#   start     live-reloading viewer, usually on http://localhost:5173/ (it prints the address it chose)
#   validate  parse and type-check the model
#   png       render every view to out/*.png
#   site      build a static site to out/site
#   check     fail when the model and the code disagree
#   sync      regenerate model/generated.c4 from the code

set here (dirname (status filename))
set bin $here/node_modules/.bin/likec4
set sync $here/sync.mjs

switch $argv[1]
    case start validate png site
        if not test -x $bin
            echo "likec4 is not installed. Run: pnpm --dir docs/architecture install" >&2
            exit 3
        end
end

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
