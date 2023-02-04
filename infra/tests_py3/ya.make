PY3TEST()

OWNER(
    g:golovan
)

SRCDIR(infra/yasm/unistat/tests)
TEST_SRCS(
    test_unistat.py
)

PEERDIR(
    infra/yasm/unistat
)

END()
