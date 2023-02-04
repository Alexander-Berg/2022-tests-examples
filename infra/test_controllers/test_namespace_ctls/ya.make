OWNER(g:awacs)

PY3TEST()

DEPENDS(
    infra/awacs/vendor/awacs/tests/deps
    jdk
)

DATA(
    arcadia/infra/awacs/vendor/awacs/tests/deps
    arcadia/infra/awacs/vendor/awacs/tests/test_controllers/test_namespace_ctls/cassettes
)

TEST_SRCS(
    conftest.py
    operations_util.py
    test_namespace_ctl.py
    test_namespace_its_ctl.py
    test_namespace_op_ctl.py
    test_namespace_order_ctl.py
    test_namespace_order_e2e.py
    test_namespace_order_processors.py
    test_namespace_validation.py
    test_operation_add_ip_to_l3.py
    test_operation_import_vs_from_l3mgr.py
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
