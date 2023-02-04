PY2TEST()

FORK_SUBTESTS()

SPLIT_FACTOR(80)

REQUIREMENTS(
    cpu:4
    ram:23
)

SIZE(MEDIUM)
TIMEOUT(600)

IF(CLANG_COVERAGE)
    TAG(ya:not_autocheck)
ENDIF()

PEERDIR(
    balancer/test/configs/lib
    balancer/test/mock/mocked_balancer
)

TEST_SRCS(${SRC_PATH})

DEPENDS(
    balancer/config/build/${PROJECT}
    balancer/production/x/sd_mock
    balancer/test/mock/mock_server
    balancer/daemons/balancer
)

DATA(
    arcadia/balancer/test/plugin/certs/data
    arcadia/balancer/test/mock/mock_server/static_responses
    arcadia/search/spi-tools/alerts-providers/l7_kubr
    arcadia/search/spi-tools/alerts-providers/l7_knoss_search
    arcadia/search/spi-tools/alerts-providers/l7_knoss_morda
)

END()
