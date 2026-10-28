#!/usr/bin/env fish
# Drift drills for sync.mjs. Each drill edits a scratch copy of the code facts and of the model,
# then runs `sync.mjs check --root --model` and reports whether the check caught it. Every drill
# is expected to be caught except D5, a negative control that is documented as not caught, and
# D13b, D16b and D17b, controls that must stay clean. A drill on a k8s fact source is caught with exit 4.
# The real model is never edited: a drill that changes a view changes the scratch copy.
#
# Drills from D12b on are rows of drills.tsv, run by run_drills in row order after the drills before
# them. Before D12b, a drill that is one sed edit on one file is a row of sed-drills.tsv, run by a
# sed_drills call at its place below, and the others are written out in full. Either way the drills
# run in the order drill-output.txt records.
#
# SYNC names another sync.mjs to run. Running the harness once with the script from before a
# rule existed and once with the current one records the drill as not caught and then caught.
# MODEL names another model folder; the not-caught-before half uses the model of the same commit
# as SYNC's script, so the old script never meets a model whose claims it cannot read.

set here (dirname (status filename))
set repo (realpath $here/../..)
set sync $here/sync.mjs
set -q SYNC; and set sync $SYNC
set model $here/model
set -q MODEL; and set model $MODEL
set scratch $here/out/drill-root

function fresh --inherit-variable repo --inherit-variable scratch --inherit-variable model
    test -e $scratch; and trash-put $scratch
    mkdir -p $scratch/src/main/resources/db $scratch/src/main/java/com/vrudenko/kanban_board
    cp -r $repo/src/main/resources/db/migration $scratch/src/main/resources/db/
    cp $repo/src/main/resources/application.properties $scratch/src/main/resources/
    cp -r $repo/src/main/java/com/vrudenko/kanban_board/. $scratch/src/main/java/com/vrudenko/kanban_board/
    cp -r $repo/k8s $scratch/
    # Later elements cite these trees as `source`, and the check reads each cited path.
    mkdir -p $scratch/.github $scratch/infra $scratch/src/main
    cp -r $repo/.github/workflows $scratch/.github/
    cp -r $repo/infra/vm $scratch/infra/
    cp -r $repo/src/main/avro $scratch/src/main/
    cp -r $model $scratch/model
end

function run --inherit-variable sync --inherit-variable scratch
    set out (node $sync check --root $scratch --model $scratch/model 2>&1)
    set code $status
    echo "  exit=$code"
    printf '  %s\n' $out
end

# Adds the element certmgr, anchored on the real k8s/platform/cert-manager directory and linked
# to traefik so it stays drawn, and places its one instance in the namespace node named by $argv[1].
function certmgr_into --inherit-variable scratch
    printf '%s\n' 'model {' "  certmgr = system 'cert-manager' {" '    metadata {' "      source 'k8s/platform/cert-manager'" '    }' '  }' "  traefik -[sync]-> certmgr 'probe'" '}' >$scratch/model/certmgr.c4
    sed -i "s|^\(      $argv[1] = namespace .*{\)\$|\1\n        instanceOf certmgr|" $scratch/model/deployment.c4
end

# Adds the element lonely and one instance of it in the data namespace. With the argument
# `linked` it also gets a relationship, which is what keeps an instance drawn in the view.
function lonely_into --inherit-variable scratch
    set body "  lonely = system 'Lonely'"
    test "$argv[1]" = linked; and set body $body "  traefik -[sync]-> lonely 'probe'"
    printf '%s\n' 'model {' $body '}' >$scratch/model/lonely.c4
    sed -i "s|^\(        instanceOf backup\)\$|\1\n        instanceOf lonely|" $scratch/model/deployment.c4
end

# Adds a relationship from kanban.publisher to postgres titled $argv[1]. It merges onto the
# kanban to postgres instance pair, which already carries the shared title SQL over JDBC.
function labeldrill --inherit-variable scratch
    printf '%s\n' 'model {' "  kanban.publisher -[sync]-> postgres '$argv[1]'" '}' >$scratch/model/labeldrill.c4
end

