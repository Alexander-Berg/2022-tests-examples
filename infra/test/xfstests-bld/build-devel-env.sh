#!/bin/bash

docker build --no-cache --force-rm -t xfstests-bld:devel -f Dockerfile.build .
docker save xfstests-bld:devel | zstdmt  -20 > xfstests-bld-devel.tar.zst
