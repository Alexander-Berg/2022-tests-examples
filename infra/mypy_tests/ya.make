PY3TEST()

OWNER(torkve)

PEERDIR(
    library/python/testing/types_test/py3

    infra/yp_dru/lib
)

TEST_SRCS(
    conftest.py
)

SIZE(MEDIUM)
TIMEOUT(600)

END()
