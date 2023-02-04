# -*- coding: utf-8 -*-
import unittest

import mock
from tests import object_builder as ob
from requests import exceptions

from muzzle.abt.usaas import request_usaas


class MockResponse:

    def __init__(self, status_code, headers):
        self.status_code = status_code
        self.headers = headers
        self.content = "content"

    def raise_for_status(self):
        if self.status_code >= 400:
            raise exceptions.HTTPError('Invalid response status')


# заголовок с флагами содержит base64-кодированную строку JSON:
# [{"HANDLER":"BALANCE","CONTEXT":{"MAIN":{"BALANCE":{"balance_new_ui":"true"}}},"TESTID":["310947"]}]
usaas_headers_with_one_exp = {'X-Yandex-ExpFlags':
                                  'W3siSEFORExFUiI6IkJBTEFOQ0UiLCJDT05URVhUIjp7Ik1BSU4iOnsiQkFMQU5DRSI6eyJiYWxhbmNlX25ld191aSI6InRydWUifX19LCJURVNUSUQiOlsiMzEwOTQ3Il19XQ==',
                              'X-Yandex-ExpConfigVersion': '1',
                              'X-Yandex-ExpBoxes': 'boxes',
                              'X-Yandex-ExpBoxes-Crypted': 'boxes crypted'}

# заголовок с флагами содержит base64-кодированную строку JSON:
# [{"HANDLER":"BALANCE","CONTEXT":{"MAIN":{"BALANCE":{"balance_admin_new_ui":true}}},"TESTID":["325209"]}],
# [{"HANDLER":"BALANCE","CONTEXT":{"MAIN":{"BALANCE":{"balance_user_paystep":true}}},"TESTID":["390421"]}]
usaas_headers_with_two_exp = {'X-Yandex-ExpFlags':
                                  'W3siSEFORExFUiI6IkJBTEFOQ0UiLCJDT05URVhUIjp7Ik1BSU4iOnsiQkFMQU5DRSI6eyJiYWxhbmNlX2FkbWluX25ld191aSI6dHJ1ZX19fSwiVEVTVElEIjpbIjMyNTIwOSJdfV0=,W3siSEFORExFUiI6IkJBTEFOQ0UiLCJDT05URVhUIjp7Ik1BSU4iOnsiQkFMQU5DRSI6eyJiYWxhbmNlX3VzZXJfcGF5c3RlcCI6dHJ1ZX19fSwiVEVTVElEIjpbIjM5MDQyMSJdfV0=',
                              'X-Yandex-ExpConfigVersion': '1',
                              'X-Yandex-ExpBoxes': 'boxes',
                              'X-Yandex-ExpBoxes-Crypted': 'boxes crypted'}

usaas_invalid_headers = {'X-Yandex-ExpFlags':
                             'W3INVALIDDATAsiSEFORExFUiI6IkJBTEFOQ0UiLCJDT05URVhUIjp7Ik1BSU4iOnsiQkFMQU5DRSI6eyJiYWxhbmNlX25ld191aSI6InRydWUifX19LCJURVNUSUQiOlsiMzEwOTQ3Il19XQ==',
                         'X-Yandex-ExpConfigVersion': '1',
                         'X-Yandex-ExpBoxes': 'boxes',
                         'X-Yandex-ExpBoxes-Crypted': 'boxes crypted'}

cookies = {
    'Session_id': str(ob.get_big_number()),
    'sessionid2': str(ob.get_big_number()),
    'yandexuid': str(ob.get_big_number())
}

user_ip = '95.108.172.0'
host = 'balance.yandex.ru'
user_agent = 'Mozilla/5.0'


class TestAbtUsass(unittest.TestCase):
    def test_request_usaas_without_client_features(self):
        with mock.patch('requests.Session') as session_mock:
            session_mock.return_value = mock.MagicMock(
                get=mock.MagicMock(return_value=MockResponse(200, usaas_headers_with_one_exp))
            )

            usaas_response = request_usaas(cookies, user_ip, host, user_agent, {})

            assert not session_mock.return_value.headers.has_key('X-Yandex-ExpClientFeatures-Int')
            assert not session_mock.return_value.headers.has_key('X-Yandex-ExpClientFeatures-Str')

            assert usaas_response.boxes_crypted == usaas_headers_with_one_exp['X-Yandex-ExpBoxes-Crypted']
            assert usaas_response.boxes == usaas_headers_with_one_exp['X-Yandex-ExpBoxes']
            assert usaas_response.version == usaas_headers_with_one_exp['X-Yandex-ExpConfigVersion']
            assert usaas_response.flags['balance_new_ui'] == 'true'
            assert usaas_response.test_ids == ['310947']

    def test_request_usaas_with_client_features(self):
        with mock.patch('requests.Session') as session_mock:
            session_mock.return_value = mock.MagicMock(
                get=mock.MagicMock(return_value=MockResponse(200, usaas_headers_with_two_exp))
            )

            usaas_response = request_usaas(cookies, user_ip, host, user_agent, {
                'flag_1': 1,
                'flag_2': 'abcd',
                'flag_3': 2,
                'flag_4': 'dcef',
            })

            assert session_mock.return_value.headers['X-Yandex-ExpClientFeatures-Int'] == 'flag_1=1;flag_3=2'
            assert session_mock.return_value.headers['X-Yandex-ExpClientFeatures-Str'] == 'flag_2=YWJjZA==;flag_4=ZGNlZg=='

            assert usaas_response.boxes_crypted == usaas_headers_with_two_exp['X-Yandex-ExpBoxes-Crypted']
            assert usaas_response.boxes == usaas_headers_with_two_exp['X-Yandex-ExpBoxes']
            assert usaas_response.version == usaas_headers_with_two_exp['X-Yandex-ExpConfigVersion']
            assert usaas_response.flags['balance_admin_new_ui']
            assert usaas_response.flags['balance_user_paystep']
            assert usaas_response.test_ids == ['325209', '390421']

    def test_request_usaas_invalid_data(self):
        with mock.patch('requests.Session') as session_mock:
            session_mock.return_value = mock.MagicMock(
                get=mock.MagicMock(return_value=MockResponse(200, usaas_invalid_headers)))

            with self.assertRaises(TypeError):
                request_usaas(cookies, user_ip, host, user_agent, {})

    def test_request_usaas_invalid_status(self):
        with mock.patch('requests.Session') as session_mock:
            session_mock.return_value = mock.MagicMock(get=mock.MagicMock(return_value=MockResponse(500, {})))

            with self.assertRaises(exceptions.HTTPError):
                request_usaas(cookies, user_ip, host, user_agent, {})
