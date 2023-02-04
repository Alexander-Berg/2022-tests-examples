UNITTEST(storage_tests_resolve_endpoints)

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
    sbr://3038721058 # endpoint_storage.json
)

SIZE(MEDIUM)

END()
