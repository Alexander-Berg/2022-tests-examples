# -*- coding: utf-8 -*-
from django.test import Client, TestCase, override_settings
from unittest.mock import patch

from events.accounts.factories import OrganizationToGroupFactory
from events.accounts.helpers import YandexClient
from events.common_app.directory import DirectorySuggest, BiSearchClient, BiSearchSuggest


class TestDirUserDataSource(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        o2g = OrganizationToGroupFactory()
        self.org = o2g.org
        self.user.groups.add(o2g.group)

    def test_should_return_empty_for_not_logged_user(self):
        client = Client()

        response = client.get('/v1/data-source/dir-user/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

        response = client.get('/admin/api/v2/data-source/dir-user/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

    def test_should_return_empty_wihtout_orgs(self):
        response = self.client.get('/v1/data-source/dir-user/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

        response = self.client.get('/admin/api/v2/data-source/dir-user/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

    def test_should_return_suggested_items(self):
        text = 'name'
        with patch.object(DirectorySuggest, 'get_people') as mock_get_people:
            mock_get_people.return_value = [
                {
                    'id': '123',
                    'title': 'My Name',
                    'url': None,
                },
                {
                    'id': '124',
                    'title': 'Your Name',
                    'url': None,
                },
            ]
            response = self.client.get(f'/v1/data-source/dir-user/?suggest={text}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Name',
            },
            {
                'id': '124',
                'text': 'Your Name',
            },
        ])
        mock_get_people.assert_called_once_with(text, [])

    def test_should_return_items_by_id(self):
        ids = '123,124'
        with patch.object(DirectorySuggest, 'get_people') as mock_get_people:
            mock_get_people.return_value = [
                {
                    'id': '123',
                    'title': 'My Name',
                    'url': None,
                },
                {
                    'id': '124',
                    'title': 'Your Name',
                    'url': None,
                },
            ]
            response = self.client.get(f'/v1/data-source/dir-user/?id={ids}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Name',
            },
            {
                'id': '124',
                'text': 'Your Name',
            },
        ])
        mock_get_people.assert_called_once_with('', ids.split(','))

    @override_settings(USE_BISEARCH=True)
    def test_should_return_suggested_items_alter(self):
        text = 'name'
        with patch.object(BiSearchClient, 'suggest') as mock_suggest:
            mock_suggest.return_value = {
                'people': {'result': [
                    {
                        'id': '123',
                        'cloud_uid': 'abcd',
                        'title': 'My Name',
                        'url': None,
                    },
                    {
                        'id': '124',
                        'cloud_uid': 'dbca',
                        'title': 'Your Name',
                        'url': None,
                    },
                ]},
            }
            response = self.client.get(f'/v1/data-source/dir-user/?suggest={text}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Name',
            },
            {
                'id': '124',
                'text': 'Your Name',
            },
        ])
        mock_suggest.assert_called_once_with(text=text, layers=['people'])

    @override_settings(USE_BISEARCH=True)
    def test_should_return_items_by_id_alter(self):
        ids = '123,124'
        with patch.object(BiSearchSuggest, 'get_objects_by_ids') as mock_get_objects:
            mock_get_objects.return_value = {
                'people': {'result': [
                    {
                        'id': '123',
                        'title': 'My Name',
                        'url': None,
                    },
                    {
                        'id': '124',
                        'title': 'Your Name',
                        'url': None,
                    },
                ]},
            }
            response = self.client.get(f'/v1/data-source/dir-user/?id={ids}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Name',
            },
            {
                'id': '124',
                'text': 'Your Name',
            },
        ])
        mock_get_objects.assert_called_once_with(ids.split(','), ['people'])


