PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_hbf_agent_rtc.py
)

PEERDIR(
    infra/rtc/juggler/bundle/pytest
)

END()
