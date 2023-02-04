PY2TEST()

OWNER(
    frolstas
)

TEST_SRCS(
    test_vmworker.py
)

PEERDIR(
    contrib/python/mock
    infra/vmagent/src/vmagent
)


NO_CHECK_IMPORTS()

END()
