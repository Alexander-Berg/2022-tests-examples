PY3TEST()

OWNER(
    torkve
    reddi
    alonger
)

INCLUDE(../../../yp/python/ya_programs.make.inc)

TEST_SRCS(
    conftest.py
    test_controller.py
)

PEERDIR(
    infra/libs/local_yp
    contrib/python/pytest
    contrib/python/mock
    contrib/python/pytest-asyncio
    infra/deploy_queue_controller/lib
)

REQUIREMENTS(
    cpu:4
    ram_disk:4 ram:9
)
TIMEOUT(600)
SIZE(MEDIUM)

END()
