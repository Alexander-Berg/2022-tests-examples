PY3TEST()

OWNER(g:tentacles)

PEERDIR(
    infra/rtc_sla_tentacles/backend/lib/juggler_checks_manager
    infra/rtc_sla_tentacles/backend/lib/harvesters_snapshots/tests_snapshots_data
    infra/rtc_sla_tentacles/backend/lib/tests
)

TEST_SRCS(
    test_snapshot_manager.py
)

END()
