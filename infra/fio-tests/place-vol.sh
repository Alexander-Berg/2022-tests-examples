#!/bin/bash -ex

DISK_ID="$1"
VOL_NAME="test-fio"
VOL_SIZE=10G
VOL_ID=$(sudo dmctl vol-create $VOL_NAME $DISK_ID $VOL_SIZE)

echo $VOL_ID > place-vol.id

mkdir -p place-vol
sudo dmctl vol-mount $VOL_ID $PWD/place-vol

sudo mkdir -p place-vol/{porto_layers,porto_volumes,porto_storage}
