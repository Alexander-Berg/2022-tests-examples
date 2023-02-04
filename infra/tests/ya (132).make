PY2TEST()

OWNER(
    i-dyachkov
    frolstas
)

TEST_SRCS(
    conftest.py
    test_vmctl.py
    test_ctl_options.py
)

PEERDIR(
    contrib/python/mock
    contrib/python/Flask
    contrib/python/WebTest
    infra/qyp/vmctl/src
)


NO_CHECK_IMPORTS()


END()