class TestDirGroupDataSource(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        o2g = OrganizationToGroupFactory()
        self.org = o2g.org
        self.user.groups.add(o2g.group)

    def test_should_return_empty_for_not_logged_user(self):
        client = Client()

        response = client.get('/v1/data-source/dir-group/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

        response = client.get('/admin/api/v2/data-source/dir-group/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

    def test_should_return_empty_wihtout_orgs(self):
        response = self.client.get('/v1/data-source/dir-group/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

        response = self.client.get('/admin/api/v2/data-source/dir-group/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

    def test_should_return_suggested_items(self):
        text = 'role'
        with patch.object(DirectorySuggest, 'get_groups') as mock_get_groups:
            mock_get_groups.return_value = [
                {
                    'id': '123',
                    'title': 'My Role',
                    'url': None,
                },
                {
                    'id': '124',
                    'title': 'Your Role',
                    'url': None,
                },
            ]
            response = self.client.get(f'/v1/data-source/dir-group/?suggest={text}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Role',
            },
            {
                'id': '124',
                'text': 'Your Role',
            },
        ])
        mock_get_groups.assert_called_once_with(text, [])

    def test_should_return_items_by_id(self):
        ids = '123,124'
        with patch.object(DirectorySuggest, 'get_groups') as mock_get_groups:
            mock_get_groups.return_value = [
                {
                    'id': '123',
                    'title': 'My Role',
                    'url': None,
                },
                {
                    'id': '124',
                    'title': 'Your Role',
                    'url': None,
                },
            ]
            response = self.client.get(f'/v1/data-source/dir-group/?id={ids}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Role',
            },
            {
                'id': '124',
                'text': 'Your Role',
            },
        ])
        mock_get_groups.assert_called_once_with('', ids.split(','))

    @override_settings(USE_BISEARCH=True)
    def test_should_return_suggested_items_alter(self):
        text = 'role'
        with patch.object(BiSearchClient, 'suggest') as mock_suggest:
            mock_suggest.return_value = {
                'groups': {'result': [
                    {
                        'id': '123',
                        'title': 'My Role',
                        'url': None,
                    },
                    {
                        'id': '124',
                        'title': 'Your Role',
                        'url': None,
                    },
                ]},
            }
            response = self.client.get(f'/v1/data-source/dir-group/?suggest={text}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Role',
            },
            {
                'id': '124',
                'text': 'Your Role',
            },
        ])
        mock_suggest.assert_called_once_with(text=text, layers=['groups'])

    @override_settings(USE_BISEARCH=True)
    def test_should_return_items_by_id_alter(self):
        ids = '123,124'
        with patch.object(BiSearchSuggest, 'get_objects_by_ids') as mock_get_objects:
            mock_get_objects.return_value = {
                'groups': {'result': [
                    {
                        'id': '123',
                        'title': 'My Role',
                        'url': None,
                    },
                    {
                        'id': '124',
                        'title': 'Your Role',
                        'url': None,
                    },
                ]},
            }
            response = self.client.get(f'/v1/data-source/dir-group/?id={ids}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Role',
            },
            {
                'id': '124',
                'text': 'Your Role',
            },
        ])
        mock_get_objects.assert_called_once_with(ids.split(','), ['groups'])


class TestDirDepartmentDataSource(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        o2g = OrganizationToGroupFactory()
        self.org = o2g.org
        self.user.groups.add(o2g.group)

    def test_should_return_empty_for_not_logged_user(self):
        client = Client()

        response = client.get('/v1/data-source/dir-department/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

        response = client.get('/admin/api/v2/data-source/dir-department/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

    def test_should_return_empty_wihtout_orgs(self):
        response = self.client.get('/v1/data-source/dir-department/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

        response = self.client.get('/admin/api/v2/data-source/dir-department/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

    def test_should_return_suggested_items(self):
        text = 'branch'
        with patch.object(DirectorySuggest, 'get_departments') as mock_get_departments:
            mock_get_departments.return_value = [
                {
                    'id': '123',
                    'title': 'My Branch',
                    'url': None,
                },
                {
                    'id': '124',
                    'title': 'Your Branch',
                    'url': None,
                },
            ]
            response = self.client.get(f'/v1/data-source/dir-department/?suggest={text}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Branch',
            },
            {
                'id': '124',
                'text': 'Your Branch',
            },
        ])
        mock_get_departments.assert_called_once_with(text, [])

    def test_should_return_items_by_id(self):
        ids = '123,124'
        with patch.object(DirectorySuggest, 'get_departments') as mock_get_departments:
            mock_get_departments.return_value = [
                {
                    'id': '123',
                    'title': 'My Branch',
                    'url': None,
                },
                {
                    'id': '124',
                    'title': 'Your Branch',
                    'url': None,
                },
            ]
            response = self.client.get(f'/v1/data-source/dir-department/?id={ids}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Branch',
            },
            {
                'id': '124',
                'text': 'Your Branch',
            },
        ])
        mock_get_departments.assert_called_once_with('', ids.split(','))

    @override_settings(USE_BISEARCH=True)
    def test_should_return_suggested_items_alter(self):
        text = 'branch'
        with patch.object(BiSearchClient, 'suggest') as mock_suggest:
            mock_suggest.return_value = {
                'departments': {'result': [
                    {
                        'id': '123',
                        'title': 'My Branch',
                        'url': None,
                    },
                    {
                        'id': '124',
                        'title': 'Your Branch',
                        'url': None,
                    },
                ]},
            }
            response = self.client.get(f'/v1/data-source/dir-department/?suggest={text}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Branch',
            },
            {
                'id': '124',
                'text': 'Your Branch',
            },
        ])
        mock_suggest.assert_called_once_with(text=text, layers=['departments'])

    @override_settings(USE_BISEARCH=True)
    def test_should_return_items_by_id_alter(self):
        ids = '123,124'
        with patch.object(BiSearchSuggest, 'get_objects_by_ids') as mock_get_objects:
            mock_get_objects.return_value = {
                'departments': {'result': [
                    {
                        'id': '123',
                        'title': 'My Branch',
                        'url': None,
                    },
                    {
                        'id': '124',
                        'title': 'Your Branch',
                        'url': None,
                    },
                ]},
            }
            response = self.client.get(f'/v1/data-source/dir-department/?id={ids}', HTTP_X_ORGS=self.org.dir_id)

        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [
            {
                'id': '123',
                'text': 'My Branch',
            },
            {
                'id': '124',
                'text': 'Your Branch',
            },
        ])
        mock_get_objects.assert_called_once_with(ids.split(','), ['departments'])
