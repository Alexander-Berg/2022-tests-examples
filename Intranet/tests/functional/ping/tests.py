# -*- coding: utf-8 -*-
import json

import psycopg2
from unittest.mock import patch, Mock

from testutils import TestCase

from hamcrest import (
    assert_that,
    equal_to,
)
from intranet.yandex_directory.src.yandex_directory.common.smoke import _smoke_check


def raise_error(*args, **kwargs):
    raise Exception


patched_services_with_error = [
    {
        'name': 'Test error',
        'func': raise_error,
        'vital': True,
    },
]


class TestPing(TestCase):
    def test_ok(self):
        # сэмулируем проверку
        _smoke_check()

        response = self.client.get('/ping/')
        self.assertEqual(response.status_code, 200)

    @patch(
        'psycopg2.connect',
        Mock(side_effect=psycopg2.OperationalError('Something bad with connect'))
    )
    def test_error(self):
        # сэмулируем проверку
        _smoke_check()
        response = self.client.get('/ping/')
        self.assertEqual(response.status_code, 500)

    @patch('intranet.yandex_directory.src.yandex_directory.common.smoke.SERVICES', patched_services_with_error)
    def test_should_return_500_code_with_data(self):
        # сэмулируем проверку
        _smoke_check()
        response = self.client.get('/ping/')
        self.assertEqual(response.status_code, 500)

        expected = {
            'code': 'ping_error',
            'message': 'Smoke tests detected errors in vital services',
            'params': {
                'smoke_tests': {
                    'has_errors_in_vital_services': True,
                    'environment': 'autotests',
                    'default_timeout': 10,
                    'errors': {
                        'test-error': '!Exception (look for traceback in logs)',
                    }
                }
            }
        }
        result = json.loads(response.get_data())
        assert_that(result, equal_to(expected))
