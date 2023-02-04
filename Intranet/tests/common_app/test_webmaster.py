# -*- coding: utf-8 -*-
import responses

from django.conf import settings
from django.test import TestCase
from requests.exceptions import HTTPError
from unittest.mock import patch, Mock

from events.common_app.webmaster.api_client import WebMasterAPIClient
from events.yauth_contrib.auth import TvmAuth


@patch.object(TvmAuth, '_get_service_ticket', Mock('123'))
class TestWebMasterAPIClient(TestCase):
    def register_uri(self, data, status=200):
        base_url = settings.WEBMASTER_API_URL
        responses.add(
            responses.GET,
            f'{base_url}/user/host/listUserHosts.json',
            json=data,
            status=status,
        )

    @responses.activate
    def test_should_get_data_with_right_method_name(self):
        self.register_uri({
            'status': 'SUCCESS',
            'data': {
                'verifiedHosts': [{
                    'hostId': 'https:yandex.ru:443',
                    'readableHostUrl': 'https://yandex.ru',
                }, {
                    'hostId': 'http:google.com:80',
                    'readableHostUrl': 'http://google.com',
                }],
            },
        })
        hosts = WebMasterAPIClient().list_user_hosts('12345')
        self.assertListEqual(hosts, [{
            'id': 'https:yandex.ru:443',
            'name': 'https://yandex.ru',
        }, {
            'id': 'http:google.com:80',
            'name': 'google.com',
        }])

    @responses.activate
    def test_on_error_should_raise_exception(self):
        self.register_uri({
            'error': 'invalid response',
        }, status=500)
        with self.assertRaises(HTTPError) as e:
            WebMasterAPIClient().list_user_hosts('12345')
            self.assertEqual(str(e.exception), 'invalid response')
