PY3_PROGRAM(dummy-tasklet)

OWNER(g:tasklet)

PY_SRCS(
    __main__.py
)

PEERDIR(
    tasklet/api/v2
    tasklet/experimental/sdk/py/dummy
)

END()
