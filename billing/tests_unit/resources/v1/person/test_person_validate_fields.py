# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import json
import httpretty
import http.client as http
import hamcrest as hm

from yb_snout_api.tests_unit.base import TestCaseApiAppBase

from yb_snout_api.tests_unit.fixtures.common import create_bank_int


SWIFT_API = 'https://refs-test.paysys.yandex.net/api/swift'


class TestCaseValidateSwift(TestCaseApiAppBase):
    BASE_API = u'/v1/person/validate-swift'
    swift = '98765432109'

    def test_valid_swift(self):
        create_bank_int(bicint=self.swift)

        res = self.test_client.get(self.BASE_API, {'value': self.swift})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(res.get_json()['data'], hm.has_entries({'valid': True, 'swift': self.swift}))

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_wo_bank(self):
        httpretty.register_uri(
            httpretty.POST,
            SWIFT_API,
            json.dumps({'data': {'bics': []}}),
            status=200,
        )

        res = self.test_client.get(self.BASE_API, {'value': self.swift})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json()['data'],
            hm.has_entries({
                'valid': False,
                'swift': self.swift,
                'error': 'Bank with BIC %s not found in DB' % self.swift,
            }),
        )

    @pytest.mark.parametrize(
        'swift',
        ['1234567', '123456789012'],
    )
    def test_invalid_swift_len(self, swift):
        res = self.test_client.get(self.BASE_API, {'value': swift})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json()['data'],
            hm.has_entries({
                'valid': False,
                'swift': swift,
                'error': 'SWIFT must be between 8 and 11 characters long',
            }),
        )

    def test_add_swift_len(self):
        create_bank_int(bicint='12345678XXX')

        res = self.test_client.get(self.BASE_API, {'value': '12345678'})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(res.get_json()['data'], hm.has_entries({'valid': True, 'swift': '12345678XXX'}))
