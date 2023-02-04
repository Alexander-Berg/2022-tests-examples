# -*- coding: utf-8 -*-
import responses

from django.conf import settings
from django.test import TestCase
from unittest.mock import patch, Mock

from events.accounts.factories import UserFactory
from events.accounts.helpers import YandexClient
from events.data_sources.sources import WebSiteDataSource
from events.yauth_contrib.auth import TvmAuth
from requests.exceptions import HTTPError


@patch.object(TvmAuth, '_get_service_ticket', Mock('123'))
class TestWebSiteDataSource(TestCase):
    def setUp(self):
        self.user = UserFactory()

    def register_uri(self, data=None, status=200):
        base_url = settings.WEBMASTER_API_URL
        responses.add(
            responses.GET,
            f'{base_url}/user/host/listUserHosts.json',
            json=data,
            status=status,
        )

    @responses.activate
    def test_should_return_empty_list_if_user_is_none(self):
        self.assertEqual(WebSiteDataSource(user=None).get_filtered_queryset(), [])

    @responses.activate
    def test_should_return_empty_list_if_user_is_anonymous(self):
        anonym = UserFactory(uid=None)
        self.assertEqual(WebSiteDataSource(user=anonym).get_filtered_queryset(), [])

    @responses.activate
    def test_should_return_empty_list_if_user_has_no_sites_at_all(self):
        self.register_uri({
            'status': 'SUCCESS',
            'data': {
                'verifiedHosts': [],
            },
        })
        self.assertEqual(WebSiteDataSource(user=self.user).get_filtered_queryset(), [])

    @responses.activate
    def test_should_return_verified_hosts_for_exact_user(self):
        self.register_uri({
            'status': 'SUCCESS',
            'data': {
                'verifiedHosts': [
                    {
                        'hostId': 'http:yandex.ru:80',
                        'readableHostUrl': 'http://yandex.ru',
                    },
                    {
                        'hostId': 'https:yandex.ru:443',
                        'readableHostUrl': 'https://yandex.ru',
                    },
                    {
                        'hostId': 'https:яндекс.рф:443',
                        'readableHostUrl': 'https://яндекс.рф',
                    },
                ],
            },
        })
        response = WebSiteDataSource(user=self.user).get_filtered_queryset()

        self.assertEqual(len(response), 3)
        self.assertEqual(response[0]['id'], 'http:yandex.ru:80')
        self.assertEqual(response[0]['name'], 'yandex.ru')

        self.assertEqual(response[1]['id'], 'https:yandex.ru:443')
        self.assertEqual(response[1]['name'], 'https://yandex.ru')

        self.assertEqual(response[2]['id'], 'https:яндекс.рф:443')
        self.assertEqual(response[2]['name'], 'https://яндекс.рф')

    @responses.activate
    def test_should_filter_by_ids(self):
        data_source_instance = WebSiteDataSource()
        data_source_instance.get_queryset = Mock(return_value=[
            {'id': 'yandex.ru', 'name': 'yandex.ru'},
            {'id': 'google.com', 'name': 'google.com'}
        ])
        response = data_source_instance.get_filtered_queryset(filter_data={
            'id': ['yandex.ru']
        })
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['name'], 'yandex.ru')

    @responses.activate
    def test_should_filter_by_text(self):
        data_source_instance = WebSiteDataSource()
        data_source_instance.get_queryset = Mock(return_value=[
            {'id': 'yandex.ru', 'name': 'yandex.ru'},
            {'id': 'google.com', 'name': 'google.com'}
        ])
        response = data_source_instance.get_filtered_queryset(filter_data={
            'text': 'yandex.ru'
        })
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['name'], 'yandex.ru')

    @responses.activate
    def test_serializer(self):
        self.register_uri({
            'status': 'SUCCESS',
            'data': {
                'verifiedHosts': [
                    {
                        'hostId': 'http:google.ru:80',
                        'readableHostUrl': 'http://google.ru',
                    },
                    {
                        'hostId': 'http:yandex.ru:80',
                        'readableHostUrl': 'http://yandex.ru',
                    },
                    {
                        'hostId': 'https:яндекс.рф:443',
                        'readableHostUrl': 'https://яндекс.рф',
                    },
                ],
            },
        })
        response = WebSiteDataSource.serializer_class(
            WebSiteDataSource(user=self.user).get_filtered_queryset(),
            many=True
        ).data
        expected = [
            {
                'id': 'http:google.ru:80',
                'text': 'google.ru',
            },
            {
                'id': 'http:yandex.ru:80',
                'text': 'yandex.ru',
            },
            {
                'id': 'https:яндекс.рф:443',
                'text': 'https://яндекс.рф',
            },
        ]
        self.assertListEqual(response, expected)

    @responses.activate
    def test_on_error_should_return_empty_list(self):
        data_source_instance = WebSiteDataSource(user=self.user)
        with patch('events.data_sources.sources.webmaster.WebMasterAPIClient.get') as mock_get:
            mock_get.side_effect = HTTPError
            response = data_source_instance.get_filtered_queryset(filter_data={
                'text': 'yandex.ru'
            })
        self.assertEqual(len(response), 0)
        mock_get.assert_called_once()


