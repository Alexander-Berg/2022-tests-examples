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
    test_components.py
    test_namespaces.py
    test_endpoint_sets.py
    test_info_service.py
    test_domains.py
    test_knobs.py
    test_certificates.py
    test_l3_balancers.py
    test_backends.py
    test_balancers.py
    test_upstreams.py
    test_dns_records.py
    test_app.py
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
