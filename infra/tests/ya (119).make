PY2TEST()

OWNER(g:netmon)

SIZE(MEDIUM)

ALLOCATOR(J)

TEST_SRCS(
    conftest.py
    test_application.py
    test_controllers.py
    test_diagnostic_planner.py
    test_encoding.py
    test_rpc.py
    test_selector.py
    test_sender.py
    test_backend_maintainer.py
    test_tasks.py
    test_ticker.py
    test_topology.py
    test_transformers.py
    test_utils.py
)

DEPENDS(infra/netmon/agent/agent)

PEERDIR(
    infra/netmon/agent/agent
    contrib/python/mock
)

DATA(
    sbr://1586663273
)

END()
