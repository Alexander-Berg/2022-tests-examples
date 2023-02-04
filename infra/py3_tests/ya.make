PY3TEST()

OWNER(
    g:nanny
)

TEST_SRCS(
    test_stubs.py
)

PEERDIR(
    infra/nanny/yp_lite_api/py_stubs
    infra/nanny/nanny_rpc_client
)

END()
