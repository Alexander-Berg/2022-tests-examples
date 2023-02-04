LIBRARY()

OWNER(
    amich
    g:deploy
)

PEERDIR(
    infra/pod_agent/libs/porto_client
)

SRCS(
    client_with_retries.cpp
    test_functions.cpp
    wrapper_client.cpp
)

END()
