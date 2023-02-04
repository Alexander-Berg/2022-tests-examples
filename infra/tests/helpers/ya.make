LIBRARY()

OWNER(g:yp-dns)

SRCS(
    replicator_wrapper.cpp
)

PEERDIR(
    infra/libs/controller/object_manager
    infra/libs/controller/standalone_controller
    infra/yp_dns_api/replicator/config
    infra/yp_dns_api/replicator/zone_replicator
    library/cpp/proto_config
)

END()

RECURSE(
    py
)
