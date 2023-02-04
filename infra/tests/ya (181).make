PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_conventions.py
    test_external.py
    test_util.py
)

PEERDIR(
    infra/rtc/juggler/bundle/checks
    infra/rtc/juggler/bundle/util
)

END()
