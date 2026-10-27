#!/usr/bin/env fish
# V5: derive top-level-package dependency edges from import statements, then ask tsort (coreutils)
# whether the graph is acyclic. Output: edge list with file counts, then the tsort verdict.

set here (dirname (status filename))
set repo (realpath $here/../../../..)
set base $repo/src/main/java/com/vrudenko/kanban_board

set edges
for dir in $base/*/
    set p (basename $dir)
    for q in (rg -N --no-filename -o '^import (static )?com\.vrudenko\.kanban_board\.([a-z_]+)' -r '$2' $dir --glob '*.java' | sort | uniq)
        if test $q != $p
            set -a edges "$p $q"
        end
    end
end

echo "== edges (importer imported), $(count $edges) total"
printf '%s\n' $edges | sort

echo "== tsort verdict"
printf '%s\n' $edges | tsort >/dev/null 2>$here/tsort-stderr.txt
set verdict $status
if test $verdict -eq 0
    echo "acyclic"
else
    echo "CYCLIC (tsort exit $verdict)"
    cat $here/tsort-stderr.txt
end
