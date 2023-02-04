# coding: utf-8

from unittest.mock import (
    patch,
    Mock,
)

from hamcrest import (
    assert_that,
    has_entries,
    equal_to,
    all_of,
    has_length,
    greater_than,
    has_item,
    contains,
    has_entry,
    has_key,
    empty,
)

from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.auth.service import Service


class TestOrganizationsByYandexSessionView(TestCase):
    def setUp(self):
        super(TestOrganizationsByYandexSessionView, self).setUp()

    def test_with_one_user_in_one_org(self, monkeypatch):
        uid = str(self.user['id'])
        mock_request = Mock()
        monkeypatch.setattr('intranet.yandex_directory.src.yandex_directory.core.views.lego.view.request', mock_request)
        with patch('intranet.yandex_directory.src.yandex_directory.app.blackbox_instance') as mock_blackbox_instance:
            mock_request.cookies.get.return_value = 'some-session-id'
            mock_blackbox_instance.get_uids_by_session.return_value = [uid]

            response_data = self.get_json('/organizations-by-session/', expected_code=200)

        assert_that(response_data, has_key(uid))
        assert_that(response_data[uid], equal_to([{
            'id': self.organization['id'],
            'name': self.organization['name'],
            'logo': None,
        }]))

    def test_with_many_users_in_one_org(self, monkeypatch):
        org_id = self.organization['id']

        user1 = self.create_user(org_id=org_id)
        user2 = self.create_user(org_id=org_id)

        mock_request = Mock()
        monkeypatch.setattr('intranet.yandex_directory.src.yandex_directory.core.views.lego.view.request', mock_request)

        with patch('intranet.yandex_directory.src.yandex_directory.app.blackbox_instance') as mock_blackbox_instance:
            mock_request.cookies.get.return_value = 'some-session-id'
            mock_blackbox_instance.get_uids_by_session.return_value = [user1['id'], user2['id']]

            response_data = self.get_json('/organizations-by-session/', expected_code=200)

        assert_that(response_data, has_key(str(user1['id'])))
        assert_that(response_data, has_key(str(user2['id'])))

        assert_that(response_data[str(user1['id'])], equal_to([{
            'id': self.organization['id'],
            'name': self.organization['name'],
            'logo': None,
        }]))

        assert_that(response_data[str(user2['id'])], equal_to([{
            'id': self.organization['id'],
            'name': self.organization['name'],
            'logo': None,
        }]))

    def test_with_many_users_in_many_orgs(self, monkeypatch):
        # user1 -> org1
        org1 = self.create_organization()
        user1 = self.create_user(org_id=org1['id'])

        # user2 -> org2
        org2 = self.create_organization()
        user2 = self.create_user(org_id=org2['id'])

        # user3 -> org2, org3
        org3 = self.create_organization()
        user3 = self.create_user(org_id=org2['id'])
        self.create_user(uid=user3['id'], org_id=org3['id'])

        # user4 -> org4, org5
        org4 = self.create_organization()
        org5 = self.create_organization()
        user4 = self.create_user(org_id=org4['id'])
        self.create_user(uid=user4['id'], org_id=org5['id'])

        mock_request = Mock()
        monkeypatch.setattr('intranet.yandex_directory.src.yandex_directory.core.views.lego.view.request', mock_request)

        with patch('intranet.yandex_directory.src.yandex_directory.app.blackbox_instance') as mock_blackbox_instance:
            mock_request.cookies.get.return_value = 'some-session-id'
            mock_blackbox_instance.get_uids_by_session.return_value = [
                user1['id'], user2['id'], user3['id'], user4['id'],
            ]
            response_data = self.get_json('/organizations-by-session/', expected_code=200)

        assert_that(response_data, has_key(str(user1['id'])))
        assert_that(response_data, has_key(str(user2['id'])))
        assert_that(response_data, has_key(str(user3['id'])))
        assert_that(response_data, has_key(str(user4['id'])))

        assert_that(response_data[str(user1['id'])], equal_to([{
            'id': org1['id'],
            'name': org1['name'],
            'logo': None,
        }]))

        assert_that(response_data[str(user2['id'])], equal_to([{
            'id': org2['id'],
            'name': org2['name'],
            'logo': None,
        }]))

        assert_that(response_data[str(user3['id'])], equal_to([{
            'id': org2['id'],
            'name': org2['name'],
            'logo': None,
        }, {
            'id': org3['id'],
            'name': org3['name'],
            'logo': None,
        }]))

        assert_that(response_data[str(user4['id'])], equal_to([{
            'id': org4['id'],
            'name': org4['name'],
            'logo': None,
        }, {
            'id': org5['id'],
            'name': org5['name'],
            'logo': None,
        }]))


