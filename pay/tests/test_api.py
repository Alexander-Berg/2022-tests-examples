import os
import pytest
from mongo_idm.api import info, get_all_roles, add_role, remove_role
from mongo_idm.config import DEFAULT_DB_ROLES
from mongo_idm.helpers import create_vault_secret


def users_set_ok(db_id):
    return {
        "users": [
            {
                "_id": db_id + ".test",
                "user": "test",
                "db": db_id,
                "customData": {
                    "idm": True
                },
                "roles": [
                    {
                        "role": "changeOwnPassword",
                        "db": db_id
                    }
                ]
            },
            {
                "_id": db_id + ".testR",
                "user": "testR",
                "db": db_id,
                "customData": {
                    "idm": True
                },
                "roles": [
                    {
                        "role": "read",
                        "db": db_id
                    },
                    {
                        "role": "changeOwnPassword",
                        "db": db_id
                    }
                ]
            },
            {
                "_id": db_id + ".testA",
                "user": "testA",
                "db": db_id,
                "customData": {
                    "idm": True
                },
                "roles": [
                    {
                        "role": "dbAdmin",
                        "db": db_id
                    },
                    {
                        "role": "changeOwnPassword",
                        "db": db_id
                    }
                ]
            },
            {
                "_id": db_id + ".testRW",
                "user": "testRW",
                "db": db_id,
                "customData": {
                    "idm": True
                },
                "roles": [
                    {
                        "role": "readWrite",
                        "db": db_id
                    },
                    {
                        "role": "changeOwnPassword",
                        "db": db_id
                    }
                ]
            },
            {
                "_id": db_id + ".testRWA",
                "user": "testRWA",
                "db": db_id,
                "customData": {
                    "idm": True
                },
                "roles": [
                    {
                        "role": "readWrite",
                        "db": db_id
                    },
                    {
                        "role": "dbAdmin",
                        "db": db_id
                    },
                    {
                        "role": "changeOwnPassword",
                        "db": db_id
                    }
                ]
            },
        ]
    }


def users_set_not_ok(db_id):
    return {
        "users": [
            {
                "_id": db_id + ".test",
                "user": "test",
                "db": db_id,
                "customData": {
                    "idm": True
                },
                "roles": [
                ]
            },
            {
                "_id": db_id + ".testR",
                "user": "testR",
                "db": db_id,
                "customData": {
                    "idm": True
                },
                "roles": [
                    {
                        "role": "changeOwnPassword",
                        "db": db_id
                    }
                ]
            },
            {
                "_id": db_id + ".testA",
                "user": "testA",
                "db": db_id,
                "roles": [
                    {
                        "role": "dbAdmin",
                        "db": db_id
                    },
                    {
                        "role": "changeOwnPassword",
                        "db": db_id
                    }
                ]
            },
            {
                "_id": db_id + ".testRW",
                "user": "testRW",
                "db": db_id,
                "customData": {
                    "idm": True
                },
                "roles": [
                    {
                        "role": "readWrite",
                        "db": db_id
                    },
                ]
            },
        ]
    }


@pytest.fixture(autouse=True)
def patch_create_vault_secret(monkeypatch):
    def create_vault_secret_noop(*args, **kwargs):
        pass

    def create_vault_secret_changed_username(db_id, user, password):
        create_vault_secret(db_id, "robot-ps-test-salt", password)

    if "YAV_OAUTH_TOKEN" in os.environ:
        monkeypatch.setattr("mongo_idm.helpers.create_vault_secret", create_vault_secret_changed_username)
    else:
        monkeypatch.setattr("mongo_idm.helpers.create_vault_secret", create_vault_secret_noop)


