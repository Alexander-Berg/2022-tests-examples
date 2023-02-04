from django.conf import settings

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin

LABAPI_BASE_URL = settings.LABAPI_BASE_URL


class PingTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'ping'

    def test_url(self):
        self.assertURLNameEqual('/ping/')

    def test_ping(self):
        with self.assertNumQueries(0):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        response_content = response.content
        self.assertEqual(response_content, b'pong')
