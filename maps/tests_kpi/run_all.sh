#!/usr/bin/env bash

SCRIPT_DIR=$(dirname "`realpath "$0"`")
REQUEST_DIR="$SCRIPT_DIR/requests"

TEMPERATURE=50000
LOCAL_THREADS=10
YT_CLUSTER="freud"
YT_THREADS=20
YT_TASK_COUNT=10

output=""
solver=annealing-mvrp
choose='best'
time_limit=600
iterations=""
yt=0

function usage_exit() {
    echo "`basename "$0"` <arguments>"
    echo "	-o|--output OUTPUT_DIR (required)"
    echo "	-s|--solver SOLVER_PATH"
    echo "	-c|--choose best|median|worst"
    echo "	-t|--time-limit <seconds>"
    echo "	-I|--iterations <iterations>"
    echo "	-y|--yt"
    exit 1
}

while [ $# -gt 0 ]; do
    case "$1" in
        -s|--solver) solver="$2"; shift; shift ;;
        -o|--output) output="$2"; shift; shift ;;
        -c|--choose) choose="$2"; shift; shift ;;
        -t|--time-limit) time_limit="$2"; shift; shift ;;
        -I|--iterations) iterations="$2"; shift; shift ;;
        -y|--yt) yt="1"; shift ;;
        *) echo "Unknown option: '$1'"; usage_exit ;;
    esac
done

[ -n "$output" ] || { echo "Please specify non-empty output directory name"; exit 2; }
mkdir -p "$output"

echo "Choosing the $choose result from several runs."

cat "$REQUEST_DIR/ids.txt" "$REQUEST_DIR/ids2.txt" |
while read name taskid; do
    request="$REQUEST_DIR/${name}_request.json"
    distances="$REQUEST_DIR/${name}_distances.proto"
    response="$output/${name}_response.json"
    echo
    echo "[$name]"
    if [ $yt -eq 0 ]; then
        [ -n "$iterations" ] && opts=" -v iterations -e $iterations" || opts="-v time-limit -e $time_limit"
        solver_deviation.py -i "$request" -d "$distances" \
            -n3 -P3 --solver-threads "$LOCAL_THREADS" \
            -S "$solver" -T "$TEMPERATURE" \
            --output-$choose "$response" \
            -p -a --std -x cost $opts;
    else
        [ -n "$iterations" ] && opts="-I $iterations" || opts="-t $time_limit"
        yt_solver.py -i "$request" -d "$distances" \
            -r "$YT_TASK_COUNT" -n "$YT_THREADS" --yt "$YT_CLUSTER" \
            -S "$solver" --temperature "$TEMPERATURE" -o "$response" $opts &
    fi
done

wait
