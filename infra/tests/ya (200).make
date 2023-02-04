PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_builder.py
)

PEERDIR(
    infra/reconf/pytest
    infra/reconf_juggler/pytest

    infra/rtc/juggler/reconf/builders/projects/sysdev_hbf_drops
)

IF (NOT AUTOCHECK)
    TAG(ya:not_autocheck)
    TIMEOUT(3600)
ENDIF()

END()
