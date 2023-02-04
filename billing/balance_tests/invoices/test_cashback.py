# -*- coding: utf-8 -*-

import datetime
import pytest
import hamcrest as hm
from dateutil.relativedelta import relativedelta
from decimal import Decimal as D

from butils.decimal_unit import DecimalUnit as DU

from balance import (
    core,
    constants as cst,
    muzzle_util as ut,
)
from balance.actions.promocodes.operations import reserve_promo_code
from balance.actions.cashback.utils import get_consumed_bonus
from tests import object_builder as ob

pytestmark = [
    pytest.mark.cashback,
]


@pytest.fixture(name='agency')
def create_agency(session):
    return ob.ClientBuilder.construct(session, is_agency=1)


@pytest.fixture(name='client')
def create_client(session, agency=None):
    return ob.ClientBuilder.construct(session, agency=agency)


@pytest.fixture(name='person')
def create_person(session, client):
    return ob.PersonBuilder.construct(session, client=client)


@pytest.fixture(name='order')
def create_order(session, client, service_id=cst.ServiceId.DIRECT, product_id=cst.DIRECT_PRODUCT_RUB_ID, **kw):
    return ob.OrderBuilder.construct(
        session,
        client=client,
        service_id=service_id,
        product_id=product_id,
        **kw
    )


@pytest.fixture(name='promo_code')
def create_promo_code(session, bonus):
    pc_group = ob.PromoCodeGroupBuilder.construct(
        session,
        calc_class_name='FixedSumBonusPromoCodeGroup',
        calc_params={
            # adjust_quantity и apply_on_create общие для всех типов промокодов
            'adjust_quantity': 1,  # увеличиваем количество (иначе уменьшаем сумму)
            'apply_on_create': 0,  # применяем при создании счёта иначе при включении (оплате)
            # остальные зависят от типа
            'currency_bonuses': {"RUB": bonus},
            'reference_currency': 'RUB',
        },
    )
    pc = pc_group.promocodes[0]
    return pc


@pytest.fixture(name='invoice')
def create_invoice(session, client, person=None, request_=None, **kwargs):
    person = person or create_person(session, client)
    request_ = request_ or create_request(session, client, [(create_order(session, client), D('100'))])
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


@pytest.fixture(name='cashback')
def create_cashback(session, client, **kw):
    return ob.ClientCashbackBuilder.construct(
        session,
        client=client,
        **kw
    )


def create_contract(session, client, person, **kwargs):
    params = dict(
        commission=0,
        firm=1,
        postpay=1,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        credit=3,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        is_signed=datetime.datetime.now()
    )
    params.update(kwargs)

    contract = ob.ContractBuilder(
        client=client,
        person=person,
        **params
    ).build(session).obj
    return contract


def do_complete(order, qty):
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})
    order.session.flush()


def test_base(session, client, person, order, invoice):
    usage_matcher = hm.has_properties(consume_qty=110)

    cashback = create_cashback(session, client, bonus=10)
    invoice.turn_on_rows()
    session.expire_all()
    co, = invoice.consumes

    hm.assert_that(
        co,
        hm.has_properties(
            current_qty=110,
            cashback_usage=usage_matcher
        )
    )
    assert cashback.bonus == 0


