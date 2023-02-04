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
    ${ARCADIA_BUILD_ROOT}/infra/qemu/vmexec/vmexec -L ${TEST_CASE_ROOT} -- ${ARCADIA_ROOT}/${VMEXEC_ROOT}/tests/ut/sandbox.sh
    STDOUT ${TEST_OUT_ROOT}/sandbox-stdout.log
    STDERR ${TEST_OUT_ROOT}/sandbox-stderr.log
    CANONIZE_LOCALLY sandbox-result.txt
)

RUN(
    ${ARCADIA_BUILD_ROOT}/infra/qemu/vmexec/vmexec -L ${TEST_CASE_ROOT} -v ${ARCADIA_ROOT}/${VMEXEC_ROOT}/tests/ut:/host-vol -- /host-vol/sandbox.sh
    STDOUT ${TEST_OUT_ROOT}/sandbox-stdout3.log
    STDERR ${TEST_OUT_ROOT}/sandbox-stderr3.log
    CANONIZE_LOCALLY sandbox-result.txt
)

RUN(
    ${ARCADIA_BUILD_ROOT}/infra/qemu/vmexec/vmexec -L ${TEST_CASE_ROOT} -- ${ARCADIA_ROOT}/${VMEXEC_ROOT}/tests/ut/sandbox-with-args.sh dog cat  mouse
    STDOUT ${TEST_OUT_ROOT}/sandbox-stdout2.log
    STDERR ${TEST_OUT_ROOT}/sandbox-stderr.log
    CANONIZE_LOCALLY sandbox-result2.txt
)

FORK_TESTS()
SIZE(MEDIUM)
REQUIREMENTS(kvm)
END()

