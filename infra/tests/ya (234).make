PY3TEST()

OWNER(g:tentacles)

PEERDIR(
    contrib/python/freezegun

    infra/rtc_sla_tentacles/backend/lib/tests
    infra/rtc_sla_tentacles/backend/lib/yp_lite
    infra/rtc_sla_tentacles/backend/lib/juggler_checks_manager
)

TEST_SRCS(
    test_pods_manager.py
)

END()
