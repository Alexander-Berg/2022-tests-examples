PY2TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_multi_dc_subticket.py
    test_power_off_message.py
)

PEERDIR(
    infra/rtc/janitor
    contrib/python/pytest
    contrib/python/mock
)

END()
