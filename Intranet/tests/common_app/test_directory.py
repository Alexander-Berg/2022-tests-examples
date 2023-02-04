# -*- coding: utf-8 -*-
import responses

from django.test import Client, TestCase
from unittest.mock import patch

from events.accounts.helpers import YandexClient
from events.accounts.factories import OrganizationToGroupFactory
from events.common_app.api_admin.v2.serializers import DirectorySuggestSerializer
from events.common_app.directory import (
    build_real_user_name,
    CachedDirectoryClient,
    DirectoryClient,
    BiSearchSuggest,
    DirectorySuggest,
    DIRECTORY_HOST,
)
from events.common_app.helpers import override_cache_settings


class TestDirectoryUtils(TestCase):
    def test_build_user_real_name_from_dir_data(self):
        data = [
            {
                'last': {
                    'ru': '',
                    'en': ''
                },
                'first': {
                    'ru': 'Робот сервиса Forms',
                    'en': 'Forms Robot'
                }
            },
            {
                'last': 'thebard',
                'first': 'leliana'
            },
        ]
        # translations
        self.assertEqual(build_real_user_name(data[0], 'ru'), 'Робот сервиса Forms')
        self.assertEqual(build_real_user_name(data[1], 'ru'), 'leliana thebard')
        self.assertEqual(build_real_user_name(data[0], 'en'), 'Forms Robot')
        self.assertEqual(build_real_user_name(data[1], 'en'), 'leliana thebard')
        # fallbacks
        self.assertEqual(build_real_user_name(data[0], 'kk'), 'Робот сервиса Forms')
        self.assertEqual(build_real_user_name(data[1], 'kk'), 'leliana thebard')
        self.assertEqual(build_real_user_name(data[0], 'fi'), 'Forms Robot')
        self.assertEqual(build_real_user_name(data[1], 'fi'), 'leliana thebard')
        # fake language
        self.assertIsNone(build_real_user_name(data[0], 'fake'))
        self.assertEqual(build_real_user_name(data[1], 'fake'), 'leliana thebard')


