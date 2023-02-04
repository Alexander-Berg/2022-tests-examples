PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_builder.py
)

PEERDIR(
    infra/reconf/pytest
    infra/reconf_juggler/examples/complex
    infra/reconf_juggler/pytest
)

END()
