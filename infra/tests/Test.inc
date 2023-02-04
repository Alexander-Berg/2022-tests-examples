PY2TEST()

SRCDIR(infra/awacs/templates/addrs.yandex.ru/tests)

TEST_SRCS(
    test_config.py
)

SET(AWACS_NAMESPACE infra/awacs/templates/addrs.yandex.ru)

INCLUDE(${ARCADIA_ROOT}/infra/awacs/test/Test.inc)

END()
