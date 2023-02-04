# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import mock
import hamcrest as hm
import httpretty
from lxml import etree

from butils.passport import PassportCfg

from balance import mapper
from balance.actions.passport_sms_verification import VerificationType
from tests.tutils import mock_transactions

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order, DEFAULT_ORDER_QTY
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.request import create_request
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.passport_sms_api import (
    DEFAULT_PHONE_ID,
    DEFAULT_SMS_CODE,
    WRONG_SMS_CODE,
    create_overdraft_verification_code,
)


@pytest.mark.smoke
class TestCasePassportSmsVerificationOverdraftSend(TestCaseApiAppBase):
    BASE_API = u'/v1/passport_sms/verification/overdraft/send'
    sms_id = 1234
    sms_text = u'{} - ваш одноразовый пароль для отсроченного платежа в Яндекс.'

    def test_verification_overdraft_send_invalid_object_type(self):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'phone_number': -1,
                'object_type': 'FAKE',
                'object_id': -1,
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(http.BAD_REQUEST), 'response code must be BAD_REQUEST')

    @mock.patch('butils.passport.PassportSmsApi.send_sms', return_value=sms_id)
    def test_verification_overdraft_send(self, mock_send_sms, request_):
        from yb_snout_api.resources.v1.passport_sms import enums

        session = self.test_session
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'phone_id': DEFAULT_PHONE_ID,
                'object_type': enums.OverdraftObjectType.REQUEST.name,
                'object_id': request_.id,
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        vc = session.query(mapper.VerificationCode).getone(
            passport_id=session.passport.passport_id,
            classname=enums.OverdraftObjectType.REQUEST.value,
            object_id=request_.id,
            type=VerificationType.OVERDRAFT,
        )
        mock_send_sms.assert_called_once_with(
            self.sms_text.format(vc.code),
            uid=session.passport.passport_id,
            phone_id=DEFAULT_PHONE_ID,
        )

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_verification_overdraft_send_headers(self, request_):
        from yb_snout_api.resources.v1.passport_sms import enums
        self.test_session.config.__dict__['PASSPORT_SMS_API_TEST_ALLOWED'] = {'uids': [self.test_session.oper_id]}

        def get_response():
            response = etree.Element('root')
            message = etree.Element('message-sent')
            message.attrib['id'] = '232'
            response.append(message)
            return response

        httpretty.register_uri(
            httpretty.GET,
            'https://phone-passport-test.yandex.ru/sendsms',
            etree.tostring(get_response()),
            status=200,
        )

        with mock.patch.object(PassportCfg, 'get_suitable_passport',
                               return_value={'SmsApiURL': 'https://phone-passport-test.yandex.ru',
                                             'name': 'test_name',
                                             'BlackBoxTimeout': 1}):
            response = self.test_client.secure_post(
                self.BASE_API,
                data={
                    'phone_id': DEFAULT_PHONE_ID,
                    'object_type': enums.OverdraftObjectType.REQUEST.name,
                    'object_id': request_.id,
                },
                headers={'User-Agent': 'User-Agent123',
                         'X_REAL_IP': '12345677'}
            )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        request = httpretty.last_request()
        assert request.headers.dict['ya-client-user-agent'] == 'User-Agent123'
        assert request.headers.dict['ya-consumer-client-ip'] == '12345677'


@pytest.mark.smoke
class TestCasePassportSmsVerificationOverdraftCheck(TestCaseApiAppBase):
    BASE_API = u'/v1/passport_sms/verification/overdraft/check'
    domain = 'snout.core.yandex.com'

    _use_test_session = False

    @pytest.mark.parametrize('params', [
        {'failed_attempts': 0},
        {'failed_attempts': 2},
        {'failed_attempts': 5, 'exception': 'MAX_FAILS_COUNT_REACHED'},
        {'is_used': True, 'res_is_valid': False},
        {'is_valid': False, 'res_is_valid': False},
    ])
    def test_verification_check(self, params, request_):
        # Тест для метода post, который проставляет куку для нового ЛК
        from yb_snout_api.resources.v1.passport_sms import enums

        vc = create_overdraft_verification_code(request_)
        vc.fails_count = params.get('failed_attempts', 0)
        vc.is_used = params.get('is_used', False)
        self.test_session.flush()

        with mock_transactions():
            response = self.test_client.get(
                self.BASE_API,
                data={
                    'code': DEFAULT_SMS_CODE if params.get('is_valid', True) else WRONG_SMS_CODE,
                    'object_type': enums.OverdraftObjectType.REQUEST.name,
                    'object_id': request_.id,
                    'update_usage_status': True,
                },
                headers={'HOST': self.domain},
            )

        if params.get('exception'):
            hm.assert_that(response.status_code, hm.equal_to(http.BAD_REQUEST))
            hm.assert_that(
                response.get_json(),
                hm.has_entries({'error': params.get('exception')}),
            )

        else:
            hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
            hm.assert_that(response.get_json()['data']['is_valid'], hm.is_(params.get('res_is_valid', True)))

            self.test_session.refresh(vc)
            if params.get('res_is_valid', True):
                hm.assert_that(
                    response.headers,
                    hm.has_item(
                        hm.contains(
                            'Set-Cookie',
                            hm.contains_string('Domain=.core.yandex.com'),
                        ),
                    ),
                )

    @pytest.mark.parametrize('params', [
        {'is_valid': True},
        {'is_valid': False}
    ])
    def test_get_verification_overdraft_check(self, params, request_):
        from yb_snout_api.resources.v1.passport_sms import enums

        vc = create_overdraft_verification_code(request_) if params['is_valid'] else None

        with mock_transactions():
            response = self.test_client.get(
                self.BASE_API,
                {
                    'code': DEFAULT_SMS_CODE,
                    'object_type': enums.OverdraftObjectType.REQUEST.name,
                    'object_id': request_.id,
                },
            )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        hm.assert_that(response.get_json()['data']['is_valid'], hm.is_(params['is_valid']))
