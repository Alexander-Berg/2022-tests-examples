PY2TEST()

OWNER(g:golovan)

PEERDIR(
    contrib/python/pytest
    infra/yasm/gateway/lib/tags
)

TEST_SRCS(
    test_request.py
)

END()
