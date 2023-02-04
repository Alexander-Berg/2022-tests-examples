PY2_PROGRAM(httpbin_server_app)

OWNER(g:awacs)

PY_SRCS(
    __main__.py
)

PEERDIR(
    contrib/python/six
    contrib/python/gevent
    infra/swatlib
    infra/awacs/vendor/awacs/tests/awtest/mocks/sdstub_app/proto
    infra/awacs/vendor/httpbin
)

END()
