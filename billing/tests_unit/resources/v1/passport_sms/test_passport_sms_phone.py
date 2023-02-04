# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

from hamcrest import (
    assert_that,
    contains,
    equal_to,
    has_entries,
)
import http.client as http
import mock
import pytest

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.passport_sms_api import create_passport_bb_api_user_phones


@pytest.mark.smoke
class TestCasePassportSmsPhone(TestCaseApiAppBase):
    BASE_API = '/v1/passport_sms/phone/list'

    @pytest.mark.failing_locally
    def test_get_phone(self):
        response = self.test_client.get(self.BASE_API)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

    def test_get_phone_mocked_api(self, passport_bb_api_user_phones):
        from butils.passport import PassportBlackbox

        with mock.patch.object(PassportBlackbox, 'get_user_phones', return_value=passport_bb_api_user_phones):
            response = self.test_client.get(self.BASE_API)

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        response_json = response.get_json().get('data', [])
        expected_json_match = [
            has_entries({
                'active': '1',
                'cyrillic': '1',
                'id': '12',
                'masked_number': '+7 999 ***-**-88',
                'valid': 'valid',
            }),
            has_entries({
                'active': '0',
                'cyrillic': '1',
                'id': '123',
                'masked_number': '+7 999 ***-**-00',
                'valid': 'msgsent',
            }),
        ]

        assert_that(response_json, contains(*expected_json_match))
