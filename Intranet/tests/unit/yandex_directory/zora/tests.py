# coding: utf-8

from testutils import (
    TestCase,
    mocked_requests,
    assert_called_once,
)

from intranet.yandex_directory.src.yandex_directory.zora.client import ZoraClient
from intranet.yandex_directory.src.yandex_directory.zora.exceptions import (
    ZoraInternalCodeError,
)

from intranet.yandex_directory.src.yandex_directory.auth import tvm


class TestZoraClient(TestCase):
    def setUp(self):
        super(TestZoraClient, self).setUp()
        tvm.tickets['gozora'] = 'ticket-2000193'

    def test_proxy_request_correct(self):
        with mocked_requests() as requests:
            requests.get.return_value.status_code = 200
            requests.get.return_value.text = 'hello'
            response = ZoraClient().get('https://yandex.ru')
            self.assertEqual(response.text, 'hello')
            assert_called_once(
                requests.get,
                url='https://yandex.ru',
                headers={},
                proxies={
                    'http': 'http://connect:ticket-2000193@go.zora.yandex.net:1080',
                    'https': 'http://connect:ticket-2000193@go.zora.yandex.net:1080',
                },
                verify=False,
            )

    def test_raise_exception_on_code(self):
        with mocked_requests() as requests:
            requests.get.return_value.status_code = 500
            requests.get.return_value.headers = {
                'X-Yandex-GoZora-Error-Code': '1009'
            }
            with self.assertRaises(ZoraInternalCodeError):
                ZoraClient().get('https://yandex.ru')
