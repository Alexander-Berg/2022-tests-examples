# -*- coding: utf-8 -*-
import responses

from django.test import TestCase
from requests.exceptions import HTTPError

from events.common_app.support.api_client import SupportAPIClient


class TestSupportAPIClient(TestCase):
    def setUp(self):
        self.url = 'https://support-api.locdoc-test.yandex.net/api/v1/visits?yandexuid=1234567'

    @responses.activate
    def test_should_get_data_from_response_success(self):
        body = [{
            'yandexuid': '1234567',
            'referer': 'https://some.referer.com/',
            'created_at': '2018-01-09T09:49:42.541812Z',
        }]
        responses.add(responses.GET, self.url, json=body, status=200)

        response = SupportAPIClient().get('referer', params={'yandexuid': '1234567'})
        expected = [{
            "yandexuid": "1234567",
            "referer": "https://some.referer.com/",
            "created_at": "2018-01-09T09:49:42.541812Z"
        }]
        self.assertEqual(response, expected)

    @responses.activate
    def test_should_raise_if_error_response(self):
        responses.add(responses.GET, self.url, json={}, status=503)
        with self.assertRaises(HTTPError):
            SupportAPIClient().get('referer', params={'yandexuid': '1234567'})
