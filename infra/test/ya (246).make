LIBRARY()

OWNER(
    amich
    g:deploy
)

PEERDIR(
    infra/pod_agent/libs/behaviour/bt/core
    infra/pod_agent/libs/behaviour/bt/nodes/base
)

SRCS(
    mock_tick_context.cpp
    test_functions.cpp
)

END()
