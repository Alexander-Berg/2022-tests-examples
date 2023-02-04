# -*- coding: utf-8 -*-
import responses
import requests
from django.test import TestCase
from events.yauth_contrib.auth import OAuth


class TestOAuth(TestCase):
    @responses.activate
    def test_should_use_authorization(self):
        responses.add(responses.GET, 'http://yandex.ru/test', body='')

        response = requests.get('http://yandex.ru/test', auth=OAuth('123'))
        self.assertEqual(len(responses.calls), 1)
        request = responses.calls[0].request

        self.assertEqual(response.status_code, 200)
        self.assertTrue('Authorization' in request.headers)
        self.assertEqual(request.headers['Authorization'], 'OAuth 123')

    @responses.activate
    def test_shouldnt_use_authorization(self):
        responses.add(responses.GET, 'http://yandex.ru/test', body='')

        response = requests.get('http://yandex.ru/test')
        self.assertEqual(len(responses.calls), 1)
        request = responses.calls[0].request

        self.assertEqual(response.status_code, 200)
        self.assertFalse('Authorization' in request.headers)
