PY3_LIBRARY()

OWNER(g:tentacles)

PEERDIR(
    infra/rtc_sla_tentacles/backend/lib/config
    infra/rtc_sla_tentacles/backend/lib/harvesters
    infra/rtc_sla_tentacles/backend/lib/harvesters_snapshots
    infra/rtc_sla_tentacles/backend/lib/tests
)

PY_SRCS(
    conftest.py
)

END()
