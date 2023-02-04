LIBRARY()

OWNER(
    amich
    g:deploy
)

PEERDIR(
    infra/pod_agent/libs/behaviour/bt/nodes/base/test
    infra/pod_agent/libs/pod_agent/object_meta/test_lib
    infra/pod_agent/libs/pod_agent/status_and_ticker_holder
    infra/pod_agent/libs/pod_agent/update_holder
)

SRCS(
    test_functions.cpp
)

END()
