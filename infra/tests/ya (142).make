PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_builder.py
)

PEERDIR(
    infra/reconf/pytest
    infra/reconf_juggler/examples/declared
    infra/reconf_juggler/pytest
)

END()
