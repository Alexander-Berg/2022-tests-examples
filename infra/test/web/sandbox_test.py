import unittest

import mock
import requests

from genisys.web import app
from genisys.web import sandbox


class MockResponse(object):
    headers = {'content-type': 'application/json; charset=utf8'}

    def __init__(self, status_code, json):
        self.status_code = status_code
        self.json = lambda: json

    def raise_for_status(self):
        if self.status_code == 200:
            return
        raise requests.HTTPError(str(self.status_code))


class BaseSansboxTest(unittest.TestCase):
    def setUp(self):
        super(BaseSansboxTest, self).setUp()
        import os
        os.environ['GENISYS_WEB_CONFIG'] = '../../test/web/config.py'
        self.app = app.make_app()
        self.ctx = self.app.test_request_context()
        self.ctx.__enter__()

    def tearDown(self):
        self.ctx.__exit__(None, None, None)
        super(BaseSansboxTest, self).tearDown()


class GetResourceTypeDescriptionTestCase(BaseSansboxTest):
    def test_success(self):
        description = "<div class=\"document\">\n<p>skynet.bin " \
                      "installation binary</p>\n</div>\n"
        with mock.patch('requests.get') as mock_get:
            mock_get.return_value = MockResponse(200, [
                {"type": "SKYNET_BINARY",
                 "description": description}
            ])
            td = sandbox.get_resource_type_description('SKYNET_BINARY')
            self.assertEquals(td, description)
            mock_get.assert_called_once_with(
                'https://sandbox.yandex-team.ru/api/v1.0/suggest/resource',
                {'type': 'SKYNET_BINARY'},
                timeout=10,
            )

    def test_not_found(self):
        errmsg = "No resource type 'SKYNET_BINARY2' found."
        with mock.patch('requests.get') as mock_get:
            mock_get.return_value = MockResponse(400, {"reason": errmsg})
            with self.assertRaises(sandbox.SandboxError) as ae:
                sandbox.get_resource_type_description('SKYNET_BINARY2')
        self.assertEquals(str(ae.exception), errmsg)

    def test_500(self):
        errmsg = 'HTTPError: 500'
        with mock.patch('requests.get') as mock_get:
            mock_get.return_value = MockResponse(500, None)
            with self.assertRaises(sandbox.SandboxError) as ae:
                sandbox.get_resource_type_description('SKYNET_BINARY')
        self.assertEquals(str(ae.exception), errmsg)


class GetResourceInfoTestCase(BaseSansboxTest):
    def test_success(self):
        info = {
            "skynet_id": "rbtorrent:1478423bab725b38ee68397a9c1d20cee88bce2e",
            "rsync": {
              "links": [
                "rsync://sandbox-storage8.search.yandex.net/sandbox-tasks/6/9/39211596/dist/skynet.bin",
                "rsync://sandbox-storage11.search.yandex.net/sandbox-tasks/6/9/39211596/dist/skynet.bin",
                "rsync://lucid.build.skydev.search.yandex.net/sandbox-tasks/6/9/39211596/dist/skynet.bin"
              ]
            },
            "task": {
              "url": "https://sandbox.yandex-team.ru/api/v1.0/task/39211596",
              "status": "RELEASED",
              "id": 39211596
            },
            "http": {
              "proxy": "http://proxy.sandbox.yandex-team.ru/79690886",
              "links": [
                "http://sandbox-storage8.search.yandex.net:13578/6/9/39211596/dist/skynet.bin",
                "http://sandbox-storage11.search.yandex.net:13578/6/9/39211596/dist/skynet.bin",
                "http://lucid.build.skydev.search.yandex.net:13578/6/9/39211596/dist/skynet.bin"
              ]
            },
            "description": "skynet.bin (14.5.20 (tc:1897))",
            "rights": "write",
            "url": "https://sandbox.yandex-team.ru/api/v1.0/resource/79690886",
            "type": "SKYNET_BINARY",
            "file_name": "dist/skynet.bin",
            "sources": [
              "zeleniy_krocodil",
              "sandbox-storage8",
              "sandbox-storage11"
            ],
            "state": "READY",
            "time": {
              "accessed": "2015-09-01T17:36:07.768000Z",
              "created": "2015-09-01T14:03:38Z"
            },
            "owner": "SKYNET",
            "attributes": {
              "mds": "25154/79690886.tar.gz",
              "released": "stable",
              "version": "14.5.20",
              "backup_task": 39214474,
              "ttl": "inf"
            },
            "md5": "0387edc48ad1d3eef545da81fbba2c6d",
            "arch": "any",
            "id": 79690886,
            "size": 142797824
        }
        with mock.patch('requests.get') as mock_get:
            mock_get.return_value = MockResponse(200, info)
            actual = sandbox.get_resource_info(79690886)
            self.assertEquals(actual, info)
            mock_get.assert_called_once_with(
                'https://sandbox.yandex-team.ru/api/v1.0/resource/79690886',
                timeout=10,
            )

    def test_not_found(self):
        errmsg = 'HTTPError: 404'
        with mock.patch('requests.get') as mock_get:
            mock_get.return_value = MockResponse(404, None)
            with self.assertRaises(sandbox.SandboxError) as ae:
                sandbox.get_resource_info(79690886)
        self.assertEquals(str(ae.exception), errmsg)
