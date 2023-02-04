OWNER(torkve)

PY3TEST()

TEST_SRCS(
    test_poll.py
)

TIMEOUT(25)

PEERDIR(
    infra/yp_drcp/lib
    contrib/python/pytest-asyncio
)

END()

