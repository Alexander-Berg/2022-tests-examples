# coding: utf-8

import datetime
from decimal import Decimal, ROUND_CEILING

import pytest

from balance import mapper, constants as cst, muzzle_util as ut
from balance.actions import consumption
from balance.actions.promocodes.operations import reserve_promo_code as actions_reserve_promo_code
from balance.processors import client_migrate_to_currency

from tests import object_builder as ob


def reserve_promo_code(client, bonus):
    pc_group = ob.PromoCodeGroupBuilder.construct(
        client.session,
        calc_class_name='FixedSumBonusPromoCodeGroup',
        calc_params={
            'adjust_quantity': 1,  # увеличиваем количество (иначе уменьшаем сумму)
            'apply_on_create': 0,  # применяем при создании счёта иначе при включении (оплате)
            'currency_bonuses': {"RUB": bonus},
            'reference_currency': 'RUB',
        },
    )
    actions_reserve_promo_code(client, pc_group.promocodes[0])


@pytest.fixture(name='client')
def create_client(session):
    client = ob.ClientBuilder.construct(session)
    client.cashback_settings[cst.DIRECT_SERVICE_ID] = mapper.ClientCashbackSettings(
        client_id=client.id,
        service_id=cst.DIRECT_SERVICE_ID,
        is_auto_charge_enabled=True
    )
    client.set_currency(cst.ServiceId.DIRECT, 'RUB', datetime.datetime(2000, 1, 1), cst.CONVERT_TYPE_MODIFY, force=True)
    client_migrate_to_currency.process_client(client, client.exports['MIGRATE_TO_CURRENCY'].input)
    session.flush()
    return client


@pytest.fixture(name='person')
def create_person(session, client):
    return ob.PersonBuilder.construct(session, client=client)


@pytest.fixture(name='order')
def create_order(session, client, service_id=cst.ServiceId.DIRECT,
                 product_id=cst.DIRECT_PRODUCT_RUB_ID, **kw):
    return ob.OrderBuilder.construct(
        session,
        client=client,
        service_id=service_id,
        product_id=product_id,
        **kw
    )


@pytest.fixture(name='invoice')
def create_invoice(session, client, person=None, request_=None, **kwargs):
    request_ = request_ or create_request(session, client,
                                          [(create_order(session, client), Decimal('100'))])
    return ob.InvoiceBuilder.construct(
        session,
        request=request_,
        person=person,
        **kwargs
    )


def create_request(session, client, orders):
    return ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=client,
            rows=[
                ob.BasketItemBuilder(order=o, quantity=qty)
                for o, qty in orders
            ],
        ),
    )


def create_cashback(client, **kw):
    cb = ob.ClientCashbackBuilder.construct(
        client.session,
        client=client,
        **kw
    )
    client.session.expire(client)
    return cb


def round_func(qty):
    return ut.round(qty, precision=cst.CASHBACK_PRECISION, rounding=ROUND_CEILING)


def reverse_invoice(invoice):
    invoice.turn_on_rows()
    consume = invoice.consumes[0]
    reverse = consumption.reverse_consume(
        consume, consume.operation, consume.current_qty
    )
    return reverse
