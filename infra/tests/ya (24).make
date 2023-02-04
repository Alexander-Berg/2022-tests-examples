PY3TEST()

OWNER(
    torkve
)

INCLUDE(../../../yp/python/ya_programs.make.inc)

TEST_SRCS(
    conftest.py
    test_poller.py
    test_updater.py
)

PEERDIR(
    yp/python/local
    infra/libs/local_yp
    contrib/python/pytest
    contrib/python/mock
    contrib/python/pytest-asyncio
    infra/yp_drp/lib
)

REQUIREMENTS(
    cpu:4
    ram_disk:4 ram:9
)
TIMEOUT(600)
SIZE(MEDIUM)

END()
