PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    test_acquire_permission.py
    test_add_host_to_cauth.py
    test_allocate_hostname.py
    test_assign_bot_project.py
    test_assign_hostname.py
    test_cancel_admin_requests.py
    test_certificate.py
    test_change_disk.py
    test_cloud_post_processor.py
    test_common.py
    test_complete_deletion.py
    test_complete_preparing.py
    test_complete_releasing.py
    test_deactivation.py
    test_dns.py
    test_drop_cms_task.py
    test_eine_profiling.py
    test_hw_errors.py
    test_hw_repair.py
    test_ipmi_errors.py
    test_log_completed_operation.py
    test_lui_deploying.py
    test_kexec_ssh.py
    test_monitor.py
    test_network_location.py
    test_power_ipmi.py
    test_power_ssh.py
    test_provide_diagnostic_host_access.py
    test_report_check_failure.py
    test_report_rack_failure.py
    test_report_stage_handler.py
    test_reset_bmc.py
    test_reset_health_status.py
    test_set_downtime.py
    test_set_state.py
    test_startrek_reports.py
    test_switch_default_cms_project.py
    test_switch_project.py
    test_vlan_switching.py
)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

SIZE(MEDIUM)

TAG(sb:LINUX_XENIAL)

IF (NOT NO_FORK_TESTS)
    FORK_SUBTESTS(MODULO)
    SPLIT_FACTOR(2)
ENDIF()

PEERDIR(
    contrib/python/deepdiff
    contrib/python/mock
    infra/walle/server/walle
    infra/walle/server/tests/lib
    library/python/resource
)

NO_DOCTESTS()

END()
