# -*- coding: utf-8 -*-
import responses

from django.test import TestCase, override_settings
from unittest.mock import patch

from events.accounts.helpers import YandexClient
from events.captcha.captcha import Captcha


@override_settings(FRONTENDS_BY_AUTH_KEY={'test_auth_key': {'auth_key': 'test_auth_key', 'name': 'events'}})
class TestCaptchaView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.headers = {
            'HTTP_X_FRONTEND_AUTHORIZATION': 'test_auth_key'
        }

    def register_uri(self):
        responses.add(
            responses.GET,
            'http://api.captcha.yandex.net/generate',
            content_type='text/xml',
            body='''<?xml version="1.0"?>
<number url='https://ext.captcha.yandex.net/image?key=12345'>12345</number>
            ''',
        )

    def test_should_return_401_if_frontend_is_not_authorized(self):
        response = self.client.post('/v1/captcha/')
        self.assertEqual(response.status_code, 401)

    @responses.activate
    def test_should_return_200_if_frontend_is_authorized(self):
        self.register_uri()
        response = self.client.post('/v1/captcha/', **self.headers)

        self.assertEqual(response.status_code, 200)
        expected = {
            'image_url': 'https://ext.captcha.yandex.net/image?key=12345',
            'key': '12345',
        }
        self.assertDictEqual(response.data, expected)
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.params['type'], 'std')

    def test_should_call_captcha_generate_func(self):
        with patch.object(Captcha, 'generate') as mock_generate:
            mock_generate.return_value = 'test url', 'test key'
            response = self.client.post('/v1/captcha/', **self.headers)

        mock_generate.assert_called_once()
        expected = {
            'image_url': 'test url',
            'key': 'test key',
        }
        self.assertDictEqual(response.data, expected)

    @responses.activate
    def test_should_call_ocr_captcha(self):
        self.register_uri()
        data = {'captcha_type': 'ocr'}
        response = self.client.post('/v1/captcha/', data=data, **self.headers)

        self.assertEqual(response.status_code, 200)
        expected = {
            'image_url': 'https://ext.captcha.yandex.net/image?key=12345',
            'key': '12345',
        }
        self.assertDictEqual(response.data, expected)
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.params['type'], 'ocr')
