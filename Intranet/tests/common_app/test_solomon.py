# -*- coding: utf-8 -*-
import json
import responses
import urllib.parse

from blackbox import Blackbox
from django.core.management import call_command
from django.http import QueryDict
from django.test import TestCase, override_settings
from freezegun import freeze_time
from unittest.mock import patch, ANY

from events.common_app.metrics.solomon import get_alert_id, Tokens, SolomonMetrics, SolomonAlerts
from events.common_app.tasks import token_expired_metrics
from events.yauth_contrib.auth import TvmAuth


class TestUtils(TestCase):
    @override_settings(
        SOLOMON_SERVICE='service',
        SOLOMON_CLUSTER='cluster',
    )
    def test_get_alert_id(self):
        self.assertEqual(get_alert_id('TOKEN'), 'service_cluster_TOKEN')


class TestTokens(TestCase):
    def test__get_expired_in_days__token_invalid(self):
        tokens = Tokens()
        with patch.object(Blackbox, 'oauth') as mock_oauth:
            mock_oauth.return_value = {
                'status': 'INVALID',
            }
            self.assertEqual(tokens._get_expired_in_days('123'), 0)
            mock_oauth.assert_called_once_with('123', userip=ANY, by_token=True)

    def test__get_expired_in_days__has_not_oauth(self):
        tokens = Tokens()
        with patch.object(Blackbox, 'oauth') as mock_oauth:
            mock_oauth.return_value = {
                'status': 'VALID',
            }
            self.assertIsNone(tokens._get_expired_in_days('123'))
            mock_oauth.assert_called_once_with('123', userip=ANY, by_token=True)

    def test__get_expired_in_days__never_expired(self):
        tokens = Tokens()
        with patch.object(Blackbox, 'oauth') as mock_oauth:
            mock_oauth.return_value = {
                'status': 'VALID',
                'oauth': {'expire_time': ''},
            }
            self.assertEqual(tokens._get_expired_in_days('123'), tokens.never_expired)
            mock_oauth.assert_called_once_with('123', userip=ANY, by_token=True)

    @freeze_time('2021-08-31 12:21:33')
    def test__get_expired_in_days__not_expired(self):
        tokens = Tokens()
        with patch.object(Blackbox, 'oauth') as mock_oauth:
            mock_oauth.return_value = {
                'status': 'VALID',
                'oauth': {'expire_time': '2021-09-10 10:42:51'},
            }
            self.assertEqual(tokens._get_expired_in_days('123'), 9)
            mock_oauth.assert_called_once_with('123', userip=ANY, by_token=True)

    @freeze_time('2021-09-10 10:42:51')
    def test__get_expired_in_days__already_expired(self):
        tokens = Tokens()
        with patch.object(Blackbox, 'oauth') as mock_oauth:
            mock_oauth.return_value = {
                'status': 'VALID',
                'oauth': {'expire_time': '2021-08-31 12:21:33'},
            }
            self.assertEqual(tokens._get_expired_in_days('123'), 0)
            mock_oauth.assert_called_once_with('123', userip=ANY, by_token=True)

    def test__get_expired_in_days__handle_error(self):
        tokens = Tokens()
        with patch.object(Blackbox, 'oauth') as mock_oauth:
            mock_oauth.return_value = {
                'status': 'VALID',
                'oauth': {'expire_time': 'not a valid datetime'},
            }
            self.assertIsNone(tokens._get_expired_in_days('123'))
            mock_oauth.assert_called_once_with('123', userip=ANY, by_token=True)

    @override_settings(FAKE_TOKEN='123', some_value='321')
    def test_items(self):
        tokens = Tokens()
        result = dict(tokens.items())
        self.assertTrue('FAKE_TOKEN' in result)
        self.assertEqual(result['FAKE_TOKEN'], '123')
        self.assertTrue('some_value' not in result)

    @override_settings(FAKE_TOKEN='123')
    def test_get_expired_in_days(self):
        tokens = Tokens()
        with patch.object(Tokens, '_get_expired_in_days') as mock_expired:
            mock_expired.return_value = 7
            result = dict(tokens.get_expired_in_days())
            self.assertTrue('FAKE_TOKEN' in result)
            self.assertEqual(result['FAKE_TOKEN'], 7)


class TestSolomonMetrics(TestCase):
    @responses.activate
    @override_settings(
        SOLOMON_PROJECT='project',
        SOLOMON_SERVICE='service',
        SOLOMON_CLUSTER='cluster',
    )
    def test_write_metrics(self):
        responses.add(
            responses.POST,
            'https://solomon.yandex.net/api/v2/push/',
            json={'status': 'OK', 'sensorsProcessed': 2},
        )
        sensor_data = [
            ('sensor1', 11),
            ('sensor2', 2222),
        ]

        metrics = SolomonMetrics()
        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            metrics.write_metrics(sensor_data)

        self.assertEqual(len(responses.calls), 1)

        url = responses.calls[0].request.url
        urlparsed = urllib.parse.urlparse(url)
        params = QueryDict(urlparsed.query)
        self.assertEqual(params['project'], 'project')
        self.assertEqual(params['cluster'], 'cluster')
        self.assertEqual(params['service'], 'service')

        request_data = json.loads(responses.calls[0].request.body)
        self.assertDictEqual(request_data['commonLabels'], {'host': 'forms-push'})
        self.assertListEqual(request_data['metrics'], [
            {'labels': {'sensor': 'sensor1'}, 'value': 11},
            {'labels': {'sensor': 'sensor2'}, 'value': 2222},
        ])

    def test_token_expired_metrics(self):
        with (
            patch.object(Tokens, 'get_expired_in_days') as mock_get_expired_in_days,
            patch.object(SolomonMetrics, 'write_metrics') as mock_write_metrics,
        ):
            token_expired_metrics.delay()

        mock_get_expired_in_days.assert_called_once()
        mock_write_metrics.assert_called_once()


class TestSolomonAlerts(TestCase):
    def test_make_alerts(self):
        with patch.object(SolomonAlerts, 'make_token_alerts') as mock_make_token_alerts:
            call_command('make_alerts')

        mock_make_token_alerts.assert_called_once()
