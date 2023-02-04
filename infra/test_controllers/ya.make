OWNER(g:awacs)

PY3TEST()

DEPENDS(
    infra/awacs/vendor/awacs/tests/deps
    jdk
)

DATA(
    arcadia/infra/awacs/vendor/awacs/tests/deps
    arcadia/infra/awacs/vendor/awacs/tests/test_controllers/cassettes
)

TEST_SRCS(
    conftest.py

    test_backend_ctl.py
    test_cron.py
    test_cron_webauth_syncer.py
    test_knobswatcher.py
    test_managers.py
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

RECURSE(
    test_cert_ctls
    test_dns_record_ctls
    test_domain_ctls
    test_l3_balancer_ctls
    test_l7_balancer_ctls
    test_l7heavy_config_ctls
    test_namespace_ctls
)
