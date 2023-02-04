PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    __init__.py
    test_jdump.py
)

PEERDIR(
    infra/reconf_juggler/pytest
    infra/reconf_juggler/tools/jdump
)

END()