class TestOrganizationsByUidsView(TestCase):
    def test_with_invalid_uids(self):
        with patch('intranet.yandex_directory.src.yandex_directory.auth.middlewares.AuthMiddleware._authenticate') as authenticate:
            authenticate.return_value = {
                'auth_type': 'tvm',
                'service': Service(
                    id=123,
                    name='some-service-name',
                    identity='some-service-slug',
                    is_internal=True,
                    ip='127.0.0.1',
                ),
                'scopes': [],
                'user': None,
                'org_id': None,
            }

            response_data = self.get_json('/organizations-by-uids/?uids=yandexuid:700', expected_code=422)
            assert response_data['code'] == 'validation_query_parameters_error'


    def test_with_empty_uids(self):
        with patch('intranet.yandex_directory.src.yandex_directory.auth.middlewares.AuthMiddleware._authenticate') as authenticate:
            authenticate.return_value = {
                'auth_type': 'tvm',
                'service': Service(
                    id=123,
                    name='some-service-name',
                    identity='some-service-slug',
                    is_internal=True,
                    ip='127.0.0.1',
                ),
                'scopes': [],
                'user': None,
                'org_id': None,
            }

            response_data = self.get_json('/organizations-by-uids/', expected_code=200)

        assert_that(response_data, empty())


    def test_with_many_users_in_many_orgs(self):
        # user1 -> org1
        org1 = self.create_organization()
        user1 = self.create_user(org_id=org1['id'])

        # user2 -> org2
        org2 = self.create_organization()
        user2 = self.create_user(org_id=org2['id'])

        # user3 -> org2, org3
        org3 = self.create_organization()
        user3 = self.create_user(org_id=org2['id'])
        self.create_user(uid=user3['id'], org_id=org3['id'])

        # user4 -> org4, org5
        org4 = self.create_organization()
        org5 = self.create_organization()
        user4 = self.create_user(org_id=org4['id'])
        self.create_user(uid=user4['id'], org_id=org5['id'])

        with patch('intranet.yandex_directory.src.yandex_directory.auth.middlewares.AuthMiddleware._authenticate') as authenticate:
            uids = ','.join(map(str, [user1.get('id'), user2.get('id'), user3.get('id'), user4.get('id')]))

            authenticate.return_value = {
                'auth_type': 'tvm',
                'service': Service(
                    id=123,
                    name='some-service-name',
                    identity='some-service-slug',
                    is_internal=True,
                    ip='127.0.0.1',
                ),
                'scopes': [],
                'user': None,
                'org_id': None,
            }

            response_data = self.get_json('/organizations-by-uids/?uids=' + uids, expected_code=200)

        assert_that(response_data, has_key(str(user1['id'])))
        assert_that(response_data, has_key(str(user2['id'])))
        assert_that(response_data, has_key(str(user3['id'])))
        assert_that(response_data, has_key(str(user4['id'])))

        assert_that(response_data[str(user1['id'])], equal_to([{
            'id': org1['id'],
            'name': org1['name'],
            'logo': None,
        }]))

        assert_that(response_data[str(user2['id'])], equal_to([{
            'id': org2['id'],
            'name': org2['name'],
            'logo': None,
        }]))

        assert_that(response_data[str(user3['id'])], equal_to([{
            'id': org2['id'],
            'name': org2['name'],
            'logo': None,
        }, {
            'id': org3['id'],
            'name': org3['name'],
            'logo': None,
        }]))

        assert_that(response_data[str(user4['id'])], equal_to([{
            'id': org4['id'],
            'name': org4['name'],
            'logo': None,
        }, {
            'id': org5['id'],
            'name': org5['name'],
            'logo': None,
        }]))
