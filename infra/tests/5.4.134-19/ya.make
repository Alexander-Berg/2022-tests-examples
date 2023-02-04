PY3TEST()

OWNER(g:rtc-sysdev)

# Generic test config
INCLUDE(${ARCADIA_ROOT}/infra/ebpf-agent/tests/config.inc)

# qemu_kvm image config
SET(QEMU_ROOTFS_DIR infra/environments/ebpf-agent-tests/release/vm-image-5.4.134-19)
INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)

END()
