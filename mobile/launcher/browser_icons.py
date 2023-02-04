import json
import mock
import requests_mock
from django.test import Client, TestCase
from rest_framework import status

from yaphone.advisor.common.mocks.geobase import LookupMock
from yaphone.advisor.launcher.tests.base import StatelessViewMixin
from yaphone.advisor.launcher.tests.fixtures import load_requests_fixtures


@mock.patch('yaphone.utils.geo.geobase_lookuper', LookupMock())
class IconsForUrlTest(StatelessViewMixin, TestCase):
    default_params = {
        'url': 'http://sports.ru'
    }
    endpoint = '/api/v2/icons_for_urls/'

    def test_ok(self):
        with requests_mock.mock() as m:
            load_requests_fixtures(m)
            super(IconsForUrlTest, self).test_ok()

    def test_valid_urls(self):
        with requests_mock.mock() as m:
            load_requests_fixtures(m)

            response = self.get()
            self.assertEqual(response.status_code, status.HTTP_200_OK)

            for item in json.loads(response.content):
                self.assertGreater(len(item['icons']), 0, 'icons not found for {url}'.format(url=item['url']))


class IconsForUrlTestV1(TestCase):
    default_params = {
        'url': 'http://sports.ru'
    }
    endpoint = '/api/v1/icons_for_urls/'

    def setUp(self):
        #  no uuid
        self.client = Client(
            HTTP_USER_AGENT='com.yandex.launcher/2.00.qa2147483647 (Yandex Nexus 9; Android 9.2.1beta)',
            HTTP_HOST='localhost',
        )

    def test_valid_urls(self):
        with requests_mock.mock() as m:
            load_requests_fixtures(m)

            response = self.get()
            self.assertEqual(response.status_code, status.HTTP_200_OK)

            for item in json.loads(response.content):
                self.assertGreater(len(item['icons']), 0, 'icons not found for {url}'.format(url=item['url']))

    def get(self, params=None, **kwargs):
        return self.client.get(
            path=self.endpoint,
            data=params or self.default_params,
            **kwargs
        )