class TestDirectoryClient(TestCase):
    def setUp(self):
        self.client = DirectoryClient()

    @responses.activate
    def test_get_service_user_should_return_user_uid(self):
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/users/',
            json={
                'links': {},
                'result': [
                    {'id': 10001},
                    {'id': 10000},
                    {'id': 10002},
                ],
            },
        )
        user_uid = self.client.get_service_user('1001', 'service')
        self.assertEqual(user_uid, 10002)
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], '1001')
        self.assertEqual(responses.calls[0].request.params['fields'], 'id')

    @responses.activate
    def test_get_service_user_should_return_none(self):
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/users/',
            json={'links': {}, 'result': []},
        )
        user_uid = self.client.get_service_user('1001', 'service')
        self.assertIsNone(user_uid)
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], '1001')
        self.assertEqual(responses.calls[0].request.params['fields'], 'id')

    @responses.activate
    def test_get_organization_should_return_org(self):
        dir_id = '123'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/organizations/{dir_id}/',
            json={'id': '123', 'name': 'test'},
        )
        org = self.client.get_organization(dir_id)
        self.assertEqual(org['id'], dir_id)
        self.assertEqual(org['name'], 'test')
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,name')

    @responses.activate
    def test_get_organization_should_return_none(self):
        dir_id = '123'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/organizations/{dir_id}/',
            status=404,
        )
        org = self.client.get_organization(dir_id)
        self.assertIsNone(org)
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,name')

    @responses.activate
    def test_get_organizations_should_return_list(self):
        user_uid = '1001'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/organizations/',
            json={
                'links': {},
                'result': [
                    {'id': '101', 'name': 'test1'},
                    {'id': '102', 'name': 'test2'},
                    {'id': '103', 'name': 'test3'},
                ],
            },
        )
        orgs = list(self.client.get_organizations(user_uid))
        self.assertListEqual(orgs, [
            {'id': '101', 'name': 'test1'},
            {'id': '102', 'name': 'test2'},
            {'id': '103', 'name': 'test3'},
        ])
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-UID'], user_uid)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,name')

    @responses.activate
    def test_get_organizations_should_return_empty(self):
        user_uid = '1001'
        cloud_uid = 'abcd'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/organizations/',
            json={'links': {}, 'result': []},
        )
        orgs = list(self.client.get_organizations(user_uid, cloud_uid=cloud_uid, fields='organization_type'))
        self.assertListEqual(orgs, [])
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-UID'], user_uid)
        self.assertEqual(responses.calls[0].request.headers['X-Cloud-UID'], cloud_uid)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'organization_type')

    @responses.activate
    def test_get_user_by_uid_should_return_one(self):
        dir_id = '123'
        user_uid = '1234'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/users/{user_uid}/',
            json={'id': 1234, 'cloud_uid': None, 'name': 'test'},
        )
        user = self.client.get_user(dir_id, user_uid)
        self.assertEqual(user['id'], 1234)
        self.assertIsNone(user['cloud_uid'])
        self.assertEqual(user['name'], 'test')
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertEqual(responses.calls[0].request.headers['X-UID'], user_uid)
        self.assertFalse('X-Cloud-UID' in responses.calls[0].request.headers)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,cloud_uid,name,nickname')

    @responses.activate
    def test_get_user_by_cloud_uid_should_return_one(self):
        dir_id = '123'
        cloud_uid = 'abcd'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/users/cloud/{cloud_uid}/',
            json={'id': 1234, 'cloud_uid': 'abcd', 'name': 'test'},
        )
        user = self.client.get_user(dir_id, None, cloud_uid=cloud_uid)
        self.assertEqual(user['id'], 1234)
        self.assertEqual(user['cloud_uid'], 'abcd')
        self.assertEqual(user['name'], 'test')
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertEqual(responses.calls[0].request.headers['X-Cloud-UID'], cloud_uid)
        self.assertFalse('X-UID' in responses.calls[0].request.headers)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,cloud_uid,name,nickname')

    @responses.activate
    def test_get_user_should_return_none(self):
        dir_id = '123'
        user_uid = '1234'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/users/{user_uid}/',
            status=404,
        )
        user = self.client.get_user(dir_id, user_uid)
        self.assertIsNone(user)
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertEqual(responses.calls[0].request.headers['X-UID'], user_uid)
        self.assertFalse('X-Cloud-UID' in responses.calls[0].request.headers)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,cloud_uid,name,nickname')

    @responses.activate
    def test_get_users_should_return_list(self):
        dir_id = '123'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/users/',
            json={
                'links': {},
                'result': [
                    {'id': 101, 'cloud_uid': None, 'name': 'test1', 'email': 'test1@ya.ru'},
                    {'id': 102, 'cloud_uid': None, 'name': 'test2', 'email': 'test2@ya.ru'},
                    {'id': 103, 'cloud_uid': 'abcd', 'name': 'test3', 'email': 'test3@ya.ru'},
                ],
            },
        )
        users = list(self.client.get_users(dir_id, fields='id,cloud_uid,name,email'))
        self.assertListEqual(users, [
            {'id': 101, 'cloud_uid': None, 'name': 'test1', 'email': 'test1@ya.ru'},
            {'id': 102, 'cloud_uid': None, 'name': 'test2', 'email': 'test2@ya.ru'},
            {'id': 103, 'cloud_uid': 'abcd', 'name': 'test3', 'email': 'test3@ya.ru'},
        ])
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,cloud_uid,name,email')

    @responses.activate
    def test_get_users_should_return_list_paging(self):
        dir_id = '123'
        fields = 'id,name'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/users/',
            json={
                'links': {'next': f'{DIRECTORY_HOST}/v6/users/?page=2&fields={fields}'},
                'result': [
                    {'id': 101, 'name': 'test1'},
                    {'id': 102, 'name': 'test2'},
                ],
            },
        )
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/users/',
            json={
                'links': {},
                'result': [
                    {'id': 103, 'name': 'test3'},
                ],
            },
        )
        users = list(self.client.get_users(dir_id, fields=fields))
        self.assertListEqual(users, [
            {'id': 101, 'name': 'test1'},
            {'id': 102, 'name': 'test2'},
            {'id': 103, 'name': 'test3'},
        ])
        self.assertEqual(len(responses.calls), 2)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], fields)
        self.assertEqual(responses.calls[1].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[1].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[1].request.params['fields'], fields)
        self.assertEqual(responses.calls[1].request.params['page'], '2')

    @responses.activate
    def test_get_users_should_return_empty(self):
        dir_id = '123'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/users/',
            json={'links': {}, 'result': []},
        )
        users = list(self.client.get_users(dir_id, fields='id,cloud_uid,name,email'))
        self.assertListEqual(users, [])
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,cloud_uid,name,email')

    @responses.activate
    def test_get_group_should_return_one(self):
        dir_id = '123'
        group_id = 11
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/groups/{group_id}/',
            json={'id': 11, 'name': 'test'},
        )
        group = self.client.get_group(dir_id, group_id)
        self.assertEqual(group['id'], 11)
        self.assertEqual(group['name'], 'test')
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,name')

    @responses.activate
    def test_get_group_should_return_none(self):
        dir_id = '123'
        group_id = 11
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/groups/{group_id}/',
            status=404,
        )
        group = self.client.get_group(dir_id, group_id)
        self.assertIsNone(group)
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,name')

    @responses.activate
    def test_get_groups_should_return_list(self):
        dir_id = '123'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/groups/',
            json={
                'links': {},
                'result': [
                    {'id': 11, 'name': 'test1'},
                    {'id': 12, 'name': 'test2'},
                    {'id': 13, 'name': 'test3'},
                ],
            },
        )
        groups = list(self.client.get_groups(dir_id))
        self.assertListEqual(groups, [
            {'id': 11, 'name': 'test1'},
            {'id': 12, 'name': 'test2'},
            {'id': 13, 'name': 'test3'},
        ])
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,name')

    @responses.activate
    def test_get_groups_should_return_empty(self):
        dir_id = '123'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/groups/',
            json={'links': {}, 'result': []},
        )
        groups = list(self.client.get_groups(dir_id))
        self.assertListEqual(groups, [])
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,name')

    @responses.activate
    def test_get_department_should_return_one(self):
        dir_id = '123'
        department_id = 11
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/departments/{department_id}/',
            json={'id': 11, 'name': 'test'},
        )
        department = self.client.get_department(dir_id, department_id)
        self.assertEqual(department['id'], 11)
        self.assertEqual(department['name'], 'test')
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,name')

    @responses.activate
    def test_get_department_should_return_none(self):
        dir_id = '123'
        department_id = 11
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/departments/{department_id}/',
            status=404,
        )
        department = self.client.get_department(dir_id, department_id)
        self.assertIsNone(department)
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,name')

    @responses.activate
    def test_get_departments_should_return_list(self):
        dir_id = '123'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/departments/',
            json={
                'links': {},
                'result': [
                    {'id': 11, 'name': 'test1'},
                    {'id': 12, 'name': 'test2'},
                    {'id': 13, 'name': 'test3'},
                ],
            },
        )
        departments = list(self.client.get_departments(dir_id))
        self.assertListEqual(departments, [
            {'id': 11, 'name': 'test1'},
            {'id': 12, 'name': 'test2'},
            {'id': 13, 'name': 'test3'},
        ])
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,name')

    @responses.activate
    def test_get_departments_should_return_empty(self):
        dir_id = '123'
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/departments/',
            json={'links': {}, 'result': []},
        )
        departments = list(self.client.get_departments(dir_id))
        self.assertListEqual(departments, [])
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Org-ID'], dir_id)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])
        self.assertEqual(responses.calls[0].request.params['fields'], 'id,name')

    @responses.activate
    def test_who_is_return_one(self):
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/who-is/',
            json={'org_id': 123, 'type': 'user', 'object_id': 1234},
        )
        who_is = self.client.who_is(email='test@yandex.ru')
        self.assertIsNotNone(who_is)
        self.assertEqual(who_is['org_id'], 123)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])

    @responses.activate
    def test_who_is_return_none(self):
        responses.add(
            responses.GET,
            f'{DIRECTORY_HOST}/v6/who-is/',
            status=404,
        )
        who_is = self.client.who_is(domain='yandex.ru')
        self.assertIsNone(who_is)
        self.assertIsNotNone(responses.calls[0].request.headers['X-User-IP'])


