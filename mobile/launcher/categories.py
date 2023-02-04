from unittest import skip

import json
import mock
from contextlib import contextmanager
from datetime import datetime, timedelta
from django.test import TestCase
from rest_framework import status
from yaphone.advisor.common.mocks import app_info

from yaphone.advisor.advisor.tests.views import BasicAdvisorViewTest
from yaphone.advisor.common.mocks.geobase import LookupMock
from yaphone.advisor.launcher.tests.base import StatelessViewMixin


@mock.patch('yaphone.advisor.advisor.app_info_loader.AppInfoLoader.lookup_rows', app_info.dyntable_lookup_mock)
class CategoriesForAppsTest(BasicAdvisorViewTest, TestCase):
    endpoint = '/api/v1/categories_for_apps/'

    default_params = {
        'package_names': 'ru.yandex.mail,ru.yandex.disk'
    }

    def test_too_many_items(self):
        params = {'package_names': ','.join(['ru.yandex.mail'] * 21)}
        self.assertEqual(self.get(params).status_code, status.HTTP_400_BAD_REQUEST)


# TODO: This test runs extremeley slow, please fix it
@skip
@mock.patch('yaphone.advisor.advisor.app_info_loader.AppInfoLoader.lookup_rows', app_info.dyntable_lookup_mock)
@mock.patch('yaphone.utils.geo.geobase_lookuper', LookupMock())
class CategoriesForAppsV2Test(StatelessViewMixin, TestCase):
    endpoint = '/api/v2/categories_for_apps/'

    default_params = {
        'package_names': 'ru.yandex.mail,ru.yandex.disk'
    }

    # 10 seconds more comfortable then 30 to wait tests
    degradation_mode_sleep_time = 10

    @contextmanager
    def degradation(self, mode):
        self.set_localization_key('degradation', mode)
        yield
        self.set_localization_key('degradation', '')

    def test_ok(self):
        self.assertEqual(self.get().status_code, status.HTTP_200_OK)

    def test_too_many_items(self):
        params = {'package_names': ','.join(['ru.yandex.mail'] * 21)}
        self.assertEqual(self.get(params).status_code, status.HTTP_400_BAD_REQUEST)

    def test_degradation_mode_binary_data(self):
        with self.degradation('binary_data'):
            fifty_mb = 50 * 1024 * 1024
            self.assertGreaterEqual(len(self.get().content), fifty_mb)

    @skip('now response.content is empty, todo: add corresponding mock to fix it')
    def test_degradation_mode_all_nulls(self):
        with self.degradation('all_nulls'):
            content = json.loads(self.get().content)
            items = set()
            for app in content.values():
                for item in app['categories']:
                    items.update(item.values())

            self.assertTrue(len(items) == 1 and items.pop() is None,
                            'we expect one is None value, but got "%s"' % content)

    def test_degradation_mode_empty_answer(self):
        with self.degradation('empty_answer'):
            self.assertEqual(len(self.get().content), 0)

    @mock.patch('yaphone.advisor.common.middleware.DegradationMiddleware.SLEEP_TIME', degradation_mode_sleep_time)
    def test_degradation_mode_sleep(self):
        with self.degradation('sleep'):
            start = datetime.now()
            self.get()
            end = datetime.now()
            diff = end - start
            self.assertGreaterEqual(diff, timedelta(seconds=self.degradation_mode_sleep_time),
                                    'request duration is equal to %s' % diff.seconds)

    def test_degradation_mode_bad_status_code(self):
        with self.degradation('bad_status_code'):
            self.assertIn(self.get().status_code, (500, 404, 400, 600, 999))

    def test_degradation_mode_bad_length(self):
        with self.degradation('bad_length'):
            response = self.get()
            self.assertNotEqual(response['Content-Length'], len(response.content))

    def test_degradation_mode_invalid_json(self):
        with self.degradation('invalid_json'):
            with self.assertRaises(ValueError):
                json.loads(self.get().content)

    @mock.patch('yaphone.advisor.common.middleware.DegradationMiddleware.SLEEP_TIME', degradation_mode_sleep_time)
    def test_degradation_mode_random(self):
        with self.degradation('random'):
            all_modes = (
                'binary_data', 'all_nulls', 'empty_answer', 'sleep', 'bad_status_code', 'bad_length', 'invalid_json'
            )
            self.assertIn(self.get()['X-YaDegradation'], all_modes)
