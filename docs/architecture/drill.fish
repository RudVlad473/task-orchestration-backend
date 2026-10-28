#!/usr/bin/env fish
# Drift drills for sync.mjs. Each drill edits a scratch copy of the code facts and of the model,
# then runs `sync.mjs check --root --model` and reports whether the check caught it. Every drill
# is expected to be caught except D5, a negative control that is documented as not caught, and
# D13b, D16b and D17b, controls that must stay clean. A drill on a k8s fact source is caught with exit 4.
# The real model is never edited: a drill that changes a view changes the scratch copy.
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

echo "== D8: a new cross-package import"
fresh
sed -i '1a import com.vrudenko.kanban_board.mapper.Unused;' $scratch/src/main/java/com/vrudenko/kanban_board/constant/KafkaTopics.java
run

echo "== D8b: a two-package cycle appears"
fresh
set j (find $scratch/src/main/java/com/vrudenko/kanban_board/exception -name '*.java' | sort | head -1)
sed -i '1a import com.vrudenko.kanban_board.handler.Unused;' $j
run

echo "== D9: the only development view is retagged"
fresh
sed -i 's|^    #development$|    #logical|' $scratch/model/views.c4
run

echo "== D10: a namespace node is deleted from the model (cert-manager)"
fresh
sed -i '/^      cert-manager = namespace .cert-manager. {$/,/^      }$/d' $scratch/model/deployment.c4
run

echo "== D10b: a manifest namespace is misspelled (kube-sytem)"
fresh
sed -i 's|namespace: kube-system|namespace: kube-sytem|' $scratch/k8s/platform/traefik/helmchartconfig.yaml
run

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

echo "== D12: a metadata key is misspelled on an element (sourc)"
fresh
sed -i "s|^      source 'k8s/platform/traefik'|      sourc 'k8s/platform/traefik'|" $scratch/model/model.c4
run

echo "== D12b: unknown metadata keys on a deployment node, an instance and a relationship"
fresh
sed -i "s|^\(      kube-system = namespace .*{\)\$|\1\n        metadata { nodekey 'x' }|" $scratch/model/deployment.c4
sed -i "s|^        instanceOf traefik\$|        instanceOf traefik {\n          metadata { instkey 'y' }\n        }|" $scratch/model/deployment.c4
sed -i "s|^  client -\[sync\]-> traefik 'HTTPS :443'\$|  client -[sync]-> traefik 'HTTPS :443' {\n    metadata { relkey 'z' }\n  }|" $scratch/model/model.c4
run

echo "== D13: an instance with no relationship (lonely)"
fresh
lonely_into
run

echo "== D13b (control): the same instance with a relationship"
fresh
lonely_into linked
run

echo "== D14: a k8s fact source directory is missing (k8s/overlays)"
fresh
trash-put $scratch/k8s/overlays
run

echo "== D14b: a k8s fact source directory is missing (k8s/monitoring)"
fresh
trash-put $scratch/k8s/monitoring
run

echo "== D14c: a k8s manifest does not parse"
fresh
printf '%s\n' 'a: [1, 2' 'b: 3' >$scratch/k8s/platform/broken.yaml
run

echo "== D17: a drawn instance pair gains a relationship with a different title"
fresh
labeldrill probe
run

echo "== D17b (control): the same pair gains a relationship with the shared title"
fresh
labeldrill 'SQL over JDBC'
run

echo "== D18: a scenario view gets a sixth participant"
fresh
sed -i "s|^\(    client <- traefik '200 {id, email, displayName, theme}'\)\$|\1\n    kanban.api -> redpanda.activity 'probe'|" $scratch/model/views.c4
run

echo "== D19: the only instance of a workload element is deleted (backup)"
fresh
sed -i '/^        instanceOf backup$/d' $scratch/model/deployment.c4
run

echo "== D20: a claimed HelmRelease is renamed (loki)"
fresh
sed -i 's|^  name: loki$|  name: lokix|' $scratch/k8s/monitoring/controllers/loki.yaml
run

echo "== D21: a claimed namespace changes (loki moved to kanban-data)"
fresh
sed -i 's|^  namespace: monitoring$|  namespace: kanban-data|' $scratch/k8s/monitoring/controllers/loki.yaml
run

trash-put $scratch
