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
from urllib.parse import urlparse
import hamcrest as hm

from brest.utils.config import get_config
from yb_snout_proxy.tests_unit.base import TestCaseProxyAppBase
from yb_snout_proxy.servant import _is_admin_host

HOST = get_config('SnoutProxy/SnoutApi')['ApiUrl'].rstrip('/')
URL = '/v1/version'


class TestHeaders(TestCaseProxyAppBase):
    host = urlparse(HOST).hostname
    auth_data = {
        'tvm_user_ticket': 'user ticket',
        'tvm_service_ticket': 'service ticket',
        'passport_id': 1234,
    }
    snout_host = 'snout.balance.yandex.ru'

    @pytest.mark.parametrize(
        'referer, answer',
        [
            ('https://admin.balance.yandex.ru/v1/test', True),
            ('http://admin.balance.yandex.ru', True),
            ('https://user.balance.yandex.ru/test', False),
            ('http://user.balance.yandex.ru', False),
            (
                # реальный пример
                'https://admin-balance.greed-tm.paysys.yandex.ru/invoices.xml?date_type=1'
                '&invoice_eid=Ð-1859798709-1&payment_status=0&post_pay_type=0&trouble_type=0'
                '&client_id=2432042&pn=1&ps=20&sf=invoice_dt&so=1',
                True,
            ),
            (b'http://\xd1\x8e\xd0\xb7\xd0\xb5\xd1\x80.balance.yandex.ru', False),
            ('', False),
            (None, False),
        ],
    )
    @mock.patch('brest.utils.security.check_auth', return_value=auth_data)
    @responses.activate
    def test_request_headers(self, _mock_check_auth, referer, answer):
        responses.add(
            responses.GET,
            HOST + URL,
            json={'version': '2.12.12'},
            status=200,
        )
        with mock.patch('brest.utils.EnvType._current', b'production'):
            response = self.test_client.get(URL, headers={'Referer': referer})
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'Response code must be OK')

        hm.assert_that(responses.calls, hm.has_length(1))
        request_headers = responses.calls[0].request.headers
        hm.assert_that(
            request_headers,
            hm.has_entries({
                'Connection': 'keep-alive',
                'Accept-Encoding': 'gzip, deflate',
                'Accept': '*/*',
                'Host': self.host,
                'X-Forwarded-Host': self.host,
                'X-Ya-Passport-Id': text(self.auth_data.get('passport_id')),
                'X-Ya-User-Ticket': self.auth_data.get('tvm_user_ticket'),
                'X-Ya-Service-Ticket': self.auth_data.get('tvm_service_ticket'),
                'X-Is-Admin': text(answer),
            }),
        )

    @responses.activate
    def test_filtering_headers(self):
        responses.add(
            responses.GET,
            HOST + URL,
            headers={'Fake-Response-Header': b'fake', 'Set-Cookie': b'cookie'},
            json={'version': '2.12.12'},
            status=200,
        )
        response = self.test_client.get(
            URL,
            headers={'X-Fields': b'id,hm.not_id', 'Fake-Request-Header': b'fake'},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'Response code must be OK')

        hm.assert_that(responses.calls, hm.has_length(1))
        request_headers = responses.calls[0].request.headers
        assert 'X-Fields' in request_headers
        assert 'Fake-Request-Header' not in request_headers

        response_headers = response.headers
        assert 'Set-Cookie' in response_headers
        assert 'Fake-Response-Header' not in response_headers

    @pytest.mark.parametrize(
        'referer, answer',
        [
            ('admin.balance.greed-tm.paysys.yandex.ru', True),
            ('admin.balance.greed-ts.paysys.yandex.ru', True),
            ('admin.balance.greed-tm.paysys.yandex-team.ru', True),
            ('admin.balance.greed-ts.paysys.yandex-team.ru', True),
            ('admin.balance-pt.yandex.ru', True),
            ('admin.balance-pt.yandex-team.ru', True),
            ('admin.balance.greed-pt.paysys.yandex.ru', True),
            ('admin.balance.greed-pt.paysys.yandex-team.ru', True),
            ('admin.balance.greed-load.paysys.yandex.ru', True),
            ('admin.balance.greed-load.paysys.yandex-team.ru', True),
            ('admin.balance.greed-load1.paysys.yandex.ru', True),
            ('admin.balance.greed-load1.paysys.yandex-team.ru', True),
            ('admin.balance.greed-load2.paysys.yandex.ru', True),
            ('admin.balance.greed-load2.paysys.yandex-team.ru', True),
            ('admin-balance.greed-dev.paysys.yandex.ru', True),
            ('admin-balance.greed-dev.paysys.yandex-team.ru', True),
            ('admin-balance.greed-tm.paysys.yandex.ru', True),
            ('admin-balance.greed-tm.paysys.yandex-team.ru', True),
            ('admin-balance.greed-ts.paysys.yandex.ru', True),
            ('admin-balance.greed-ts.paysys.yandex-team.ru', True),
            ('admin-balance.greed-load.paysys.yandex.ru', True),
            ('admin-balance.greed-load.paysys.yandex-team.ru', True),
            ('admin-balance.greed-load1.paysys.yandex.ru', True),
            ('admin-balance.greed-load1.paysys.yandex-team.ru', True),
            ('admin-balance.greed-load2.paysys.yandex.ru', True),
            ('admin-balance.greed-load2.paysys.yandex-team.ru', True),
            ('admin-balance.greed-pt.paysys.yandex.ru', True),
            ('admin-balance.greed-pt.paysys.yandex-team.ru', True),
            ('admin.balance.yandex.ru', True),
            ('admin.balance.yandex-team.ru', True),
            ('admin-balance-123456.greed-branch.paysys.yandex.ru', True),
            ('user-balance-123456.greed-branch.paysys.yandex.ru', False),
            ('admin-eda-selfemployed.greed-branch.paysys.yandex.ru', True),
            ('user.balance.yandex.ru', False),
            ('snout.balance.yandex.ru', False),
            ('user.admin.balance.yandex.ru', False),
            ('', False),
            (None, False),
        ],
    )
    def test_is_admin_func(self, referer, answer):
        hm.assert_that(
            _is_admin_host(referer),
            hm.equal_to(answer),
        )

    @pytest.mark.parametrize('is_admin_header', [True, False])
    @pytest.mark.parametrize('env_type', [b'production', b'testing'])
    @responses.activate
    @mock.patch('yb_snout_proxy.servant._is_admin_host', return_value='calculated')
    def test_is_admin_header(self, _mocked_get_is_admin, is_admin_header, env_type):
        responses.add(
            responses.GET,
            HOST + URL,
            json={'version': '2.12.12'},
            status=200,
        )

        with mock.patch('brest.utils.EnvType._current', env_type):
            self.test_client.get(URL, headers={'X-Is-Admin': is_admin_header}, is_admin=False)

        res_header = 'calculated' if env_type == b'production' else text(is_admin_header)
        request_headers = responses.calls[0].request.headers
        hm.assert_that(request_headers, hm.has_entries({'X-Is-Admin': res_header}))

    @responses.activate
    def test_redirect_header(self):
        redirect_url = b'/internal-proxy/?path=mds.com/picture.png'
        responses.add(
            responses.GET,
            HOST + URL,
            headers={'X-Snout-Accel-Redirect': redirect_url},
            json={'version': '2.12.12'},
            status=200,
        )
        res = self.test_client.get(URL)

        response_headers = dict(res.headers.items())
        assert 'X-Snout-Accel-Redirect' not in response_headers
        hm.assert_that(
            response_headers,
            hm.has_entry('X-Accel-Redirect', redirect_url),
        )
