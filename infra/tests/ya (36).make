OWNER(g:awacs)

PY3TEST()

DATA(
    arcadia/infra/awacs/vendor/awacs/tests/awtest/balancer/config
    arcadia/infra/awacs/vendor/awacs/tests/fixtures
    arcadia/infra/awacs/vendor/awacs/tests/cassettes
    arcadia/infra/awacs/vendor/awacs/tests/deps
)

TEST_SRCS(
    conftest.py

    test_alerting.py
    test_apicache.py
    test_cache.py
    test_config_bundle.py
    test_controls_updater.py
    test_easy_mode_generator.py
    test_gencfgclient.py
    test_generator.py
    test_ip_validators.py
    test_itsclient.py

    test_juggler_client.py
    test_l3_mgr_real_server.py
    test_l3mgrclient.py
    test_lib.py
    test_luaparser.py
    test_luautil.py

    test_modern_storage.py
    test_nanny_resolver.py
    test_nannyclient.py
    test_order_processor.py
    test_parser.py

    test_staffclient.py
    test_uiaspectsutil.py
    test_util.py
    test_validation_ctx.py
    test_vectors.py
    test_yamlparser.py
    test_yasm_client.py
    test_yp_resolver.py
    test_zk_storage.py
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

DEPENDS(
    infra/awacs/vendor/awacs/tests/deps
    infra/awacs/vendor/awacs/tests/awtest/mocks/sdstub_app
    infra/awacs/vendor/awacs/tests/awtest/mocks/httpbin_server_app
    jdk
)

TIMEOUT(600)
SIZE(MEDIUM)

NO_CHECK_IMPORTS()

FORK_TESTS()
FORK_SUBTESTS()
SPLIT_FACTOR(10)

END()

RECURSE(
    deps
    test_api
    test_wrappers
    test_controllers
    test_easy_mode
    test_lib_rpc
)