def test_base_several_bonuses(session, client, person, order, invoice):
    cashback_1 = create_cashback(session, client, bonus=5)
    finish_dt = ut.trunc_date(datetime.datetime.now() + relativedelta(days=60))
    cashback_2 = create_cashback(session, client, bonus=5, finish_dt=finish_dt)
    finish_dt = ut.trunc_date(datetime.datetime.now() + relativedelta(days=61))
    cashback_3 = create_cashback(session, client, bonus=5, finish_dt=finish_dt)
    invoice.turn_on_rows()
    session.expire_all()

    co1, co2, co3 = invoice.consumes

    hm.assert_that(
        co1,
        hm.has_properties(
            current_sum=D('33.33'),
            current_qty=D('38.33'),
            cashback_usage=hm.has_properties(consume_qty=D('38.33')),
            client_cashback=cashback_2,
        )
    )
    hm.assert_that(
        co2,
        hm.has_properties(
            current_sum=D('33.34'),
            current_qty=D('38.34'),
            cashback_usage=hm.has_properties(consume_qty=D('38.34')),
            client_cashback=cashback_3,
        )
    )
    hm.assert_that(
        co3,
        hm.has_properties(
            current_sum=D('33.33'),
            current_qty=D('38.33'),
            cashback_usage=hm.has_properties(consume_qty=D('38.33')),
            client_cashback=cashback_1,
        )
    )
    assert sum([co.current_sum for co in invoice.consumes]) == D('100')
    assert sum([co.current_qty for co in invoice.consumes]) == D('115')
    assert sum([co.current_cashback_bonus for co in invoice.consumes]) == D('15')
    assert cashback_1.bonus == 0
    assert cashback_2.bonus == 0
    assert cashback_3.bonus == 0


def test_base_several_bonuses_use_io_cb_amount_limit(session, client, person, order):
    session.config.__dict__['CASHBACK_TURNON_SPLIT_AMOUNTS'] = [
        {"iso_currency": "RUB", "amount_w_cashback": "300.0"}
    ]
    qty = D('333')
    request = create_request(session, client, [(order, qty)])
    invoice = create_invoice(session, client, person, request)

    cashbacks = list()

    cashbacks.append(create_cashback(session, client, bonus=5))
    for i in range(8):
        finish_dt = ut.trunc_date(datetime.datetime.now() + relativedelta(days=60+i))
        cashbacks.append(create_cashback(session, client, bonus=5, finish_dt=finish_dt))

    invoice.turn_on_rows()
    session.expire_all()

    co1, co2, co3, co4, co5, co6, co7, co8, co9, co10 = invoice.consumes

    assert sum([co.cashback_base for co in invoice.consumes if co.cashback_base is not None]) == 300

    for co in [co1, co3, co4, co6, co7, co9]:
        hm.assert_that(
            co,
            hm.has_properties(
                current_sum=D('33.33'),
                current_qty=D('38.33'),
                cashback_usage=hm.has_properties(consume_qty=D('38.33'))
            )
        )

    for co in [co2, co5, co8]:
        hm.assert_that(
            co,
            hm.has_properties(
                current_sum=D('33.34'),
                current_qty=D('38.34'),
                cashback_usage=hm.has_properties(consume_qty=D('38.34'))
            )
        )

    hm.assert_that(
        co10,
        hm.has_properties(
            current_sum=D('33'),
            current_qty=D('33'),
            cashback_usage=hm.is_(None)
        )
    )

    assert sum([co.current_sum for co in invoice.consumes]) == D('333')
    assert sum([co.current_qty for co in invoice.consumes]) == D('378')
    assert sum([co.current_cashback_bonus for co in invoice.consumes]) == D('45')

    for cashback in cashbacks:
        assert cashback.bonus == 0


def test_base_several_bonuses_use_io_cb_amount_limit_huge_bonus(session, client, person, order):
    session.config.__dict__['CASHBACK_TURNON_SPLIT_AMOUNTS'] = [
        {"iso_currency": "RUB", "amount_w_cashback": "300.0"}
    ]
    qty = D('333')
    request = create_request(session, client, [(order, qty)])
    invoice = create_invoice(session, client, person, request)

    cashback_1 = create_cashback(session, client, bonus=2000000)
    finish_dt = ut.trunc_date(datetime.datetime.now() + relativedelta(days=60))
    cashback_2 = create_cashback(session, client, bonus=2000000, finish_dt=finish_dt)

    invoice.turn_on_rows()
    session.expire_all()

    assert cashback_1.bonus == D('1000300')
    assert cashback_2.bonus == D('0.02')  # из-за округления

    co1, co2, co3 = invoice.consumes

    assert sum([co.cashback_base for co in invoice.consumes if co.cashback_base is not None]) == 300

    hm.assert_that(
        co1,
        hm.has_properties(
            current_sum=D('200.02'),
            current_qty=D('2000200'),
            cashback_usage=hm.has_properties(consume_qty=D('2000200'))
        )
    )
    hm.assert_that(
        co2,
        hm.has_properties(
            current_sum=D('99.98'),
            current_qty=D('999799.98'),
            cashback_usage=hm.has_properties(consume_qty=D('999799.98'))
        )
    )
    hm.assert_that(
        co3,
        hm.has_properties(
            current_sum=D('33'),
            current_qty=D('33'),
            cashback_usage=hm.is_(None)
        )
    )

    assert sum([co.current_sum for co in invoice.consumes]) == D('333')
    assert sum([co.current_qty for co in invoice.consumes]) == D('3000032.98')
    assert sum([co.current_cashback_bonus for co in invoice.consumes]) == D('2999699.98')


