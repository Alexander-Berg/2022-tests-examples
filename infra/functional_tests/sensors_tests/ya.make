UNITTEST(sensors_tests)

OWNER(
    armoking
    g:yp-sd
)

PEERDIR(
    infra/yp_service_discovery/functional_tests/common/storage_tests
)

SRCS(
    sensors_tests.cpp
)

DATA(
    sbr://1568742196 # storage
)

SIZE(MEDIUM)

END()
