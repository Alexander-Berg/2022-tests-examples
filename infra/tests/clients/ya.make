PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    network/test_netmap.py
    network/test_network_client.py
    network/tests-racktables-client.py
    test_abc.py
    test_bot.py
    test_cauth.py
    test_certificator_client.py
    test_cms.py
    test_dns_api.py
    test_eine.py
    test_hbf.py
    test_idm.py
    test_inventory.py
    test_ipmiproxy.py
    test_juggler.py
    test_netmon.py
    test_ok.py
    test_provider.py
    test_qloud.py
    test_racktables.py
    test_ssh.py
    test_staff.py
    test_startrek.py
    test_tvm.py
    test_vlan_toggler.py
    test_utils.py
    test_yasubr.py
)

RESOURCE_FILES(
    mocks/bot_get_known_hosts.txt
    mocks/bot_consist_of_response.json
    mocks/bot_os_info_response.json
    mocks/bot_get_locations.txt
    mocks/bot_get_host_location_info.txt
    mocks/bot_get_oebs_projects.json
    mocks/bot_missed_preordered_hosts.json
    mocks/dns_data.json
    mocks/get-l123-active.txt
    mocks/get-l123-not-active.txt
    mocks/hbf-drills.json
    mocks/interconnect_switch_list.txt
    mocks/l3-tors.txt
    mocks/networklist.nat64.txt
    mocks/networklist.tun64.txt
    mocks/staff_groups_batch.json
    mocks/vm-projects.txt
)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

SIZE(MEDIUM)

TAG(sb:LINUX_XENIAL)

# for test_bot.py, test_inventory.py, test_netmon.py
REQUIREMENTS(
    network:full
    ram:9
)

IF (NOT NO_FORK_TESTS)
    FORK_SUBTESTS(MODULO)
    SPLIT_FACTOR(4)
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
