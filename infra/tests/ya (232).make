PY3TEST()

OWNER(g:tentacles)

PEERDIR(
    contrib/python/freezegun
    infra/rtc_sla_tentacles/backend/lib/tentacle_agent
)

TEST_SRCS(
    test_tentacle_state.py
)

END()
