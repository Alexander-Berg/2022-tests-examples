PY2TEST()

OWNER(
    g:deploy-orchestration
)

TEST_SRCS(
    conftest.py
    test_app.py
    test_rs_updater.py
    test_podutil.py
    test_yp_client.py
    test_pod_set_labels_validation.py
    test_storage.py
)

PEERDIR(
    contrib/python/pytest
    contrib/python/mock
    infra/nanny/sepelib/core
    infra/rsc/src
)

END()
