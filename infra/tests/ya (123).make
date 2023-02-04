UNITTEST_FOR(infra/pod_agent/libs/daemon)

OWNER(
    amich
    g:deploy
)

TAG(
    ya:force_sandbox
    ya:fat
    sb:portod
)

SIZE(LARGE)

TIMEOUT(1300)

DATA(
    arcadia/infra/pod_agent/libs/daemon/tests/test_lib/specs.json
    sbr://948421454=search_ubuntu_precise
    sbr://948421454=search_ubuntu_precise_copy
    sbr://942043863=layer_small_data_0
    sbr://836357881=layer_small_data_0_copy
    sbr://617090665=layer_small_data_1
)

DEPENDS(
    infra/pod_agent/daemons/pod_agent
)

PEERDIR(
    library/cpp/digest/md5

    infra/pod_agent/libs/daemon/tests/test_lib

    yt/yt/core
)

SRCS(
    daemon_ut.cpp
)

DEPENDS(
    infra/pod_agent/daemons/pod_agent
)

END()