def test_several_orders_several_bonuses(session, client, person):
    cashback = create_cashback(session, client, bonus=5)
    finish_dt = ut.trunc_date(datetime.datetime.now() + relativedelta(days=60))
    cashback_time_limit = create_cashback(session, client, bonus=5, finish_dt=finish_dt)

    order_rub_1 = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    order_rub_2 = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    order_rub_3 = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(
        session,
        client,
        [
            (order_rub_1, D('10')),
            (order_rub_2, D('10')),
            (order_rub_3, D('10')),
        ]
    )
    invoice = create_invoice(session, client, person, request)

    invoice.turn_on_rows()
    session.expire_all()

    co1, co2, co3, co4 = invoice.consumes

    hm.assert_that(
        co1,
        hm.has_properties(
            consume_sum=10,
            current_qty=D('13.3333'),
            cashback_usage=hm.has_properties(consume_qty=D('13.3333')),
            client_cashback=cashback_time_limit,
        )
    )
    hm.assert_that(
        co2,
        hm.has_properties(
            consume_sum=5,
            current_qty=D('6.6667'),
            cashback_usage=hm.has_properties(consume_qty=D('6.6667')),
            client_cashback=cashback_time_limit,
        )
    )
    hm.assert_that(
        co3,
        hm.has_properties(
            consume_sum=5,
            current_qty=D('6.6666'),
            cashback_usage=hm.has_properties(consume_qty=D('6.6666')),
            client_cashback=cashback,
        )
    )
    hm.assert_that(
        co4,
        hm.has_properties(
            consume_sum=10,
            current_qty=D('13.3333'),
            cashback_usage=hm.has_properties(consume_qty=D('13.3333')),
            client_cashback=cashback,
        )
    )

    assert sum([co.current_sum for co in invoice.consumes]) == D('30')
    assert sum([co.current_qty for co in invoice.consumes]) == D('39.9999')
    assert sum([co.current_cashback_bonus for co in invoice.consumes]) == D('9.9999')

    assert cashback.bonus == D('0.0001')  # из-за округления
    assert cashback_time_limit.bonus == 0


