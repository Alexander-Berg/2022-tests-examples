PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    brewer_test.py
    test_skyboned_helper.py
)

DEPENDS(
    infra/rtc/docker_registry/docker_torrents
    infra/rtc/docker_registry/docker_torrents/tests/util
    infra/rtc/docker_registry/docker_torrents/lib
)

PEERDIR(
    infra/rtc/docker_registry/docker_torrents/tests/util
    infra/rtc/docker_registry/docker_torrents/lib
)

END()

RECURSE(
    util
)
