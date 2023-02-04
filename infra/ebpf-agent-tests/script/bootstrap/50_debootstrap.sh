debootstrap --foreign --variant=minbase --arch amd64 xenial . http://mirror.yandex.ru/ubuntu

# do not create devices
tar cz -T /dev/null > debootstrap/devices.tar.gz

# do not mount/umount anything
tee -a debootstrap/functions <<EOF
mount () { warning "" "skip mount \$*"; }
umount () { warning "" "skip umount \$*"; }
EOF
