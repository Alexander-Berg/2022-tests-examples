PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_builder.py
)

PEERDIR(
    infra/rtc/juggler/reconf/builders/projects/salt_masters

    infra/reconf_juggler/pytest
)

END()
