# coding=utf-8

import datetime

import pytest
import mock

from balance import mapper
from balance.exc.muzzle import ALREADY_SENT_CODE, MAX_AMOUNT_OF_SMS_WERE_SENT
from balance.actions.passport_sms_verification import OverdraftVerificator, \
    SecureAuthVerificator, DirectPaymentVerificator


def create_vc(session, dt, code, type_):
    vc = mapper.VerificationCode(
        passport_id=session.passport.passport_id,
        dt=dt,
        code=code,
        type=type_,
    )
    session.add(vc)
    session.flush()
    return vc


standard_parametrize = pytest.mark.parametrize(
        'type_, Verificator, code',
        [
            ('OVERDRAFT_VERIFICATION', OverdraftVerificator, None),
            ('SECURE_AUTH_VERIFICATION', SecureAuthVerificator, None),
            ('DIRECT_PAYMENT_VERIFICATION', DirectPaymentVerificator, 'test')
        ]
    )


class TestPassportSmsVerificator(object):
    @pytest.mark.parametrize(
        'Verificator, code, message',
        [
            (OverdraftVerificator, None, u'пароль для отсроченного платежа'),
            (SecureAuthVerificator, None, u'пароль для входа'),
            (DirectPaymentVerificator, 'test', u'Код платежа для оплаты Яндекс.Директа')
        ]
    )
    def test_send_sms(self, session, Verificator, code, message):
        verificator = Verificator(passport=session.passport)
        if code:
            verificator.code = code

        with mock.patch('butils.passport.PassportSmsApi.send_sms', return_value=666) as mock_obj:
            sms_id = verificator.send()
            assert mock_obj.call_args[0][0].find(message) != -1
        assert sms_id is not None

    @standard_parametrize
    def test_get_on_dt_small_delta(self, session, Verificator, type_, code):
        on_dt = datetime.datetime.now()
        created_vc = create_vc(session, on_dt, '1234567', type_=type_)

        verificator = Verificator(passport=session.passport)
        if code:
            verificator.code = code

        vc = verificator._get_on_dt(on_dt)
        assert len(vc) == 1
        assert vc[0].passport_id == created_vc.passport_id

    @standard_parametrize
    def test_sent_on_dt(self, session, Verificator, type_, code):
        on_dt = datetime.datetime.now()
        create_vc(session, on_dt, '1234567', type_=type_)

        verificator = Verificator(passport=session.passport)

        assert verificator.sent_on_dt(on_dt)

    @standard_parametrize
    def test_get_on_dt_big_delta(self, session, Verificator, type_, code):
        on_dt = datetime.datetime.now()
        create_vc(session, on_dt, '1234567', type_=type_)

        verificator = Verificator(passport=session.passport, classname='Request', object_id=0)
        if code:
            verificator.code = code

        vc_with_big_delta = verificator._get_on_dt(on_dt + datetime.timedelta(minutes=10))
        assert len(vc_with_big_delta) == 0

    @pytest.mark.parametrize(
        'type_, Verificator, code, exc_type, max_sms', [
            ('OVERDRAFT_VERIFICATION', OverdraftVerificator, None, ALREADY_SENT_CODE, 1),
            ('SECURE_AUTH_VERIFICATION', SecureAuthVerificator, None, ALREADY_SENT_CODE, 1),
            ('DIRECT_PAYMENT_VERIFICATION', DirectPaymentVerificator, 'test', MAX_AMOUNT_OF_SMS_WERE_SENT, 5)
        ]
    )
    def test_check_on_dt_exc(self, session, type_, Verificator, code, exc_type, max_sms):
        on_dt = datetime.datetime.now()

        for i in range(max_sms):
            created_vc = create_vc(session, on_dt + datetime.timedelta(seconds=i), str(i), type_=type_)
            created_vc.input = {'sms_id': 123}
            session.flush()
        verificator = Verificator(passport=session.passport, max_sms_per_delta=max_sms)
        if code:
            verificator.code = code
        with pytest.raises(exc_type):
            verificator._check_on_dt(on_dt)


class TestOverdraft(object):
    def test_check_and_vc(self, session):
        on_dt = datetime.datetime.now()
        created_vc = create_vc(session, on_dt, '1234567', type_='OVERDRAFT_VERIFICATION')

        verificator = OverdraftVerificator(passport=session.passport)
        is_matched = verificator.check('1234567')

        assert is_matched
