OWNER(torkve)

PY3TEST()

TEST_SRCS(
    conftest.py
    test_updater.py
)

TIMEOUT(25)

REQUIREMENTS(
    ram_disk:4
)

PEERDIR(
    infra/yp_drcp/lib
    infra/yp_dru/lib
    contrib/python/pytest-asyncio
    contrib/python/mock
)

END()

RECURSE_FOR_TESTS(mypy_tests)
