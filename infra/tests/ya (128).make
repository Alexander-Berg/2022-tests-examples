PY2TEST()

OWNER(
    frolstas
)

TEST_SRCS(
    conftest.py
    test_account_poller.py
    test_quota_summary.py
    test_backups.py
    test_list_user_accounts_action.py
    api_tests/test_backup_api.py
    api_tests/test_user_accounts_api.py
    test_lib.py
)

PEERDIR(
    contrib/python/pytest
    contrib/python/mock
    infra/nanny/sepelib/core
    infra/qyp/account_manager/src
)

END()
