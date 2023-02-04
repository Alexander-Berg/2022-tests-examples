PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    conftest.py
    test_bot_project.py
    test_eine_netmap.py
    test_inventory.py
    test_netmap.py
    test_network_info_sync.py
    test_physical_location.py
    test_sync_rack_topology.py
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
