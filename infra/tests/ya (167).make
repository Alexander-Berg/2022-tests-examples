PY2TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    data_host_resolver_cache.py

    test_stage_resolve_hosts.py
    test_stage_validate_dest_project.py
    test_validate_scenario.py
    test_check_intersept_started.py
    test_check_intersept_poweroff.py
    test_check_task_count.py
    test_testdata_scenario_desc.py
    test_process_multi_dc_task.py
    test_add_preorder.py
    test_process_multi_dc_ticket.py
)

PEERDIR(
    infra/rtc/janitor
    infra/rtc/janitor/templates
    infra/rtc/janitor/clients
    infra/rtc/janitor/fsm_stages
    contrib/python/pytest
    contrib/python/mock
)

END()
