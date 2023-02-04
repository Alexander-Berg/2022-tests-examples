#!/bin/sh

set -e

swagger_json_docs="http://autoru-api-server.vrts-slb.test.vertis.yandex.net/api-docs"

dir=$PWD
cd ../../Tools

tmpfile=$(mktemp)

swift run ProtoGenerator used-proto-types --root-protomodels $(<Sources/RootProtomodels) --proto-source-folder Sources/schema-registry/proto > $tmpfile
cd $dir

curl $swagger_json_docs | ./generate.py $tmpfile > ../../Tests/UITests/Sources/UITests/API/GeneratedBackendMethod.swift

rm $tmpfile
