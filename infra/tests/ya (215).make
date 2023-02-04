PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    __init__.py
    test_builder.py
)

PEERDIR(
    infra/rtc/juggler/reconf/builders
    infra/rtc/juggler/reconf/checks
)

IF (NOT AUTOCHECK)
    TAG(ya:not_autocheck)
    TIMEOUT(3600)
ENDIF()

END()