def test_several_orders_several_bonuses_w_fish(session, client, person):
    cashback = create_cashback(session, client, bonus=5, iso_currency='RUB')
    finish_dt = ut.trunc_date(datetime.datetime.now() + relativedelta(days=60))
    cashback_time_limit = create_cashback(session, client, bonus=5, iso_currency='RUB', finish_dt=finish_dt)

    order_fish_1 = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)
    order_fish_2 = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)
    order_rub = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(
        session,
        client,
        [
            (order_fish_1, D('10')),
            (order_fish_2, D('10')),
            (order_rub, D('10')),
        ]
    )
    invoice = create_invoice(session, client, person, request)

    invoice.turn_on_rows()
    session.expire_all()

    assert (
               sum([co.current_sum for co in order_fish_1.consumes]) +
               sum([co.current_sum for co in order_fish_2.consumes]) +
               sum([co.current_sum for co in order_rub.consumes])
           ) == D('610')
    assert (
               sum([co.current_qty * D('30') for co in order_fish_1.consumes]) +
               sum([co.current_qty * D('30') for co in order_fish_2.consumes]) +
               sum([co.current_qty for co in order_rub.consumes])
           ) == D('619.99996')
    assert (
               sum([co.current_cashback_bonus * D('30') for co in order_fish_1.consumes]) +
               sum([co.current_cashback_bonus * D('30') for co in order_fish_2.consumes]) +
               sum([co.current_cashback_bonus for co in order_rub.consumes])
           ) == D('9.99996')
    assert cashback.bonus == D('0.00002')  # из-за округления
    assert cashback_time_limit.bonus == D('0.00002')

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            hm.has_properties(
                current_qty=DU('10.111111', 'QTY'),
                cashback_base=DU('10', 'QTY'),
                cashback_bonus=DU('0.111111', 'QTY'),
                client_cashback=cashback_time_limit,
                cashback_usage=hm.has_properties(
                    consume_qty=D('303.33333'),
                )
            ),
            hm.has_properties(
                current_qty=DU('5.055555', 'QTY'),
                cashback_base=DU('5', 'QTY'),
                cashback_bonus=DU('0.055555', 'QTY'),
                client_cashback=cashback_time_limit,
                cashback_usage=hm.has_properties(
                    consume_qty=D('151.66665'),
                )
            ),
            hm.has_properties(
                current_qty=DU('5.055556', 'QTY'),
                cashback_base=DU('5', 'QTY'),
                cashback_bonus=DU('0.055556', 'QTY'),
                client_cashback=cashback,
                cashback_usage=hm.has_properties(
                    consume_qty=D('151.66668'),
                )
            ),
            hm.has_properties(
                current_qty=DU('13.3333', 'QTY'),
                cashback_base=DU('10', 'QTY'),
                cashback_bonus=DU('3.3333', 'QTY'),
                client_cashback=cashback,
                cashback_usage=hm.has_properties(
                    consume_qty=D('13.3333'),
                )
            ),
        ),
    )


@pytest.mark.parametrize(
    'cashback_qty, co_qty, res_qty',
    [
        pytest.param(D('6.66666'), D('106.6666'), D('0.00006'), id='less that 5'),
        pytest.param(D('6.66665'), D('106.6666'), D('0.00005'), id='5'),
        pytest.param(D('6.66664'), D('106.6666'), D('0.00004'), id='more than 5'),
    ],
)
def test_product_precision(session, client, person, order, invoice, cashback_qty, co_qty, res_qty):
    """Из кешбека отнимается кол-во, которое будет зачислено на конзюм с учётом округления
    """
    cashback = create_cashback(session, client, bonus=cashback_qty)
    invoice.turn_on_rows()
    session.expire_all()
    co, = invoice.consumes

    hm.assert_that(
        co,
        hm.has_properties(
            current_qty=co_qty,
            cashback_usage=hm.has_properties(consume_qty=co_qty)
        )
    )
    assert cashback.bonus == res_qty


def test_w_promo_code(session, client, person, order):
    pc = create_promo_code(session, D('100'))
    reserve_promo_code(client, pc)

    cashback = create_cashback(session, client, bonus=D('100'))

    qty = D('120')
    request = create_request(session, client, [(order, qty)])

    invoice = create_invoice(session, client, person, request)

    invoice.turn_on_rows(apply_promocode=True)
    assert invoice.current_qty() == DU('340', 'QTY')

    co, = invoice.consumes
    session.expire_all()

    assert co.cashback_base == DU('240', 'QTY')  # qty + promo_code
    assert co.cashback_bonus == DU('100', 'QTY')
    assert co.current_qty == DU('340', 'QTY')  # qty + promo_code + cashback
    assert co.current_sum == DU('120', 'FISH')

    discount_obj = co.discount_obj
    assert discount_obj.cashback_base == DU('240', 'QTY')
    assert discount_obj.cashback_bonus == DU('100', 'QTY')
    assert ut.round(discount_obj.cashback_relation, 6) == D('0.705882')
    assert co.current_cashback_bonus == DU('100', 'QTY')

    assert cashback.bonus == DU('0', 'QTY')


