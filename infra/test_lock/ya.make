PY2_PROGRAM()

OWNER(okats)

PEERDIR(
    contrib/python/gevent
    infra/callisto/libraries/discovery
    infra/callisto/libraries/yt
)

PY_SRCS(
    MAIN main.py
)

END()
