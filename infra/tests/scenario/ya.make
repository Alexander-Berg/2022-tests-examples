PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    conftest.py
    data_storage/test_default_data_storage.py
    host_groups_builders/test_by_bot_project_id.py
    host_groups_builders/test_by_maintenance_plot.py
    host_groups_builders/test_by_specific_project_tag.py
    host_groups_builders/test_hosts_list_splitters.py
    host_groups_builders/test_hosts_properties_getter.py
    host_groups_builders/test_maintenance_plot_getters.py
    scenario_execution/test_hosts_add_script.py
    scenario_execution/test_switch_to_maintenance_with_groups_approves.py
    scenario_execution/test_noc_soft_scenario_execution.py
    scenario_execution/noc_hard.py
    stage/test_acquire_permission.py
    stage/test_approve_stage.py
    stage/test_check_and_report_about_x_stages.py
    stage/test_conditional_host_stage.py
    stage/test_create_startrek_ticket.py
    stage/test_dump_approvers_list.py
    stage/test_ensure_dns_access_stage.py
    stage/test_detect_storage_stage.py
    stage/test_host_group_approve_stage.py
    stage/test_host_group_scheduler_stage.py
    stage/test_host_group_wait_before_requesting_cms_stage.py
    stage/test_lambda_stage.py
    stage/test_noc_maintenance_stage.py
    stage/test_scheduler_stage.py
    stage/test_timeout_stage.py
    test_cancel.py
    test_common.py
    test_error_handlers.py
    test_handlers.py
    test_hsi_gc.py
    test_iteration_strategy.py
    test_maintenance_plot_checks.py
    test_mixins.py
    test_scenario.py
    test_scenario_authorization.py
    test_scenario_message.py
    test_scheduler.py
    test_script.py
    test_stage_info.py
    test_stage_generators.py
    test_stages.py
    test_utils.py
    test_validators.py
)

PY_SRCS(utils.py)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

SIZE(MEDIUM)

TAG(sb:LINUX_XENIAL)

PEERDIR(
    contrib/python/deepdiff
    contrib/python/mock
    infra/walle/server/walle
    infra/walle/server/proto
    infra/walle/server/tests/lib
    library/python/resource
)

NO_DOCTESTS()

END()
