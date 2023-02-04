# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import httpretty
import urllib
import http.client as http
import hamcrest as hm

from muzzle.captcha import URL as CAPTCHA_URL, CHECKS_RATE, TYPE

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestCaseGenerateCaptcha(TestCaseApiAppBase):
    BASE_API = '/v1/common/generate-captcha'

    def test_ok(self):
        httpretty.register_uri(
            httpretty.GET,
            CAPTCHA_URL + '/generate',
            '''<?xml version="1.0"?><number url='u_r_l'>k_e_y</number>''',
            status=200,
        )
        res = self.test_client.get(self.BASE_API)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        last_request = httpretty.last_request()
        assert last_request.method == 'GET'
        path, qs = urllib.splitquery(last_request.path)
        assert path == '/generate'
        hm.assert_that(
            qs.split('&'),
            hm.contains_inanyorder(
                'type=%s' % TYPE,
                'http=on',
                'checks=%s' % CHECKS_RATE,
            ),
        )

        hm.assert_that(
            res.get_json()['data'],
            hm.has_entries({
                'url': 'u_r_l',
                'key': 'k_e_y',
                'checks': CHECKS_RATE,
            }),
        )

    def test_raise(self):
        httpretty.register_uri(
            httpretty.GET,
            CAPTCHA_URL + '/generate',
            status=502,
        )
        res = self.test_client.get(self.BASE_API)
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'CAPTCHA_API_UNAVAILABLE',
                'description': 'Captcha API is unavailable',
            }),
        )
