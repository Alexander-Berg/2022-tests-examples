#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(dirname "$0")
REPO_DIR=$(realpath "$SCRIPT_DIR/../../../../..")
TOOLS_DIR="$REPO_DIR/tools"

rm -rf "$SCRIPT_DIR"/../*/build/idl
rm -rf "$SCRIPT_DIR"/../../naviprovider/*/build/idl

PLATFORM=$(uname | tr '[:upper:]' '[:lower:]')
MAPKIT_ARTIFACTS=$("$SCRIPT_DIR/../../mmobi/find-mapkit-artifacts" "$PLATFORM")
export MAPKIT_ARTIFACTS

if [ ! -d "$MAPKIT_ARTIFACTS" ]; then
    MAPKIT_VERSION=$(grep MAPKIT_VERSION= "$SCRIPT_DIR/../../mmobi/mapkit.properties" \
        | cut -d= -f2)
    echo "Package root for current mapkit version (${MAPKIT_VERSION}) for $PLATFORM not found"
    echo "Run: ${TOOLS_DIR}/pkg.py install ${MAPKIT_VERSION} ${PLATFORM}"
    exit 1
fi

pushd "$SCRIPT_DIR/../bundle"
"$REPO_DIR/client/yandexnavi.android/gradlew" \
    -b auto_bindings.gradle \
    --no-daemon \
    -PidlRootPaths=..,../../naviprovider,../../navikit \
    -Pplatform="$PLATFORM" \
    generateAutoBindings
popd

"$TOOLS_DIR/gmock/navi_gmock_gen.py" "$SCRIPT_DIR/../mocks/mocks.json"
"$TOOLS_DIR/gmock/navi_gmock_gen.py" "$SCRIPT_DIR/../../navikit/mocks/mocks.json"

"$TOOLS_DIR/cmake/navi_cmake.py"
