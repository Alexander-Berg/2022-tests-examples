PY3TEST()

OWNER(
    slonnn
    g:yp
)

TEST_SRCS(
    test_main.py
)


PEERDIR(
    contrib/python/mock
    infra/yp/monitoring/account_overuse_monitoring/lib
)

FORK_SUBTESTS()


END()