def test_complete_consumes(session, client, person, order):
    cashback = create_cashback(session, client, bonus=D('100'))
    invoice = create_invoice(session, client, person, create_request(session, client, [(order, D('50'))]))
    invoice.turn_on_rows()
    session.expire_all()
    co, = invoice.consumes

    assert cashback.bonus == DU('0', 'QTY')
    assert co.current_qty == DU('150', 'QTY')
    assert co.current_cashback_bonus == DU('100', 'QTY')

    do_complete(order, D('50'))
    assert co.completion_qty == DU('50', 'QTY')
    assert co.completion_cashback_bonus == DU('33.3333', 'QTY')

    do_complete(order, D('150'))
    assert co.completion_qty == DU('150', 'QTY')
    assert co.completion_cashback_bonus == DU('100', 'QTY')

    assert co.cashback_base == DU('50', 'QTY')
    assert co.cashback_bonus == DU('100', 'QTY')


def test_negative_cashback(session, client, invoice):
    cashback = create_cashback(session, client, bonus=D('-0.1'))
    invoice.turn_on_rows()
    session.expire_all()

    co = invoice.consumes[0]
    assert co.cashback_base is None
    assert co.cashback_bonus is None
    assert co.current_qty == DU('100', 'QTY')  # qty + promo_code + cashback

    assert cashback.bonus == DU('-0.1', 'QTY')


def test_w_fish_mixed(session, client, person):
    cashback = create_cashback(session, client, bonus=D('100'), iso_currency='RUB')

    order_rub_1 = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    order_fish = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)
    order_rub_2 = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)

    request = create_request(
        session,
        client,
        [
            (order_rub_1, D('10')),
            (order_rub_2, D('10')),
            (order_fish, D('10')),
        ]
    )
    invoice = create_invoice(session, client, person, request)

    invoice.turn_on_rows()
    session.expire_all()

    assert invoice.current_qty() == DU('97.777711', 'QTY')
    assert cashback.bonus == DU('0.00007', 'QTY')

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            hm.has_properties(
                current_qty=DU('43.3333', 'QTY'),
                cashback_base=DU('10', 'QTY'),
                cashback_bonus=DU('33.3333', 'QTY'),
                cashback_usage=hm.has_properties(
                    consume_qty=D('43.3333'),
                )
            ),
            hm.has_properties(
                current_qty=DU('11.111111', 'QTY'),
                cashback_base=DU('10', 'QTY'),
                cashback_bonus=DU('1.111111', 'QTY'),
                cashback_usage=hm.has_properties(
                    consume_qty=D('333.33333'),
                )
            ),
            hm.has_properties(
                current_qty=DU('43.3333', 'QTY'),
                cashback_base=DU('10', 'QTY'),
                cashback_bonus=DU('33.3333', 'QTY'),
                cashback_usage=hm.has_properties(
                    consume_qty=D('43.3333'),
                )
            ),
        ),
    )


def test_w_fish_multiple(session, client, person):
    cashback = create_cashback(session, client, bonus=D('100'), iso_currency='RUB')
    orders = [create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID) for _ in range(3)]

    request = create_request(
        session,
        client,
        [(o, D('10')) for o in orders]
    )
    invoice = create_invoice(session, client, person, request)

    invoice.turn_on_rows()
    session.expire_all()

    assert cashback.bonus == DU('0.00001', 'QTY')

    hm.assert_that(
        invoice.consumes,
        hm.contains(*[
            hm.has_properties(
                parent_order_id=o.id,
                current_qty=DU('11.111111', 'QTY'),
                cashback_base=DU('10', 'QTY'),
                cashback_bonus=DU('1.111111', 'QTY'),
                cashback_usage=hm.has_properties(
                    consume_qty=D('333.33333'),
                )
            )
            for o in orders
        ]),
    )


