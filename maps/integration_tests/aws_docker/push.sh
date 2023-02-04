#!/usr/bin/env sh
set -ex


VERS_DESCR=$1
NO_PUSH=$2

if [ -z $VERS_DESCR ] || [ -z $ARCADIA_ROOT ];
then
    echo "Usage:
  ./push.sh <version_description> (--no-push)
  Variable ARCADIA_ROOT should be configured.

  Optional args:
    --no-push: skips pushing image to registry."
    exit 1
fi

$ARCADIA_ROOT/maps/b2bgeo/aws_scripts/push_image_common.sh solver-integration-tests solver-integration-tests $VERS_DESCR $NO_PUSH
