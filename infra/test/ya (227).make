GO_TEST()

# broken recepie

INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)

# PEERDIR(
#  ${QEMU_BIN_ROOT}
#  ${VMEXEC_DEF_ROOTFS}
# )
#
#INCLUDE(${ARCADIA_ROOT}/infra/qemu/vmexec/env.inc)

SET(
    BALANCER_RESOURCE_ID
    1239427119
)

#SET(BALANCER_RESOURCE_ID 1352068255) # archive with balancer executable
#SET(BALANCER_BIN balancer.tar.gz)
# TODO: where is located the result?

DATA(arcadia/infra/goxcart/internal/config/testdata/)

DATA(sbr://${BALANCER_RESOURCE_ID})

# nice try, but this do not work :(

SET(
    QEMU_USER
    root
)

GO_TEST_SRCS(
    balancer_test.go
    config_test.go
    interface_test.go
    main_test.go
    stat_test.go
)

END()
