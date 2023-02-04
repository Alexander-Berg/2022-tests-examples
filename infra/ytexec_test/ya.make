EXECTEST()

DEPENDS(infra/rsm/nvgpumanager/tools/cuda-check)

RUN(cuda-check)
YT_SPEC(infra/rsm/nvgpumanager/tools/cuda-check/ytexec_test/yt_spec.yson)

TAG(
    ya:yt
    ya:noretries
)
SIZE(MEDIUM)
END()


