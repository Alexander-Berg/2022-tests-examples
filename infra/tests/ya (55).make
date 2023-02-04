PY2TEST()

INCLUDE(../../../yp/python/ya_programs.make.inc)

OWNER(
    amich
    g:deploy
)

PEERDIR(
    infra/libs/controller/tests
    infra/deploy_monitoring_controller/libs/python
)

TIMEOUT(300)

SIZE(MEDIUM)

TEST_SRCS(
    test.py
)

END()
