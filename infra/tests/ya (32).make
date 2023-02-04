OWNER(keepclean)

PY2TEST()

TEST_SRCS(
    test_config.py
)

SET(AWACS_NAMESPACE infra/awacs/templates/ab-lb.search.yandex.net)

INCLUDE(${ARCADIA_ROOT}/infra/awacs/test/Test.inc)

END()
