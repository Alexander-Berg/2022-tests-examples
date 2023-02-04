OWNER(g:awacs)

PY3TEST()

DATA(
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
    test_lib_rpc_authentication.py
    test_lib_rpc_blueprint.py
    test_lib_rpc_parse_request.py
)

PEERDIR(
    contrib/python/mock
    contrib/python/flaky
    contrib/python/pytest-vcr
    contrib/python/vcrpy
    infra/swatlib
    infra/awacs/vendor/awacs
    infra/awacs/vendor/awacs/tests/awtest
    infra/awacs/vendor/awacs/tests/test_lib_rpc/proto
)

TIMEOUT(600)
SIZE(MEDIUM)

NO_CHECK_IMPORTS()

END()


RECURSE(
    proto
)