@patch.object(TvmAuth, '_get_service_ticket', Mock('123'))
class TestWebSiteDataSourceViewSet(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(is_superuser=True)
        self.base_url = '/v1/data-source/web-master-site/'

    def register_uri(self, data=None, status=200):
        base_url = settings.WEBMASTER_API_URL
        responses.add(
            responses.GET,
            f'{base_url}/user/host/listUserHosts.json',
            json=data,
            status=status,
        )

    def get_webmaster_response(self):
        return {
            'status': 'SUCCESS',
            'data': {
                'verifiedHosts': [
                    {
                        'hostId': 'http:google.ru:80',
                        'readableHostUrl': 'http://google.ru',
                    },
                    {
                        'hostId': 'http:yandex.ru:80',
                        'readableHostUrl': 'http://yandex.ru',
                    },
                    {
                        'hostId': 'http:диван.рф:80',
                        'readableHostUrl': 'http://диван.рф',
                    },
                    {
                        'hostId': 'https:кровать.рф:443',
                        'readableHostUrl': 'https://кровать.рф',
                    },
                ],
            },
        }

    @responses.activate
    def test_without_filters_should_return_all_items(self):
        self.register_uri(self.get_webmaster_response())
        response = self.client.get(self.base_url)
        self.assertEqual(response.status_code, 200)

        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            'http:google.ru:80': 'google.ru',
            'http:yandex.ru:80': 'yandex.ru',
            'http:диван.рф:80': 'диван.рф',
            'https:кровать.рф:443': 'https://кровать.рф',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    @responses.activate
    def test_with_filter_in_should_return_one_item(self):
        self.register_uri(self.get_webmaster_response())
        response = self.client.get('%s?id=http:yandex.ru:80' % self.base_url)
        self.assertEqual(response.status_code, 200)

        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            'http:yandex.ru:80': 'yandex.ru',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    @responses.activate
    def test_with_filter_in_should_return_one_item_take2(self):
        self.register_uri(self.get_webmaster_response())
        response = self.client.get('%s?id=http:диван.рф:80' % self.base_url)
        self.assertEqual(response.status_code, 200)

        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            'http:диван.рф:80': 'диван.рф',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    @responses.activate
    def test_with_filter_in_without_param_should_return_all_items(self):
        self.register_uri(self.get_webmaster_response())
        response = self.client.get('%s?id=' % self.base_url)
        self.assertEqual(response.status_code, 200)

        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            'http:google.ru:80': 'google.ru',
            'http:yandex.ru:80': 'yandex.ru',
            'http:диван.рф:80': 'диван.рф',
            'https:кровать.рф:443': 'https://кровать.рф',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)
