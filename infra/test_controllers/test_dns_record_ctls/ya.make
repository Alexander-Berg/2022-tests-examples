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
    test_dns_record_ctl.py
    test_dns_record_op_ctl.py
    test_dns_record_op_modify_addresses.py
    test_dns_record_order_ctl.py
    test_dns_record_order_processors.py
    test_dns_record_removal_ctl.py
    test_dns_record_removal_processors.py
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
