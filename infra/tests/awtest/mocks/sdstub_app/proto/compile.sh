#!/bin/bash
set -e

if [[ "$OSTYPE" == "linux-gnu" ]]; then
    platform="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    platform="macosx"
else
    echo "platform is not detected or not supported"
    exit 1;
fi

if [ ! -z $(which protoc3 2>/dev/null) ]; then
    protoc="$(which protoc3)"
else
    protoc="protoc"
fi

${protoc} ./api.proto --python_out=../pb/

