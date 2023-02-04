PY2TEST()

OWNER(
    frolstas
    i-dyachkov
)

TEST_SRCS(
    conftest.py
    test_create.py
    test_validation.py
    test_backup_action.py
    test_config_action.py
    test_list_accounts_action.py
    test_list_free_nodes_action.py
    test_pod_controller.py
    test_service_authorization.py
    test_update_action.py
    test_vm_helpers.py
    test_vmproxy_config.py
    api_tests/test_backup_api.py
    api_tests/test_vmset_service.py
)
DATA(
    arcadia/infra/qyp/vmproxy/cfg_default.yml
)

PEERDIR(
    contrib/python/freezegun
    contrib/python/pytest
    contrib/python/mock
    infra/nanny/sepelib/core
    infra/qyp/vmproxy/src
    library/python/resource
)

REQUIREMENTS(ram:10)

END()
