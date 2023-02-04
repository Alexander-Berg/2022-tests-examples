# -*- coding: utf-8 -*-
from django.conf import settings
from django.test import TestCase
from requests.exceptions import HTTPError, ConnectTimeout, ReadTimeout
from unittest.mock import patch, ANY

from events.common_app.helpers import MockResponse
from events.common_app.datalens.dataset_rls import (
    add_new_logins,
    new_logins,
    DataLensColumnNotFound,
    DataLensRlsDataNotFound,
    DataLensNothingToAppend,
)
from events.staff.factories import StaffPersonFactory


class TestDatasetRls(TestCase):
    def setUp(self):
        self.dataset_key = '1112'
        self.url = settings.DATALENS_URL.format(
            host=settings.DATALENS_HOST,
            dataset_key=self.dataset_key,
        )
        self.people = [
            StaffPersonFactory(login='kdunaev'),
            StaffPersonFactory(login='masloval'),
            StaffPersonFactory(login='irkka', is_dismissed=True),
            StaffPersonFactory(login='riotta'),
        ]

    def test_new_logins(self):
        rls_data = new_logins('', ['kdunaev', 'masloval'])
        expected = set([
            "'kdunaev': kdunaev",
            "'masloval': masloval",
        ])
        self.assertSetEqual(set(rls_data.split('\n')), expected)

        rls_data = new_logins(rls_data, ['kdunaev', 'riotta'])
        expected = set([
            "'kdunaev': kdunaev",
            "'masloval': masloval",
            "'riotta': riotta",
        ])
        self.assertSetEqual(set(rls_data.split('\n')), expected)

        self.assertIsNone(new_logins(rls_data, ['kdunaev', 'irkka', 'johndoe']))
        self.assertIsNone(new_logins(rls_data, ['']))
        self.assertIsNone(new_logins(rls_data, []))

    def test_shouldnt_raise_any_exception(self):
        dataset_info = {
            'result_schema': [
                {
                    'source': 'column1',
                    'guid': '111'
                },
                {
                    'source': 'column2',
                    'guid': '112'
                },
                {
                    'source': 'login',
                    'guid': '113'
                },
            ],
            'rls': {
                '111': '',
                '112': '',
                '113': '',
            },
        }

        with patch('events.common_app.datalens.dataset_rls.requests_session.get', return_value=MockResponse(dataset_info)) as mock_get:
            with patch('events.common_app.datalens.dataset_rls.requests_session.put', return_value=MockResponse('')) as mock_put:
                add_new_logins(self.dataset_key, ['kdunaev'])

        mock_get.assert_called_once_with(self.url, headers=ANY, timeout=ANY, verify=ANY)
        dataset_info['rls']['113'] = "'kdunaev': kdunaev"
        mock_put.assert_called_once_with(self.url, json=dataset_info, headers=ANY, timeout=ANY, verify=ANY)

    def test_should_raise_400_error(self):
        with patch('events.common_app.datalens.dataset_rls.requests_session.get', return_value=MockResponse('invalid request', 400)) as mock_get:
            with self.assertRaises(HTTPError):
                add_new_logins(self.dataset_key, ['kdunaev'])
        mock_get.assert_called_once_with(self.url, headers=ANY, timeout=ANY, verify=ANY)

    def test_should_raise_403_error(self):
        with patch('events.common_app.datalens.dataset_rls.requests_session.get', return_value=MockResponse('permission error', 403)) as mock_get:
            with self.assertRaises(HTTPError):
                add_new_logins(self.dataset_key, ['kdunaev'])

        mock_get.assert_called_once_with(self.url, headers=ANY, timeout=ANY, verify=ANY)

    def test_should_raise_500_error(self):
        with patch('events.common_app.datalens.dataset_rls.requests_session.get', return_value=MockResponse('internal error', 500)) as mock_get:
            with self.assertRaises(HTTPError):
                add_new_logins(self.dataset_key, ['kdunaev'])

        mock_get.assert_called_once_with(self.url, headers=ANY, timeout=ANY, verify=ANY)

    def test_should_raise_connect_timeout_error(self):
        with patch('events.common_app.datalens.dataset_rls.requests_session.get', side_effect=ConnectTimeout) as mock_get:
            with self.assertRaises(ConnectTimeout):
                add_new_logins(self.dataset_key, ['kdunaev'])

        mock_get.assert_called_once_with(self.url, headers=ANY, timeout=ANY, verify=ANY)

    def test_should_raise_read_timeout_error(self):
        dataset_info = {
            'result_schema': [
                {
                    'source': 'login',
                    'guid': '113'
                },
            ],
            'rls': {
                '113': '',
            },
        }

        with patch('events.common_app.datalens.dataset_rls.requests_session.get', return_value=MockResponse(dataset_info)) as mock_get:
            with patch('events.common_app.datalens.dataset_rls.requests_session.put', side_effect=ReadTimeout) as mock_put:
                with self.assertRaises(ReadTimeout):
                    add_new_logins(self.dataset_key, ['kdunaev'])

        mock_get.assert_called_once_with(self.url, headers=ANY, timeout=ANY, verify=ANY)
        dataset_info['rls']['113'] = "'kdunaev': kdunaev"
        mock_put.assert_called_once_with(self.url, json=dataset_info, headers=ANY, timeout=ANY, verify=ANY)

    def test_should_raise_column_not_found(self):
        dataset_info = {
            'result_schema': [
                {
                    'source': 'column1',
                    'guid': '111'
                },
                {
                    'source': 'column2',
                    'guid': '112'
                },
            ],
            'rls': {
                '111': '',
                '112': '',
            },
        }
        with patch('events.common_app.datalens.dataset_rls.get_dataset_info', return_value=dataset_info) as mock_get_dataset_info:
            with self.assertRaises(DataLensColumnNotFound):
                add_new_logins(self.dataset_key, ['kdunaev'])

        mock_get_dataset_info.assert_called_once_with(self.dataset_key)

    def test_should_raise_rls_data_not_found(self):
        dataset_info = {
            'result_schema': [
                {
                    'source': 'column1',
                    'guid': '111'
                },
                {
                    'source': 'column2',
                    'guid': '112'
                },
                {
                    'source': 'login',
                    'guid': '113'
                },
            ],
            'rls': {
                '111': '',
                '112': '',
            },
        }
        with patch('events.common_app.datalens.dataset_rls.get_dataset_info', return_value=dataset_info) as mock_get_dataset_info:
            with self.assertRaises(DataLensRlsDataNotFound):
                add_new_logins(self.dataset_key, ['kdunaev'])

        mock_get_dataset_info.assert_called_once_with(self.dataset_key)

    def test_should_raise_nothing_to_append(self):
        dataset_info = {
            'result_schema': [
                {
                    'source': 'column1',
                    'guid': '111'
                },
                {
                    'source': 'column2',
                    'guid': '112'
                },
                {
                    'source': 'login',
                    'guid': '113'
                },
            ],
            'rls': {
                '111': '',
                '112': '',
                '113': "'kdunaev': kdunaev",
            },
        }
        with patch('events.common_app.datalens.dataset_rls.get_dataset_info', return_value=dataset_info) as mock_get_dataset_info:
            with self.assertRaises(DataLensNothingToAppend):
                add_new_logins(self.dataset_key, ['kdunaev'])

        mock_get_dataset_info.assert_called_once_with(self.dataset_key)
