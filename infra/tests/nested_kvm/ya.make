EXECTEST()

OWNER(
    dmtrmonakhov
)

INCLUDE(${ARCADIA_ROOT}/infra/qemu/vmexec/env.inc)

DEPENDS(
    ${VMEXEC_ROOT}
    ${QEMU_BIN_ROOT}
    ${QAVM_DEFAULT_IMAGE}
)
DATA(
    arcadia/${QAVM_ROOT}/keys
)

RUN(
    ${ARCADIA_BUILD_ROOT}/infra/qemu/vmexec/vmexec --mem 1G -L ${TEST_CASE_ROOT} -- echo hello world from nested vmexec
    STDOUT ${TEST_OUT_ROOT}/sandbox-stdout.log
    STDERR ${TEST_OUT_ROOT}/sandbox-stderr.log
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/sandbox-stdout.log
)

TAG(ya:manual)
FORK_TESTS()
SIZE(MEDIUM)
REQUIREMENTS(kvm)

# Tests will be executed inside VM
INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)

END()

