#!/usr/bin/env fish
# Drift drills for sync.mjs. Each drill edits a scratch copy of the code facts and of the model,
# then runs `sync.mjs check --root --model` and reports whether the check caught it. Every drill
# is expected to be caught except D5, a negative control that is documented as not caught.
# The real model is never edited: a drill that changes a view changes the scratch copy.

set here (dirname (status filename))
set repo (realpath $here/../..)
set sync $here/sync.mjs
set scratch $here/out/drill-root

function fresh --inherit-variable repo --inherit-variable scratch --inherit-variable here
    test -e $scratch; and trash-put $scratch
    mkdir -p $scratch/src/main/resources/db $scratch/src/main/java/com/vrudenko/kanban_board $scratch/k8s
    cp -r $repo/src/main/resources/db/migration $scratch/src/main/resources/db/
    cp -r $repo/src/main/java/com/vrudenko/kanban_board/constant $scratch/src/main/java/com/vrudenko/kanban_board/
    cp -r $repo/src/main/java/com/vrudenko/kanban_board/controller $scratch/src/main/java/com/vrudenko/kanban_board/
    cp -r $repo/src/main/java/com/vrudenko/kanban_board/event $scratch/src/main/java/com/vrudenko/kanban_board/
    cp -r $repo/src/main/java/com/vrudenko/kanban_board/activitylog $scratch/src/main/java/com/vrudenko/kanban_board/
    cp -r $repo/k8s/base $repo/k8s/data $repo/k8s/platform $scratch/k8s/
    cp -r $here/model $scratch/model
end

function run --inherit-variable sync --inherit-variable scratch
    set out (node $sync check --root $scratch --model $scratch/model 2>&1)
    set code $status
    echo "  exit=$code"
    printf '  %s\n' $out
end

echo "== baseline: untouched scratch copy"
fresh
run

echo "== D1: a new migration adds table attachments"
fresh
printf 'CREATE TABLE attachments (id VARCHAR(20) PRIMARY KEY);\n' >$scratch/src/main/resources/db/migration/V10__add_attachments.sql
run

echo "== D2: a new Kafka topic constant"
fresh
sed -i 's|^}|    public static final String AUDIT = "kanban.audit";\n}|' $scratch/src/main/java/com/vrudenko/kanban_board/constant/KafkaTopics.java
run

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

echo "== D6: a view loses its viewpoint tag"
fresh
sed -i '/^    #process$/d' $scratch/model/views.c4
run

echo "== D6b: a view carries two viewpoint tags"
fresh
sed -i 's|^    #process$|    #process\n    #logical|' $scratch/model/views.c4
run

trash-put $scratch
