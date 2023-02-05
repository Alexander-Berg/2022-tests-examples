#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

PROJECT_BUNDLE_DIR="$SCRIPT_DIR/../../bundle/metrokit/bundle/common/metrokit/schemes"
MAKE_BUNDLE_BIN="$SCRIPT_DIR/make_schemes_bundle.py"
SCHEMES_JSON="$SCRIPT_DIR/test_schemes.json"

rm -rf $PROJECT_BUNDLE_DIR
$MAKE_BUNDLE_BIN --schemes $SCHEMES_JSON --env test --out $PROJECT_BUNDLE_DIR
