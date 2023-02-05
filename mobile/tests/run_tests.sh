#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(dirname "$0")

"$SCRIPT_DIR/configure.sh"

CMAKE=${CMAKE-cmake}

mkdir -p "$SCRIPT_DIR/build"
pushd "$SCRIPT_DIR/build"
"$CMAKE" .. -G Ninja -DCMAKE_EXPORT_COMPILE_COMMANDS=On -Wdev --warn-uninitialized "$@"
popd
"$CMAKE" --build "$SCRIPT_DIR/build" -- run_navi_tests
