# -*- coding: utf-8 -*-
import json
import mock
import requests_mock
from django.test import TestCase, Client as DjangoClient

from yaphone.advisor.advisor.jafar import Jafar
from yaphone.advisor.advisor.models.client import Client
from yaphone.advisor.advisor.tests.views.device import AndroidClientInfoV2Test


@mock.patch('yaphone.advisor.common.updates_manager.UpdatesManager._reinit_required',
            lambda self: True)  # force cache reinit
class InitializationOrderTest(TestCase):
    android_client_info_data = AndroidClientInfoV2Test.default_data
    uuid = AndroidClientInfoV2Test.uuid

    def setUp(self):
        self.jafar_mock_adapter = requests_mock.Adapter()
        Jafar.http.mount('mock', self.jafar_mock_adapter)
        self.client = DjangoClient(
            HTTP_USER_AGENT='com.yandex.launcher/2.00.qa2147483647 (Yandex Nexus 9; Android 9.2.1beta)',
            HTTP_X_YAUUID=self.uuid,
            HTTP_X_YACLID1=self.android_client_info_data['clids']['clid1'],
            HTTP_ACCEPT_LANGUAGE='ru_RU',
            HTTP_HOST='launcher',
        )

    def tearDown(self):
        Client.objects.delete()

    def assertGetStatus(self, path, params, status_code):
        response = self.client.get(path=path, data=params)
        self.assertEqual(response.status_code, status_code, response.content)

    def assertPostStatus(self, path, data, status_code):
        response = self.client.post(path=path, data=json.dumps(data), content_type='application/json')
        self.assertEqual(response.status_code, status_code, response.content)

    def test_android_client_info_first(self):
        self.assertPostStatus('/api/v2/android_client_info', self.android_client_info_data, 200)
        self.assertGetStatus('/api/v2/experiments', {}, 200)

    def test_experiments_first(self):
        self.assertGetStatus('/api/v2/experiments', {}, 200)
        self.assertPostStatus('/api/v2/android_client_info', self.android_client_info_data, 200)

    def test_client_is_implicitly_saved(self):
        self.assertGetStatus('/api/v2/experiments', {}, 200)
        self.assertEqual(Client.objects.count(), 1)
