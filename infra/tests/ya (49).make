PY2TEST()

OWNER(torkve)

FORK_TESTS()
# SIZE(LARGE)
# REQUIREMENTS(
#     container:706562633
# )

PEERDIR(
    infra/cqudp/src
    infra/netlibus/pylib
    infra/skylib/porto
    contrib/python/mock
    contrib/python/gevent
)

PY_SRCS(
    TOP_LEVEL
    common.py
    compat.py
)

TEST_SRCS(
    test_aggregation.py
    test_api.py
    test_client_daemon_client.py
    test_client_daemon.py
    test_client_unpickling.py
    test_eggs.py
    test_execution.py
    test_gevent.py
    test_messagebus.py
    test_mocksoul_rpc.py
    test_poll.py
    test_processhandle.py
    test_protocol.py
    test_resending.py
    test_rpc.py
    test_scheduler.py
    test_session.py
    test_taskhandle.py
    test_taskmgr.py
    test_window.py
)

TAG(
    # ya:fat
    ya:external
    ya:manual
    ya:not_autocheck
    # ya:privileged
)

NO_CHECK_IMPORTS()

END()
