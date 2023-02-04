PY2TEST()

OWNER(
    g:nanny
)

TAG(
    sb:portod
    ya:manual
)

SIZE(MEDIUM)

INCLUDE(../func/ya.make.inc)

SRCDIR(${INSTANCECTL_TEST_SRCDIR})

TEST_SRCS(
    ${INSTANCECTL_TEST_SRCS}
    func_legacy/coredump_utils.py
    func_legacy/coredump_limits/test_coredumps.py
    func_legacy/faulty_daemon/test_progressive_restart_intervals.py
    func_legacy/its_controls/test_its_controls.py
    func_legacy/minidump_gdb_traces_sending/test_minidump_gdb_traces_sending.py
    func_legacy/coredump_gdb_traces_sending/test_coredump_gdb_traces_sending.py
    func_legacy/coredump_gdb_pattern_match/test_coredump_gdb_pattern_match.py
    func_legacy/notify_script/test_notify_script.py
    func_legacy/report_to_hq/test_report_status_to_hq.py
    func_legacy/scripts/test_scripts.py
    func_legacy/status/test_status.py
    func_legacy/status_process_liveness/test_status.py
    func_legacy/stop_action/test_stop_action.py
    func_legacy/terminate_timeout/test_terminate_timeout.py
    func_legacy/liveness_criterion/test_liveness_criterion.py
    func_legacy/composite_status_check/test_composite_status_check.py
    func_legacy/status_script_timeout/test_status_script_timeout.py
    func_legacy/status_tcp_check/test_status_tcp_check.py
    func_legacy/spec_containers/test_spec_containers.py
    func_legacy/spec_reopen_log_action/test_spec_reopen_log_action.py
    func_legacy/spec_stop_handler/test_spec_stop_handler.py
    func_legacy/spec_porto_access/test_spec_porto_access.py
)

DATA(
    arcadia/infra/nanny/instancectl/tests/func
)

DEPENDS(
    infra/nanny/instancectl/bin
    infra/nanny/instancectl/sd_bin
)

PEERDIR(
    contrib/python/gevent
    contrib/python/Flask

    infra/nanny/clusterpb
    infra/nanny/instancectl/src
    infra/nanny/sepelib/subprocess
    infra/nanny/sepelib/flask
)

REQUIREMENTS(
    ram_disk:4
)

ENV(RUN_PORTO_TESTS=1)

NO_CHECK_IMPORTS()

FORK_TEST_FILES()



END()
