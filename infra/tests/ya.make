PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    test_admin_requests.py
    test_audit_log.py
    test_cert.py
    test_cms.py
    test_config_files.py
    test_default_cms.py
    test_downtimes.py
    test_fsm.py
    test_gevent.py
    test_hbf_drills.py
    test_host_health.py
    test_host_macs.py
    test_host_state_gc.py
    test_hosts.py
    test_juggler_checks.py
    test_models.py
    test_mongoengine.py
    test_network.py
    test_operations_log.py
    test_physical_location_tree.py
    test_preorders.py
    test_profile_stat.py
    test_project_automation.py
    test_project_builder.py
    test_projects.py
    test_restrictions.py
    test_shadow_hosts.py
    test_stages.py
    test_statbox.py
    test_stats.py
    test_status_report.py
    test_tasks.py
    test_trypo_radix.py
    test_util_tasks.py
    test_utils.py
)

RESOURCE_FILES(
    ../conf/walle.prod.yaml
    ../conf/walle.test.yaml
)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

SIZE(MEDIUM)

TAG(sb:LINUX_XENIAL)

# for test_admin_requests.py
REQUIREMENTS(network:full)

PEERDIR(
    contrib/python/deepdiff
    contrib/python/mock
    infra/walle/server/walle
    infra/walle/server/tests/lib
    library/python/resource
)

NO_DOCTESTS()

END()

RECURSE_FOR_TESTS(
    api
    clients
    cron
    db_sync
    dns
    expert
    failure_reports
    fsm_stages
    host_platforms
    idm
    maintenance_plot
    scenario
    tasks
    utilities
)
