PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    test_api.py
    test_approvement_tools.py
    test_cache.py
    test_counter.py
    test_cron.py
    test_db_cache.py
    test_deploy_config_gen_policies.py
    test_jsonschema.py
    test_limits.py
    test_locks.py
    test_log_tools.py
    test_misc.py
    test_mongo_partitioner.py
    test_net.py
    test_notifications.py
    test_rate_limiter.py
    test_template_renderer.py
    test_validation.py
    test_workdays.py
)

RESOURCE_FILES(infra/walle/server/walle/templates/test.html)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

PEERDIR(
    contrib/python/deepdiff
    contrib/python/mock
    infra/walle/server/walle
    infra/walle/server/tests/lib
    library/python/resource
)

SIZE(MEDIUM)

TAG(sb:LINUX_XENIAL)

NO_DOCTESTS()

END()
