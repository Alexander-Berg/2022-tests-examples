#!/usr/bin/env bash
set -e
set -o pipefail

FILE_PATH=$(dirname $(dirname ${ECSTATIC_DATA_PATH}))/"hook_env"
touch $FILE_PATH
echo $FILE_PATH
env >> $FILE_PATH
