# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future.builtins import str as text
from future import standard_library

standard_library.install_aliases()

import http.client as http
import responses
import mock
import hamcrest as hm

from brest.utils.config import get_config
from yb_snout_proxy.tests_unit.base import TestCaseProxyAppBase

HOST = get_config('SnoutProxy/SnoutApi')['ApiUrl'].rstrip('/')
URL = '/any-path'


class TestOptionsRequest(TestCaseProxyAppBase):
    @mock.patch('brest.utils.security.check_auth')
    @responses.activate
    def test_success(self, mock_check_auth):
        responses.add(
            responses.OPTIONS,
            HOST + URL,
            status=200,
            headers={'Allow': b'DELETE, OPTIONS', 'Origin': 'http://aaa.com'},
        )

        response = self.test_client.options(URL)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        # в options можно всем
        mock_check_auth.assert_not_called()
        hm.assert_that(responses.calls, hm.has_length(1))

        hm.assert_that(
            response.headers,
            hm.has_items(
                hm.contains('Origin', 'http://aaa.com'),
                hm.contains('Allow', b'DELETE, OPTIONS'),
                hm.contains('Access-Control-Request-Method', b'DELETE, OPTIONS'),
                hm.contains('Access-Control-Allow-Methods', b'DELETE, OPTIONS'),
                hm.contains('Access-Control-Allow-Headers', hm.contains_string('content-type')),
            ),
        )

    @mock.patch('brest.utils.security.check_auth')
    @responses.activate
    def test_not_found(self, _mock_check_auth):
        responses.add(
            responses.OPTIONS,
            HOST + URL,
            status=404,
            headers={'Allow': b'DELETE, OPTIONS', 'Origin': 'http://aaa.com'},
        )

        response = self.test_client.options(URL)
        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND))

        hm.assert_that(responses.calls, hm.has_length(1))
        hm.assert_that(
            response.headers,
            hm.has_items(
                hm.contains('Origin', 'http://aaa.com'),
                hm.contains('Allow', b'DELETE, OPTIONS'),
                hm.not_(hm.contains('Access-Control-Request-Method', b'DELETE, OPTIONS')),
                hm.not_(hm.contains('Access-Control-Allow-Headers', hm.contains_string('content-type'))),
            ),
        )
