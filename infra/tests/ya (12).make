PY2TEST()

OWNER(g:golovan)

PEERDIR(
    contrib/python/mock
    contrib/python/pytest
    contrib/python/tornado/tornado-4
    infra/yasm/gateway/lib/handlers
)

TEST_SRCS(
    test_local.py
    test_meta_alert.py
    test_rt.py
    test_series.py
    test_top.py
    utils.py
)

END()