def test_w_different_services(session, client, person):
    cashback_1 = create_cashback(session, client, bonus=D('333.3333'), iso_currency='RUB', service_id=cst.ServiceId.TAXI_CASH)
    cashback_2 = create_cashback(session, client, bonus=D('666.6666'), iso_currency='RUB', service_id=cst.ServiceId.TAXI_CARD)

    order_1 = create_order(session, client, cst.ServiceId.TAXI_CASH, product_iso_currency='RUB')
    order_2 = create_order(session, client, cst.ServiceId.TAXI_CARD, product_iso_currency='RUB')
    order_3 = create_order(session, client, cst.ServiceId.TAXI_CASH, product_iso_currency='RUB')

    request = create_request(session, client, [(order_1, D('10')), (order_2, D('10')), (order_3, D('10'))])
    invoice = create_invoice(session, client, person, request)

    invoice.turn_on_rows()
    session.expire_all()

    assert invoice.current_qty() == DU('1029.9998', 'QTY')
    assert cashback_1.bonus == DU('0.0001', 'QTY')  # из-за округлений
    assert cashback_2.bonus == DU('0', 'QTY')

    hm.assert_that(
        invoice.consumes,
        hm.contains(
            hm.has_properties({
                'current_qty': DU('176.6666', 'QTY'),
                'cashback_base': DU('10', 'QTY'),
                'cashback_bonus': DU('166.6666', 'QTY')
            }),
            hm.has_properties({
                'current_qty': DU('676.6666', 'QTY'),
                'cashback_base': DU('10', 'QTY'),
                'cashback_bonus': DU('666.6666', 'QTY')
            }),
            hm.has_properties({
                'current_qty': DU('176.6666', 'QTY'),
                'cashback_base': DU('10', 'QTY'),
                'cashback_bonus': DU('166.6666', 'QTY')
            }),
        ),
    )


def test_product_quasi_currency(session, client):
    session.config.__dict__['CASHBACK_TURNON_SPLIT_AMOUNTS'] = {}
    cashback = create_cashback(session, client, iso_currency='BYN', bonus=D('50'))
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_QUASI_BYN_ID)
    invoice = create_invoice(
        session,
        client,
        create_person(session, client),
        create_request(session, client, [(order, D('100'))])
    )
    invoice.turn_on_rows()
    session.expire_all()

    assert cashback.bonus == D('0')
    hm.assert_that(
        invoice.consumes,
        hm.contains(hm.has_properties({
            'parent_order_id': order.id,
            'current_qty': D('150'),
            'current_sum': D('3525.06'),
            'current_cashback_bonus': D('50'),
            'cashback_usage': hm.has_properties(consume_qty=D('150'))
        })),
    )


def test_wo_iso_currency_not_allowed(session, client):
    session.config.__dict__['CASHBACK_TURNON_SPLIT_AMOUNTS'] = {}
    cashback = create_cashback(session, client, iso_currency=None, bonus=D('50'), service_id=cst.ServiceId.DIRECT)
    product = ob.ProductBuilder.construct(session)
    order = create_order(session, client, service_id=cst.ServiceId.DIRECT, product_id=product.id)
    invoice = create_invoice(
        session,
        client,
        create_person(session, client),
        create_request(session, client, [(order, D('100'))])
    )
    invoice.turn_on_rows()

    assert cashback.bonus == D('50')
    hm.assert_that(
        invoice.consumes,
        hm.contains(hm.has_properties({
            'parent_order_id': order.id,
            'current_qty': D('100'),
            'current_sum': D('10000'),
            'current_cashback_bonus': D('0')
        })),
    )


