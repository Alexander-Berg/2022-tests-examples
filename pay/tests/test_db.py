import os
import pytest
import mongo_idm


@pytest.mark.skipif("DATABASES" not in os.environ, reason="requires configured database instance")
class TestMongoDBCached:
    def test_get_mongo_db(self):
        mongo_cached = mongo_idm.db.MongoDBCached()
        client1 = mongo_cached.get_mongo_db("IDM_TEST1")
        client2 = mongo_cached.get_mongo_db("IDM_TEST1")
        assert client1.client == client2.client
        client1.client.close()
        client3 = mongo_cached.get_mongo_db("IDM_TEST1")
        assert client1 == client3


@pytest.mark.skipif("DATABASES" not in os.environ, reason="requires configured database instance")
class TestDB:
    def test_mongo_create_user_if_not_exists(self):
        mongo_idm.db.mongo_create_user_if_not_exists("IDM_TEST1", "test", "test")
        assert mongo_idm.db.mongo_user_info("IDM_TEST1", "test") == {
            u'ok': 1.0,
            u'users': [
                {
                    u'_id': u'idm-test1.test',
                    u'customData': {u'idm': True},
                    u'db': u'idm-test1',
                    u'roles': [
                        {u'db': u'idm-test1', u'role': u'changeOwnPassword'}
                    ],
                    u'user': u'test'
                }
            ]
        }
        assert mongo_idm.db.mongo_users_info("IDM_TEST1") == {
            u'ok': 1.0,
            u'users': [
                {
                    u'_id': u'idm-test1.idm-test1',
                    u'db': u'idm-test1',
                    u'roles': [{u'db': u'idm-test1', u'role': u'userAdmin'}],
                    u'user': u'idm-test1'
                },
                {
                    u'_id': u'idm-test1.test',
                    u'customData': {u'idm': True},
                    u'db': u'idm-test1',
                    u'roles': [
                        {u'db': u'idm-test1', u'role': u'changeOwnPassword'}
                    ],
                    u'user': u'test'
                }
            ]
        }
        mongo_idm.db.mongo_drop_user_if_exists("IDM_TEST1", "test")
        assert mongo_idm.db.mongo_user_info("IDM_TEST1", "test") == {
            u'ok': 1.0,
            u'users': [],
        }

    def test_mongo_grant_role_to_user(self):
        mongo_idm.db.mongo_create_user_if_not_exists("IDM_TEST1", "test", "test")
        mongo_idm.db.mongo_grant_role_to_user_if_not_granted("IDM_TEST1", "test", "read")
        assert sorted(mongo_idm.db.mongo_user_info("IDM_TEST1", "test")["users"][0]["roles"]) == sorted([
            {u'db': u'idm-test1', u'role': u'read'},
            {u'db': u'idm-test1', u'role': u'changeOwnPassword'},
        ])
        mongo_idm.db.mongo_grant_role_to_user_if_not_granted("IDM_TEST1", "test", "read")
        mongo_idm.db.mongo_revoke_role_from_user_if_exists("IDM_TEST1", "test", "read")
        assert mongo_idm.db.mongo_user_info("IDM_TEST1", "test")["users"][0]["roles"] == [
            {u'db': u'idm-test1', u'role': u'changeOwnPassword'},
        ]
        mongo_idm.db.mongo_revoke_role_from_user_if_exists("IDM_TEST1", "test", "read")
        mongo_idm.db.mongo_drop_user_if_exists("IDM_TEST1", "test")

    def test_mongo_drop_user_without_roles(self):
        mongo_idm.db.mongo_create_user_if_not_exists("IDM_TEST1", "test", "test")
        mongo_idm.db.mongo_grant_role_to_user_if_not_granted("IDM_TEST1", "test", "read")
        mongo_idm.db.mongo_drop_user_if_has_no_roles("IDM_TEST1", "test")
        assert len(mongo_idm.db.mongo_user_info("IDM_TEST1", "test")["users"]) == 1
        mongo_idm.db.mongo_revoke_role_from_user_if_exists("IDM_TEST1", "test", "read")
        mongo_idm.db.mongo_drop_user_if_has_no_roles("IDM_TEST1", "test")
        assert len(mongo_idm.db.mongo_user_info("IDM_TEST1", "test")["users"]) == 0

    def teardown_method(self):
        mongo_idm.db.mongo_drop_user_if_exists("IDM_TEST1", "test")
