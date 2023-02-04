OWNER(g:awacs)

PY3TEST()

DATA(
    arcadia/infra/awacs/vendor/awacs/tests/awtest/balancer/config
    arcadia/infra/awacs/vendor/awacs/tests/fixtures
    arcadia/infra/awacs/vendor/awacs/tests/deps
)

DEPENDS(
    infra/awacs/vendor/awacs/tests/deps
    infra/awacs/vendor/awacs/tests/awtest/mocks/sdstub_app
    infra/awacs/vendor/awacs/tests/awtest/mocks/httpbin_server_app
    jdk
)

TEST_SRCS(
    __init__.py
    test_easy_mode.py
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

FORK_TESTS()
FORK_SUBTESTS()
SPLIT_FACTOR(10)

END()
