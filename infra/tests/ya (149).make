PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    __init__.py
    test_jdiff.py
)

PEERDIR(
    infra/reconf_juggler/pytest
    infra/reconf_juggler/tools/jdiff
)

END()
