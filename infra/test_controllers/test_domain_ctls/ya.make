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
    test_domain_ctl.py
    test_domain_order_ctl.py
    test_domain_order_processors.py
    test_domain_set_cert_ctl.py
    test_domain_set_cert_processors.py
    test_domain_set_fqdns_ctl.py
    test_domain_set_fqdns_processors.py
    test_domain_set_protocol_ctl.py
    test_domain_set_protocol_processors.py
    test_domain_set_upstreams_ctl.py
    test_domain_transfer_ctl.py
    test_domain_transfer_processors.py
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
