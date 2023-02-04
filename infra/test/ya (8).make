OWNER(dmtrmonakhov)

EXECTEST()
DEPENDS(
    infra/diskmanager/utils/dqsync
)
RUN(
    dqsync
)
SET(QEMU_SSH_USER root)
INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)
END()
