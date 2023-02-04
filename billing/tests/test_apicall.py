import json
from requests import Response
from unittest import mock

import pytest

from billing.dwh.tools.apicall.apicall import (
    main,
    ApiClient,
    ApiError,
    TvmCredentials,
)


class TestApiCall:
    """Минимальный тест с имитаторами твм и тестового стенда."""

    @pytest.fixture(autouse=True)
    def setup(self):
        self.prefix = 'billing.dwh.tools.apicall.apicall'
        self.args = [
            '--tvm-id=123',
            '--tvm-secret=xxx',
            '--hint=me',
            '--in-testing',
            '--operation-uid=5a7103f6-3c87-4d9c-b9c6-be805c949385'
        ]

    @pytest.fixture
    def patch_tvm_client(self):
        class MockClient:

            def __init__(self, *args, **kwargs):
                pass

            def get_service_ticket_for(self, *args, **kwargs):
                return 'zzz'

            def stop(self):
                pass

        with mock.patch(f'{self.prefix}.TvmClient', new=MockClient):
            yield

    @pytest.fixture
    def patch_api_client(self):
        counter = 0

        def mock_publish(_self, task_data, operation_uid):
            nonlocal counter
            counter += 1

            assert task_data['meta']['hint'] == 'me'
            assert operation_uid
            return counter

        def mock_request(_self, _url):
            return {'data': {'status': 'finished', 'url': '/here/'}}

        with (mock.patch(f'{self.prefix}.ApiClient.publish', new=mock_publish),
              mock.patch(f'{self.prefix}.ApiClient._request', new=mock_request)):
            yield

    @pytest.fixture(autouse=True)
    def patch_all(self, setup, patch_api_client, patch_tvm_client):
        pass

    def test(self):
        with open('in.json', 'w') as f:
            f.write('{"meta": {"task_name": "echo"}, "params": {"a": 10}}')

        result = main(self.args)
        assert result.response == {'data': {'status': 'finished', 'url': '/here/'}}

        with open('wait_result.json') as f:
            out = json.load(f)

        assert out == result._asdict()

        # проверка обработки 404
        def mock_request(_self, _url):
            response = Response()
            response.status_code = 404
            raise ApiError('dummy', response=response)

        client = ApiClient(tvm_credentials=TvmCredentials(1, 'xxx'))
        client.details_404_sleep = 0
        ApiClient._request = mock_request
        raised = False

        try:
            client.wait(work_id=-1000)
        except ApiError:
            raised = True

        assert raised, '404 exception was not raised'
