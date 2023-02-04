PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_hostman_units_integration.py
)

PEERDIR(
    infra/rtc/juggler/reconf
)

END()
