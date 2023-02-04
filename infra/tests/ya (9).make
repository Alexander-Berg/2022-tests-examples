PY2TEST()

OWNER(
    g:golovan
)

TEST_SRCS(
    test_player.py
)

DEPENDS(
    infra/yasm/agent/player
)

PEERDIR(
    infra/yasm/interfaces
    contrib/python/msgpack
    contrib/python/protobuf_to_dict
)

END()
