# -*- coding: utf-8 -*-
import json
import responses

from django.test import TestCase
from requests.exceptions import HTTPError

from events.tanker.client import TankerClient


class TestTankerClient(TestCase):
    @responses.activate
    def test_get_keyset_success(self):
        keyset = 'testit'
        responses.add(
            responses.GET,
            f'https://tanker.yandex-team.ru/api/v1/project/test/keyset/{keyset}/',
            json={'code': 'testit'},
        )
        client = TankerClient('test')
        result = client.get_keyset(keyset)
        self.assertEqual(result['code'], keyset)

    @responses.activate
    def test_get_keyset_not_found(self):
        keyset = 'testit'
        responses.add(
            responses.GET,
            f'https://tanker.yandex-team.ru/api/v1/project/test/keyset/{keyset}/',
            json={'code': 'DOES_NOT_EXIST'},
            status=404,
        )
        client = TankerClient('test')
        result = client.get_keyset(keyset)
        self.assertIsNone(result)

    @responses.activate
    def test_create_keyset_success(self):
        keyset = 'testit'
        languages = ['ru', 'en', 'de']
        original_language = 'en'
        data = {
            'code': keyset,
            'name': keyset,
            'meta': {
                'languages': languages,
                'original_language': original_language,
                'auto_approve_original': True,
                'auto_approve': False,
            },
        }
        responses.add(
            responses.POST,
            'https://tanker.yandex-team.ru/api/v1/project/test/keyset/',
            json=data,
        )
        client = TankerClient('test')
        result = client.create_keyset(keyset, languages, original_language)
        self.assertEqual(result['code'], keyset)
        self.assertEqual(len(responses.calls), 1)
        request_data = json.loads(responses.calls[0].request.body)
        self.assertEqual(set(request_data['meta']['languages']), set(languages))
        del request_data['meta']['languages']
        del data['meta']['languages']
        self.assertEqual(request_data, data)

    @responses.activate
    def test_create_keyset_conflict(self):
        keyset = 'testit'
        responses.add(
            responses.POST,
            'https://tanker.yandex-team.ru/api/v1/project/test/keyset/',
            json={'code': 'CONFLICT_STATE'},
            status=409,
        )
        client = TankerClient('test')
        with self.assertRaises(HTTPError):
            client.create_keyset(keyset)

    @responses.activate
    def test_change_keyset_success(self):
        keyset = 'testit'
        languages = ['ru', 'en', 'de']
        original_language = 'en'
        data = {
            'code': keyset,
            'name': keyset,
            'status': 'ACTIVE',
            'meta': {
                'languages': languages,
                'original_language': original_language,
                'auto_approve_original': True,
                'auto_approve': False,
            },
        }
        responses.add(
            responses.PATCH,
            f'https://tanker.yandex-team.ru/api/v1/project/test/keyset/{keyset}/',
            json=data,
        )
        client = TankerClient('test')
        result = client.change_keyset(keyset, languages, original_language)
        self.assertEqual(result['code'], keyset)
        self.assertEqual(len(responses.calls), 1)
        request_data = json.loads(responses.calls[0].request.body)
        self.assertEqual(set(request_data['meta']['languages']), set(languages))
        del request_data['meta']['languages']
        del data['meta']['languages']
        self.assertEqual(request_data, data)

    @responses.activate
    def test_change_keyset_not_found(self):
        keyset = 'testit'
        responses.add(
            responses.PATCH,
            f'https://tanker.yandex-team.ru/api/v1/project/test/keyset/{keyset}/',
            json={'code': 'DOES_NOT_EXIST'},
            status=404,
        )
        client = TankerClient('test')
        with self.assertRaises(HTTPError):
            client.change_keyset(keyset)

    @responses.activate
    def test_get_keys_success(self):
        keyset = 'testit'
        responses.add(
            responses.GET,
            f'https://tanker.yandex-team.ru/api/v1/project/test/keyset/{keyset}/key/',
            json={'items': [{'name': 'testkey'}]},
        )
        client = TankerClient('test')
        keys = client.get_keys(keyset)
        self.assertEqual(len(keys), 1)
        self.assertEqual(keys[0]['name'], 'testkey')

    @responses.activate
    def test_get_keys_empty(self):
        keyset = 'testit'
        responses.add(
            responses.GET,
            f'https://tanker.yandex-team.ru/api/v1/project/test/keyset/{keyset}/key/',
            json={'items': []},
        )
        client = TankerClient('test')
        keys = client.get_keys(keyset)
        self.assertEqual(len(keys), 0)

    @responses.activate
    def test_get_keys_not_found(self):
        keyset = 'testit'
        responses.add(
            responses.GET,
            f'https://tanker.yandex-team.ru/api/v1/project/test/keyset/{keyset}/key/',
            json={'code': 'DOES_NOT_EXIST'},
            status=404,
        )
        client = TankerClient('test')
        with self.assertRaises(HTTPError):
            client.get_keys(keyset)

    @responses.activate
    def test_change_keys_success(self):
        keyset = 'testit'
        create_keys = {'key1': 'value1'}
        update_keys = {'key2': 'value2'}
        delete_keys = ['key3']
        responses.add(
            responses.POST,
            f'https://tanker.yandex-team.ru/api/v1/project/test/keyset/{keyset}/batch/',
            json={},
        )
        client = TankerClient('test')
        client.change_keys(
            keyset,
            create_keys=create_keys,
            update_keys=update_keys,
            delete_keys=delete_keys,
        )
        self.assertEqual(len(responses.calls), 1)
        request_data = json.loads(responses.calls[0].request.body)
        self.assertTrue(request_data['commit_message'].startswith('Update keys'))
        del request_data['commit_message']
        expected = {
            'update': {
                'action': 'CREATE',
                'keys': [
                    {
                        'name': 'key1',
                        'action': 'CREATE',
                        'plural': False,
                        'translations': {
                            'ru': {
                                'status': 'APPROVED',
                                'payload': {
                                    'singular_form': 'value1',
                                },
                            },
                        },
                    },
                    {
                        'name': 'key2',
                        'action': 'UPDATE',
                        'plural': False,
                        'translations': {
                            'ru': {
                                'status': 'APPROVED',
                                'payload': {
                                    'singular_form': 'value2',
                                },
                            },
                        },
                    },
                ],
            },
            'delete': {
                'keys': [
                    'key3',
                ],
            },
        }
        self.assertEqual(request_data, expected)

    @responses.activate
    def test_get_keysets_success(self):
        responses.add(
            responses.GET,
            'https://tanker.yandex-team.ru/api/v1/project/test/keyset/',
            json={
                'items': [
                    {'code': 'keyset1'},
                    {'code': 'keyset2'},
                ],
            },
        )
        client = TankerClient('test')
        result = client.get_keysets()
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0]['code'], 'keyset1')
        self.assertEqual(result[1]['code'], 'keyset2')

    @responses.activate
    def test_get_keysets_not_found(self):
        responses.add(
            responses.GET,
            'https://tanker.yandex-team.ru/api/v1/project/test/keyset/',
            json={'code': 'DOES_NOT_EXIST'},
            status=404,
        )
        client = TankerClient('test')
        with self.assertRaises(HTTPError):
            client.get_keysets()
