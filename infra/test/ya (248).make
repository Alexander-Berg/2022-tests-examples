LIBRARY()

OWNER(
    amich
    g:deploy
)

PEERDIR(
    infra/pod_agent/libs/behaviour/trees/base/test

    infra/libs/http_service
    infra/libs/service_iface

    library/cpp/protobuf/json
)

SRCS(
    workload_test_canon.cpp
)

END()
