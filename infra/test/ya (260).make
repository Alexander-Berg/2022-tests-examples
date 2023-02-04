PY3TEST()

OWNER(g:rtc-sysdev)

DEPENDS("infra/skyboned/go/cmd")

PEERDIR(
    infra/skyboned/api
)

TEST_SRCS(
    test.py
)

REQUIREMENTS(
    network:full
)

END()
