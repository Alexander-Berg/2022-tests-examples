PROGRAM(ip_client)

ALLOCATOR(LF)

OWNER(
    amich
    g:deploy
)

PEERDIR(
    infra/pod_agent/libs/ip_client

    library/cpp/getopt
)

SRCS(
    console_client.cpp
    main.cpp
)

END()
