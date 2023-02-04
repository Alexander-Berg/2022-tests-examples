PY3TEST()

OWNER(g:tentacles)

PEERDIR(
    infra/rtc_sla_tentacles/backend/lib/tests
    infra/rtc_sla_tentacles/backend/lib/juggler_checks_manager
    infra/rtc_sla_tentacles/backend/lib/reroll_history
)

TEST_SRCS(
    test_history.py
)

END()