def test_wo_iso_currency_allowed(session, client):
    session.config.__dict__['CASHBACK_TURNON_SPLIT_AMOUNTS'] = {}
    cashback = create_cashback(session, client, iso_currency=None, bonus=D('50'), service_id=cst.ServiceId.MARKET)
    order = create_order(session, client, service_id=cst.ServiceId.MARKET, product_id=cst.MARKET_FISH_PRODUCT_ID)
    invoice = create_invoice(
        session,
        client,
        create_person(session, client),
        create_request(session, client, [(order, D('100'))])
    )
    invoice.turn_on_rows()
    session.expire_all()

    assert cashback.bonus == D('0')
    hm.assert_that(
        invoice.consumes,
        hm.contains(hm.has_properties({
            'parent_order_id': order.id,
            'current_qty': D('150'),
            'current_sum': D('3000'),
            'current_cashback_bonus': D('50')
        })),
    )


def test_generate_act(session, client, order):
    cashback = create_cashback(session, client, iso_currency='RUB', bonus=D('50'))
    invoice = create_invoice(
        session,
        client,
        create_person(session, client),
        create_request(session, client, [(order, D('100'))]),
    )
    invoice.turn_on_rows()
    do_complete(order, D('150'))
    act, = invoice.generate_act(force=1)
    session.expire_all()
    co, = invoice.consumes

    assert cashback.bonus == D('0')
    hm.assert_that(
        co,
        hm.has_properties({
            'current_qty': D('150'),
            'completion_qty': D('150'),
            'act_qty': D('150'),
            'act_cashback_bonus': D('50')
        }),
    )


