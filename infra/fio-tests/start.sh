#!/bin/bash -ex

CT="test-fio"

if test -d "place-vol"; then
	PLACE=$PWD/place-vol
else
	PLACE=/place
fi

wget -c -O layer-base.tar.xz https://proxy.sandbox.yandex-team.ru/778255631
wget -c -O layer-fio.tar.xz https://proxy.sandbox.yandex-team.ru/809890393

portoctl layer -P $PLACE -I test-layer-base layer-base.tar.xz || true
portoctl layer -P $PLACE -I test-layer-fio layer-fio.tar.xz || true

portoctl destroy $CT || true

portoctl create $CT

mkdir -p rootfs
portoctl vcreate $PWD/rootfs backend=overlay place=$PLACE layers="test-layer-fio;test-layer-base" space_limit=1G containers=$CT

mkdir rootfs/test
portoctl vcreate $PWD/rootfs/test place=$PLACE backend=native space_limit=2G containers=$CT

portoctl set $CT root $PWD/rootfs
portoctl set $CT cwd /test

portoctl set $CT command "fio --server=ip6:::"

portoctl set $CT io_limit "$PLACE: 10M"

portoctl set $CT memory_limit 1G
portoctl set $CT memory_guarantee 1G

portoctl set $CT cpu_limit 2c
portoctl set $CT cpu_guarantee 2c

portoctl start $CT
