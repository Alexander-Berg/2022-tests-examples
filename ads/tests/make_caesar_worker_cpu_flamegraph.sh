#!/bin/bash

set -e -o pipefail

cd $(dirname $0)

NAME=caesar_worker
SLEEP_QUANT=0.5

while true ; do
    SERVICE_PID=$(pidof $NAME | cut -f -1 -d ' ' || true)
    if [ -n "${SERVICE_PID}" ] ; then
        echo "SERVICE_PID=${SERVICE_PID}"
        break
    fi ;
    echo -n .
    sleep ${SLEEP_QUANT}
done

set -x # do not print trace of commands in cycle

rm perf.data || true
ya tool perf record -F 250 -g --call-graph=dwarf --pid=${SERVICE_PID} --proc-map-timeout=10000 -- sleep 120 || true  # sometimes fail is OK

if [[ ! -e perf.data ]] ; then
    echo "Perf record failed - no perf.data file"
    exit -1
fi

ya tool perf script > out.perf

FG_PATH=../../../../contrib/tools/flame-graph

$FG_PATH/stackcollapse-perf.pl out.perf | c++filt > out.folded
$FG_PATH/flamegraph.pl out.folded > fgraph.svg
ya upload --mds fgraph.svg
