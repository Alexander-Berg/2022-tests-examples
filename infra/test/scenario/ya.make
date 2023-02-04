PY3_LIBRARY()

OWNER(
    g:yp-controllers-lib
    g:yp-sd
    g:yp-dns
)

PEERDIR(
    infra/libs/yp_replica
    infra/libs/updatable_proto_config
    infra/libs/updatable_proto_config/protos
    yp/cpp/yp
    library/cpp/yson/node
)

PY_SRCS(
    scenario.pyx
)

SRCS(
    scenario.cpp
)

END()
