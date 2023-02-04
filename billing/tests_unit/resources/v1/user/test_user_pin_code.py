# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import re
import mock
import json
import http.client as http
import hamcrest as hm
import responses
import urlparse

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase


class MockTvmClient(object):
    def get_service_ticket_for(self, _alias):
        return b'service-ticket'


@mock.patch('balance.tvm.get_or_create_tvm_client', return_value=MockTvmClient())
class TestCaseUserPincode(TestCaseApiAppBase):
    BASE_URL = '/v1/user/pin-code'

    cookies = {
        b'Session_id': b'12456',
        b'yandexuid': b'666',
        b'login': b'kamchatochka',
        b'extra_cookie': b'abc',
    }
    headers = {
        'X-Forwarded-Host': 'snout-host.ru',
        'X-Real-IP': '1-2-3-4-5',
    }

    def _create_response(self, data, status_code=http.OK):
        def request_callback(request):
            hm.assert_that(
                request.headers,
                hm.has_entries({
                    'Ya-Consumer-Client-Ip': '1-2-3-4-5',
                    'Ya-Client-Host': 'snout-host.ru',
                    'X-Ya-Service-Ticket': 'service-ticket',
                }),
            )
            hm.assert_that(
                request.headers.get('Ya-Client-Cookie', '').split('; '),
                hm.contains_inanyorder(
                    'Session_id=12456',
                    'yandexuid=666',
                    'login=kamchatochka',
                ),
            )
            url = urlparse.urlparse(request.url)
            hm.assert_that(
                url.path,
                hm.equal_to('/1/bundle/support_code/create/'),
            )
            hm.assert_that(
                url.query.split('&'),
                hm.contains_inanyorder(
                    hm.equal_to('consumer=balance-test'),
                    hm.contains_string('uid='),
                ),
            )
            return (status_code, {}, json.dumps(data))

        for key, val in self.cookies.items():
            self.test_client.set_cookie(b'', key, val)

        responses.add_callback(
            responses.POST,
            re.compile('.+/1/bundle/support_code/create/'),
            callback=request_callback,
            content_type='application/json',
        )

    @responses.activate
    def test_ok(self, _mock_tvm):
        self._create_response({
            'status': 'ok',
            'support_code': '666',
            'expires_at': 1,
        })

        security.set_roles([])
        res = self.test_client.get(
            self.BASE_URL,
            is_admin=False,
            headers=self.headers,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json().get('data', {}),
            hm.has_entries({
                'support_code': '666',
                'expires_at': 1,
            }),
        )

    @responses.activate
    def test_405(self, _mock_tvm):
        self._create_response({}, http.METHOD_NOT_ALLOWED)

        security.set_roles([])
        res = self.test_client.get(
            self.BASE_URL,
            is_admin=False,
            headers=self.headers,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PASSPORT_API_INVALID_RESPONSE',
                'description': 'Can\'t get a pin code. Response from Passport: status_code=405, data=',
            }),
        )

    @responses.activate
    def test_forbidden_for_intranet(self, _mock_tvm):
        headers = self.headers.copy()
        headers.update({'X-Forwarded-Host': 'snout.yandex-team.ru'})

        security.set_roles([])
        res = self.test_client.get(
            self.BASE_URL,
            is_admin=False,
            headers=headers,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'INTRANET_ACCESS_FORBIDDEN',
                'description': 'Access for yandex-team is prohibited',
            }),
        )

    @responses.activate
    @mock.patch('butils.application.plugins.components_cfg.get_component_cfg', return_value={})
    def test_no_passport_url(self, _mock_tvm, _mock_get_component):
        security.set_roles([])
        res = self.test_client.get(
            self.BASE_URL,
            is_admin=False,
            headers=self.headers,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'REQUIRE_PASSPORT_URL',
                'description': 'Passport api url is not defined',
            }),
        )

    @responses.activate
    def test_internal_passport_error(self, _mock_tvm):
        error = {'status': 'error', 'errors': ['internal.error']}
        self._create_response(error)

        security.set_roles([])
        res = self.test_client.get(
            self.BASE_URL,
            is_admin=False,
            headers=self.headers,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PASSPORT_API_INVALID_RESPONSE',
                'description': 'Can\'t get a pin code. Response from Passport: status_code=200, data=%s' % error,
            }),
        )

    @responses.activate
    def test_forbidden_from_passport(self, _mock_tvm):
        error = {'status': 'error', 'errors': ['sessionid.invalid']}
        self._create_response(error)

        security.set_roles([])
        res = self.test_client.get(
            self.BASE_URL,
            is_admin=False,
            headers=self.headers,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PASSPORT_API_FORBIDDEN',
                'description': 'Passport (%s) is forbidden or not authorised' % self.test_session.oper_id,
            }),
        )
