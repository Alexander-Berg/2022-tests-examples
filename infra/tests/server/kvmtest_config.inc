# Generic test config
PEERDIR(
    # Workaround for https://st.yandex-team.ru/DEVTOOLS-5861
    # Tests are actually declared in peerdir library
    infra/diskmanager/tests/server
)
DEPENDS(
    infra/diskmanager/client
    infra/diskmanager/server
    infra/diskmanager/utils/dqsync
    infra/diskmanager/utils/remount
)
SIZE(MEDIUM)
#FORK_TESTS()

SET(RECIPE_TMP_FILE_NAME nvme.img)
SET(RECIPE_TMP_FILE_SIZE 10485760) # 10M
INCLUDE(${ARCADIA_ROOT}/infra/diskmanager/tests/recipes/tmp_file/recipe.inc)

SET(QEMU_OPTIONS \"-drive file=/tmp/nvme.img,if=none,id=NVME1
                   -device nvme,drive=NVME1,serial=nvme-serial\")

INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)
