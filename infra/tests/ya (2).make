PY2TEST()

OWNER(
    g:nanny
)

TEST_SRCS(
    test_alemate_client.py
    test_snapshots.py
    test_orphaned_yp_pod_sets.py
    test_eviction_requested_pods.py
)

DATA(
    arcadia/infra/watchdog/cfg_default.yml
)

PEERDIR(
    contrib/python/gevent
    contrib/python/mock
    contrib/python/pytest
    contrib/python/requests-mock
    infra/nanny/nanny_repo
    infra/watchdog/src
)

END()
