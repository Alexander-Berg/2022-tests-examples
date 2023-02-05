#!/bin/bash -e

TEST_HOST=spdytest1.mobmaps-ext01e.tst.maps.yandex.ru
SIGNATURE_HOST=mobmaps-ext01e.tst.maps.yandex.ru
BASE_BIN="./fetch_spdy -h"
BIN="$BASE_BIN $TEST_HOST"
SIGNATURE_BIN="$BASE_BIN $SIGNATURE_HOST"

for SIZE in \
        100 \
        1000001 \
        `# 17000000 (uncomment this when server is able to split large frames)` \
        ; do \
    diff -q \
        <($BIN -g /test/get/$SIZE.seq) \
        <(seq 1 $SIZE | head -c $SIZE); \
done


PATH_SMALL=/test/get/100.seq
PATH_404=/test/get/jhhjbjkkjb
PATH_SIGNATURE=/yruntime/init/2.x/random

$BIN -g $PATH_SMALL -c200 >/dev/null
$BIN -g $PATH_404 -c4.. >/dev/null
! $BIN -g $PATH_404 -c200 >/dev/null 2>/dev/null || exit 255

etag=$($BIN -g $PATH_SMALL -l info 2>&1 | grep etag | awk '{print $4}')
$BIN -g $PATH_SMALL -c 304 -H "If-None-Match: $etag" > /dev/null || exit 255
$BIN -g $PATH_SMALL -c 304 -H "test: non standard header" -H "If-None-Match: $etag" -H "test1: non standard header"> /dev/null || exit 255
$SIGNATURE_BIN -g $PATH_SIGNATURE -c 200 > /dev/null || exit 255
$SIGNATURE_BIN -g $PATH_SIGNATURE -c 401 -S > /dev/null || exit 255
