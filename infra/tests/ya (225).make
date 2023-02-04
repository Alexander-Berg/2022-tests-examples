PY3TEST()

OWNER(g:tentacles)

PEERDIR(
    infra/rtc_sla_tentacles/backend/lib
    infra/rtc_sla_tentacles/backend/lib/config
    infra/rtc_sla_tentacles/backend/lib/api
    infra/rtc_sla_tentacles/backend/lib/juggler_checks_manager
    infra/rtc_sla_tentacles/backend/lib/incidents
    infra/rtc_sla_tentacles/backend/lib/tests
)

TEST_SRCS(
    test_api.py
    conftest.py
)

END()
