#!/bin/sh

set -xe

git_commit=$1
git_url=${2:-https://x-oauth-token:${BB_TOKEN}@bb.yandex-team.ru/scm/kernel/linux.git}
base_kconfig=${3:-../kernel-configs/x86_64-config-4.4}
test_kconfig=${4:-debian/configs/config}
build_img=${5:-registry.yandex.net/dmtrmonakhov/kbuild-env:xenial}

mkdir -p BLD
mkdir -p RESULTS
mkdir -p ART

cd BLD
rm -rf *

git init .
git fetch --depth=3 $git_url $git_commit
git checkout -f FETCH_HEAD
DIR=`pwd`

cp $base_kconfig base.conf
cp $test_kconfig test.conf

docker run --rm -v $DIR:/kernel -w /kernel $build_img scripts/kconfig/merge_config.sh base.conf test.conf | tee ../RESULTS/merge_kconfig.log
docker run --rm -v $DIR:/kernel -w /kernel $build_img /opt/kbuild-tool/kbuild-tool make-config
docker run --rm -v $DIR:/kernel -w /kernel $build_img make -j $(nproc) bzImage | tee  ../RESULTS/compile.log
# Save artifact
cp arch/x86/boot/bzImage ../ART/
