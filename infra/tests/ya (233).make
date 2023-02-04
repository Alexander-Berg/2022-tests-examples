PY3_LIBRARY()

OWNER(g:tentacles)

PEERDIR(
    contrib/python/PyYAML
    contrib/python/mongomock
    infra/rtc_sla_tentacles/backend/lib/config
)

PY_SRCS(
    config_example1.py
    conftest.py
)

END()
