PY2TEST()

OWNER(
    g:deploy
    g:deploy-orchestration
)

INCLUDE(../../../../yp/python/ya_programs.make.inc)

TEST_SRCS(
    conftest.py
    test_release_controller.py
    test_release_processor.py
    test_release_selector.py
)

PEERDIR(
    yp/python/local
    infra/libs/local_yp
    contrib/python/pytest
    contrib/python/mock
    infra/release_controller/src
    infra/release_controller/tests/helpers
)

REQUIREMENTS(
    cpu:4
    ram_disk:4
)
SIZE(MEDIUM)
TIMEOUT(600)

END()
