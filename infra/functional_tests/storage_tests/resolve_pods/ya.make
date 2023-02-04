UNITTEST(storage_tests_resolve_pods)

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
    sbr://3039023918 # pod_storage.json
)

SIZE(MEDIUM)

END()
