PY2TEST()
DEPENDS(
    infra/diskmanager/utils/remount
)

TEST_SRCS(
    conftest.py
    test.py
)
PEERDIR(
    infra/diskmanager/lib
)
SET(QEMU_SSH_USER root)
INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)
END()
