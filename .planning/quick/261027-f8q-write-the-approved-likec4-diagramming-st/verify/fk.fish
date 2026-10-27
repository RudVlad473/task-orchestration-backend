#!/usr/bin/env fish
# V4: replay the real migrations (V1..V9, numeric order) into a throwaway postgres:16 and read the
# tables and foreign keys back from information_schema. Trust auth on a loopback port, no secret
# involved. The container is always removed at the end.

set here (dirname (status filename))
set repo (realpath $here/../../../..)
set mig $repo/src/main/resources/db/migration
set name diag-fk-verify
set port 55433

docker run -d --name $name -e POSTGRES_HOST_AUTH_METHOD=trust -p 127.0.0.1:$port:5432 postgres:16 >/dev/null
or begin
    echo "could not start container"
    exit 1
end

set ready 0
for i in (seq 1 30)
    docker exec $name pg_isready -U postgres >/dev/null 2>&1
    if test $status -eq 0
        set ready 1
        break
    end
    sleep 1
end
if test $ready -ne 1
    echo "postgres not ready after 30 s"
    docker rm -f $name >/dev/null
    exit 1
end
sleep 2

set psqlbase psql -h 127.0.0.1 -p $port -U postgres -d postgres -v ON_ERROR_STOP=1 -q

for f in (ls $mig | sort -V)
    $psqlbase -f $mig/$f >/dev/null
    or echo "migration $f FAILED"
end

echo "== tables (information_schema, base tables in public)"
$psqlbase -At -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY 1"

echo "== foreign keys (child -> parent)"
$psqlbase -At -c "SELECT tc.table_name || ' -> ' || ccu.table_name FROM information_schema.table_constraints tc JOIN information_schema.constraint_column_usage ccu USING (constraint_schema, constraint_name) WHERE tc.constraint_type='FOREIGN KEY' ORDER BY 1"

docker rm -f $name >/dev/null
