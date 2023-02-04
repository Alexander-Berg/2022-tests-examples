PY2TEST()

OWNER(g:hostman)

TEST_SRCS(
    test_cmd.py
)

PEERDIR(
    contrib/python/mock
    infra/ya_salt/cmd
)

NO_CHECK_IMPORTS()

END()
