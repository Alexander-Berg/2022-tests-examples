PY3TEST()

OWNER(g:tasklet)

SIZE(SMALL)

PY_SRCS(
    __init__.py
    conftest.py
)

TEST_SRCS(
    test_executor.py
    test_local_service.py
)

DEPENDS(
    jdk
    tasklet/experimental/cmd/executor
    tasklet/experimental/tests/tasklets/dummy_go_tasklet
    tasklet/experimental/tests/tasklets/dummy_java_tasklet
    tasklet/experimental/tests/tasklets/dummy_tasklet
)

PEERDIR(
    contrib/python/protobuf

    tasklet/api/v2
    tasklet/experimental/tests/common
)

FORK_SUBTESTS()

INCLUDE(../common/ya.make.tasklet_server_recipe.inc)

END()
