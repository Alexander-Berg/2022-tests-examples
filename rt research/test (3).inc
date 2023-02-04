SIZE(MEDIUM)

PEERDIR(
    rt-research/multik/pylib/key_value_store/ut
)

IF(OS_LINUX)
    DEPENDS(
        yt/packages/latest
    )
ENDIF()
