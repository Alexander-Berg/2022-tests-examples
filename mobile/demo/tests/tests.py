import json
from django.test import TestCase
from django.test.client import Client
from rest_framework import status
from yaphone.advisor.common.mocks import localization_mock

from yaphone.advisor.demo.serializers import DeviceOwnerSerializer, ProvisioningSerializer


class BaseTestCase(object):
    result_serializer = None

    def setUp(self):
        self.client = Client(
            HTTP_USER_AGENT='com.yandex.phone.demo/1.0.qa2147483647 (Yandex Phone; Android 8.1)',
            HTTP_HOST='demo.localhost'
        )
        super(BaseTestCase, self).setUp()

    def test_ok(self):
        response = self.get()
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.content)

    def test_schema(self):
        response = self.get()
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.content)
        response_content = json.loads(response.content)
        serializer = self.result_serializer(data=response_content)
        self.assertTrue(serializer.is_valid(raise_exception=False))

    def test_missing_localization_values(self):
        saved_demo = localization_mock.localization_values['demo']
        # temporary remove all key/values from demo project
        localization_mock.localization_values['demo'] = {}
        try:
            self.test_schema()
        finally:
            localization_mock.localization_values['demo'] = saved_demo

    # noinspection PyUnusedLocal
    def get(self, *args, **kwargs):
        return self.client.get(path=self.endpoint, **kwargs)


class DemoProvisioningViewTest(BaseTestCase, TestCase):
    endpoint = '/api/v1/provisioning'
    result_serializer = ProvisioningSerializer


class DemoDeviceOwnerViewTest(BaseTestCase, TestCase):
    endpoint = '/api/v1/device_owner'
    result_serializer = DeviceOwnerSerializer
