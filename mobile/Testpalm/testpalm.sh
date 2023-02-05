#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

export TESTPALM_CONFIG="./config.json"

cd "$DIR/.."

./gradlew :testpalm:run -q --args="$1 $2 $3"
