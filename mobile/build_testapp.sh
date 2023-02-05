#!/usr/bin/env bash

set +o posix
set -e

log() {
    echo "<shell:$$> $*" >&2
}

fail() {
    log "$*"
    exit 1
}

TARGET_PLATFORM=$1

if [ -z "$TARGET_PLATFORM" ]; then
    echo "Error: No target platform specified. Usage: ./build_testapp.sh \$PLATFORM"
    exit 1
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../.."

cd "$PROJECT_ROOT/bundle/apps/test_app/$TARGET_PLATFORM"

if [ "$TARGET_PLATFORM" = "ios" ]; then
    BUILDKIT_URL="https://bitbucket.browser.yandex-team.ru/scm/~likhogrud/buildkit.git"
    BUILDKIT_VERSION_TAG="1.9.40"
    BUILD_CONFIG="AdHoc"
    BUILD_WORKSPACE="MetroKitTestApp"
    BUILD_SCHEME="MetroKitTestApp"

    plutil -replace "com\.yandex\.metrokit\.Environment" -string "$METROKIT_SERVICE_ENV" "MetroKitTestApp/Info.plist"

    git clone --depth 1 --branch "$BUILDKIT_VERSION_TAG" "$BUILDKIT_URL" YXBuildKit
    /bin/bash --login -c "./YXBuildKit/build.sh --upload-branch $BETA_UPLOAD_BRANCH --build-configuration $BUILD_CONFIG --workspace $BUILD_WORKSPACE.xcworkspace --scheme $BUILD_SCHEME --verbose"
elif [ "$TARGET_PLATFORM" = "android" ]; then
    ./gradlew clean
    ./gradlew -s assembleEnvProdRelease uploadBeta -PCI -Pdeploy.branch=$BETA_UPLOAD_BRANCH -Pbuild.number=$BUILD_NUMBER
    ./gradlew -s assembleEnvTestRelease uploadBeta -PCI -Pdeploy.branch=$BETA_UPLOAD_BRANCH-test -Pbuild.number=$BUILD_NUMBER
    ./gradlew -s assembleEnvDevRelease uploadBeta -PCI -Pdeploy.branch=$BETA_UPLOAD_BRANCH-dev -Pbuild.number=$BUILD_NUMBER
fi