# Runs every drill of the table named by $argv[1], in row order.
#
# The columns are drill, title, kind, file and value, and a column the kind does not use holds a
# dash. Consecutive rows with the same drill and title are one drill: one header, one fresh copy,
# each row's edit in turn and one run. Paths in file and value are relative to the scratch root.
# The kinds are:
#
# - sed: sed --sandbox edits file in place with value as the expression, so the e, r and w commands are refused
# - write: printf writes value to file as its format, so \n is a newline and a percent sign is written %%
# - delete: trash-put removes file, which may be a file or a directory
# - lonely_into, labeldrill: the helper of that name, with value as its one argument
#
# Each field reaches its command as one argument from a variable that fish never parses again, so
# quotes, dollar signs and backslashes arrive as written. The rows are read whole before the loop,
# so no command in it can read a row from stdin. A missing or empty table, a row that is not five
# non-empty fields, an unknown kind or a drill whose rows are not one block exits 2.
function run_drills --inherit-variable scratch
    set rows (tail -n +2 $argv[1] 2>/dev/null)
    and set -q rows[1]
    or begin
        echo "run_drills: no drill rows in $argv[1]" >&2
        exit 2
    end
    set seen
    set prev
    for row in $rows
        set f (string split \t -- $row)
        if test (count $f) -ne 5 || contains -- '' $f
            echo "run_drills: not 5 non-empty tab-separated fields: $row" >&2
            exit 2
        end
        if test "$f[1]: $f[2]" != "$prev"
            if contains -- $f[1] $seen
                echo "run_drills: $f[1] is not one block of consecutive rows with one title" >&2
                exit 2
            end
            set -a seen $f[1]
            set -q prev[1]; and run
            set prev "$f[1]: $f[2]"
            echo "== $prev"
            fresh
        end
        switch $f[3]
            case sed
                sed -i --sandbox -e $f[5] $scratch/$f[4]
            case write
                printf $f[5] >$scratch/$f[4]
            case delete
                trash-put $scratch/$f[4]
            case lonely_into
                lonely_into $f[5]
            case labeldrill
                labeldrill $f[5]
            case '*'
                echo "run_drills: unknown kind $f[3] in drill $f[1]" >&2
                exit 2
        end
    end
    run
end

# Runs the named drills from their rows in sed-drills.tsv, whose columns are id, title, the file
# to edit relative to the scratch root, and the sed expression. The expression is passed to sed
# as one argument that fish never parses again, so its quotes, dollar signs and backslashes reach sed as written.
function sed_drills --inherit-variable here --inherit-variable scratch
    for id in $argv
        set row (string match -- "$id"\t'*' <$here/sed-drills.tsv)
        or begin
            echo "sed_drills: no row for $id in sed-drills.tsv" >&2
            exit 2
        end
        set f (string split -m3 \t -- $row)
        echo "== $id: $f[2]"
        fresh
        sed -i --sandbox -e $f[4] $scratch/$f[3]
        run
    end
end

echo "== baseline: untouched scratch copy"
fresh
run

echo "== D1: a new migration adds table attachments"
fresh
printf 'CREATE TABLE attachments (id VARCHAR(20) PRIMARY KEY);\n' >$scratch/src/main/resources/db/migration/V10__add_attachments.sql
run

sed_drills D2

echo "== D3: a new k8s Deployment named worker"
fresh
printf 'apiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: worker\n' >$scratch/k8s/base/worker.yaml
run

echo "== D4: a manifest the model cites as its source is deleted"
fresh
trash-put $scratch/k8s/data/postgres/postgres.yaml
run

echo "== D5 (negative control): a controller class is renamed"
fresh
set c (find $scratch/src/main/java -path '*controller*' -name '*.java' | head -1)
mv $c (dirname $c)/RenamedController.java
run

sed_drills D6 D6b

echo "== D7: a migration adds a foreign key by ALTER TABLE"
fresh
printf 'ALTER TABLE subtasks ADD COLUMN owner_id VARCHAR(20) REFERENCES users (id);\n' >$scratch/src/main/resources/db/migration/V11__add_owner.sql
run

echo "== D7b: a migration drops a named foreign key"
fresh
printf 'ALTER TABLE boards DROP CONSTRAINT fk_boards_user;\n' >$scratch/src/main/resources/db/migration/V11__drop_fk.sql
run

echo "== D7c: a migration drops a foreign-key column"
fresh
printf 'ALTER TABLE tasks DROP COLUMN column_id;\n' >$scratch/src/main/resources/db/migration/V11__drop_col.sql
run

sed_drills D8

echo "== D8b: a two-package cycle appears"
fresh
set j (find $scratch/src/main/java/com/vrudenko/kanban_board/exception -name '*.java' | sort | head -1)
sed -i '1a import com.vrudenko.kanban_board.handler.Unused;' $j
run

sed_drills D9 D10 D10b

echo "== D11: an instance is moved into the wrong namespace (backup to kanban-prod)"
fresh
sed -i '/^        instanceOf backup$/d' $scratch/model/deployment.c4
sed -i 's|^\(      prod = namespace .*{\)$|\1\n        instanceOf backup|' $scratch/model/deployment.c4
run

echo "== D15: a new k8s Deployment under k8s/platform"
fresh
printf 'apiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: worker\n' >$scratch/k8s/platform/worker.yaml
run

echo "== D16: a HelmRelease-backed element is drawn where the HelmRelease object lives (cert-manager in flux-system)"
fresh
certmgr_into flux-system
run

echo "== D16b (control): the same element drawn in its targetNamespace (cert-manager)"
fresh
certmgr_into cert-manager
run

sed_drills D12

run_drills $here/drills.tsv

trash-put $scratch
