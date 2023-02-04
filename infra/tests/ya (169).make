PY2TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_scenario.py
    test_parse_ticket.py
    test_resolv_hosts.py
    test_common.py
    test_testdata_scenario_desc.py
    test_checks.py
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
