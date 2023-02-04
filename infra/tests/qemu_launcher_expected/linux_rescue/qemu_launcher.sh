#!/bin/bash -x
# Redirect tcp connections to unix sockets
socat TCP6-LISTEN:7256,reuseaddr,fork UNIX-CONNECT:/workdir/vnc.sock &
socat UDP6-LISTEN:7256,reuseaddr,fork UNIX-CONNECT:/workdir/vnc.sock &

export QEMU_AUDIO_DRV=none

/usr/local/bin/qemu-system-x86_64 \
-nographic \
-nodefaults \
--enable-kvm \
-machine q35,usb=on \
-cpu host,host-phys-bits,kvm=off \
-smp "10" \
-m "12288M" \
-chardev socket,id=monsk,path=/workdir/mon.sock,server,nowait -mon monsk -monitor none \
-device usb-tablet -vnc unix:/workdir/vnc.sock,password \
-vga std \
-chardev file,id=serial_log,path=/workdir/serial.log -serial chardev:serial_log \
-drive file="/qemu-persistence/image",id=root_disk,if=none,cache=none \
-device virtio-blk-pci,id=root_disk,bus=pcie.0,scsi=off,drive=root_disk \
-kernel /boot/vmlinuz -initrd /boot/initrd \
-append 'root=/dev/ram0 rdinit=/bin/sh init=/bin/sh ro console=tty1 loglevel=7' \
-drive file="/qemu-empty/image",id=empty,if=none,cache=none \
-device virtio-blk-pci,id=empty,bus=pcie.0,scsi=off,drive=empty \
-drive file="/qemu-resource/image",id=resource,if=none,cache=none \
-device virtio-blk-pci,id=resource,bus=pcie.0,scsi=off,drive=resource


RET=$?

pkill -9 -P $$

exit $RET
