PEERDIR(
    rt-research/common/pylib/utils/ut
)

DATA(
    sbr://1009319433
)

IF(OS_LINUX)
    DEPENDS(
        yt/packages/latest
    )
ENDIF()

ENV(ENV_NAME=env_name)

ENV(TEST_DATA_PATH_ENV_NAME=yql.token)
