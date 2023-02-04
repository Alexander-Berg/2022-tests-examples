#!/bin/bash

#sudo modprobe kvm
#sudo modprobe kvm-intel
[ -e  /dev/kvm ] || sudo mknod /dev/kvm c 10 232

VOL=$1
DOCKER_IMG=${2:-kvm-xfstests}
[ -z "$VOL" ] && exit 1

VOL_PATH=$(realpath $VOL)

mkdir -p $VOL_PATH/logs
docker run --device /dev/kvm:/dev/kvm --rm \
       -v $VOL_PATH:/vol \
       -v $VOL_PATH/logs:/usr/local/lib/kvm-xfstests/logs $DOCKER_IMG  \
       --tmpfs /usr/local/lib/kvm-xfstests/disks:rw,size=40980000k,mode=1777 \
       kvm-xfstests --archive -o debug --cache=unsafe --aio=threads \
       --kernel /vol/bzImage \
       smoke
