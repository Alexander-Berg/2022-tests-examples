PY2TEST()

OWNER(
    g:deploy-orchestration
)

TEST_SRCS(
    conftest.py
    test_controller.py
    test_podutil.py
    test_reflector.py
    test_runner.py
    test_state.py
    test_status_maker.py
    test_storage.py
    test_yp_client.py
    test_yp_pb_client.py
    test_model.py
)

DATA(
    arcadia/infra/mc_rsc/tests/unit/cfg_test.yaml
)

PEERDIR(
    contrib/python/pytest
    contrib/python/mock
    infra/nanny/sepelib/core
    infra/mc_rsc/src
)

END()
