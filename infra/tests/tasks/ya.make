PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    test_hardware_repair_task.py
    test_sb_helpers.py
    test_schedule_from_scenario.py
    test_stages.py
    test_task_args.py
    test_task_builders.py
)

SIZE(MEDIUM)

TAG(sb:LINUX_XENIAL)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

PEERDIR(
    contrib/python/deepdiff
    contrib/python/mock
    infra/walle/server/walle
    infra/walle/server/tests/lib
    library/python/resource
)

END()
