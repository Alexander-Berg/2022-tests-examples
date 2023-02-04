OWNER(g:tasklet)

PY3TEST()

SIZE(MEDIUM)

TEST_SRCS(
    __init__.py
    conftest.py
    test_app.py
    test_builds.py
    test_executions.py
    test_labels.py
    test_namespaces.py
    test_schema_registry.py
    test_tasklets.py
)

PEERDIR(
    tasklet/experimental/tests/common
    tasklet/experimental/examples/proto
)

FORK_SUBTESTS()

INCLUDE(common/ya.make.tasklet_server_recipe.inc)

END()

RECURSE(
    common
    executor
    tasklets
)
