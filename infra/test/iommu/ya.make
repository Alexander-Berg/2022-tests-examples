OWNER(g:kernel)

PY3TEST()
SET(
    QEMU_ROOTFS_DIR
    infra/environments/rtc-xenial-gpu/vm-image
)

DEFAULT(
    QEMU_USER
    root
)

DEFAULT(
    #Explicitly set machine type until it is not default option
    QEMU_OPTIONS
    \"
    -device secondary-vga,addr=05.0
    -device secondary-vga,addr=06.0
    -device virtio-gpu-pci,addr=07.0
    -device virtio-gpu-pci,addr=08.0
    -machine q35,accel=kvm,kernel-irqchip=split
    -device intel-iommu,intremap=on
    \"
)

TEST_SRCS(
    test.py
)

INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)

END()
