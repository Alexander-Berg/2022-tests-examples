PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_tcp_sampler_dump.py
)

PEERDIR(
    infra/rtc/juggler/bundle/pytest
)

END()
