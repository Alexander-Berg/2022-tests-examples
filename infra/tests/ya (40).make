OWNER(g:hostman)

PY3TEST()

TEST_SRCS(
    __init__.py
    test_cauth_userd.py
)

PEERDIR(
    contrib/python/mock
    infra/cauth/agent/linux/juggler/bundle/checks
)

END()
