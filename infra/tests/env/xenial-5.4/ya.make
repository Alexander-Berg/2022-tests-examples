PY3TEST()

OWNER(g:rtc-sysdev)

# Generic test config
INCLUDE(${ARCADIA_ROOT}/infra/porto/tests/ut/config.inc)

INCLUDE(${ARCADIA_ROOT}/infra/environments/porto-build-xenial-5.4/release/vm-image/env.inc)

DEPENDS(
    ${PORTO_XENIAL_5_4_RELEASE_ROOTFS}
)

# qemu_kvm image config
SET(QEMU_ROOTFS_DIR ${PORTO_XENIAL_5_4_RELEASE_ROOTFS})
SET(QEMU_PROC 4)
SET(QEMU_MEM 8G)
INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)

TEST_SRCS(env_ut.py)

FORK_TESTS()

END()
