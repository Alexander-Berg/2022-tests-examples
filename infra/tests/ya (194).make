PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_builder.py
)

PEERDIR(
    infra/rtc/juggler/reconf/builders/projects/rtc

    infra/reconf_juggler/pytest
)

IF (NOT AUTOCHECK)
    TAG(ya:not_autocheck)
    TIMEOUT(3600)
ENDIF()

END()
