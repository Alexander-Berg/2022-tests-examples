PY3TEST()

INCLUDE(../../../yp/python/ya_programs.make.inc)

OWNER(
    g:yp-sc
    g:yp
)

PEERDIR(
    infra/libs/controller/tests
    infra/service_controller/libs/python
)

TIMEOUT(300)

SIZE(MEDIUM)

TEST_SRCS(
    test.py
)

REQUIREMENTS(ram:9)

END()
