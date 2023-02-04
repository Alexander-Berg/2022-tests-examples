#!/bin/bash

#sudo modprobe kvm
#sudo modprobe kvm-intel
[ -e  /dev/kvm ] || sudo mknod /dev/kvm c 10 232

GROUP=$1
VOL=work/$GROUP
shift
[ -z "$VOL" ] && exit 1
mkdir -p $VOL

VOL_PATH=$(realpath $VOL)
DOCKER_IMG=${KVM_XFSTESTS_IMG:-kvm-xfstests}
XFSTEST_LIST=${XFSTEST_LIST:-'-g auto'}
XFSTEST_TIMEOUT=${XFSTEST_TIMEOUT:-9000}
XFSTEST_KERNEL=${XFSTEST_KERNEL:-bzImage}

mkdir -p $VOL_PATH/logs
mkdir -p $VOL_PATH/results
mkdir -p $VOL_PATH/disks
cp $XFSTEST_KERNEL $VOL_PATH/bzImage

timeout $XFSTEST_TIMEOUT docker run --device /dev/kvm:/dev/kvm --rm \
       -v $VOL_PATH:/vol \
       -v $VOL_PATH/logs:/usr/local/lib/kvm-xfstests/logs \
       -v $VOL_PATH/disks:/usr/local/lib/kvm-xfstests/disks \
       $DOCKER_IMG  \
       kvm-xfstests --archive -o debug --cache=unsafe --aio=threads \
       --kernel /vol/bzImage -c $GROUP $XFSTEST_LIST

rm -rf $VOL_PATH/disks

for res in `find $VOL_PATH/logs -name 'results-*.tar.xz'`
do
    fname=$(basename $res)
    dir=${fname%".tar.xz"}
    mkdir -p $VOL_PATH/results/$dir
    tar vJxf $res -C $VOL_PATH/results/$dir
done

