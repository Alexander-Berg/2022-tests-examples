PY2TEST()

OWNER(g:golovan)

PEERDIR(
    contrib/libs/grpc/python
    contrib/python/mock
    contrib/python/pytest
    contrib/python/pytest-tornado
    contrib/python/tornado/tornado-4
    infra/yasm/gateway/lib/client
)

TEST_SRCS(
    test_cluster_provider.py
    test_requester.py
)

END()
