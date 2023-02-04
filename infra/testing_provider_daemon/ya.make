PROGRAM()

OWNER(g:yp-dns)

SRCS(
    main.cpp
)

PEERDIR(
    infra/libs/yp_updates_coordinator/service
    infra/libs/yp_updates_coordinator/service/protos/config
    infra/libs/logger
    library/cpp/proto_config
    library/cpp/getopt/small
)

RESOURCE(
    config.json /provider_daemon/proto_config.json
)

END()
