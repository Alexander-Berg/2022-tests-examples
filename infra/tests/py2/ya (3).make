PY2TEST()

OWNER(
    alonger
)

PEERDIR(
    contrib/python/mock
    infra/nanny/nanny_rpc_client
)

TEST_SRCS(
    test_retrying_rpc_client.py
)

END()