class TestApi:
    def test_info(self, mocker):
        mocker.patch("mongo_idm.helpers.get_databases", return_value=[])
        assert info() == {
            "slug": "database",
            "name": {
                    "en": u"Database",
                    "ru": u"База",
            },
            "values": {},
        }

        mocker.patch(
            "mongo_idm.helpers.get_databases",
            return_value=["test", "test-1", "test_2"]
        )
        assert info() == {
            "slug": "database",
            "name": {
                    "en": u"Database",
                    "ru": u"База",
            },
            "values": {
                "test": {
                    "name": {
                        "ru": "test",
                        "en": "test"
                    },
                    "roles": {
                        "values": DEFAULT_DB_ROLES,
                        "slug": "role",
                    }
                },
                "test-1": {
                    "name": {
                        "ru": "test-1",
                        "en": "test-1"
                    },
                    "roles": {
                        "values": DEFAULT_DB_ROLES,
                        "slug": "role",
                    }
                },
                "test_2": {
                    "name": {
                        "ru": "test_2",
                        "en": "test_2"
                    },
                    "roles": {
                        "values": DEFAULT_DB_ROLES,
                        "slug": "role",
                    }
                },
            },
        }

    def test_get_all_roles_empty(self, mocker):
        mocker.patch("mongo_idm.helpers.get_databases", return_value=[])
        assert get_all_roles() == []

    def test_get_all_roles_custom(self, mocker):
        def patched_mongo_users_info(db_id):
            if db_id == "foo":
                return users_set_ok(db_id)
            else:
                return users_set_not_ok(db_id)
        mocker.patch(
            "mongo_idm.helpers.get_databases", return_value=["foo", "bar"]
        )
        mocker.patch("mongo_idm.db.mongo_users_info", patched_mongo_users_info)
        assert get_all_roles() == [
            {"login": "test", "roles": []},
            {"login": "testA", "roles": [{"foo": "dbAdmin"}]},
            {"login": "testR", "roles": [{"foo": "read"}]},
            {"login": "testRWA", "roles": [{"foo": "readWrite"}, {"foo": "dbAdmin"}]},
            {"login": "testRW", "roles": [{"foo": "readWrite"}, {"bar": "readWrite"}]},
        ]


@pytest.mark.skipif("DATABASES" not in os.environ, reason="requires configured database instance")
class TestApiWithMongodb:
    def test_add_and_remove_role(self):
        add_role("IDM_TEST1", "test", "read")
        assert get_all_roles() == [{"login": "test", "roles": [{"IDM_TEST1": "read"}]}]
        remove_role("IDM_TEST1", "test", "read", False)
        assert get_all_roles() == []

    def test_add_and_remove_role_db2(self):
        add_role("IDM_TEST2", "test", "read")
        assert get_all_roles() == [{"login": "test", "roles": [{"IDM_TEST2": "read"}]}]
        remove_role("IDM_TEST2", "test", "read", False)
        assert get_all_roles() == []

    def test_add2_and_fire(self):
        add_role("IDM_TEST1", "test", "read")
        add_role("IDM_TEST1", "test", "readWrite")
        assert get_all_roles() == [{"login": "test", "roles": [{"IDM_TEST1": "readWrite"}, {"IDM_TEST1": "read"}]}]
        remove_role("IDM_TEST1", "test", "read", True)
        assert get_all_roles() == []

    def test_add3_and_fire(self):
        add_role("IDM_TEST1", "test", "read")
        add_role("IDM_TEST1", "test", "readWrite")
        add_role("IDM_TEST1", "test", "dbAdmin")
        assert get_all_roles() == [{"login": "test", "roles": [{"IDM_TEST1": "readWrite"}, {"IDM_TEST1": "dbAdmin"}, {"IDM_TEST1": "read"}]}]
        remove_role("IDM_TEST1", "test", "read", True)
        assert get_all_roles() == []

    def teardown_method(self):
        remove_role("IDM_TEST1", "test", "read", False)
        remove_role("IDM_TEST1", "test", "readWrite", False)
        remove_role("IDM_TEST1", "test", "dbAdmin", False)
        remove_role("IDM_TEST2", "test", "read", False)
        remove_role("IDM_TEST2", "test", "readWrite", False)
