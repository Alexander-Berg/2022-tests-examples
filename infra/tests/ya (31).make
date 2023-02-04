PY2TEST()

OWNER(romanovich)

TEST_SRCS(
    test_main.py
)

PEERDIR(
    infra/awacs/awacsctl2/src
    contrib/python/mock
)

DATA(
    arcadia/infra/awacs/awacsctl2/src/tests/awacsctl-test.cfg
)

END()
