OWNER(
    g:cores
    mvel
)

PY3_PROGRAM(cores_ui_tests)

PY_SRCS(
    __main__.py
)

PEERDIR(
    contrib/python/coloredlogs
    infra/cores/ui_tests
)

END()
