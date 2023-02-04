# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D
import re
import pytest
import mock

from balance import mapper
from balance.constants import (
    DIRECT_PRODUCT_ID,
    ServiceId,
    DIRECT_PRODUCT_RUB_ID,
    FirmId,
)
from balance.actions.passport_sms_verification import VerificationType
import balance.exc as exc
from balance import core
from butils.decimal_unit import DecimalUnit as DU

from tests import object_builder as ob


@pytest.fixture
def coreobj(session):
    return core.Core(session)


def create_client(session, currency='RUB', overdraft_limit=99999999999, firm_id=FirmId.YANDEX_OOO, is_agency=0):
    client = ob.ClientBuilder().build(session).obj
    client.is_agency = is_agency
    client.set_currency(ServiceId.DIRECT, currency, datetime.datetime(2000, 1, 1), None)

    if overdraft_limit is not None:
        client.set_overdraft_limit(ServiceId.DIRECT, firm_id, overdraft_limit, currency)
    session.flush()
    return client


def get_person(session, client, person_type='ur'):
    return ob.PersonBuilder(client=client, type=person_type).build(session).obj


def get_paysys(session, paysys_id=1000):
    return ob.Getter(mapper.Paysys, paysys_id).build(session).obj


def spent_sum(session, client, person, spent_sum_params):
    product_id, qty = spent_sum_params[0], spent_sum_params[1]
    if not qty:
        return

    request_obj = _create_request_rows(session, client, product_rows=[(product_id, qty)])
    invoice = ob.InvoiceBuilder(
        request=request_obj,
        person=person,
        overdraft=1
    ).build(session).obj
    invoice.turn_on_rows()
    session.flush()


def _create_request_rows(session, client, order_rows=None, product_rows=None, firm_id=FirmId.YANDEX_OOO):
    if order_rows is None:
        order_rows = []
        for product_id, qty in product_rows:
            product = ob.Getter(mapper.Product, product_id).build(session).obj
            order = ob.OrderBuilder(
                product=product,
                client=client,
                service_id=product.engine_id
            ).build(session).obj
            order_rows.append((order, qty))

    return ob.RequestBuilder(
        firm_id=firm_id,
        basket=ob.BasketBuilder(
            client=client,
            rows=[
                ob.BasketItemBuilder(order=order, quantity=qty)
                for order, qty in order_rows
            ]
        )
    ).build(session).obj


