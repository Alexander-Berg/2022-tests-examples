import pytest
import mock
import json
from django.test import Client

from smarttv.droideka.tests.mock import required_android_headers
from smarttv.droideka.tests.mock import MockTvmClient

client = Client(content_type='application.json')
mock_tvm_client = MockTvmClient()


@mock.patch('smarttv.droideka.proxy.tvm.tvm_client', mock.Mock(mock_tvm_client))
@mock.patch('smarttv.droideka.proxy.views.force_sync.ForceSyncView.load_memento_configs',
            mock.Mock(return_value=None))
class TestForceSync:

    @pytest.mark.parametrize('shared_pref_value, expected_result', [
        (None, {}),
        (123456789, {'programs': 123456789}),
    ])
    @mock.patch('smarttv.droideka.proxy.models.SharedPreferences.get_int')
    def test_ok(self, get_int_mock, shared_pref_value, expected_result):
        get_int_mock.return_value = shared_pref_value
        http_response = client.get(
            path='/api/v6/forcesync',
            **required_android_headers
        )

        actual_result = json.loads(http_response.content)

        assert expected_result == actual_result
