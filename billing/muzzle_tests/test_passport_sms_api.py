# -*- coding: utf-8 -*-
import pytest
import mock
import hamcrest as hm

from balance.constants import DIRECT_PRODUCT_ID
from balance import mapper
from balance.corba_buffers import StateBuffer, RequestBuffer
from muzzle.ajax.passport_sms_api import sms_user_phones
from muzzle.muzzle_logic import MuzzleLogic
from tests import object_builder as ob


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


def _create_order(session, client, product_id=DIRECT_PRODUCT_ID, product=None, service_id=7):
    product = product or ob.Getter(mapper.Product, product_id).build(session).obj
    return ob.OrderBuilder(
        product=product,
        service=ob.Getter(mapper.Service, service_id),
        client=client,
    ).build(session).obj


PHONES_DATA = [
    {
         'default': True,
         'id': 12,
         'phone': '+79998889988',
         'masked_phone': '+7 999 ***-**-88',
         'validated': True,
    },
    {
         'default': False,
         'id': 123,
         'phone': '+79990009900',
         'masked_phone': '+7 999 ***-**-00',
         'validated': False,
    },
]

RES_PHONES = [
    {
        'id': 12,
        'active': '1',
        'cyrillic': '1',
        'masked_number': '+7 999 ***-**-88',
        'secure': '',
        'valid': 'valid',
    },
    {
        'id': 123,
        'active': '0',
        'cyrillic': '1',
        'masked_number': '+7 999 ***-**-00',
        'secure': '',
        'valid': 'valid',
    },
]


class TestPassportSmsApi(object):
    ajax_kw = {
        'state_obj': StateBuffer(params={'prot_method': 'GET'}),
        'request_obj': RequestBuffer(params=([], [('X-Requested-With', 'XMLHttpRequest')], []))
    }

    @pytest.mark.parametrize(
        'get_phones_func, additional_kw',
        [
            (MuzzleLogic().get_user_phones, {}),
            (sms_user_phones, ajax_kw),
        ],
        ids=['logic_func', 'ajax_func'],
    )
    @mock.patch('butils.passport.PassportBlackbox.get_user_phones', return_value=PHONES_DATA)
    def test_user_phones(self, _mock_func, session, get_phones_func, additional_kw):
        kw = {
            'session': session,
            'passport_id': session.oper_id,
        }
        kw.update(additional_kw)
        res = get_phones_func(**kw)
        if res.tag == 'result':
            res = res.find('doc')
        assert res.find('uid').text == str(session.oper_id)

        phones = [phone.attrib for phone in res.findall('phone')]
        hm.assert_that(
            phones,
            hm.contains_inanyorder(*RES_PHONES),
        )

    @mock.patch('butils.passport.PassportSmsApi.send_sms')
    def test_send_overdraft_verification(self, mock_send_sms, session, muzzle_logic):
        phone_id = 1234
        sms_id = 4321
        request = ob.RequestBuilder().build(session).obj
        mock_send_sms.return_value = sms_id

        res = muzzle_logic.send_overdraft_verification_sms(session, phone_id, request.id)

        # должна быть запись в БД с кодом
        vc = session.query(mapper.VerificationCode).getone(
            passport_id=session.oper_id,
            classname='Request',
            object_id=request.id,
            type='OVERDRAFT_VERIFICATION',
        )
        assert vc.input.get('phone_id') == phone_id
        assert vc.input.get('sms_id') == sms_id

        mock_send_sms.called_once_with_args(
            text=u'%s - ваш одноразовый пароль для отсроченного платежа в Яндекс.' % vc.code,
            from_uid=session.oper_id,
            phone_id=phone_id,
        )

        hm.assert_that(
            res.attrib,
            hm.has_entries({'sms-id': str(sms_id), 'send-now': '1'}),
        )

        # повторный вызов
        res = muzzle_logic.send_overdraft_verification_sms(session, phone_id, request.id)
        hm.assert_that(
            res.attrib,
            hm.has_entries({'sms-id': str(sms_id), 'send-now': '0'}),
        )

    @mock.patch('butils.passport.PassportSmsApi.send_sms')
    def test_send_sms_direct_payment(self, mock_send_sms, session, muzzle_logic, client):
        phone_id = 1234
        sms_id = 4321
        mock_send_sms.return_value = sms_id
        order = _create_order(session, client)
        state_obj = StateBuffer(
            params={'req_orders': str(order.id), 'phone': client.phone}
        )
        muzzle_logic.send_sms_direct_payment(session, phone_id, state_obj)
        assert mock_send_sms.call_args[0][0].find(u'Код платежа для оплаты Яндекс.Директа') != -1

        # должна быть запись в БД с кодом
        vc = session.query(mapper.VerificationCode).getone(
            passport_id=order.passport.passport_id,
            type='DIRECT_PAYMENT_VERIFICATION'
        )
        assert vc
