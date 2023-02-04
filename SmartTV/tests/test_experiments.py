import json
from mock import patch
from copy import copy

import pytest
import requests_mock
from django.test import Client
import mock

from smarttv.droideka.proxy import api
from smarttv.droideka.tests.mock import MockTvmClient

TEST_USER_AGENT = 'me.anyone.testing/2.5 (ANY DEVICE ANYONE; Android 7.1.1) TV'
TEST_BUILD_FINGERPRINT = 'Realtek/RealtekATV/RealtekATV:7.1.1/NMF26Q/586:userdebug/dev-keys'
TEST_USER_IP = '91.98.113.43'

client = Client(content_type='application.json')
mock_tvm_client = MockTvmClient()


def remove_param(data, param_to_remove):
    data = copy(data)
    del data[param_to_remove]
    return data


@mock.patch('smarttv.droideka.proxy.tvm.tvm_client', mock.Mock(mock_tvm_client))
@mock.patch('smarttv.droideka.proxy.views.experiments.ExperimentsView.load_memento_configs',
                mock.Mock(return_value=None))
class TestExperimentsView:
    MOCK_USAAS_URL = 'http://test.tst'
    endpoint = '/api/v6/experiments'

    HEADERS = {
        'HTTP_USER_AGENT': TEST_USER_AGENT,
        'HTTP_BUILD_FINGERPRINT': TEST_BUILD_FINGERPRINT,
        'HTTP_X_YADEVICEID': 'ca9b68da30474d5db0bbd1e8a25565bb',
        'HTTP_X_ETHERNET_MAC': '00:11:22:33:44:55',
    }

    mock_usaas_client = api.usaas.USaaSApi(
        url=MOCK_USAAS_URL,
        timeout=1,
        retries=1,
    )

    MOCK_USAAS_DATA = '''
    {
        "all": {
            "CONTEXT": {
                "MAIN": {
                    "SMARTTV-BACK": {
                        "recommendations": true,
                        "load": 100500
                    },
                    "SMARTTV-HOMEAPP": {
                        "show_good": true,
                        "show_bad": false
                    }
                }
            }
        },
        "exp_boxes": "252749,0,-1;252719,0,-1"
    }
    '''

    @pytest.mark.parametrize('missing_header', HEADERS)
    def test_skip_required_param_400_returned(self, missing_header):
        headers = remove_param(self.HEADERS, missing_header)
        response = client.get(path=self.endpoint, **headers)
        assert response.status_code == 400

    @pytest.mark.django_db
    @patch('smarttv.droideka.proxy.api.usaas.client', mock_usaas_client)
    def test_ok_with_correct_data(self):
        with requests_mock.Mocker(real_http=True) as m:
            m.register_uri(
                method='GET',
                url=self.MOCK_USAAS_URL,
                text=self.MOCK_USAAS_DATA,
                status_code=200
            )
            response = client.get(path=self.endpoint, **self.HEADERS)
            data = json.loads(response.content)
            assert response.status_code == 200, response.content
            assert 'experiments' in data
            assert 'log_data' in data
            assert data['experiments'] == [
                {'key': 'show_good', 'value': True, 'handler': 'SMARTTV-HOMEAPP'},
                {'key': 'show_bad', 'value': False, 'handler': 'SMARTTV-HOMEAPP'},
            ]
            assert json.loads(data['log_data']) == {
                "exp_boxes": "252749,0,-1;252719,0,-1",
                "device_icookie": "3104721941003451528",
                "experiments_icookie": "3104721941003451528",
            }

    @pytest.mark.django_db
    @patch('smarttv.droideka.proxy.api.usaas.client', mock_usaas_client)
    def test_different_icookies_1_3(self):
        with requests_mock.Mocker(real_http=True) as m:
            m.register_uri(
                method='GET',
                url=self.MOCK_USAAS_URL,
                text=self.MOCK_USAAS_DATA,
                status_code=200
            )

            headers = self.HEADERS.copy()
            headers['HTTP_USER_AGENT'] = 'me.anyone.testing/1.3 (ANY DEVICE ANYONE; Android 7.1.1) TV'
            response = client.get(path=self.endpoint, **headers)
            data = json.loads(response.content)
            assert response.status_code == 200, response.content
            assert 'log_data' in data
            assert json.loads(data['log_data']) == {
                "exp_boxes": "252749,0,-1;252719,0,-1",
                "device_icookie": "3104721941003451528",
                "experiments_icookie": "1545665550632380427",
            }
