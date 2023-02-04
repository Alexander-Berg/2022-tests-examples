# -*- coding: utf-8 -*-
from django.test import TestCase

from events.accounts.factories import UserFactory
from events.accounts.helpers import YandexClient


class TestUsers(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def test_get_user_view(self):
        user = self.client.login_yandex()

        response = self.client.get('/v3/users/me/')
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(result['id'], user.pk)
        self.assertEqual(result['uid'], user.uid)

    def test_get_user_by_id_view(self):
        self.client.login_yandex()
        user = UserFactory()

        response = self.client.get(f'/v3/users/{user.pk}/')
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(result['id'], user.pk)
        self.assertEqual(result['uid'], user.uid)

        response = self.client.get('/v3/users/123/')
        self.assertEqual(response.status_code, 404)

    def test_get_user_by_uid_view(self):
        self.client.login_yandex()
        user = UserFactory(cloud_uid='abcd')

        response = self.client.get('/v3/users/', {'uid': user.uid})
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(result['id'], user.pk)
        self.assertEqual(result['uid'], user.uid)
        self.assertEqual(result['cloud_uid'], user.cloud_uid)

        response = self.client.get('/v3/users/', {'cloud_uid': user.cloud_uid})
        self.assertEqual(response.status_code, 200)

        result = response.json()
        self.assertEqual(result['id'], user.pk)
        self.assertEqual(result['uid'], user.uid)
        self.assertEqual(result['cloud_uid'], user.cloud_uid)

        response = self.client.get('/v3/users/')
        self.assertEqual(response.status_code, 400)

    def test_user_settings(self):
        user = self.client.login_yandex()

        response = self.client.get('/v3/users/me/settings/')
        self.assertEqual(response.status_code, 200)
        response_data = response.json()
        self.assertEqual(response_data['id'], user.pk)
        settings = response_data['settings']
        self.assertEqual(len(settings), 0)

        response = self.client.post('/v3/users/me/settings/', {'settings': {
            'key1': 'value1',
            'key2': 'value2',
        }}, format='json')
        self.assertEqual(response.status_code, 200)

        response = self.client.get('/v3/users/me/settings/')
        self.assertEqual(response.status_code, 200)
        settings = response.json()['settings']
        self.assertEqual(settings['key1'], 'value1')
        self.assertEqual(settings['key2'], 'value2')

        response = self.client.post('/v3/users/me/settings/', {'settings': {
            'key': 'new_value',
        }}, format='json')
        self.assertEqual(response.status_code, 200)
        settings = response.json()['settings']
        self.assertEqual(len(settings), 1)
        self.assertEqual(settings['key'], 'new_value')

        response = self.client.get('/v3/users/me/settings/')
        self.assertEqual(response.status_code, 200)
        settings = response.json()['settings']
        self.assertEqual(len(settings), 1)
        self.assertEqual(settings['key'], 'new_value')

        response = self.client.delete('/v3/users/me/settings/')
        self.assertEqual(response.status_code, 200)

        response = self.client.get('/v3/users/me/settings/')
        self.assertEqual(response.status_code, 200)
        settings = response.json()['settings']
        self.assertEqual(len(settings), 0)