class TestCachedDirectoryClient(TestCase):
    def setUp(self):
        self.client = CachedDirectoryClient()

    def test_get_service_user_should_return_user_uid(self):
        dir_id = '123'
        service_slug = 'test'
        result = 1234
        with patch.object(DirectoryClient, 'get_service_user') as mock_get_service_user:
            mock_get_service_user.return_value = result
            self.assertEqual(self.client.get_service_user(dir_id, service_slug), result)
            self.assertEqual(self.client.get_service_user(dir_id, service_slug), result)
        mock_get_service_user.assert_called_once_with(dir_id, service_slug, fields=None)

    def test_get_service_user_should_return_none(self):
        dir_id = '123'
        service_slug = 'test'
        result = None
        with patch.object(DirectoryClient, 'get_service_user') as mock_get_service_user:
            mock_get_service_user.return_value = result
            self.assertEqual(self.client.get_service_user(dir_id, service_slug), result)
            self.assertEqual(self.client.get_service_user(dir_id, service_slug), result)
        mock_get_service_user.assert_called_once_with(dir_id, service_slug, fields=None)

    def test_get_organizations_should_return_list(self):
        user_uid = '1234'
        result = [
            {'id': 123, 'name': 'test1'},
            {'id': 124, 'name': 'test2'},
        ]
        with patch.object(DirectoryClient, 'get_organizations') as mock_get_organizations:
            mock_get_organizations.return_value = iter(result)
            self.assertListEqual(self.client.get_organizations(user_uid), result)
            self.assertListEqual(self.client.get_organizations(user_uid), result)
        mock_get_organizations.assert_called_once_with(user_uid, cloud_uid=None, fields=None)

    def test_get_organizations_should_return_empty(self):
        user_uid = '1234'
        result = []
        with patch.object(DirectoryClient, 'get_organizations') as mock_get_organizations:
            mock_get_organizations.return_value = iter(result)
            self.assertListEqual(self.client.get_organizations(user_uid), result)
            self.assertListEqual(self.client.get_organizations(user_uid), result)
        mock_get_organizations.assert_called_once_with(user_uid, cloud_uid=None, fields=None)

    def test_get_organization_should_return_one(self):
        dir_id = '123'
        result = {'id': 123, 'name': 'test'}
        with patch.object(DirectoryClient, 'get_organization') as mock_get_organization:
            mock_get_organization.return_value = result
            self.assertEqual(self.client.get_organization(dir_id), result)
            self.assertEqual(self.client.get_organization(dir_id), result)
        mock_get_organization.assert_called_once_with(dir_id, fields=None)

    def test_get_organization_should_return_none(self):
        dir_id = '123'
        result = None
        with patch.object(DirectoryClient, 'get_organization') as mock_get_organization:
            mock_get_organization.return_value = result
            self.assertEqual(self.client.get_organization(dir_id), result)
            self.assertEqual(self.client.get_organization(dir_id), result)
        mock_get_organization.assert_called_once_with(dir_id, fields=None)

    def test_get_group_should_return_one(self):
        dir_id = '123'
        group_id = 11
        result = {'id': 11, 'name': 'test'}
        with patch.object(DirectoryClient, 'get_group') as mock_get_group:
            mock_get_group.return_value = result
            self.assertEqual(self.client.get_group(dir_id, group_id), result)
            self.assertEqual(self.client.get_group(dir_id, group_id), result)
        mock_get_group.assert_called_once_with(dir_id, group_id, fields=None)

    def test_get_group_should_return_none(self):
        dir_id = '123'
        group_id = 11
        result = None
        with patch.object(DirectoryClient, 'get_group') as mock_get_group:
            mock_get_group.return_value = result
            self.assertEqual(self.client.get_group(dir_id, group_id), result)
            self.assertEqual(self.client.get_group(dir_id, group_id), result)
        mock_get_group.assert_called_once_with(dir_id, group_id, fields=None)

    def test_get_groups_should_return_list(self):
        dir_id = '123'
        result = [
            {'id': 11, 'name': 'test1'},
            {'id': 12, 'name': 'test2'},
        ]
        with patch.object(DirectoryClient, 'get_groups') as mock_get_groups:
            mock_get_groups.return_value = iter(result)
            self.assertListEqual(self.client.get_groups(dir_id), result)
            self.assertListEqual(self.client.get_groups(dir_id), result)
        mock_get_groups.assert_called_once_with(dir_id, fields=None)

    def test_get_groups_should_return_empty(self):
        dir_id = '123'
        result = []
        with patch.object(DirectoryClient, 'get_groups') as mock_get_groups:
            mock_get_groups.return_value = iter(result)
            self.assertListEqual(self.client.get_groups(dir_id), result)
            self.assertListEqual(self.client.get_groups(dir_id), result)
        mock_get_groups.assert_called_once_with(dir_id, fields=None)

    def test_get_department_should_return_one(self):
        dir_id = '123'
        department_id = 11
        result = {'id': 11, 'name': 'test'}
        with patch.object(DirectoryClient, 'get_department') as mock_get_department:
            mock_get_department.return_value = result
            self.assertEqual(self.client.get_department(dir_id, department_id), result)
            self.assertEqual(self.client.get_department(dir_id, department_id), result)
        mock_get_department.assert_called_once_with(dir_id, department_id, fields=None)

    def test_get_department_should_return_none(self):
        dir_id = '123'
        department_id = 11
        result = None
        with patch.object(DirectoryClient, 'get_department') as mock_get_department:
            mock_get_department.return_value = result
            self.assertEqual(self.client.get_department(dir_id, department_id), result)
            self.assertEqual(self.client.get_department(dir_id, department_id), result)
        mock_get_department.assert_called_once_with(dir_id, department_id, fields=None)

    def test_get_departments_should_return_list(self):
        dir_id = '123'
        result = [
            {'id': 11, 'name': 'test1'},
            {'id': 12, 'name': 'test2'},
        ]
        with patch.object(DirectoryClient, 'get_departments') as mock_get_departments:
            mock_get_departments.return_value = iter(result)
            self.assertListEqual(self.client.get_departments(dir_id), result)
            self.assertListEqual(self.client.get_departments(dir_id), result)
        mock_get_departments.assert_called_once_with(dir_id, fields=None)

    def test_get_departments_should_return_empty(self):
        dir_id = '123'
        result = []
        with patch.object(DirectoryClient, 'get_departments') as mock_get_departments:
            mock_get_departments.return_value = iter(result)
            self.assertListEqual(self.client.get_departments(dir_id), result)
            self.assertListEqual(self.client.get_departments(dir_id), result)
        mock_get_departments.assert_called_once_with(dir_id, fields=None)

    def test_get_user_should_return_one(self):
        dir_id = '123'
        user_uid = '1234'
        result = {'id': 1234, 'cloud_uid': 'abcd', 'name': 'test', 'nickname': 'test1'}
        with patch.object(DirectoryClient, 'get_user') as mock_get_user:
            mock_get_user.return_value = result
            self.assertEqual(self.client.get_user(dir_id, user_uid), result)
            self.assertEqual(self.client.get_user(dir_id, user_uid), result)
        mock_get_user.assert_called_once_with(dir_id, user_uid, cloud_uid=None, fields=None)

    def test_get_user_should_return_none(self):
        dir_id = '123'
        user_uid = '1234'
        result = None
        with patch.object(DirectoryClient, 'get_user') as mock_get_user:
            mock_get_user.return_value = result
            self.assertEqual(self.client.get_user(dir_id, user_uid), result)
            self.assertEqual(self.client.get_user(dir_id, user_uid), result)
        mock_get_user.assert_called_once_with(dir_id, user_uid, cloud_uid=None, fields=None)

    def test_get_user_by_cloud_uid_should_return_one(self):
        dir_id = '123'
        cloud_uid = 'abcd'
        result = {'id': 1234, 'cloud_uid': 'abcd', 'name': 'test', 'nickname': 'test1'}
        with patch.object(DirectoryClient, 'get_user') as mock_get_user:
            mock_get_user.return_value = result
            self.assertEqual(self.client.get_user(dir_id, None, cloud_uid=cloud_uid), result)
            self.assertEqual(self.client.get_user(dir_id, None, cloud_uid=cloud_uid), result)
        mock_get_user.assert_called_once_with(dir_id, None, cloud_uid=cloud_uid, fields=None)

    def test_get_user_by_cloud_uid_should_return_none(self):
        dir_id = '123'
        cloud_uid = 'abcd'
        result = None
        with patch.object(DirectoryClient, 'get_user') as mock_get_user:
            mock_get_user.return_value = result
            self.assertEqual(self.client.get_user(dir_id, None, cloud_uid=cloud_uid), result)
            self.assertEqual(self.client.get_user(dir_id, None, cloud_uid=cloud_uid), result)
        mock_get_user.assert_called_once_with(dir_id, None, cloud_uid=cloud_uid, fields=None)

    def test_get_users_should_return_list(self):
        dir_id = '123'
        fields='id,name'
        result = [
            {'id': 11, 'name': 'test1'},
            {'id': 12, 'name': 'test2'},
        ]
        with patch.object(DirectoryClient, 'get_users') as mock_get_users:
            mock_get_users.return_value = iter(result)
            self.assertListEqual(self.client.get_users(dir_id, fields=fields), result)
            self.assertListEqual(self.client.get_users(dir_id, fields=fields), result)
        mock_get_users.assert_called_once_with(dir_id, fields=fields)

    def test_get_users_should_return_empty(self):
        dir_id = '123'
        fields='id,name'
        result = []
        with patch.object(DirectoryClient, 'get_users') as mock_get_users:
            mock_get_users.return_value = iter(result)
            self.assertListEqual(self.client.get_users(dir_id, fields=fields), result)
            self.assertListEqual(self.client.get_users(dir_id, fields=fields), result)
        mock_get_users.assert_called_once_with(dir_id, fields=fields)

    def test_who_is_should_return_one(self):
        email = 'test@yandex.ru'
        result = {'org_id': 123, 'type': 'user', 'object_id': 1234}
        with patch.object(DirectoryClient, 'who_is') as mock_who_is:
            mock_who_is.return_value = result
            self.assertEqual(self.client.who_is(email=email), result)
            self.assertEqual(self.client.who_is(email=email), result)
        mock_who_is.assert_called_once_with(email=email, domain=None)

    def test_who_is_should_return_none(self):
        domain = 'yandex.ru'
        result = None
        with patch.object(DirectoryClient, 'who_is') as mock_who_is:
            mock_who_is.return_value = result
            self.assertEqual(self.client.who_is(domain=domain), result)
            self.assertEqual(self.client.who_is(domain=domain), result)
        mock_who_is.assert_called_once_with(email=None, domain=domain)


