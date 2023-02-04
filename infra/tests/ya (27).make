PY3TEST()

OWNER(g:yp)

PEERDIR(
    infra/yp_util/lib

    contrib/python/mock
)

TEST_SRCS(
    helpers.py
    test_account_explain.py
    test_nodes_resources.py
)

END()
