PY2TEST()

INCLUDE(../../../yp/python/ya_programs.make.inc)

OWNER(
    g:deploy
)

PEERDIR(
    infra/libs/controller/tests
    infra/resource_cache_controller/libs/python
)

TIMEOUT(300)

SIZE(MEDIUM)

TEST_SRCS(
    test.py
)

REQUIREMENTS(ram:10)

END()
