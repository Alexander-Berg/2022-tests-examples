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
-cpu host,host-phys-bits,+vmx,kvm=off \
-smp "10,cores=10,threads=1,sockets=1" \
-m "12288M" \
-chardev socket,id=monsk,path=/workdir/mon.sock,server,nowait -mon monsk -monitor none \
-device usb-tablet -vnc unix:/workdir/vnc.sock,password \
-device VGA,id=video0,vgamem_mb=64 \
-chardev file,id=serial_log,path=/workdir/serial.log -serial chardev:serial_log \
-rtc base=utc,driftfix=slew -global kvm-pit.lost_tick_policy=discard -no-hpet \
-drive file="/qemu-persistence/image",id=root_disk,if=none,cache=none \
-device virtio-blk-pci,id=root_disk,bus=pcie.0,scsi=off,drive=root_disk \
-netdev tap,id=qemu_net,ifname=test,script=no \
-device virtio-net-pci,netdev=qemu_net,mac=test \
-device vfio-pci,host=03:00.0 \
-device vfio-pci,host=04:00.0 \
-drive file=fat:"/cloud_init_configs",id=ciconfig_disk,if=none,file.label=config-2,readonly=on,cache=none \
-device virtio-blk-pci,id=ciconfig_disk,bus=pcie.0,scsi=off,drive=ciconfig_disk


RET=$?

pkill -9 -P $$

exit $RET
