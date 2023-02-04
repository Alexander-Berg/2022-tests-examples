PY2TEST()

OWNER(g:hostman)

TEST_SRCS(
    modules/test_linux_sysctl.py
)

PEERDIR(
    contrib/python/mock
    infra/ya_salt/vendor/salt
    infra/ya_salt/lib
)

NO_CHECK_IMPORTS(
)

END()
