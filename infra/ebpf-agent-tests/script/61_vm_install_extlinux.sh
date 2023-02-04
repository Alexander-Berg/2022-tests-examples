[ "$virt_mode" != "vm" ] && exit 0

# symlinks set at installing kernel
: ${KERNEL_VMLINUX="vmlinuz"}
: ${KERNEL_INITRD="initrd.img"}
: ${with_kernel="1"}

[ "$with_kernel" != "1" ] && exit 0

test -e ${KERNEL_VMLINUX}
test -e ${KERNEL_INITRD}

# label hardcoded in sandbox task
: ${KERNEL_ROOT="LABEL=rootfs"}
: ${KERNEL_ROOT_FSTYPE="ext4"}
: ${KERNEL_ROOT_FLAGS="errors=panic"}

# redirect /dev/console to serial port and duplicate kernel log at first vc
# do not ignore errors: panic, reboot and do fsck at any error

: ${KERNEL_CMDLINE="ro console=tty1 console=ttyS0,115200n8 loglevel=7 oops=panic panic=60 biosdevname=0 net.ifnames=0"}

# extlinux --install . # installed outside in sandbox task

tee extlinux.conf <<EOF
DEFAULT linux
LABEL linux
KERNEL ${KERNEL_VMLINUX}
APPEND initrd=${KERNEL_INITRD} root=${KERNEL_ROOT} rootfstype=${KERNEL_ROOT_FSTYPE} rootflags=${KERNEL_ROOT_FLAGS} ${KERNEL_CMDLINE}
EOF

[ -f etc/fstab ] && cat etc/fstab

tee etc/fstab <<EOF
${KERNEL_ROOT}	/	${KERNEL_ROOT_FSTYPE}	defaults,${KERNEL_ROOT_FLAGS}	0 1
EOF
