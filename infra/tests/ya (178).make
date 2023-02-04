PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_numa_memorybw.py
)

PEERDIR(
    infra/rtc/juggler/bundle/pytest
)

END()
