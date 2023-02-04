PY2TEST()

OWNER(
    frolstas
    i-dyachkov
)

PEERDIR(
    contrib/python/freezegun
    contrib/python/pytest
    contrib/python/mock
    contrib/python/fabric
    infra/swatlib
    infra/nanny/sepelib/core
    infra/qyp/vmproxy/src
    infra/qyp/vmctl/src
)

TEST_SRCS(
    vmproxy_tests/__init__.py
    vmproxy_tests/conftest.py
    vmproxy_tests/conftest_test.py
    vmproxy_tests/test_create.py
)

PY_SRCS(
    vmproxy_tests/vmproxy_local.py
)

DATA(
    arcadia/infra/qyp/vmproxy/cfg_default.yml
)

SIZE(MEDIUM)
TAG(
    ya:manual
)

DEPENDS(
    infra/qyp/vmproxy/bin
)

END()
