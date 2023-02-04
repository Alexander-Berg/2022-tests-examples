PY2TEST()

OWNER(
    minil1
    frolstas
)

TEST_SRCS(
    test_vmagent_watcher.py
)

PEERDIR(
    infra/qyp/vmagent_monitoring/src
    contrib/python/mock
    yp/python/client

)



NO_CHECK_IMPORTS()

END()
