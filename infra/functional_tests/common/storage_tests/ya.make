LIBRARY()

PEERDIR(
    infra/libs/yp_replica/testing
    infra/libs/yp_replica
    infra/yp_service_discovery/api
    infra/yp_service_discovery/functional_tests/common

    library/cpp/json
    library/cpp/protobuf/json
    library/cpp/scheme/ut_utils
    library/cpp/string_utils/base64
    library/cpp/testing/unittest
)

SRCS(
    common.cpp
)

END()
