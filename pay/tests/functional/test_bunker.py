import pytest
from paysys.sre.balance_notifier.bunker import bunker
from unittest import mock
import requests


class TestBunker:
    class TestApi:
        @pytest.fixture
        def env(self):
            return ''

        @pytest.fixture
        def node(self):
            return ''

        @pytest.fixture
        def bunker_url(self):
            return 'http://bunker-api-dot.yandex.net/v1/ls?node=/balance-notifier/test/&version=latest'

        @pytest.fixture
        def requests_get_mock(self, mocker):
            return mocker.patch('requests.get')

        @pytest.fixture
        def json_data(self):
            return [{'name': 'contract_notify', 'fullName': '/balance-notifier/test/contract_notify', 'version': 18,
                     'isDeleted': False,
                     'mime': 'application/json; charset=utf-8; schema="bunker:/balance-notifier/templates/contract_notify#"',
                     'saveDate': '2021-06-28T15:15:36.341Z'}]

        @pytest.fixture
        def side_effect(self, mocker):
            mocker.raise_for_status = mock.Mock()
            mocker.text = "some error"
            mocker.raise_for_status.side_effect = requests.HTTPError("some error")
            return mocker

        @pytest.mark.parametrize('env', ['test'])
        def test_api_url_by_env(self, env, bunker_url):
            result_host = bunker.BunkerApi.api_url_by_env(env)
            assert bunker_url == result_host

        @pytest.mark.parametrize('env', ['test'])
        @pytest.mark.parametrize('node', ['/balance-notifier/test/signed_notify'])
        def test_api_url_by_env_notify_name(self, env, node):
            test_url = 'http://bunker-api-dot.yandex.net/v1/cat?node=/balance-notifier/test/signed_notify&version=latest'
            result_url = bunker.BunkerApi.api_url_by_env_notify_name(env, node)
            assert test_url == result_url

        def test_call_api_ok(self, requests_get_mock, bunker_url, json_data):
            requests_get_mock.return_value(json_data)
            assert bunker.call_api(bunker_url)

        def test_call_api_error(self, requests_get_mock, bunker_url, side_effect):
            requests_get_mock.return_value = side_effect
            with pytest.raises(requests.HTTPError):
                assert bunker.call_api(bunker_url)

    class TestWriteFile:
        @pytest.fixture
        def open_mock(self):
            return mock.mock_open()

        def test_json_result_write_out_file(self, open_mock):
            with mock.patch('builtins.open', open_mock, create=True):
                bunker.json_result_write_out_file(["some_data"], "output.json")
                open_mock.assert_called_once_with("output.json", "a+")
                handle = open_mock()

                calls = [mock.call('[\n    "some_data"'),
                         mock.call('\n'),
                         mock.call(']')]

                assert handle.write.mock_calls == calls
