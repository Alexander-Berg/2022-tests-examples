LIBRARY()

OWNER(
    amich
    g:deploy
)

PEERDIR(
    infra/pod_agent/libs/client
    infra/pod_agent/libs/daemon
    infra/pod_agent/libs/porto_client/porto_test_lib
    infra/pod_agent/libs/service_iface/protos

    library/cpp/http/simple
    library/cpp/testing/unittest
)

SRCS(
    test_canon.cpp
    test_functions.cpp
)

END()
