# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import allure

from balance import mapper
from balance.actions.passport_sms_verification import VerificationType
from tests import object_builder as ob

from brest.core.tests import utils as test_utils
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.request import create_request


DEFAULT_PHONE_ID = ob.get_big_number()
DEFAULT_PHONE = '+79998889988'
DEFAULT_MASKED_PHONE = '+7 999 ***-**-88'

DEFAULT_SMS_CODE = 12345
WRONG_SMS_CODE = 123456


@pytest.fixture(name='passport_bb_api_user_phones')
@allure.step('mock PassportBlackbox user phones')
def create_passport_bb_api_user_phones():
    return [
        {
            'default': True,
            'id': '12',
            'phone': '+79998889988',
            'masked_phone': '+7 999 ***-**-88',
            'validated': True,
        },
        {
            'default': False,
            'id': '123',
            'phone': '+79990009900',
            'masked_phone': '+7 999 ***-**-00',
            'validated': False,
        },
    ]


@pytest.fixture(name='overdraft_verification_code')
def create_overdraft_verification_code(request_):
    session = test_utils.get_test_session()
    with allure.step('create overdraft_verification_code'):
        vc = ob.VerificationCodeBuilder(
            passport_id=session.passport.passport_id,
            classname=request_.__class__.__name__,
            object_id=request_.id,
            type=VerificationType.OVERDRAFT,
            code=DEFAULT_SMS_CODE,
        ).build(session).obj
    return vc


@pytest.fixture(name='auth_verification_code')
def create_auth_verification_code():
    session = test_utils.get_test_session()
    passport = session.passport
    with allure.step('create auth_verification_code'):
        vc = ob.VerificationCodeBuilder(
            passport_id=passport.passport_id,
            classname=passport.__class__.__name__,
            object_id=passport.passport_id,
            type=VerificationType.SECURE_AUTH,
            code=DEFAULT_SMS_CODE,
        ).build(session).obj
    return vc
