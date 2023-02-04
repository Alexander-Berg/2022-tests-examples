LIBRARY()

OWNER(
    g:yp-sd
)

PEERDIR(
    infra/yp_service_discovery/config
    infra/yp_service_discovery/libs/client
    infra/yp_service_discovery/libs/config
    infra/yp_service_discovery/libs/main
    infra/yp_service_discovery/libs/router_api

    library/cpp/json
    library/cpp/neh
    library/cpp/proto_config
    library/cpp/protobuf/json
    library/cpp/resource
    library/cpp/testing/unittest
)

SRCS(
    common.cpp
    daemon_runner.cpp
)

END()

RECURSE(
    storage_tests
)
