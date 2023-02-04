OWNER(g:awacs)

PY3TEST()

DEPENDS(
    infra/awacs/vendor/awacs/tests/deps
    jdk
)

DATA(
    arcadia/infra/awacs/vendor/awacs/tests/deps
    arcadia/infra/awacs/vendor/awacs/tests/test_controllers/fixtures
)

TEST_SRCS(
    test_balancer_ctl.py
    test_balancer_ctl_validator.py
    test_balancer_gencfg_migrate_processors.py
    test_balancer_operation_ctl.py
    test_balancer_order_ctl.py
    test_balancer_order_processors.py
    test_balancer_removal_ctl.py
    test_balancer_removal_processors.py
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
