PY3_LIBRARY()

OWNER(g:tentacles)

PEERDIR(
    contrib/python/freezegun
    contrib/python/mongomock

    infra/rtc_sla_tentacles/backend/lib/config
    infra/rtc_sla_tentacles/backend/lib/incidents
    infra/rtc_sla_tentacles/backend/lib/tests
)

PY_SRCS(
    conftest.py
)

END()
