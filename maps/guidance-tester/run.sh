#!/bin/bash -e

die() {
    echo 2>&1 $@
    exit 1
}

APP=build/exe/app/release/app

[ -x "$APP" ] || die "guidance-tester executable is not built, run tapp@[linux|darwin]"


if [ -n "$1" ]; then
    TESTSETS=test-data/$1
    if ! [ -d $TESTSETS ]; then
        die Invalid testset: "'"$1"'"
    fi
else
    TESTSETS=$(find test-data -mindepth 1 -maxdepth 1 -type d | sort -n)

    if [ -z "${TESTSETS:-}" ]; then
        die No test data found
    fi
fi

total_success=0
total_fail=0

for testset in $TESTSETS; do
    success=0
    fail=0
    for test in $(find $testset/ -mindepth 1 -name '*.pb.test' | sort -n); do
        echo -n "Running $(basename $test .pb.test)... "
        if $APP $test 2>/dev/null >/dev/null; then
            echo "OK"
            : $((++success))
        else
            echo "FAILED"
            : $((++fail))
        fi
    done

    echo "Testset $(basename $testset): $((success+fail)) tests, $success passed, $fail failed"
    : $((total_success += success))
    : $((total_fail += fail))
done

echo "Overall: $((total_success+total_fail)) tests, $total_success passed, $total_fail failed"