@override_cache_settings()
class TestDirectorySuggest(TestCase):
    def register_user(self, data, status=200):
        responses.add(
            responses.GET,
            'https://api-integration-qa.directory.ws.yandex.net/v6/users/',
            json=data,
            status=status,
        )

    def register_group(self, data, status=200):
        responses.add(
            responses.GET,
            'https://api-integration-qa.directory.ws.yandex.net/v6/groups/',
            json=data,
            status=status,
        )

    def register_department(self, data, status=200):
        responses.add(
            responses.GET,
            'https://api-integration-qa.directory.ws.yandex.net/v6/departments/',
            json=data,
            status=status,
        )

    def register_uri(self):
        self.register_user({
            'links': {},
            'result': [
                {
                    'id': 123,
                    'cloud_uid': 'abcd',
                    'name': {
                        'first': {
                            'ru': 'Вася',
                            'en': 'Vasia',
                        },
                        'last': {
                            'ru': 'Пупкин',
                            'en': 'Pupkin',
                        },
                    },
                    'nickname': 'vasilek',
                },
                {
                    'id': 124,
                    'name': {
                        'first': 'Name',
                        'last': 'Surname',
                    },
                    'nickname': 'login',
                },
            ]
        })
        self.register_group({
            'links': {},
            'result': [
                {
                    'id': 1,
                    'name': 'First Role'
                },
                {
                    'id': 2,
                    'name': 'Second Role'
                },
            ],
        })
        self.register_department({
            'links': {},
            'result': [
                {
                    'id': 2,
                    'name': 'First Branch'
                },
                {
                    'id': 5,
                    'name': 'Second Branch'
                },
            ],
        })

    @responses.activate
    def test_should_return_empty_without_text_or_ids(self):
        self.register_uri()
        ds = DirectorySuggest('47')
        result = ds.suggest(text='')

        self.assertTrue('people' in result)
        self.assertEqual(result['people']['result'], [])

        self.assertTrue('groups' in result)
        self.assertEqual(result['groups']['result'], [])

        self.assertTrue('departments' in result)
        self.assertEqual(result['departments']['result'], [])

    @responses.activate
    def test_should_return_empty_if_org_not_exist(self):
        self.register_user({}, status=404)
        self.register_group({}, status=404)
        self.register_department({}, status=404)

        ds = DirectorySuggest('47')
        result = ds.suggest(text='')

        self.assertTrue('people' in result)
        self.assertEqual(result['people']['result'], [])

        self.assertTrue('groups' in result)
        self.assertEqual(result['groups']['result'], [])

        self.assertTrue('departments' in result)
        self.assertEqual(result['departments']['result'], [])

    @responses.activate
    def test_should_suggest_people_by_text(self):
        self.register_uri()
        ds = DirectorySuggest('47')
        result = ds.suggest(text='пуп', layers=['people'])

        self.assertTrue('people' in result)
        self.assertEqual(len(result['people']['result']), 1)
        self.assertEqual(result['people']['result'][0]['id'], '123')
        self.assertEqual(result['people']['result'][0]['cloud_uid'], 'abcd')
        self.assertEqual(result['people']['result'][0]['title'], 'Вася Пупкин')
        self.assertEqual(len(result['people']['result'][0]['fields']), 1)
        self.assertDictEqual(result['people']['result'][0]['fields'][0], {
            'type': 'login',
            'value': 'vasilek',
        })

        self.assertFalse('groups' in result)
        self.assertFalse('departments' in result)

    @responses.activate
    def test_should_suggest_people_by_text_in_english(self):
        self.register_uri()
        ds = DirectorySuggest('47', 'en')
        result = ds.suggest(text='pup', layers=['people'])

        self.assertTrue('people' in result)
        self.assertEqual(len(result['people']['result']), 1)
        self.assertEqual(result['people']['result'][0]['id'], '123')
        self.assertEqual(result['people']['result'][0]['cloud_uid'], 'abcd')
        self.assertEqual(result['people']['result'][0]['title'], 'Vasia Pupkin')
        self.assertEqual(len(result['people']['result'][0]['fields']), 1)
        self.assertDictEqual(result['people']['result'][0]['fields'][0], {
            'type': 'login',
            'value': 'vasilek',
        })

        self.assertFalse('groups' in result)
        self.assertFalse('departments' in result)

    @responses.activate
    def test_should_suggest_people_by_alternate_text(self):
        self.register_uri()
        ds = DirectorySuggest('47', 'en')
        result = ds.suggest(text='name', layers=['people'])

        self.assertTrue('people' in result)
        self.assertEqual(len(result['people']['result']), 1)
        self.assertEqual(result['people']['result'][0]['id'], '124')
        self.assertIsNone(result['people']['result'][0]['cloud_uid'])
        self.assertEqual(result['people']['result'][0]['title'], 'Name Surname')
        self.assertEqual(len(result['people']['result'][0]['fields']), 1)
        self.assertDictEqual(result['people']['result'][0]['fields'][0], {
            'type': 'login',
            'value': 'login',
        })

        self.assertFalse('groups' in result)
        self.assertFalse('departments' in result)

    @responses.activate
    def test_should_suggest_people_by_ids(self):
        self.register_uri()
        ds = DirectorySuggest('47')
        result = ds.suggest(ids=['123'], layers=['people'])

        self.assertTrue('people' in result)
        self.assertEqual(len(result['people']['result']), 1)
        self.assertEqual(result['people']['result'][0]['id'], '123')
        self.assertEqual(result['people']['result'][0]['cloud_uid'], 'abcd')
        self.assertEqual(result['people']['result'][0]['title'], 'Вася Пупкин')
        self.assertEqual(len(result['people']['result'][0]['fields']), 1)
        self.assertDictEqual(result['people']['result'][0]['fields'][0], {
            'type': 'login',
            'value': 'vasilek',
        })

        self.assertFalse('groups' in result)
        self.assertFalse('departments' in result)

    @responses.activate
    def test_should_suggest_group_by_text(self):
        self.register_uri()
        ds = DirectorySuggest('47')
        result = ds.suggest(text='rol', layers=['groups'])

        self.assertTrue('groups' in result)
        self.assertEqual(len(result['groups']['result']), 2)
        self.assertEqual(result['groups']['result'][0]['id'], '1')
        self.assertEqual(result['groups']['result'][0]['title'], 'First Role')
        self.assertEqual(result['groups']['result'][1]['id'], '2')
        self.assertEqual(result['groups']['result'][1]['title'], 'Second Role')

        self.assertFalse('people' in result)
        self.assertFalse('departments' in result)

    @responses.activate
    def test_should_suggest_group_by_ids(self):
        self.register_uri()
        ds = DirectorySuggest('47')
        result = ds.suggest(ids=['1', '2'], layers=['groups'])

        self.assertTrue('groups' in result)
        self.assertEqual(len(result['groups']['result']), 2)
        self.assertEqual(result['groups']['result'][0]['id'], '1')
        self.assertEqual(result['groups']['result'][0]['title'], 'First Role')
        self.assertEqual(result['groups']['result'][1]['id'], '2')
        self.assertEqual(result['groups']['result'][1]['title'], 'Second Role')

        self.assertFalse('people' in result)
        self.assertFalse('departments' in result)

    @responses.activate
    def test_should_suggest_department_by_text(self):
        self.register_uri()
        ds = DirectorySuggest('47')
        result = ds.suggest(text='bra', layers=['departments'])

        self.assertTrue('departments' in result)
        self.assertEqual(len(result['departments']['result']), 2)
        self.assertEqual(result['departments']['result'][0]['id'], '2')
        self.assertEqual(result['departments']['result'][0]['title'], 'First Branch')
        self.assertEqual(result['departments']['result'][1]['id'], '5')
        self.assertEqual(result['departments']['result'][1]['title'], 'Second Branch')

        self.assertFalse('people' in result)
        self.assertFalse('groups' in result)

    @responses.activate
    def test_should_suggest_department_by_ids(self):
        self.register_uri()
        ds = DirectorySuggest('47')
        result = ds.suggest(ids=['2', '5'], layers=['departments'])

        self.assertTrue('departments' in result)
        self.assertEqual(len(result['departments']['result']), 2)
        self.assertEqual(result['departments']['result'][0]['id'], '2')
        self.assertEqual(result['departments']['result'][0]['title'], 'First Branch')
        self.assertEqual(result['departments']['result'][1]['id'], '5')
        self.assertEqual(result['departments']['result'][1]['title'], 'Second Branch')

        self.assertFalse('people' in result)
        self.assertFalse('groups' in result)


