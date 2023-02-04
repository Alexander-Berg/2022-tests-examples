# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future.builtins import str as text
from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
import responses
import mock
import hamcrest as hm

from brest.utils.config import get_config
from yb_snout_proxy.tests_unit.base import TestCaseProxyAppBase

HOST = get_config('SnoutProxy/SnoutApi')['ApiUrl'].rstrip('/')
URL = '/v1/version'


class TestCookies(TestCaseProxyAppBase):
    auth_data = {
        'tvm_user_ticket': 'user ticket',
        'tvm_service_ticket': 'service ticket',
        'passport_id': 1234,
    }

    @pytest.mark.parametrize(
        'cookie_in, cookie_out',
        [
            (('balance-cookie', 'Highway to Hell'), ('balance-cookie', 'Highway to Hell')),
            ((b'balance-cookie', b'Highway to Hell'), ('balance-cookie', 'Highway to Hell')),
            (('balance-cookie', 'Дорога в Ад'), ('balance-cookie', 'Дорога в Ад'.encode('utf-8'))),
            (('balance-cookie', b'\xd0\x94\xd0\xbe\xd1\x80\xd0\xbe\xd0\xb3\xd0\xb0 \xd0\xb2 \xd0\x90\xd0\xb4'), ('balance-cookie', 'Дорога в Ад'.encode('utf-8'))),
            (('balance-cookie', '♥O◘♦♥O◘♦'), ('balance-cookie', '♥O◘♦♥O◘♦'.encode('utf-8'))),
            (('Гром', 'test'), None),
            ((b'Гром', 'test'), None),
            (('Гром'.encode('utf-8'), 'test'), None),
            (('testÐ', 'test'), None),
            ((b'testÐ', 'test'), None),
            (('testÐ'.encode('utf-8'), 'test'), None),
        ],
    )
    @mock.patch('brest.utils.security.check_auth', return_value=auth_data)
    @responses.activate
    def test_request_headers(self, _mock_check_auth, cookie_in, cookie_out):
        responses.add(
            responses.GET,
            HOST + URL,
            json={'version': '2.12.12'},
            status=200,
        )
        self.test_client.set_cookie('', *cookie_in)
        with mock.patch('brest.utils.EnvType._current', b'production'):
            response = self.test_client.get(URL)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        hm.assert_that(responses.calls, hm.has_length(1))
        request_cookies = responses.calls[0].request._cookies.items()
        cookies_match = hm.has_item(hm.contains(*cookie_out)) if cookie_out else hm.empty()
        hm.assert_that(request_cookies, cookies_match)
