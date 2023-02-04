PY3TEST()

OWNER(torkve reddi alonger)

PEERDIR(
    library/python/testing/types_test/py3

    infra/deploy_queue_controller/lib
)

TEST_SRCS(
    conftest.py
)

SIZE(MEDIUM)
TIMEOUT(600)
TAG(ya:not_autocheck)  # because currently check is long and fails on yp proto types

END()
