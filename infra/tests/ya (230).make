PY3TEST()

OWNER(g:tentacles)

PEERDIR(
    infra/rtc_sla_tentacles/backend/lib/incidents
    infra/rtc_sla_tentacles/backend/lib/incidents/tests_incidents_data
    infra/rtc_sla_tentacles/backend/lib/tests
)

TEST_SRCS(
    test_storage.py
)

END()
