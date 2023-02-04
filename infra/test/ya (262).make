PY2TEST()

OWNER(g:walle)

TEST_SRCS(
    test.py
)

PEERDIR(
    infra/wall-e/agent
    contrib/python/pytest
    contrib/python/mock

    contrib/python/pyroute2
    contrib/deprecated/python/ipaddress
)

END()
