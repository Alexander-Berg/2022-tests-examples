#!/bin/bash -x

CT="test-fio"

if test -d place-vol ; then
	PLACE="$PWD/place"
else
	PLACE="/place"
fi

portoctl destroy $CT

portoctl layer -P $PLACE -R test-layer-base
portoctl layer -P $PLACE -R test-layer-fio

rm -f layer-base.tar.xz
rm -f layer-fio.tar.xz

rmdir rootfs

if test -f place-vol.id ; then
	VOL_ID=$(cat place-vol.id)
	sudo dmctl vol-umount $VOL_ID
	rmdir place-vol
	sudo dmctl vol-delete $VOL_ID
	rm -f place-vol.id
fi
