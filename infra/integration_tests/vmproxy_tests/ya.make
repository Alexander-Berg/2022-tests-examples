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
    infra/nanny/sepelib/core
    infra/qyp/vmproxy/src
    infra/qyp/vmctl/src
)

TEST_SRCS(
    conftest.py
    test_vmproxy.py
)

SIZE(MEDIUM)

DEPENDS(
    infra/qyp/vmproxy/bin
)

END()
