# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
standard_library.install_aliases()

import hamcrest as hm
import http.client as http
import pytest
import mock

from balance import mapper
from balance.actions.passport_sms_verification import VerificationType

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.passport_sms_api import (
    DEFAULT_PHONE_ID,
    DEFAULT_SMS_CODE,
    WRONG_SMS_CODE,
    create_auth_verification_code,
)

@pytest.mark.smoke
class TestCasePassportSmsVerificationSecureAuthSend(TestCaseApiAppBase):
    BASE_API = u'/v1/passport_sms/verification/secure_auth/send'
    sms_id = 1234
    sms_text = u'{} - ваш одноразовый пароль для входа в Яндекс.'

    @mock.patch('butils.passport.PassportSmsApi.send_sms', return_value=sms_id)
    def test_verification_send(self, mock_send_sms):
        session = self.test_session
        passport = session.passport

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'phone_id': DEFAULT_PHONE_ID},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        vc = session.query(mapper.VerificationCode).getone(
            passport_id=passport.passport_id,
            classname=passport.__class__.__name__,
            object_id=passport.passport_id,
            type=VerificationType.SECURE_AUTH,
        )
        hm.assert_that(vc.fails_count, hm.equal_to(0))
        mock_send_sms.assert_called_once_with(
            self.sms_text.format(vc.code),
            uid=passport.passport_id,
            phone_id=DEFAULT_PHONE_ID,
        )


@pytest.mark.smoke
class TestCasePassportSmsVerificationSecureAuthCheck(TestCaseApiAppBase):
    BASE_API = u'/v1/passport_sms/verification/secure_auth/check'
    domain = 'snout.core.yandex.com'

    _use_test_session = False

    @pytest.mark.parametrize('params', [
        {'is_valid': True,  'failed_attempts': 0},
        {'is_valid': True,  'failed_attempts': 2},
        {'is_valid': True,  'failed_attempts': 4},
        {'is_valid': True,  'failed_attempts': 5, 'exception': True},
        {'is_valid': False, 'declined': True}
    ])
    def test_verification_check(self, params):
        vc = create_auth_verification_code() if params['is_valid'] else None

        for _ in range(params.get('failed_attempts', 0)):
            self.test_client.secure_post(
                self.BASE_API,
                data={'code': WRONG_SMS_CODE},
                headers={'HOST': self.domain},
            )

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'code': DEFAULT_SMS_CODE},
            headers={'HOST': self.domain},
        )
        if params.get('exception', False):
            hm.assert_that(response.status_code, hm.equal_to(http.BAD_REQUEST))
            hm.assert_that(
                response.get_json(),
                hm.has_entries({
                    'error': 'MAX_FAILS_COUNT_REACHED',
                    'description': 'Maximal count of failed attempts has been reached. Please request for another code',
                }),
            )
        else:
            hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
            hm.assert_that(response.get_json()['data']['is_valid'], hm.is_(not params.get('declined', False)))
            # проверяем только в случае, когда не было исключения,
            # потому что в юнит тестах сессия не клонируется и откатывается при исключениях
            if params['is_valid']:
                self.test_session.refresh(vc)
                assert vc.is_used is not params.get('declined', False)
                assert vc.fails_count == min(params['failed_attempts'], 5)

                if not params.get('declined', False):
                    hm.assert_that(
                        response.headers,
                        hm.has_item(
                            hm.contains(
                                'Set-Cookie',
                                hm.contains_string('Domain=.core.yandex.com'),
                            ),
                        ),
                    )

    def test_wo_host(self):
        create_auth_verification_code()

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'code': DEFAULT_SMS_CODE},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'error': 'SnoutException',
                'description': 'Unknown first level domain.',
            }),
        )
