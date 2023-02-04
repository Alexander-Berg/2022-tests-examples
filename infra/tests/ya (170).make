PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_check.py
)

PEERDIR(
    infra/rtc/juggler/bundle/pytest
)

END()
