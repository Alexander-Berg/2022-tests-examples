PY2TEST()

OWNER(g:netmon)

TEST_SRCS(
    test_monotonic.py
)

PEERDIR(
    infra/netmon/agent/contrib/monotonic
)

END()
