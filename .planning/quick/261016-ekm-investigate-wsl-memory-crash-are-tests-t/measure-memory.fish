#!/usr/bin/env fish
# Samples guest memory every 2s while a test run is in flight, and trips a circuit breaker so the
# measurement cannot take WSL down.
#
# usage: measure-memory.fish <out.csv> <floor_mb>
#
# Columns: guest MemAvailable, swap in use, summed RSS of the Gradle test worker JVMs, the Gradle
# daemon RSS, summed cgroup memory of every docker container, and the per-container detail
# (id12:MiB;...) to be mapped to images afterwards with `docker ps -a`.
#
# When MemAvailable drops below <floor_mb> it kills the test workers and stops the Testcontainers
# containers, writes an ABORT row and exits.
set out $argv[1]
set floor $argv[2]

echo "epoch,avail_mb,swap_used_mb,test_jvm_rss_mb,gradle_daemon_rss_mb,docker_mb,docker_detail" >$out

while true
    set avail (awk '/MemAvailable/ {printf "%d", $2/1024}' /proc/meminfo)
    set swap (awk '/SwapTotal/ {t=$2} /SwapFree/ {f=$2} END {printf "%d", (t-f)/1024}' /proc/meminfo)
    set tj (ps -eo rss,args | awk '/Gradle Test Executor/ && !/awk/ {s+=$1} END {printf "%d", s/1024}')
    set gd (ps -eo rss,args | awk '/GradleDaemon/ && !/awk/ {s+=$1} END {printf "%d", s/1024}')

    set total 0
    set detail
    for f in /sys/fs/cgroup/system.slice/docker-*.scope/memory.current
        test -r $f; or continue
        set id (string match -r 'docker-([0-9a-f]{12})' $f)[2]
        set mb (math --scale=0 (cat $f) / 1048576)
        set total (math $total + $mb)
        set detail $detail "$id:$mb"
    end

    echo (date +%s),$avail,$swap,$tj,$gd,$total,(string join ';' $detail) >>$out

    if test $avail -lt $floor
        echo (date +%s),ABORT,avail=$avail floor=$floor >>$out
        pkill -f 'Gradle Test Executor'
        docker ps -q --filter label=org.testcontainers=true | xargs -r docker stop -t 2
        exit 3
    end
    sleep 2
end
