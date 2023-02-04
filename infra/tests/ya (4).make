PY2TEST()

OWNER(g:hostman)

TEST_SRCS(
    test_hostmanager.py
    test_shim.py
)


PEERDIR(
    contrib/python/mock
    infra/ya_salt/hostmanager
)
NO_CHECK_IMPORTS()
END()
