PY3TEST()

OWNER(g:tentacles)

PEERDIR(
    contrib/python/freezegun
    infra/rtc_sla_tentacles/backend/lib/funccall_stats_server
)

TEST_SRCS(
    test_funccall_stats_server.py
)

END()
