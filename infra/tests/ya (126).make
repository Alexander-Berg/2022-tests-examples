EXECTEST()
OWNER(g:kernel)

INCLUDE(${ARCADIA_ROOT}/infra/qemu/env.inc)

DEPENDS(
    ${QEMU_BIN_ROOT}
)
RUN(
    ${ARCADIA_BUILD_ROOT}/${QEMU_BIN_APP} --help
    STDOUT qemu-help.txt
    CANONIZE_LOCALLY qemu-help.txt
)
END()
