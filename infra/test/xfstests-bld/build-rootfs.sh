#!/bin/sh

set -xe
mkdir -p BLD
mkdir -p RESULTS

cd BLD
rm -rf *
git clone --depth 20 https://github.yandex-team.ru/dmtrmonakhov/xfstests-bld.git
cd xfstests-bld
cp ../../make-rootfs.sh .
DIR=`pwd`

sudo docker run --rm --privileged -v $DIR:/xfstests-bld xfstests-bld:devel xfstests-bld/make-rootfs.sh
docker build -t kvm-xfstests xfstests-bld/kvm-xfstests
docker save kvm-xfstests | zstdmt  -20 >                    RESULTS/kvm-xfstests.tar.zst
cp BLD/xfstests-bld/kvm-xfstests/test-appliance/root_fs.img RESULTS/root_fs.img
