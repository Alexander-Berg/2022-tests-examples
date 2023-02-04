PY2_PROGRAM(sdstub_app)

OWNER(g:awacs)

PY_SRCS(
    __main__.py
)

PEERDIR(
    contrib/python/six
    contrib/python/gevent
    infra/awacs/vendor/awacs/tests/awtest/mocks/sdstub_app/proto
)

END()
