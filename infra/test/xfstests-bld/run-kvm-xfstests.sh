#!/bin/bash

#sudo modprobe kvm
#sudo modprobe kvm-intel
[ -e  /dev/kvm ] || sudo mknod /dev/kvm c 10 232

VOL=$1
shift
[ -z "$VOL" ] && exit 1
VOL_PATH=$(realpath $VOL)
DOCKER_IMG=${KVM_XFSTESTS_IMG:-kvm-xfstests}
XFSTEST_TIMEOUT=${XFSTEST_TIMEOUT:-9000}

mkdir -p $VOL_PATH/logs
mkdir -p $VOL_PATH/results
timeout $XFSTEST_TIMEOUT docker run --device /dev/kvm:/dev/kvm --rm \
       -v $VOL_PATH:/vol \
       -v $VOL_PATH/logs:/usr/local/lib/kvm-xfstests/logs $DOCKER_IMG  \
       kvm-xfstests --archive -o debug --cache=unsafe --aio=threads \
       --kernel /vol/bzImage $@

for res in `find $VOL_PATH/logs -name 'results-*.tar.xz'`
do
    fname=$(basename $res)
    dir=${fname%".tar.xz"}
    mkdir -p $VOL_PATH/results/$dir
    tar vJxf $res -C $VOL_PATH/results/$dir
done