class TestDirectorySuggestView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        o2g = OrganizationToGroupFactory()
        self.org = o2g.org
        self.user.groups.add(o2g.group)

    @responses.activate
    def test_should_return_empty_for_not_logged_user(self):
        client = Client()

        response = client.get('/admin/api/v2/directory/suggest/')
        self.assertEqual(response.status_code, 401)

    @responses.activate
    def test_should_return_empty_without_orgs(self):
        response = self.client.get(f'/admin/api/v2/directory/suggest/?org_id={self.org.dir_id}')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data, {})

    @responses.activate
    def test_should_return_empty_without_text_or_ids(self):
        response = self.client.get(
            f'/admin/api/v2/directory/suggest/?org_id={self.org.dir_id}',
            HTTP_X_ORGS=self.org.dir_id,
        )
        self.assertEqual(response.status_code, 200)

        self.assertTrue('people' in response.data)
        self.assertEqual(response.data['people']['result'], [])

        self.assertTrue('groups' in response.data)
        self.assertEqual(response.data['groups']['result'], [])

        self.assertTrue('departments' in response.data)
        self.assertEqual(response.data['departments']['result'], [])

    def test_serializer(self):
        serializer = DirectorySuggestSerializer(data={})
        self.assertFalse(serializer.is_valid())
        self.assertTrue('org_id' in serializer.errors)

        serializer = DirectorySuggestSerializer(data={'org_id': '47'})
        self.assertTrue(serializer.is_valid())
        self.assertEqual(serializer.validated_data['org_id'], '47')
        self.assertFalse('text' in serializer.validated_data)
        self.assertFalse('id' in serializer.validated_data)
        self.assertFalse('layers' in serializer.validated_data)

        serializer = DirectorySuggestSerializer(data={
            'org_id': '47',
            'text': 'test',
            'id': '12,35',
            'layers': 'people,groups',
        })
        self.assertTrue(serializer.is_valid())
        self.assertEqual(serializer.validated_data['org_id'], '47')
        self.assertEqual(serializer.validated_data['text'], 'test')
        self.assertListEqual(serializer.validated_data['id'], ['12', '35'])
        self.assertListEqual(serializer.validated_data['layers'], ['people', 'groups'])

    @responses.activate
    def test_should_return_suggested_people(self):
        with patch.object(DirectorySuggest, 'get_people') as mock_get_people:
            mock_get_people.return_value = [
                {
                    'id': '123',
                    'title': 'My Name',
                },
                {
                    'id': '124',
                    'title': 'Your Name',
                },
            ]
            response = self.client.get(
                f'/admin/api/v2/directory/suggest/?org_id={self.org.dir_id}&text=name&layers=people',
                HTTP_X_ORGS=self.org.dir_id,
            )

        self.assertEqual(response.status_code, 200)
        self.assertTrue('people' in response.data)
        self.assertListEqual(response.data['people']['result'], [
            {
                'id': '123',
                'title': 'My Name',
            },
            {
                'id': '124',
                'title': 'Your Name',
            },
        ])
        mock_get_people.assert_called_once_with('name', None)
        self.assertFalse('groups' in response.data)
        self.assertFalse('departments' in response.data)

    @responses.activate
    def test_should_return_suggested_groups(self):
        with patch.object(DirectorySuggest, 'get_groups') as mock_get_groups:
            mock_get_groups.return_value = [
                {
                    'id': '1',
                    'title': 'My Role',
                },
                {
                    'id': '2',
                    'title': 'Your Role',
                },
            ]
            response = self.client.get(
                f'/admin/api/v2/directory/suggest/?org_id={self.org.dir_id}&text=role&layers=groups',
                HTTP_X_ORGS=self.org.dir_id,
            )

        self.assertEqual(response.status_code, 200)
        self.assertTrue('groups' in response.data)
        self.assertListEqual(response.data['groups']['result'], [
            {
                'id': '1',
                'title': 'My Role',
            },
            {
                'id': '2',
                'title': 'Your Role',
            },
        ])
        mock_get_groups.assert_called_once_with('role', None)
        self.assertFalse('people' in response.data)
        self.assertFalse('departments' in response.data)

    @responses.activate
    def test_should_return_suggested_departments(self):
        with patch.object(DirectorySuggest, 'get_departments') as mock_get_departments:
            mock_get_departments.return_value = [
                {
                    'id': '2',
                    'title': 'My Branch',
                },
                {
                    'id': '5',
                    'title': 'Your Branch',
                },
            ]
            response = self.client.get(
                f'/admin/api/v2/directory/suggest/?org_id={self.org.dir_id}&text=bran&layers=departments',
                HTTP_X_ORGS=self.org.dir_id,
            )

        self.assertEqual(response.status_code, 200)
        self.assertTrue('departments' in response.data)
        self.assertListEqual(response.data['departments']['result'], [
            {
                'id': '2',
                'title': 'My Branch',
            },
            {
                'id': '5',
                'title': 'Your Branch',
            },
        ])
        mock_get_departments.assert_called_once_with('bran', None)
        self.assertFalse('people' in response.data)
        self.assertFalse('groups' in response.data)