class TestTakeOverdraft(object):
    @pytest.mark.parametrize(
        'params',
        [
            dict(description='migr-less-fish',
                 client_params=dict(currency='RUB', overdraft_limit=666),
                 request_obj_params=(DIRECT_PRODUCT_ID, 10),
                 spent_sum_params=(DIRECT_PRODUCT_ID, 6), req_amount=300),
            dict(description='migr-slack-fish',
                 client_params=dict(currency='RUB', overdraft_limit=666),
                 request_obj_params=(DIRECT_PRODUCT_ID, 22),
                 spent_sum_params=(DIRECT_PRODUCT_RUB_ID, D('6.9')), req_amount=660),
            dict(description='migr-exact-fish',
                 client_params=dict(currency='RUB', overdraft_limit=600),
                 request_obj_params=(DIRECT_PRODUCT_ID, 13),
                 spent_sum_params=(DIRECT_PRODUCT_ID, 7), req_amount=390),
            dict(description='migr-exact-rub',
                 client_params=dict(currency='RUB', overdraft_limit=600),
                 request_obj_params=(DIRECT_PRODUCT_RUB_ID, 360),
                 spent_sum_params=(DIRECT_PRODUCT_ID, 8), req_amount=360),
            dict(description='nonmigr-exact-fish',
                 client_params=dict(currency=None, overdraft_limit=15),
                 request_obj_params=(DIRECT_PRODUCT_ID, 14),
                 spent_sum_params=(DIRECT_PRODUCT_ID, 1), req_amount=420),
            dict(description='nonmigr-slack-fish',
                 client_params=dict(currency=None, overdraft_limit=30),
                 request_obj_params=(DIRECT_PRODUCT_ID, 19),
                 spent_sum_params=(DIRECT_PRODUCT_ID, D('11.034')), req_amount=570),
            dict(description='agency_migr-exact-rub',
                 client_params=dict(currency='RUB', overdraft_limit=600, is_agency=1),
                 request_obj_params=(DIRECT_PRODUCT_RUB_ID, 360),
                 spent_sum_params=(DIRECT_PRODUCT_ID, 8), req_amount=360),
        ],
        ids=lambda x: str(x['description'])
    )
    def test_can_take(self, session, coreobj, params):
        client = create_client(session, **params['client_params'])
        person = get_person(session, client)
        spent_sum(session, client, person, params['spent_sum_params'])
        request_obj = _create_request_rows(session, client, product_rows=[params['request_obj_params'], ])
        paysys = get_paysys(session)

        invoice = coreobj.take_overdraft(request_obj.id, paysys.id, person.id, skip_verification=True)

        assert 'overdraft' == invoice.type
        assert DU(params['req_amount'], 'RUB') == invoice.total_sum
        assert DU(params['req_amount'], 'FISH') == invoice.consume_sum

    @pytest.mark.parametrize(
        'params',
        [
            dict(description='migr-wo-spent',
                 client_params=dict(currency='RUB', overdraft_limit=100),
                 request_obj_params=(DIRECT_PRODUCT_ID, 666),
                 spent_sum_params=(None, 0)),
            dict(description='migr-w-spent',
                 client_params=dict(currency='RUB', overdraft_limit=600),
                 request_obj_params=(DIRECT_PRODUCT_ID, 29),
                 spent_sum_params=(DIRECT_PRODUCT_RUB_ID, 31)),
            dict(description='nonmigr',
                 client_params=dict(currency=None, overdraft_limit=30),
                 request_obj_params=(DIRECT_PRODUCT_ID, 19),
                 spent_sum_params=(DIRECT_PRODUCT_ID, D('12.0001'))),
        ],
        ids=lambda x: str(x['description'])
    )
    def test_not_enough_limit(self, session, coreobj, params):
        client = create_client(session, **params['client_params'])
        person = get_person(session, client)
        spent_sum(session, client, person, params['spent_sum_params'])
        request_obj = _create_request_rows(session, client, product_rows=[params['request_obj_params'], ])
        paysys = get_paysys(session)

        with pytest.raises(exc.NOT_ENOUGH_OVERDRAFT_LIMIT) as exc_info:
            coreobj.take_overdraft(request_obj.id, paysys.id, person.id, skip_verification=True)

        assert re.match(
            r'Client has no enough overdraft limit \d*\.\d*(RUB|QTY) to proceed with this invoice sum \d*\.\d*(RUB|QTY)',
            exc_info.value.msg)
        assert exc_info.value.error_booster_msg == u'Client has no enough overdraft limit %(limit)s to proceed with this invoice sum %(sum)s'
        assert 1 == request_obj.seq
        assert [] == request_obj.invoices

    @pytest.mark.parametrize(
        'params',
        [
            dict(description='migr-wo-spent',
                 client_params=dict(currency='RUB', overdraft_limit=100, is_agency=1),
                 request_obj_params=(DIRECT_PRODUCT_ID, 666),
                 spent_sum_params=(None, 0)),
            dict(description='migr-w-spent',
                 client_params=dict(currency='RUB', overdraft_limit=600, is_agency=1),
                 request_obj_params=(DIRECT_PRODUCT_ID, 29),
                 spent_sum_params=(DIRECT_PRODUCT_RUB_ID, 31)),
            dict(description='nonmigr',
                 client_params=dict(currency=None, overdraft_limit=30, is_agency=1),
                 request_obj_params=(DIRECT_PRODUCT_ID, 19),
                 spent_sum_params=(DIRECT_PRODUCT_ID, D('12.0001'))),
        ],
        ids=lambda x: str(x['description'])
    )
    def test_agency_no_overdraft_params(self, session, coreobj, params):
        firm = ob.FirmBuilder().build(session).obj
        client = create_client(session, **params['client_params'])
        person = get_person(session, client)
        spent_sum(session, client, person, params['spent_sum_params'])
        request_obj = _create_request_rows(session, client, product_rows=[params['request_obj_params'], ], firm_id=firm.id)
        paysys = get_paysys(session)

        with pytest.raises(exc.NOT_ENOUGH_OVERDRAFT_LIMIT) as exc_info,\
            mock.patch('balance.providers.pay_policy.PayPolicyRoutingManager.get_firm', return_value=firm):
            coreobj.take_overdraft(request_obj.id, paysys.id, person.id, skip_verification=True)

        assert re.match(
            r'No overdraft params for service %s firm %s' % (ServiceId.DIRECT, firm.id),
            exc_info.value.msg)
        assert exc_info.value.error_booster_msg == u'No overdraft params for service %s firm %s' % (ServiceId.DIRECT, firm.id)
        assert 1 == request_obj.seq
        assert [] == request_obj.invoices

    @pytest.mark.parametrize('params', [
            {'count': 2},
            {'count': 4},
            {'count': 5, 'fails': True},
    ])
    def test_with_verification(self, session, coreobj, params):
        client_params = {'currency': 'RUB', 'overdraft_limit': 600}
        request_obj_params = (DIRECT_PRODUCT_RUB_ID, 360)
        spent_sum_params = (DIRECT_PRODUCT_ID, 8)
        req_amount = 360

        client = create_client(session, **client_params)
        person = get_person(session, client)
        spent_sum(session, client, person, spent_sum_params)
        request_obj = _create_request_rows(session, client, product_rows=[request_obj_params, ])
        paysys = get_paysys(session)

        vc = ob.VerificationCodeBuilder(
            passport_id=person.passport_id,
            classname=request_obj.__class__.__name__,
            object_id=request_obj.id,
            type=VerificationType.OVERDRAFT,
            code=123456,
        ).build(session).obj

        for _ in range(params['count']):
            with pytest.raises((exc.INVALID_OVERDRAFT_VERIFICATION_CODE, exc.MAX_FAILS_COUNT_REACHED)):
                coreobj.take_overdraft(request_obj.id, paysys.id, person.id, verification_code='12345', skip_verification=False)

        if params.get('fails', False):
            with pytest.raises(exc.MAX_FAILS_COUNT_REACHED):
                coreobj.take_overdraft(request_obj.id, paysys.id, person.id, verification_code='123456', skip_verification=False)
        else:
            invoice = coreobj.take_overdraft(request_obj.id, paysys.id, person.id, verification_code='123456', skip_verification=False)
            vc = session.query(mapper.VerificationCode).getone(
                passport_id=person.passport_id,
                classname=request_obj.__class__.__name__,
                object_id=request_obj.id,
                type=VerificationType.OVERDRAFT,
            )
            assert vc.fails_count == min(5, params['count'])
            assert vc.is_used != params.get('fails', False)
