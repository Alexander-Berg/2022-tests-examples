OWNER(g:awacs)

PY3TEST()

DEPENDS(
    infra/awacs/vendor/awacs/tests/deps
    jdk
)

DATA(
    arcadia/infra/awacs/vendor/awacs/tests/deps
)

TEST_SRCS(
    conftest.py
    test_l3_balancer_ctl.py
    test_l3_balancer_order_ctl.py
    test_l3_balancer_order_processors.py
)

PEERDIR(
    contrib/python/mock
    contrib/python/flaky
    contrib/python/pytest-vcr
    contrib/python/vcrpy
    infra/swatlib
    infra/awacs/vendor/awacs
    infra/awacs/vendor/awacs/tests/awtest
)

TIMEOUT(600)
SIZE(MEDIUM)

NO_CHECK_IMPORTS()

END()
