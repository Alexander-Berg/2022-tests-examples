PROGRAM()

OWNER(
    amich
    g:deploy
)

PEERDIR(
    infra/libs/http_service
    infra/libs/service_iface
    library/cpp/getopt/small
)

SRCS(
    main.cpp
)

END()
