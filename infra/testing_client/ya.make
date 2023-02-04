PROGRAM()

OWNER(g:yp-dns)

SRCS(
    main.cpp
)

PEERDIR(
    infra/libs/yp_updates_coordinator/client
    infra/libs/yp_updates_coordinator/instance_state/state
    infra/libs/logger
    library/cpp/getopt/small
)

END()
