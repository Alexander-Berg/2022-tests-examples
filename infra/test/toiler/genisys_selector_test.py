import unittest

import requests
import msgpack
import mock

from lacmus2.toiler.genisys_selector import GenisysSelectorVtypeProcessor as GSVP

from . import exhaust


class MockResponse(object):
    def __init__(self, status_code, data):
        self.status_code = status_code
        self.content = msgpack.dumps(data, encoding='utf-8')

    def raise_for_status(self):
        if self.status_code == 200:
            return
        raise requests.HTTPError(str(self.status_code))


class GSVPTestCase(unittest.TestCase):
    def test(self):
        proc = GSVP(None)
        record = {'source': {'path': 'skynet.vrsn', 'rule': 'best rule'},
                  'meta': {'old': 'meta'}}

        with mock.patch('requests.get') as mock_get:
            mock_get.return_value = MockResponse(200, {'hosts': ['h1', 'h4']})
            value, log = exhaust(self, proc(None, record, forced=False), 4)

        self.assertEquals(value, (['h1', 'h4'], {'old': 'meta'}))

        mock_get.assert_called_once_with(
            'http://genisys.yandex-team.ru/v2/hosts-by-path-and-rulename',
            {'rulename': 'best rule', 'fmt': 'msgpack', 'path': 'skynet.vrsn'}
        )

        with mock.patch('requests.get') as mock_get:
            mock_get.return_value = MockResponse(200, {'hosts': None})
            value, log = exhaust(self, proc(None, record, forced=False), 4)

        self.assertEquals(value, (None, {'old': 'meta'}))
