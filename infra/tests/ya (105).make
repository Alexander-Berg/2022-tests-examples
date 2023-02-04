PY2TEST()

OWNER(
    alonger
)

PEERDIR(
    contrib/python/mock
    infra/nanny/its_client
    infra/nanny/sepelib/flask
)

TEST_SRCS(
    conftest.py
    test_its_poller.py
)

NO_CHECK_IMPORTS()

END()
