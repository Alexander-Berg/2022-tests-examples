[ "$virt_mode" != "vm" ] && exit 0

: ${KERNEL_PACKAGE="yandex-linux-image-stable"}
: ${with_kernel="1"}

[ "$with_kernel" != "1" ] && exit 0

[ -x /usr/sbin/update-grub ] || ln -s /bin/true /usr/sbin/update-grub

export DEBIAN_FRONTEND="noninteractive"

apt-get --quiet --yes --no-install-recommends install initramfs-tools

tee -a etc/initramfs-tools/modules <<EOF
9p
9pnet_virtio
EOF

apt-get --quiet --yes --no-install-recommends install ${KERNEL_PACKAGE}

test -e vmlinuz
test -e initrd.img
