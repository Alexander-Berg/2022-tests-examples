UNITTEST_FOR(infra/pod_agent/libs/porto_client/porto_test_lib)

OWNER(
    amich
    g:deploy
)

PEERDIR(
    infra/pod_agent/libs/porto_client
)

SRCS(
    client_with_retries_ut.cpp
    wrapper_client_ut.cpp
)

END()