def test_auto_overdraft(session, client):
    """Не зачисляем кешбеки на автоовердрафт
    """
    cashback = create_cashback(session, client, iso_currency='RUB', bonus=D('50'), service_id=cst.ServiceId.DIRECT)
    order = create_order(session, client, service_id=cst.ServiceId.DIRECT, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    invoice = create_invoice(
        session,
        client,
        create_person(session, client),
        create_request(session, client, [(order, D('100'))]),
        overdraft=True,
        is_auto_overdraft=True,
    )
    invoice.turn_on_rows()

    assert cashback.bonus == D('50')
    hm.assert_that(
        invoice.consumes,
        hm.contains(hm.has_properties({
            'parent_order_id': order.id,
            'current_qty': D('100'),
            'current_cashback_bonus': D('0')
        })),
    )


@pytest.mark.parametrize(
    'param, flag',
    [
        pytest.param('overdraft', 'CASHBACK_ALLOW_OVERDRAFT', id='overdraft'),
        pytest.param('credit', 'CASHBACK_ALLOW_CREDIT', id='credit'),
    ],
)
@pytest.mark.parametrize(
    'flag_val',
    [True, False],
)
def test_different_invoices(session, client, param, flag, flag_val):
    session.config.set(flag, flag_val)
    cashback = create_cashback(session, client, iso_currency='RUB', bonus=D('50'), service_id=cst.ServiceId.DIRECT)
    order = create_order(session, client, service_id=cst.ServiceId.DIRECT, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    person = create_person(session, client)

    if param == 'overdraft':
        request = create_request(session, client, [(order, D('100'))])
        invoice = create_invoice(
            session,
            client,
            person,
            request,
            **{param: True}
        )
        invoice.turn_on_rows()
    else:
        contract = create_contract(session, client, person)
        request = create_request(session, client, [(order, D('100'))])
        invoice, = core.Core(session).pay_on_credit(
            request_id=request.id,
            paysys_id=1003,  # банк юрики
            person_id=contract.person_id,
            contract_id=contract.id
        )

    session.expire_all()
    co, = invoice.consumes

    assert cashback.bonus == (D('0') if flag_val else D('50'))
    assert co.current_cashback_bonus == (D('50') if flag_val else D('0'))


def test_consumed_bonus_w_fish(session, client):
    cashback = create_cashback(session, client, iso_currency='RUB', bonus=D('666.6666'), service_id=cst.ServiceId.DIRECT)
    order_rub = create_order(session, client, service_id=cst.ServiceId.DIRECT, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    order_fish = create_order(session, client, service_id=cst.ServiceId.DIRECT, product_id=cst.DIRECT_PRODUCT_ID)
    invoice = create_invoice(
        session,
        client,
        create_person(session, client),
        create_request(session, client, [(order_rub, D('1')), (order_fish, D('1'))])
    )
    invoice.turn_on_rows()
    session.expire_all()

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('666.6666')
    assert free_bonus == D('666.6666')
    assert cashback.bonus == D('0')


def test_consumed_bonus_in_different_invoices(session, client):
    cashback = create_cashback(session, client, iso_currency='RUB', bonus=D('0'), service_id=cst.ServiceId.DIRECT)
    bonuses = [D('1'), D('66.6666'), D('0.00001'), D('1000'), D('0.00669')]

    for bonus in bonuses:
        cashback.bonus += bonus
        session.flush()

        order_rub = create_order(session, client, service_id=cst.ServiceId.DIRECT, product_id=cst.DIRECT_PRODUCT_RUB_ID)
        invoice = create_invoice(
            session,
            client,
            create_person(session, client),
            create_request(session, client, [(order_rub, D('1'))])
        )
        invoice.turn_on_rows()
        session.refresh(cashback)

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('1067.67330')
    assert free_bonus == D('1067.67330')


def test_consumed_bonus_w_promocode(session, client):
    pc = create_promo_code(session, D('666.667'))
    reserve_promo_code(client, pc)
    cashback = create_cashback(session, client, iso_currency='RUB', bonus=D('1234.56789'), service_id=cst.ServiceId.DIRECT)

    invoice = create_invoice(session, client)
    invoice.turn_on_rows(apply_promocode=True)
    session.refresh(cashback)

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('1234.567800')
    assert free_bonus == D('1234.567800')
    assert cashback.bonus == D('0.00009')


def test_consumed_bonus_completed(session, client):
    """Считается весь кешбек в конзюмах, без разделения на откручено/заакчено
    """
    cashback = create_cashback(session, client, iso_currency='RUB', bonus=D('123.456'), service_id=cst.ServiceId.DIRECT)
    invoice = create_invoice(session, client)
    invoice.turn_on_rows(apply_promocode=True)
    session.refresh(cashback)

    order, = [co.order for co in invoice.consumes]
    do_complete(order, D('100'))
    invoice.generate_act(force=True)

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('123.456')
    assert free_bonus == D('68.207539')
    assert cashback.bonus == D('0')


def test_consumed_bonus_overacted(session, client):
    """Считаем свободные средства по максимуму из открученного и заакченного"""
    cashback = create_cashback(session, client, iso_currency='RUB', bonus=D('123.456'), service_id=cst.ServiceId.DIRECT)
    invoice = create_invoice(session, client)
    invoice.turn_on_rows(apply_promocode=True)
    session.refresh(cashback)

    order, = [co.order for co in invoice.consumes]
    do_complete(order, D('100'))
    invoice.generate_act(force=True)
    do_complete(order, D('50'))

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('123.456')
    assert free_bonus == D('68.207539')
    assert cashback.bonus == D('0')


def test_consumed_bonus_completions_only(session, client):
    """Считаем свободные средства по открученному"""
    cashback = create_cashback(session, client, iso_currency='RUB', bonus=D('123.456'), service_id=cst.ServiceId.DIRECT)
    invoice = create_invoice(session, client)
    invoice.turn_on_rows(apply_promocode=True)
    session.refresh(cashback)

    order, = [co.order for co in invoice.consumes]
    do_complete(order, D('50'))

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('123.456')
    assert free_bonus == D('95.831770')
    assert cashback.bonus == D('0')


def test_consumed_bonus_for_market(session, client):
    cashback = create_cashback(session, client, iso_currency=None, bonus=D('123.4567891'), service_id=cst.ServiceId.MARKET)
    order = create_order(session, client, service_id=cst.ServiceId.MARKET, product_id=cst.MARKET_FISH_PRODUCT_ID)
    invoice = create_invoice(
        session,
        client,
        create_person(session, client),
        create_request(session, client, [(order, D('1'))])
    )
    invoice.turn_on_rows()
    session.refresh(cashback)

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('123.456789')
    assert free_bonus == D('123.456789')
    assert cashback.bonus == D('0.0000001')
