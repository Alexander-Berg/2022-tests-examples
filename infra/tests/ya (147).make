PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    __init__.py
    test_jconv.py
)

PEERDIR(
    infra/reconf_juggler/tools/jconv
)

END()
