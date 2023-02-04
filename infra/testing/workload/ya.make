PY3_PROGRAM()

OWNER(okats)

PEERDIR(
    contrib/python/gevent
    infra/callisto/protos/deploy
)

PY_SRCS(
    MAIN main.py
)

END()
