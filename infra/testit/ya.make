PY2_PROGRAM(testit)

OWNER(g:golovan)

PY_SRCS(
    TOP_LEVEL
    __main__.py
    app.py
    handlers.py
    push_worker.py
)

PEERDIR(
    contrib/python/tornado/tornado-4
)

END()
