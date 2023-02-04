PY3TEST()

OWNER(g:skynet)

PEERDIR(
    library/python/testing/types_test/py3

    infra/logger
)

TEST_SRCS(
    conftest.py
)

SIZE(MEDIUM)
TIMEOUT(600)

END()
