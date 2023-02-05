#!/bin/sh -e
CACHEDIR=genfiles/test_cache

rm -rf "$CACHEDIR"
mkdir -p "$CACHEDIR"
for _ in $(seq 1 20); do
    for x in $(seq 1 37); do
        HOSTINFO_CACHEDIR=$CACHEDIR ./hostinfo >/dev/null
        HOSTINFO_CACHEDIR=$CACHEDIR ./hostinfo --dc >/dev/null
    done &
    pids="$pids $!"
done
for pid in $pids; do
    wait $pid
done

test -f $CACHEDIR/canonical_names
test -f $CACHEDIR/dc_name
