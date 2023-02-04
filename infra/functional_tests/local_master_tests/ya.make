PY3TEST()

INCLUDE(${ARCADIA_ROOT}/yp/python/ya_programs.make.inc)

OWNER(
    g:yp-sd
)

PEERDIR(
    infra/yp_service_discovery/functional_tests/local_master_tests/scenario

    infra/libs/local_yp

    yp/python/local
)

ENABLE(NO_STRIP)

TEST_SRCS(
    test.py
)

SIZE(MEDIUM)

END()
