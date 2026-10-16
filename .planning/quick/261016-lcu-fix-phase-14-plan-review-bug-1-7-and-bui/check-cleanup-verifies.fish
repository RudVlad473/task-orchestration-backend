#!/usr/bin/env fish
# Run every cleanup verify in the Phase 14 plans against stubbed ssh, k3s and python3, and check each exit status.
#
# A cleanup verify is an automated line that queries netcup-prod for suspended Flux objects or for the 14-10
# scratch objects. Each must fail when ssh or kubectl fails, fail when something is still suspended or left
# behind, and pass only when the query succeeded and found nothing. No network, cluster or container is used.
# Run from the checkout root. Exit 0 only if every case behaves; otherwise print each FAIL and exit 1.
# An optional first argument replaces the phase directory (used once to dry-run the rewrite on a scratch copy).

set phase_dir .planning/phases/14-production-ownership-backend-observability-alerting-and-reco
if set -q argv[1]
    set phase_dir $argv[1]
end
set other_gates 'python3() { return 0; }; grep() { case "$*" in *MEASURED*) return 0;; *) command grep "$@";; esac; };'
set k3s_stub 'k3s() { case "$*" in *prometheusrule*) [ -n "$FORBID" ] && { echo "Error from server (Forbidden)" >&2; return 1; }; [ -n "$RULE" ] && echo prometheusrule/kanban-measure-scratch; return 0;; *networkpolicy*) return 0;; *) printf "%s" "${REP:-1}";; esac; }; export -f k3s; ssh() { bash -c "$2"; };'

set failures 0
set checked 0

function expect --argument-names label want env_assign stub cmd
    env $env_assign bash -c "$stub $cmd" >/dev/null 2>&1
    set got $status
    if test "$want" = zero -a $got -ne 0
        echo "FAIL $label: expected exit 0, got $got"
        return 1
    else if test "$want" = nonzero -a $got -eq 0
        echo "FAIL $label: expected non-zero exit, got 0"
        return 1
    end
    return 0
end

for file in $phase_dir/*-PLAN.md
    for cmd in (rg -o '<automated>(.*ssh netcup-prod.*(suspend==true|measure-block-management-port).*)</automated>' -r '$1' $file)
        set checked (math $checked + 1)
        set name (basename $file)
        expect "$name ssh fails" nonzero X=1 "$other_gates ssh() { echo 'ssh: connect to host netcup-prod: Connection refused' >&2; return 255; };" "$cmd"; or set failures (math $failures + 1)
        if string match -q '*suspend==true*' -- $cmd
            expect "$name nothing suspended" zero X=1 "$other_gates ssh() { return 0; };" "$cmd"; or set failures (math $failures + 1)
            expect "$name apps-nonprod suspended" nonzero X=1 "$other_gates ssh() { echo apps-nonprod; };" "$cmd"; or set failures (math $failures + 1)
        else
            expect "$name Forbidden on the scratch-rule query" nonzero FORBID=1 "$other_gates $k3s_stub" "$cmd"; or set failures (math $failures + 1)
            expect "$name everything deleted, replicas 1" zero X=1 "$other_gates $k3s_stub" "$cmd"; or set failures (math $failures + 1)
            expect "$name scratch rule still present" nonzero RULE=1 "$other_gates $k3s_stub" "$cmd"; or set failures (math $failures + 1)
            expect "$name replicas 0" nonzero REP=0 "$other_gates $k3s_stub" "$cmd"; or set failures (math $failures + 1)
        end
    end
end

echo "checked $checked cleanup verifies, $failures failing case(s)"
if test $checked -ne 7
    echo "FAIL expected 7 cleanup verifies (six suspension checks and the 14-10 deletion check), found $checked"
    exit 1
end
test $failures -eq 0
