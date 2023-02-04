PY2TEST()

OWNER(
    g:deploy
    g:deploy-orchestration
)

INCLUDE(../../../../yp/python/ya_programs.make.inc)

TEST_SRCS(
    conftest.py
    test_release_status_controller.py
)

PEERDIR(
    yp/python/local
    infra/libs/local_yp
    contrib/python/pytest
    contrib/python/mock
    infra/release_status_controller/src
    infra/release_status_controller/tests/helpers
)

REQUIREMENTS(
    cpu:4
    ram_disk:4
)
SIZE(MEDIUM)
TIMEOUT(600)

END()
