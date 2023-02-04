PY23_LIBRARY()

OWNER(
    g:cores
)

PY_SRCS(
    __init__.py
    site_checker.py
)

PEERDIR(
    library/python/selenium_ui_test
    contrib/python/requests

    infra/cores/app
)

END()

RECURSE(bin)
