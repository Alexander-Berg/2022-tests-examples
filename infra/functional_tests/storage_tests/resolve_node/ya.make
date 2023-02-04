UNITTEST(storage_tests_resolve_node)

OWNER(
    g:yp-sd
)

PEERDIR(
    infra/yp_service_discovery/functional_tests/common/storage_tests
)

SRCS(
    tests.cpp
)

DATA(
    sbr://3038859977 # node_storage.json
)

SIZE(MEDIUM)

END()