@override_cache_settings()
class TestBiSearchSuggest(TestCase):
    def register_user(self, data, status=200):
        responses.add(
            responses.GET,
            'https://api-integration-qa.directory.ws.yandex.net/v6/users/',
            json=data,
            status=status,
        )

    def register_group(self, data, status=200):
        responses.add(
            responses.GET,
            'https://api-integration-qa.directory.ws.yandex.net/v6/groups/',
            json=data,
            status=status,
        )

    def register_department(self, data, status=200):
        responses.add(
            responses.GET,
            'https://api-integration-qa.directory.ws.yandex.net/v6/departments/',
            json=data,
            status=status,
        )

    def register_bisearch(self, data, status=200):
        responses.add(
            responses.GET,
            'https://bisearch-backend-prestable.tools.yandex.ru/_abovemeta/suggest/',
            json=data,
            status=status,
        )

    @responses.activate
    def test_should_return_empty_without_text_or_ids(self):
        self.register_bisearch({
            'people': {'result': [], 'pagination': {}},
            'groups': {'result': [], 'pagination': {}},
            'departments': {'result': [], 'pagination': {}},
        })
        ds = BiSearchSuggest('123', 'abcd', '47')
        result = ds.suggest(text='')

        self.assertTrue('people' in result)
        self.assertEqual(result['people']['result'], [])

        self.assertTrue('groups' in result)
        self.assertEqual(result['groups']['result'], [])

        self.assertTrue('departments' in result)
        self.assertEqual(result['departments']['result'], [])
        self.assertEqual(responses.calls[0].request.params['layers'], 'people,groups,departments')

    @responses.activate
    def test_should_return_empty_if_org_not_exist(self):
        self.register_bisearch({}, status=404)

        ds = BiSearchSuggest('123', 'abcd', '47')
        result = ds.suggest(text='')

        self.assertTrue('people' in result)
        self.assertEqual(result['people']['result'], [])

        self.assertTrue('groups' in result)
        self.assertEqual(result['groups']['result'], [])

        self.assertTrue('departments' in result)
        self.assertEqual(result['departments']['result'], [])
        self.assertEqual(responses.calls[0].request.params['layers'], 'people,groups,departments')

    @responses.activate
    def test_should_suggest_people_by_text(self):
        self.register_bisearch({
            'people': {'result': [{
                'id': '123',
                'cloud_uid': 'abcd',
                'title': 'Вася Пупкин',
                'fields': [{'type': 'login', 'value': 'vasilek'}],
            }], 'pagination': {}},
        })
        ds = BiSearchSuggest('123', 'abcd', '47')
        result = ds.suggest(text='пуп', layers=['people'])

        self.assertTrue('people' in result)
        self.assertEqual(len(result['people']['result']), 1)
        self.assertEqual(result['people']['result'][0]['id'], '123')
        self.assertEqual(result['people']['result'][0]['cloud_uid'], 'abcd')
        self.assertEqual(result['people']['result'][0]['title'], 'Вася Пупкин')
        self.assertEqual(len(result['people']['result'][0]['fields']), 1)
        self.assertDictEqual(result['people']['result'][0]['fields'][0], {
            'type': 'login',
            'value': 'vasilek',
        })
        self.assertEqual(responses.calls[0].request.params['layers'], 'people')

        self.assertFalse('groups' in result)
        self.assertFalse('departments' in result)

    @responses.activate
    def test_should_suggest_people_by_text_in_english(self):
        self.register_bisearch({
            'people': {'result': [{
                'id': '123',
                'cloud_uid': 'abcd',
                'title': 'Vasia Pupkin',
                'fields': [{'type': 'login', 'value': 'vasilek'}],
            }], 'pagination': {}},
        })
        ds = BiSearchSuggest('123', 'abcd', '47', 'en')
        result = ds.suggest(text='pup', layers=['people'])

        self.assertTrue('people' in result)
        self.assertEqual(len(result['people']['result']), 1)
        self.assertEqual(result['people']['result'][0]['id'], '123')
        self.assertEqual(result['people']['result'][0]['cloud_uid'], 'abcd')
        self.assertEqual(result['people']['result'][0]['title'], 'Vasia Pupkin')
        self.assertEqual(len(result['people']['result'][0]['fields']), 1)
        self.assertDictEqual(result['people']['result'][0]['fields'][0], {
            'type': 'login',
            'value': 'vasilek',
        })
        self.assertEqual(responses.calls[0].request.params['language'], 'en')
        self.assertEqual(responses.calls[0].request.params['layers'], 'people')

        self.assertFalse('groups' in result)
        self.assertFalse('departments' in result)

    @responses.activate
    def test_should_suggest_people_by_ids(self):
        self.register_user({
            'links': {},
            'result': [
                {
                    'id': 123,
                    'cloud_uid': 'abcd',
                    'nickname': 'vasilek',
                    'name': {
                        'first': {'ru': 'Вася', 'en': 'Vasia'},
                        'last': {'ru': 'Пупкин', 'en': 'Pupkin'},
                    },
                },
            ]
        })
        ds = BiSearchSuggest('123', 'abcd', '47')
        result = ds.suggest(ids=['123'], layers=['people'])

        self.assertTrue('people' in result)
        self.assertEqual(len(result['people']['result']), 1)
        self.assertEqual(result['people']['result'][0]['id'], '123')
        self.assertEqual(result['people']['result'][0]['title'], 'Вася Пупкин')

        self.assertFalse('groups' in result)
        self.assertFalse('departments' in result)

    @responses.activate
    def test_should_suggest_group_by_text(self):
        self.register_bisearch({
            'groups': {'result': [
                {'id': '1', 'title': 'First Role'},
                {'id': '2', 'title': 'Second Role'},
            ], 'pagination': {}},
        })
        ds = BiSearchSuggest('123', 'abcd', '47')
        result = ds.suggest(text='rol', layers=['groups'])

        self.assertTrue('groups' in result)
        self.assertEqual(len(result['groups']['result']), 2)
        self.assertEqual(result['groups']['result'][0]['id'], '1')
        self.assertEqual(result['groups']['result'][0]['title'], 'First Role')
        self.assertEqual(result['groups']['result'][1]['id'], '2')
        self.assertEqual(result['groups']['result'][1]['title'], 'Second Role')
        self.assertEqual(responses.calls[0].request.params['layers'], 'groups')

        self.assertFalse('people' in result)
        self.assertFalse('departments' in result)

    @responses.activate
    def test_should_suggest_group_by_ids(self):
        self.register_group({
            'links': {},
            'result': [
                {'id': 1, 'name': 'First Role'},
                {'id': 2, 'name': 'Second Role'},
            ],
        })
        ds = BiSearchSuggest('123', 'abcd', '47')
        result = ds.suggest(ids=['1', '2'], layers=['groups'])

        self.assertTrue('groups' in result)
        self.assertEqual(len(result['groups']['result']), 2)
        self.assertEqual(result['groups']['result'][0]['id'], '1')
        self.assertEqual(result['groups']['result'][0]['title'], 'First Role')
        self.assertEqual(result['groups']['result'][1]['id'], '2')
        self.assertEqual(result['groups']['result'][1]['title'], 'Second Role')

        self.assertFalse('people' in result)
        self.assertFalse('departments' in result)

    @responses.activate
    def test_should_suggest_department_by_text(self):
        self.register_bisearch({
            'departments': {'result': [
                {'id': '2', 'title': 'First Branch'},
                {'id': '5', 'title': 'Second Branch'},
            ], 'pagination': {}},
        })
        ds = BiSearchSuggest('123', 'abcd', '47')
        result = ds.suggest(text='bra', layers=['departments'])

        self.assertTrue('departments' in result)
        self.assertEqual(len(result['departments']['result']), 2)
        self.assertEqual(result['departments']['result'][0]['id'], '2')
        self.assertEqual(result['departments']['result'][0]['title'], 'First Branch')
        self.assertEqual(result['departments']['result'][1]['id'], '5')
        self.assertEqual(result['departments']['result'][1]['title'], 'Second Branch')
        self.assertEqual(responses.calls[0].request.params['layers'], 'departments')

        self.assertFalse('people' in result)
        self.assertFalse('groups' in result)

    @responses.activate
    def test_should_suggest_department_by_ids(self):
        self.register_department({
            'links': {},
            'result': [
                {'id': 2, 'name': 'First Branch'},
                {'id': 5, 'name': 'Second Branch'},
            ],
        })
        ds = BiSearchSuggest('123', 'abcd', '47')
        result = ds.suggest(ids=['2', '5'], layers=['departments'])

        self.assertTrue('departments' in result)
        self.assertEqual(len(result['departments']['result']), 2)
        self.assertEqual(result['departments']['result'][0]['id'], '2')
        self.assertEqual(result['departments']['result'][0]['title'], 'First Branch')
        self.assertEqual(result['departments']['result'][1]['id'], '5')
        self.assertEqual(result['departments']['result'][1]['title'], 'Second Branch')

        self.assertFalse('people' in result)
        self.assertFalse('groups' in result)
