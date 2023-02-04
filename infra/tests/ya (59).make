PY2TEST()

OWNER(g:hostman)

TEST_SRCS(
    conftest.py
    test_common.py
    test_repo_manage.py
    test_version.py
    utils.py
    dbal/__init__.py
    dbal/test_package_repository.py
    dbal/test_package.py
    dbal/test_keyring.py
    dbal/test_deleted_key.py
    dbal/test_ubuntu_upstream.py
    dbal/test_package_index.py
    dbal/test_package_index_history.py
    notifications/__init__.py
    notifications/test_factory.py
    notifications/test_policy.py
    notifications/test_notification.py
    test_microdinstall.py
)

DATA(
    arcadia/infra/dist/cacus/tests/cacus.yaml
    arcadia/infra/dist/cacus/tests/test.changes
)

PEERDIR(
    contrib/python/mock
    contrib/python/mongomock
    infra/dist/cacus/lib
)


END()
