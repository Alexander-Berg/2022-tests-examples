# -*- coding: utf-8 -*-

import datetime
from dateutil.relativedelta import relativedelta
import pytest
import hamcrest as hm
from decimal import Decimal as D

from butils.decimal_unit import DecimalUnit as DU

from balance import (
    core,
    constants as cst,
    exc,
    mapper,
    muzzle_util as ut,
)
from balance.actions.promocodes.operations import reserve_promo_code
from balance.actions.cashback import get_consumed_bonus
from balance.actions.transfers_qty.interface import (
    TransferMultiple,
    TransferMultipleMedium,
    SrcItem,
    DstItem,
)
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


def _upd_split_amounts(session):
    session.config.__dict__['CASHBACK_TURNON_SPLIT_AMOUNTS'] = [
        {"iso_currency": "BYN", "amount_w_cashback": "30.666", "commentary": None},
        {"iso_currency": "KZT", "amount_w_cashback": "5000.0", "commentary": None},
        {"iso_currency": "RUB", "amount_w_cashback": "300.0",
         "commentary": "для фишек Директа и рублей"}]

    # Выпилить этот флаг после успешно внедрения!
    session.config.__dict__['CASHBACK_TURN_ON_SKIP_CASHBACK4CLIENTS_IN_TRANSFER'] = 1


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
def create_invoice(session, client, person, request_=None):
    request_ = request_ or create_request(session, client, [(create_order(session, client), D('100'))])
    return ob.InvoiceBuilder.construct(
        session,
        request=request_,
        person=person,
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


def mk_cashback_usage_id(cashback):
    return ob.CashbackUsageBuilder.construct(cashback.session, client_cashback=cashback).id


def do_completion(order, qty):
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})
    order.session.flush()


@pytest.mark.parametrize('w_split', [True, False])
@pytest.mark.parametrize('product_id, promo_bonus, promo_pct', [
    (cst.DIRECT_PRODUCT_ID, 1000 * 9999 * 30, '99.99'),
    (cst.DIRECT_PRODUCT_RUB_ID, 1000 * 9999, '99.99'),
    (cst.DIRECT_PRODUCT_RUB_ID, 0, 0),
    (cst.DIRECT_PRODUCT_RUB_ID, '1000', '50'),
    (cst.DIRECT_PRODUCT_ID, 0, 0),
    (cst.DIRECT_PRODUCT_ID, '30000', '50'),
])
def test_transfer_huge_cashback(medium_xmlrpc, session, w_split, promo_bonus,
                                promo_pct, product_id):
    session.config.__dict__['CASHBACK_TURNON_SPLIT_AMOUNTS'] = {}
    if w_split:
        _upd_split_amounts(session)
    client = create_client(session)
    person = create_person(session, client)
    order = create_order(session, client, product_id=product_id)

    if promo_bonus:
        pc = create_promo_code(session, D(promo_bonus))
        reserve_promo_code(client, pc)

    create_cashback(session, client, bonus=D('1300000500'))

    request = create_request(session, client, [(order, D('1200'))])
    invoice = create_invoice(session, client, person, request)

    invoice.turn_on_rows(apply_promocode=True)

    if w_split:
        if len(invoice.consumes) == 1:
            co = invoice.consumes[0]
            assert co.cashback_usage is None
            assert co.discount_obj.promo_code_pct == DU('99.99', '%')
            assert co.discount_pct == DU('99.99', '%')
        else:
            co = [c for c in invoice.consumes if c.cashback_usage is not None][0]
            assert co.discount_obj.promo_code_pct == DU(promo_pct, '%')
            assert co.discount_pct == DU('99.99', '%')
    else:
        co, = invoice.consumes
        assert co.discount_obj.promo_code_pct == DU(promo_pct, '%')
        assert co.discount_pct == DU('99.99', '%')


@pytest.mark.parametrize('w_split', [True, False])
@pytest.mark.parametrize('product_id, promo_bonus, promo_pct', [
    (cst.DIRECT_PRODUCT_ID, 1000 * 9999 * 30, '99.99'),
    (cst.DIRECT_PRODUCT_RUB_ID, 1000 * 9999, '99.99'),
    (cst.DIRECT_PRODUCT_RUB_ID, 0, 0),
    (cst.DIRECT_PRODUCT_RUB_ID, '1000', '50'),
    (cst.DIRECT_PRODUCT_ID, 0, 0),
    (cst.DIRECT_PRODUCT_ID, '30000', '50'),
])
def test_transfer_huge_several_cashbacks(medium_xmlrpc, session, w_split, promo_bonus, promo_pct, product_id):
    session.config.__dict__['CASHBACK_TURNON_SPLIT_AMOUNTS'] = {}
    if w_split:
        _upd_split_amounts(session)
    client = create_client(session)
    person = create_person(session, client)
    order = create_order(session, client, product_id=product_id)

    if promo_bonus:
        pc = create_promo_code(session, D(promo_bonus))
        reserve_promo_code(client, pc)

    create_cashback(session, client, bonus=D('1300000000'))
    finish_dt = ut.trunc_date(datetime.datetime.now() + relativedelta(days=60))
    create_cashback(session, client, bonus=D('500'), finish_dt=finish_dt)

    request = create_request(session, client, [(order, D('1200'))])
    invoice = create_invoice(session, client, person, request)

    invoice.turn_on_rows(apply_promocode=True)

    if len(invoice.consumes) == 1:
        co = invoice.consumes[0]
        assert co.cashback_usage is None
        assert co.discount_obj.promo_code_pct == DU('99.99', '%')
        assert co.discount_pct == DU('99.99', '%')
    elif w_split:
        co1, co2, co3 = invoice.consumes

        assert co1.consume_sum + co2.consume_sum == 300

        assert co1.cashback_usage is not None
        assert co1.discount_obj.promo_code_pct == DU(promo_pct, '%')
        assert DU('0', '%') < co1.discount_pct <= DU('99.99', '%')

        assert co2.cashback_usage is not None
        assert co2.discount_obj.promo_code_pct == DU(promo_pct, '%')
        assert co2.discount_pct == DU('99.99', '%')

        assert co3.cashback_usage is None
        assert co3.discount_obj.promo_code_pct == DU(promo_pct, '%')
        assert co3.discount_pct == DU(promo_pct, '%')
    else:
        co1, co2 = invoice.consumes

        assert co1.cashback_usage is not None
        assert co1.discount_obj.promo_code_pct == DU(promo_pct, '%')
        assert DU('0', '%') < co1.discount_pct <= DU('99.99', '%')

        assert co2.cashback_usage is not None
        assert co2.discount_obj.promo_code_pct == DU(promo_pct, '%')
        assert co2.discount_pct == DU('99.99', '%')


def test_transfer_to_unused_funds(session, client, person, order):
    pc = create_promo_code(session, D('10'))
    reserve_promo_code(client, pc)

    cashback = create_cashback(session, client, bonus=D('1000'))

    request = create_request(session, client, [(order, D('12'))])
    invoice = create_invoice(session, client, person, request)

    invoice.turn_on_rows(apply_promocode=True)
    co, = invoice.consumes
    cb_co = co.cashback_usage
    session.expire_all()

    assert co.current_sum == DU('12', 'FISH')
    assert co.current_qty == DU('1024', 'QTY')  # qty + promo_code + cashback
    assert co.discount_obj.promo_code_pct == D('50')

    # кешбек соответствует 98% скидке
    assert ut.round(co.discount_obj.cashback_relation, 6) == D('0.023438')
    assert cashback.bonus == D('0')
    assert cb_co.consume_qty == D('1024')

    do_completion(order, D('512'))  # откручиваем половину
    assert co.completion_sum == DU('6', 'FISH')
    assert co.completion_qty == DU('512', 'QTY')

    # а теперь переводим половину от оставшегося на беззаказье
    order.transfer(None, mode=cst.TransferMode.src, qty=D('256'))

    session.expire_all()
    assert co.current_sum == DU('9', 'FISH')
    assert co.current_qty == DU('768', 'QTY')

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert cashback.bonus == DU('250', 'QTY')  # бонус ушёл обратно клиенту
    assert total_bonus == D('750')
    assert free_bonus == D('250')
    assert cb_co.consume_qty == D('768')
    assert co.current_cashback_bonus == DU('750', 'QTY')
    assert co.completion_cashback_bonus == DU('500', 'QTY')


def test_iteration_transfer(session, client, person, order):
    cashback = create_cashback(session, client, bonus=D('100'))

    invoice = create_invoice(session, client, person, create_request(session, client, [(order, D('10'))]))
    invoice.turn_on_rows()
    session.expire_all()

    co, = invoice.consumes
    cb_co = co.cashback_usage

    assert co.current_qty == D('110')
    assert cashback.bonus == D('0')

    for co_qty, cashback_qty in [(D('55'), D('50')), (D('0'), D('100'))]:
        for _i in range(55):
            order.transfer(None, mode=cst.TransferMode.src, qty=D('1'))

        session.expire_all()
        assert co.current_qty == co_qty
        assert cb_co.consume_qty == co_qty
        assert cashback.bonus == cashback_qty


def test_iteration_transfer_fish(session, client, person):
    cashback = create_cashback(session, client, bonus=D('100'))

    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)
    request = create_request(session, client, [(order, D('10'))])
    invoice = create_invoice(session, client, person, request)
    invoice.turn_on_rows()
    session.expire_all()

    co, = invoice.consumes
    cb_co = co.cashback_usage

    assert co.current_qty == D('13.333333')
    assert cashback.bonus == D('0.00001')
    assert cb_co.consume_qty == D('399.99999')
    base_qty = co.current_qty
    steps_cnt = 50

    for _i in range(1, steps_cnt + 1):
        prev_qty = base_qty - ut.round(base_qty / 50 * (_i - 1), 6)
        new_qty = base_qty - ut.round(base_qty / 50 * _i, 6)
        order.transfer(None, mode=cst.TransferMode.src, qty=-(new_qty - prev_qty))
        session.expire_all()

        assert co.current_qty == new_qty
        assert cb_co.consume_qty == new_qty * 30

    assert co.current_qty == 0
    assert cb_co.consume_qty == 0
    assert cashback.bonus == D('100')
    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('0')
    assert free_bonus == D('0')


@pytest.mark.parametrize(
    'cashback_bonus, cashback_discount, invoice_transfer, cashback_res, co_qty, co_sum, co_cashback',
    [
        pytest.param(D('100'), (D('1'), D('1')), D('100'), D('50'), D('100'), D('50'), D('50'), id='simple case'),
        pytest.param(D('100'), (D('1'), D('2')), D('100'), D('33.333333'), D('100'), D('33.33'), D('66.6667'),
                     id='round case'),
        pytest.param(D('99'), (D('1'), D('99')), D('100'), D('0'), D('100'), D('1'), D('99'), id='all cashback'),
        pytest.param(D('99'), (D('1'), D('100')), D('101'), D('-1'), D('101'), D('1'), D('100'), id='extra'),
    ],
)
def test_invoice_transfer(session, client, person, order, cashback_bonus, cashback_discount, invoice_transfer,
                          cashback_res, co_qty, co_sum, co_cashback):
    cashback = create_cashback(session, client=client, iso_currency='RUB', bonus=cashback_bonus)
    base, bonus = cashback_discount
    invoice = create_invoice(
        session,
        client,
        person,
        create_request(session, client, [(order, D('1000'))])
    )
    invoice.create_receipt(invoice.effective_sum)

    order_to = create_order(session, client)
    invoice.transfer(
        order_to,
        cst.TransferMode.dst,
        sum=invoice_transfer,
        discount_obj=mapper.DiscountObj().with_cashback(base, bonus, mk_cashback_usage_id(cashback)),
    )
    session.expire_all()

    assert ut.round(cashback.bonus, 6) == cashback_res
    hm.assert_that(
        invoice.consumes,
        hm.contains(hm.has_properties({
            'parent_order_id': order_to.id,
            'current_qty': co_qty,
            'current_sum': co_sum,
            'current_cashback_bonus': co_cashback,
            'cashback_usage': hm.has_properties(consume_qty=co_qty)
        })),
    )


def test_invoice_transfer_to_client_wo_cashback(session, client, invoice):
    cashback = create_cashback(session, create_client(session), bonus=1)
    invoice.create_receipt(invoice.effective_sum)
    order_to = create_order(session, client)

    with pytest.raises(exc.CASHBACK_TRANSFER_DENIED) as exc_info:
        invoice.transfer(
            order_to,
            cst.TransferMode.dst,
            sum=D('1'),
            discount_obj=mapper.DiscountObj().with_cashback(D('1'), D('1'), mk_cashback_usage_id(cashback)),
        )
    assert 'Wrong cashback source' in exc_info.value.msg


def test_invoice_transfer_to_different_subclient(session, agency):
    client_1 = create_client(session, agency)
    client_2 = create_client(session, agency)
    cashback = create_cashback(session, client_1, bonus=D('100'), iso_currency='RUB')
    create_cashback(session, client_2, bonus=D('100'), iso_currency='RUB')

    invoice = create_invoice(
        session,
        agency,
        create_person(session, agency),
        create_request(
            session,
            agency,
            [(create_order(session, client_1, agency=agency), D('1'))],
        ),
    )
    invoice.create_receipt(invoice.effective_sum)
    order_to = create_order(session, client_2)

    with pytest.raises(exc.CASHBACK_TRANSFER_DENIED) as exc_info:
        invoice.transfer(
            order_to,
            cst.TransferMode.dst,
            sum=D('1'),
            discount_obj=mapper.DiscountObj().with_cashback(D('1'), D('1'), mk_cashback_usage_id(cashback)),
        )
    assert 'Wrong cashback source' in exc_info.value.msg


def test_transfer_from_orders(session, client, person):
    cashback = create_cashback(session, client, bonus=D('100'), iso_currency='RUB')

    order_rub = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    order_fish = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)

    request = create_request(session, client, [(order_rub, D('10')), (order_fish, D('10'))])
    invoice = create_invoice(session, client, person, request)

    invoice.turn_on_rows()
    session.expire_all()

    co_rub, = order_rub.consumes
    co_fish, = order_fish.consumes

    assert cashback.bonus == DU('0.00002', 'QTY')
    hm.assert_that(
        co_rub,
        hm.has_properties(
            current_qty=60,
            cashback_bonus=50,
            current_cashback_bonus=50,
            cashback_usage=hm.has_properties(consume_qty=60),
        )
    )
    hm.assert_that(
        co_fish,
        hm.has_properties(
            current_qty=D('11.666666'),
            cashback_bonus=D('1.666666'),
            current_cashback_bonus=D('1.666666'),
            cashback_usage=hm.has_properties(consume_qty=D('349.99998')),
        )
    )

    order_rub.transfer(None, mode=cst.TransferMode.src, qty=D('1'))
    order_fish.transfer(None, mode=cst.TransferMode.src, qty=D('1'))
    session.expire_all()

    assert cashback.bonus == DU('5.119066', 'QTY')
    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('94.880934')
    assert free_bonus == D('94.880934')
    hm.assert_that(
        co_rub,
        hm.has_properties(
            current_qty=59,
            cashback_bonus=50,
            current_cashback_bonus=D('49.1667'),
            cashback_usage=hm.has_properties(consume_qty=59),
        )
    )
    hm.assert_that(
        co_fish,
        hm.has_properties(
            current_qty=D('10.666666'),
            cashback_bonus=D('1.666666'),
            current_cashback_bonus=D('1.523809'),
            cashback_usage=hm.has_properties(consume_qty=D('319.99998')),
        )
    )


@pytest.mark.parametrize(
    'rollback_amount, cash_bonus, rub_qty, rub_sum, rub_bonus, fish_qty, fish_sum, fish_bonus',
    [
        pytest.param(
            D('20'), D('33.333332'), D('26.6667'), D('10'), D('16.6667'), D('2.666666'), D('30'), D('1.666666'),
            id='part rub co'
        ),
        # вот здесь ошибка округления из-за того, что кешбэк для фишек не дает ровно 50.
        pytest.param(D('30'), D('50.00002'), D('0'), D('0'), D('0'), D('2.666666'), D('30'), D('1.666666'),
                     id='all rub co'),
        pytest.param(D('50'), D('83.333334'), D('0'), D('0'), D('0'), D('0.888889'), D('10'), D('0.555556'),
                     id='part fish co'),
        # а здесь вернулся ровно тот кешбэк, который и был изначально.
        pytest.param(D('60'), D('100'), D('0'), D('0'), D('0'), D('0'), D('0'), D('0'), id='all fish co'),
    ],
)
def test_invoice_rollback(session, client, person,
                          rollback_amount, cash_bonus, rub_qty, rub_sum, rub_bonus, fish_qty, fish_sum, fish_bonus):
    cashback = create_cashback(session, client, bonus=D('100'), iso_currency='RUB')

    order_rub = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    order_fish = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)

    request = create_request(session, client, [(order_rub, D('30')), (order_fish, D('1'))])
    invoice = create_invoice(session, client, person, request)
    invoice.turn_on_rows()
    session.expire_all()

    assert cashback.bonus == D('0.00002')

    core.Core(session).invoice_rollback(
        invoice_id=invoice.id,
        amount=rollback_amount,
        unused_funds_lock=cst.InvoiceReceiptLockType.TRANSFER,
        order_id=None,
        strict=True,
    )
    session.expire_all()

    co_rub, = order_rub.consumes
    co_fish, = order_fish.consumes

    assert cashback.bonus == cash_bonus
    hm.assert_that(
        co_rub,
        hm.has_properties(
            current_qty=rub_qty,
            current_sum=rub_sum,
            current_cashback_bonus=rub_bonus,
            cashback_usage=hm.has_properties(consume_qty=rub_qty),
        ),
    )
    hm.assert_that(
        co_fish,
        hm.has_properties(
            current_qty=fish_qty,
            current_sum=fish_sum,
            current_cashback_bonus=fish_bonus,
            cashback_usage=hm.has_properties(consume_qty=fish_qty * 30)
        ),
    )


@pytest.mark.parametrize(
    'type_',
    ['agency', 'client'],
)
def test_transfer_between_same_orders(session, type_):
    session.config.__dict__['ALLOWED_CASHBACK_NOTIFICATIONS'] = 1

    agency = create_agency(session) if type_ == 'agency' else None
    client = create_client(session, agency=agency)
    person = create_person(session, agency or client)
    cashback = create_cashback(session, client, bonus=D('1500'), iso_currency='RUB')

    order_1 = create_order(session, client, agency=agency)
    order_2 = create_order(session, client, agency=agency)

    invoice = create_invoice(
        session,
        agency or client,
        person,
        create_request(session, agency or client, [(order_1, D('10')), (order_2, D('10'))]),
    )
    invoice.manual_turn_on(D('100'))

    co1, = order_1.consumes
    cb_co1 = co1.cashback_usage
    co2, = order_1.consumes
    cb_co2 = co1.cashback_usage

    assert co1.current_qty == DU('760', 'QTY')
    assert cb_co1.consume_qty == 760
    assert co2.current_qty == DU('760', 'QTY')
    assert cb_co2.consume_qty == 760

    order_1.transfer(order_2, mode=cst.TransferMode.src, qty=D('380'))
    session.expire_all()

    assert cashback.bonus == D('0')
    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('1500')
    assert free_bonus == D('1500')
    assert cb_co1.consume_qty == 760
    assert cb_co2.consume_qty == 760
    hm.assert_that(
        order_2.consumes,
        hm.contains_inanyorder(
            hm.has_properties({
                'current_sum': D('10'),
                'current_qty': D('760'),
                'cashback_base': D('10'),
                'cashback_bonus': D('750'),
                'current_cashback_bonus': D('750')
            }),
            hm.has_properties({
                'current_sum': D('5'),
                'current_qty': D('380'),
                'cashback_base': D('10'),
                'cashback_bonus': D('750'),
                'current_cashback_bonus': D('375')
            })
        )
    )
    hm.assert_that(
        order_1.consumes,
        hm.contains(hm.has_properties({
            'current_sum': D('5'),
            'current_qty': D('380'),
            'cashback_base': D('10'),
            'cashback_bonus': D('750'),
            'current_cashback_bonus': D('375')
        })),
    )

    notification = (
        session
            .execute(
            'select * from bo.t_object_notification where opcode = :opcode and object_id in (:id)',
            {'opcode': cst.NOTIFY_CLIENT_CASHBACK_OPCODE, 'id': cashback.id},
        )
            .fetchall()
    )
    assert len(notification) == 1


@pytest.mark.parametrize(
    'direct_transfer',
    [
        pytest.param(True, id='from rub to fish'),
        pytest.param(False, id='from fish to rub'),
    ],
)
def test_transfer_to_fish_order(session, client, person, direct_transfer):
    """При переводе денег на заказ с другим продуктом создается ещё один конзюм
    """
    cashback = create_cashback(session, client, bonus=D('1500'), iso_currency='RUB')

    order_rub = create_order(session, client)
    order_fish = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)

    request = create_request(session, client, [(order_rub, D('10')), (order_fish, D('10'))])
    invoice = create_invoice(session, client, person, request)
    invoice.manual_turn_on(D('1000'))
    session.expire_all()

    assert cashback.bonus == D('0')
    assert order_rub.consumes[0].current_qty == DU('760', 'QTY')
    assert order_fish.consumes[0].current_qty == DU('35', 'QTY')

    if direct_transfer:
        order_rub.transfer(order_fish, mode=cst.TransferMode.src, qty=D('380'))
        session.expire_all()

        assert cashback.bonus == D('-0.00001')
        total_bonus, free_bonus = get_consumed_bonus(cashback)
        assert total_bonus == D('1500.00001')
        assert free_bonus == D('1500.00001')

        hm.assert_that(
            order_rub.consumes,
            hm.contains(hm.has_properties({
                'current_sum': D('5'),
                'current_qty': D('380'),
                'cashback_base': D('10'),
                'cashback_bonus': D('750'),
                'current_cashback_bonus': D('375'),
                'cashback_usage': hm.has_properties(consume_qty=D('760.00001')),
            })),
        )
        hm.assert_that(
            order_fish.consumes,
            hm.contains(
                hm.has_properties({
                    'current_sum': D('300'),
                    'current_qty': D('35'),
                    'cashback_base': D('10'),
                    'cashback_bonus': D('25'),
                    'current_cashback_bonus': D('25'),
                    'cashback_usage': hm.has_properties(consume_qty=D('1050'))
                }),
                hm.has_properties({
                    'current_sum': D('5'),
                    'current_qty': D('12.666667'),
                    'cashback_base': D('10'),
                    'cashback_bonus': D('750'),
                    'current_cashback_bonus': D('12.5'),
                    'cashback_usage': hm.has_properties(consume_qty=D('760.00001'))
                }),
            ),
        )
    else:
        order_fish.transfer(order_rub, mode=cst.TransferMode.src, qty=D('17.5'))
        session.expire_all()

        assert cashback.bonus == D('0')
        total_bonus, free_bonus = get_consumed_bonus(cashback)
        assert total_bonus == D('1500')
        assert free_bonus == D('1500')

        hm.assert_that(
            order_fish.consumes,
            hm.contains(hm.has_properties({
                'current_sum': D('150'),
                'current_qty': D('17.5'),
                'cashback_base': D('10'),
                'cashback_bonus': D('25'),
                'current_cashback_bonus': D('12.5'),
                'cashback_usage': hm.has_properties(consume_qty=D('1050')),
            })),
        )
        hm.assert_that(
            order_rub.consumes,
            hm.contains(
                hm.has_properties({
                    'current_sum': D('10'),
                    'current_qty': D('760'),
                    'cashback_base': D('10'),
                    'cashback_bonus': D('750'),
                    'current_cashback_bonus': D('750'),
                    'cashback_usage': hm.has_properties(consume_qty=D('760')),
                }),
                hm.has_properties({
                    'current_sum': D('150'),
                    'current_qty': D('525'),
                    'cashback_base': D('10'),
                    'cashback_bonus': D('25'),
                    'current_cashback_bonus': D('375'),
                    'cashback_usage': hm.has_properties(consume_qty=D('1050')),
                }),
            ),
        )


def test_from_order_to_unfunds_and_back(session, client, person, order):
    """С заказа переносим средства на беззаказья, а потом обратно.
    Создается ещё один конзюм и на него уже не зачисляются кешбековые средства.
    """
    cashback = create_cashback(session, client, bonus=D('6666'))

    invoice = create_invoice(session, client, person, create_request(session, client, [(order, D('10'))]))

    invoice.manual_turn_on(D('10'))
    co, = order.consumes
    cb_ro = co.cashback_usage
    session.expire_all()

    assert cashback.bonus == D('0')
    assert cb_ro.consume_qty == 6676
    assert co.current_qty == D('6676')
    assert invoice.unused_funds == D('0')

    do_completion(order, D('10'))
    order.transfer(None, mode=cst.TransferMode.all)
    session.expire_all()

    assert cashback.bonus == D('6656.014979')
    assert cb_ro.consume_qty == 10
    assert co.current_qty == D('10')
    assert invoice.unused_funds == DU('9.99', 'FISH')

    invoice.transfer(order, mode=cst.TransferMode.all)
    session.expire_all()

    assert cashback.bonus == D('6656.014979')
    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('9.985021')
    assert free_bonus == D('0')
    assert cb_ro.consume_qty == 10
    assert invoice.unused_funds == D('0')
    hm.assert_that(
        order.consumes,
        hm.contains(
            hm.has_properties({
                'current_sum': D('0.01'),
                'current_qty': D('10'),
                'cashback_base': D('10'),
                'cashback_bonus': D('6666'),
                'current_cashback_bonus': D('9.985')
            }),
            hm.has_properties({
                'current_sum': D('9.99'),
                'current_qty': D('9.99'),
                'cashback_base': None,
                'cashback_bonus': None,
                'current_cashback_bonus': D('0')
            }),
        ),
    )


def test_transfer_w_discount(session, client, person, order):
    cashback = create_cashback(session, client, bonus=D('500'))

    invoice = create_invoice(session, client, person, create_request(session, client, [(order, D('10'))]))
    invoice.manual_turn_on(D('10'))

    do_completion(order, D('255'))
    order.transfer(None, mode=cst.TransferMode.src, qty=D('127.5'), discount_pct=D('50'))
    session.expire_all()

    assert invoice.unused_funds == D('2.5')
    assert cashback.bonus == D('125')

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('375')
    assert free_bonus == D('125')

    co = invoice.consumes[0]
    hm.assert_that(
        co,
        hm.has_properties(
            current_sum=D('7.5'),
            current_qty=D('382.5'),
            current_cashback_bonus=D('375'),
            discount_obj=hm.has_properties(
                cashback_base=10,
                cashback_bonus=500,
            ),
            cashback_usage=hm.has_properties(consume_qty=D('382.5')),
        )
    )


@pytest.mark.parametrize(
    'type_',
    ['agency', 'client'],
)
def test_transfer_to_other_client(session, client, person, order, type_):
    """Разрешен трансфер только внутри одного субклиента
    """
    _upd_split_amounts(session)
    agency = create_agency(session) if type_ == 'agency' else None
    client = create_client(session, agency)

    create_cashback(session, client, bonus=D('10'))

    person = create_person(session, agency or client)
    order = create_order(session, client, agency=agency)
    request = create_request(session, agency or client, [(order, D('10'))])
    invoice = create_invoice(session, agency or client, person, request)
    invoice.manual_turn_on(D('10'))

    order_to = create_order(session, create_client(session, agency=agency), agency=agency)

    with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_TRANSFER) as exc_info:
        order.transfer(order_to, mode=cst.TransferMode.all)


@pytest.mark.parametrize(
    'type_',
    ['agency', 'client'],
)
def test_transfer_to_other_client_order_mode_all_funds(session, client, person, order, type_):
    """Разрешен трансфер только внутри одного субклиента
    """
    _upd_split_amounts(session)
    agency = create_agency(session) if type_ == 'agency' else None
    client = create_client(session, agency)

    create_cashback(session, client, bonus=D('10'))

    person = create_person(session, agency or client)
    order = create_order(session, client, agency=agency)
    request = create_request(session, agency or client, [(order, D('500'))])
    invoice = create_invoice(session, agency or client, person, request)
    invoice.manual_turn_on(D('500'))

    order_to = create_order(session, create_client(session, agency=agency), agency=agency)

    order.transfer(order_to, mode=cst.TransferMode.all)
    assert order_to.consume_qty == D(200)


@pytest.mark.parametrize(
    'type_',
    ['agency', 'client'],
)
def test_transfer_to_other_client_order_mode_all_funds_w_completion(session, client, person, order, type_):
    _upd_split_amounts(session)
    agency = create_agency(session) if type_ == 'agency' else None
    client = create_client(session, agency)

    create_cashback(session, client, bonus=D('10'))
    completed = D(320)

    person = create_person(session, agency or client)
    order = create_order(session, client, agency=agency)
    request = create_request(session, agency or client, [(order, D('500'))])
    invoice = create_invoice(session, agency or client, person, request)
    invoice.manual_turn_on(D('500'))
    do_completion(order, completed)

    order_to = create_order(session, create_client(session, agency=agency), agency=agency)

    order.transfer(order_to, mode=cst.TransferMode.all)
    assert order_to.consume_qty == D(190)


@pytest.mark.parametrize(
    'type_',
    ['agency', 'client'],
)
@pytest.mark.parametrize(
    'enough_qty',
    [True, False],
)
def test_transfer_to_other_client_order_mode_src_funds(session, client, person, order, type_, enough_qty):
    """Разрешен трансфер только внутри одного субклиента
    """
    _upd_split_amounts(session)
    agency = create_agency(session) if type_ == 'agency' else None
    client = create_client(session, agency)

    create_cashback(session, client, bonus=D('10'))

    person = create_person(session, agency or client)
    order = create_order(session, client, agency=agency)
    request = create_request(session, agency or client, [(order, D('500'))])
    invoice = create_invoice(session, agency or client, person, request)
    invoice.manual_turn_on(D('500'))

    order_to = create_order(session, create_client(session, agency=agency), agency=agency)

    t_allowed_qty = D(200.0)
    if enough_qty:
        order.transfer(order_to, mode=cst.TransferMode.src, qty=t_allowed_qty)
        assert order_to.consume_qty == t_allowed_qty
    else:
        with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_TRANSFER) as exc_info:
            order.transfer(order_to, mode=cst.TransferMode.src, qty=t_allowed_qty + D('0.01'))


@pytest.mark.parametrize(
    'splitted',
    [True, False],
)
def test_transfer_to_other_client_check_splitted(session, client, person, order, splitted):
    _upd_split_amounts(session)
    client = create_client(session)
    create_cashback(session, client, bonus=D('10'))

    if splitted:
        to_consume_qty = D(500)
    else:
        to_consume_qty = D(300)
    person = create_person(session, client)
    order = create_order(session, client)
    request = create_request(session, client, [(order, to_consume_qty)])
    invoice = create_invoice(session, client, person, request)
    invoice.manual_turn_on(to_consume_qty)

    cons = [hm.has_properties({
        'parent_order_id': order.id,
        'current_qty': D('310'),
        'current_cashback_bonus': D('10'),
        'cashback_usage': hm.has_properties(consume_qty=D('310')),
    })]
    if splitted:
        cons.extend([hm.has_properties({
            'parent_order_id': order.id,
            'current_qty': D('200'),
            'current_cashback_bonus': D('0'),
            'cashback_usage': None,
        }), ])

    hm.assert_that(invoice.consumes, hm.contains_inanyorder(*cons))


@pytest.mark.parametrize(
    'param_name, param_id',
    [
        pytest.param('service_id', cst.ServiceId.DRIVE, id='different service'),
        pytest.param('product_id', cst.DIRECT_PRODUCT_USD_ID, id='different currency'),
        # продукт Директа без валюты, не денежный и не "фишки" в понимании кешбэка
        pytest.param('product_id', 508862, id='different product'),
    ],
)
def test_transfer_to_other_service(session, client, person, order, param_name, param_id):
    """Разрешен трансфер только внутри одного сервиса & валюты
    """
    create_cashback(session, client, bonus=D('10'))
    invoice = create_invoice(session, client, person, create_request(session, client, [(order, D('10'))]))
    invoice.manual_turn_on(D('10'))

    order_to = create_order(session, client, **{param_name: param_id})

    with pytest.raises(exc.CASHBACK_TRANSFER_DENIED) as exc_info:
        order.transfer(order_to, mode=cst.TransferMode.all)
    assert exc_info.value.msg == 'Transfer with cashback is denied: Services and currency should match.'


def test_transfer_wo_currencies_allowed(session, client, person):
    cashback = create_cashback(session, client, bonus=D('100'), service_id=cst.ServiceId.MARKET, iso_currency=None)

    order = create_order(session, client, service_id=cst.ServiceId.MARKET, product_id=cst.MARKET_FISH_PRODUCT_ID)
    invoice = create_invoice(session, client, person, create_request(session, client, [(order, D('10'))]))
    invoice.turn_on_rows()
    session.expire_all()

    assert cashback.bonus == D('0')

    invoice.create_receipt(D('10000'))
    order_to = create_order(session, client, service_id=cst.ServiceId.MARKET, product_id=cst.MARKET_FISH_PRODUCT_ID)
    order.transfer(order_to, mode=cst.TransferMode.all)
    session.expire_all()

    assert cashback.bonus == D('0')
    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('100')
    assert free_bonus == D('100')
    hm.assert_that(
        invoice.consumes,
        hm.contains(
            hm.has_properties({
                'parent_order_id': order.id,
                'current_sum': D('0'),
                'current_qty': D('0'),
                'cashback_base': D('10'),
                'cashback_bonus': D('100'),
                'current_cashback_bonus': D('0')
            }),
            hm.has_properties({
                'parent_order_id': order_to.id,
                'current_sum': D('300'),
                'current_qty': D('110'),
                'cashback_base': D('10'),
                'cashback_bonus': D('100'),
                'current_cashback_bonus': D('100')
            }),
        ),
    )


def test_transfer_balance_rounding(session, client, person):
    cashback_usage_matcher = hm.has_properties(consume_qty=D('21.1111'))

    cashback = create_cashback(session, client, bonus=D('11.1111'))

    order = create_order(session, client)
    invoice = create_invoice(session, client, person, create_request(session, client, [(order, D('10'))]))
    invoice.create_receipt(10)
    invoice.turn_on_rows()
    session.expire_all()

    assert cashback.bonus == D('0')

    dst_orders = [create_order(session, client) for _ in range(10)]

    TransferMultiple(
        session,
        [SrcItem(D('10'), D('21.1111'), order)],
        [DstItem(1, o) for o in dst_orders]
    ).do()
    session.flush()
    session.expire_all()

    hm.assert_that(
        invoice.consumes,
        hm.contains_inanyorder(
            hm.has_properties({
                'parent_order_id': order.id,
                'current_qty': D('11.1111'),
                'current_cashback_bonus': D('5.8479'),
                'cashback_usage': cashback_usage_matcher,
            }),
            *[
                hm.has_properties({
                    'parent_order_id': o.id,
                    'current_qty': D('1'),
                    'current_cashback_bonus': D('0.5263'),
                    'cashback_usage': cashback_usage_matcher,
                })
                for o in dst_orders
            ]
        ),
    )
    assert cashback.bonus == D('0')
    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('11.1111')
    assert free_bonus == D('11.1111')


def test_transfer_balance_rounding_simple(session, client, person):
    cashback = create_cashback(session, client, bonus=D('10'))

    order = create_order(session, client)
    invoice = create_invoice(session, client, person, create_request(session, client, [(order, D('90'))]))
    invoice.create_receipt(90)
    invoice.turn_on_rows()
    session.expire_all()

    assert cashback.bonus == D('0')

    dst_orders = [create_order(session, client) for _ in range(3)]

    TransferMultiple(
        session,
        [SrcItem(D('100'), D('100'), order)],
        [DstItem(1, o) for o in dst_orders]
    ).do()
    session.expire_all()

    hm.assert_that(
        invoice.consumes,
        hm.contains_inanyorder(
            hm.has_properties({
                'parent_order_id': order.id,
                'current_qty': 0,
                'cashback_usage': hm.has_properties(consume_qty=D('99.9999')),
            }),
            *[
                hm.has_properties({
                    'parent_order_id': o.id,
                    'current_qty': D('33.3333'),
                    'current_cashback_bonus': D('3.3333'),
                    'cashback_usage': hm.has_properties(consume_qty=D('99.9999')),
                })
                for o in dst_orders
            ]
        ),
    )
    assert cashback.bonus == D('0.00001')
    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('9.99999')
    assert free_bonus == D('9.99999')


def test_transfer_balance_rounding_products(session, client, person):
    cashback = create_cashback(session, client, bonus=D('11.1111'))

    order = create_order(session, client)
    invoice = create_invoice(session, client, person, create_request(session, client, [(order, D('10'))]))
    invoice.create_receipt(10)
    invoice.turn_on_rows()
    session.expire_all()

    assert cashback.bonus == D('0')

    dst_orders_rub = [create_order(session, client) for _ in range(5)]
    dst_orders_fish = [create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID) for _ in range(5)]

    TransferMultiple(
        session,
        [SrcItem(D('10'), D('21.1111'), order)],
        [DstItem(1, o) for o in dst_orders_rub + dst_orders_fish]
    ).do()
    session.expire_all()

    hm.assert_that(
        invoice.consumes,
        hm.contains_inanyorder(
            hm.has_properties({
                'parent_order_id': order.id,
                'current_qty': D('11.1111'),
                'current_cashback_bonus': D('5.8479'),
                'cashback_usage': hm.has_properties(consume_qty=D('21.11105')),
            }),
            *([
                  hm.has_properties({
                      'parent_order_id': o.id,
                      'current_qty': D('1'),
                      'current_cashback_bonus': D('0.5263'),
                      'cashback_usage': hm.has_properties(consume_qty=D('21.11105')),
                  })
                  for o in dst_orders_rub
              ] + [
                  hm.has_properties({
                      'parent_order_id': o.id,
                      'current_qty': D('0.033333'),
                      'current_cashback_bonus': D('0.017544'),
                      'cashback_usage': hm.has_properties(consume_qty=D('21.11105')),
                  })
                  for o in dst_orders_fish
              ])
        ),
    )
    assert cashback.bonus == D('0.000026')

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('11.111074')
    assert free_bonus == D('11.111074')


def test_transfer_balance_rounding_products_combine(session, client, person):
    session.config.__dict__['CASHBACK_FORCE_NO_USAGE'] = False

    cashback = create_cashback(session, client, bonus=D('11.1111'))

    orders_rub = [create_order(session, client) for _ in range(5)]
    orders_fish = [create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID) for _ in range(5)]

    request = create_request(
        session,
        client,
        [(o, D('1')) for o in orders_rub]
        + [(o, D('0.033333')) for o in orders_fish]
    )
    invoice = create_invoice(session, client, person, request)
    invoice.create_receipt(10)
    invoice.turn_on_rows()
    session.expire_all()

    assert cashback.bonus == D('0.00005')

    dst_order = create_order(session, client)
    TransferMultiple(
        session,
        [SrcItem(D('2.1111'), D('2.1111'), o) for o in orders_rub]
        + [SrcItem(D('0.07037'), D('0.07037'), o) for o in orders_fish],
        [DstItem(1, dst_order)]
    ).do()
    session.expire_all()

    hm.assert_that(
        [c for c in invoice.consumes if c.current_qty > 0],
        hm.contains_inanyorder(
            *[
                hm.has_properties({
                    'parent_order_id': dst_order.id,
                    'current_qty': D('2.1111'),
                    'current_cashback_bonus': D('1.1111'),
                    'cashback_usage': hm.has_properties(consume_qty=D('2.1111')),
                })
                for _ in orders_rub + orders_fish
            ]
        ),
    )
    assert cashback.bonus == D('0.00005')

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('11.11105')
    assert free_bonus == D('11.11105')


def test_transfer_balance_rounding_combine_rub2fish(session, client, person):
    cashback = create_cashback(session, client, bonus=D('11.1111'))

    orders = [create_order(session, client) for _ in range(10)]

    request = create_request(
        session,
        client,
        [(o, D('1')) for o in orders]
    )
    invoice = create_invoice(session, client, person, request)
    invoice.create_receipt(10)
    invoice.turn_on_rows()
    session.expire_all()

    assert cashback.bonus == D('0.0001')

    dst_order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)
    TransferMultiple(
        session,
        [SrcItem(D('2.1111'), D('2.1111'), o) for o in orders],
        [DstItem(1, dst_order)]
    ).do()
    session.expire_all()

    assert cashback.bonus == D('0.0001')
    hm.assert_that(
        [c for c in invoice.consumes if c.current_qty > 0],
        hm.contains_inanyorder(
            *[
                hm.has_properties({
                    'parent_order_id': dst_order.id,
                    'current_qty': D('0.07037'),
                    'current_cashback_bonus': D('0.037037'),
                    'cashback_usage': hm.has_properties(consume_qty=D('2.1111')),
                })
                for _ in orders
            ]
        ),
    )

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('11.111')
    assert free_bonus == D('11.111')


@pytest.mark.parametrize(
    'compleated, ans_total_bonus, ans_bonus',
    [
        pytest.param(None, D('0'), D('1000'), id='all transfered'),
        pytest.param(D('506'), D('500'), D('500'), id='half completed'),
        pytest.param(D('1011'), D('999.011858'), D('0.988142'), id='almost all completed'),
    ],
)
def test_consumed_bonus_all_transfered(session, client, person, compleated, ans_total_bonus, ans_bonus):
    cashback = create_cashback(session, client, bonus=D('1000'))

    order = create_order(session, client)
    request = create_request(session, client, [(order, D('12'))])
    invoice = create_invoice(session, client, person, request)

    invoice.turn_on_rows()
    session.expire_all()

    if compleated is not None:
        do_completion(order, compleated)
    order.transfer(None, mode=cst.TransferMode.all)

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == ans_total_bonus
    assert free_bonus == 0

    assert cashback.bonus == ans_bonus


@pytest.mark.parametrize(
    'use_transfer_multiple_medium',
    [
        pytest.param(True, id='with TransferMultipleMedium'),
        pytest.param(False, id='with TransferMultiple'),
    ],
)
@pytest.mark.parametrize(
    'with_another_subclient',
    [
        pytest.param(True, id='with another subclient dest_order'),
        pytest.param(False, id='without another subclient dest_order'),
    ],
)
def test_transfer_tm_cases(session, client, person, use_transfer_multiple_medium, with_another_subclient):
    _upd_split_amounts(session)
    cashback = create_cashback(session, client, bonus=D('10'))
    agency = create_agency(session)
    order = create_order(session, client, agency=agency)
    invoice = create_invoice(session, client, person, create_request(session, agency, [(order, D('500'))]))
    invoice.create_receipt(500)
    invoice.turn_on_rows()
    session.expire_all()

    assert cashback.bonus == D('0')

    order_maybe_another_client = create_order(session,
                                              create_client(session,
                                                            agency=agency) if with_another_subclient else client,
                                              agency=agency)

    dst_orders = [create_order(session, client, agency=agency) for _ in range(3)] + [order_maybe_another_client]

    if use_transfer_multiple_medium:
        TransferMultipleMedium(session,
                               [{'ServiceID': order.service_id,
                                 'ServiceOrderID': order.service_order_id,
                                 'QtyOld': order.consume_qty,
                                 'AllQty': True
                                 }],
                               [{'ServiceID': o.service_id,
                                 'ServiceOrderID': o.service_order_id,
                                 'QtyDelta': 1} for o in dst_orders]
                               ).do()
    else:
        TransferMultiple(
            session,
            [SrcItem(D('200') if with_another_subclient else D(510),
                     D('510'), order)],
            [DstItem(1, o) for o in dst_orders]
        ).do()
    session.expire_all()

    hm.assert_that(
        [c for c in invoice.consumes if c.parent_order_id == order.id],
        hm.contains_inanyorder(
            hm.has_properties({
                'parent_order_id': order.id,
                'current_qty': D('310') if with_another_subclient else D(0),
                'current_cashback_bonus': D('10') if with_another_subclient else D(0),
                'cashback_usage': hm.has_properties(consume_qty=D('310')),
            }),
            hm.has_properties({
                'parent_order_id': order.id,
                'current_qty': D('0'),
                'current_cashback_bonus': D('0'),
                'cashback_usage': None,
            }),
        ),
    )

    dst_consumes = [c for c in invoice.consumes if c.parent_order_id != order.id]

    if with_another_subclient:
        dst_cons = [
            hm.has_properties({
                'parent_order_id': o.id,
                'current_qty': D('50'),
                'current_cashback_bonus': D('0'),
                'cashback_usage': None,
            })
            for o in dst_orders
        ]
        hm.assert_that(
            dst_consumes,
            hm.contains_inanyorder(*dst_cons))
    else:
        assert sum(c.current_qty for c in dst_consumes) == D(510)
        assert sum(c.current_cashback_bonus for c in dst_consumes) == D(10)

    assert cashback.bonus == D('0')

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('10')
    assert free_bonus == D('10')


@pytest.mark.parametrize(
    'use_transfer_multiple_medium',
    [
        pytest.param(True, id='with TransferMultipleMedium'),
        pytest.param(False, id='with TransferMultiple'),
    ],
)
@pytest.mark.parametrize(
    'with_another_subclient',
    [
        pytest.param(True, id='with another subclient dest_order'),
        pytest.param(False, id='without another subclient dest_order'),
    ],
)
def test_transfer_tm_cases_w_completed(session, client, person, use_transfer_multiple_medium, with_another_subclient):
    _upd_split_amounts(session)
    cashback = create_cashback(session, client, bonus=D('10'))
    agency = create_agency(session)
    order = create_order(session, client, agency=agency)
    invoice = create_invoice(session, client, person, create_request(session, agency, [(order, D('500'))]))
    invoice.create_receipt(500)
    invoice.turn_on_rows()
    session.expire_all()
    compl_qty = D(300) if with_another_subclient else D(400)
    do_completion(order, compl_qty)

    assert cashback.bonus == D('0')

    order_maybe_another_client = create_order(session,
                                              create_client(session,
                                                            agency=agency) if with_another_subclient else client,
                                              agency=agency)

    dst_orders = [create_order(session, client, agency=agency) for _ in range(3)] + [order_maybe_another_client]

    if use_transfer_multiple_medium:
        TransferMultipleMedium(session,
                               [{'ServiceID': order.service_id,
                                 'ServiceOrderID': order.service_order_id,
                                 'QtyOld': order.consume_qty,
                                 'AllQty': True
                                 }],
                               [{'ServiceID': o.service_id,
                                 'ServiceOrderID': o.service_order_id,
                                 'QtyDelta': 1} for o in dst_orders]
                               ).do()
    else:
        TransferMultiple(
            session,
            [SrcItem(D(200) if with_another_subclient else D(110),
                     D(510), order)],
            [DstItem(1, o) for o in dst_orders]
        ).do()
    session.expire_all()

    hm.assert_that(
        [c for c in invoice.consumes if c.parent_order_id == order.id],
        hm.contains_inanyorder(
            hm.has_properties({
                'parent_order_id': order.id,
                'current_qty': D('310'),
                'current_cashback_bonus': D('10'),
                'cashback_usage': hm.has_properties(consume_qty=D('310')),
            }),
            hm.has_properties({
                'parent_order_id': order.id,
                'current_qty': D('0') if with_another_subclient else D(90),
                'current_cashback_bonus': D('0'),
                'cashback_usage': None,
            }),
        ),
    )

    dst_consumes = [c for c in invoice.consumes if c.parent_order_id != order.id]

    if with_another_subclient:
        dst_cons = [
            hm.has_properties({
                'parent_order_id': o.id,
                'current_qty': D('50') if with_another_subclient else D('27.5'),
                'current_cashback_bonus': D('0'),
                'cashback_usage': None,
            })
            for o in dst_orders
        ]
        hm.assert_that(
            dst_consumes,
            hm.contains_inanyorder(*dst_cons))
    else:
        assert sum(c.current_qty for c in dst_consumes) == (D(200) if with_another_subclient else D(110))
        assert sum(c.current_cashback_bonus for c in dst_consumes) == D(0)

    assert cashback.bonus == D('0')

    total_bonus, free_bonus = get_consumed_bonus(cashback)
    assert total_bonus == D('10')
    if with_another_subclient:
        assert free_bonus == D('0.322581')
    else:
        assert free_bonus == D('0')

@pytest.mark.parametrize(
    'mode',
    [
        pytest.param('order_transfer'),
        pytest.param('transfer_multiple'),
        pytest.param('transfer_multiple_medium'),
    ],
)
def test_transfer_after_transfer(session, client, person, order, mode):
    _upd_split_amounts(session)
    client = create_client(session)

    create_cashback(session, client, bonus=D('10'))

    person = create_person(session, client)
    order = create_order(session, client)
    request = create_request(session, client, [(order, D('300'))])
    invoice = create_invoice(session, client, person, request)
    invoice.manual_turn_on(D('300'))

    order_to = create_order(session, client)

    if mode == 'order_transfer':
        order.transfer(order_to, mode=cst.TransferMode.src, qty=10)
        assert order.consume_qty == D(300)
        order.transfer(order_to, mode=cst.TransferMode.all)
    elif mode == 'transfer_multiple':
        TransferMultiple(
            session,
            [SrcItem(D('10'), D('310'), order)],
            [DstItem(1, order_to)]
        ).do()
        assert order.consume_qty == D(300)
        TransferMultiple(
            session,
            [SrcItem(D('300'), D('300'), order)],
            [DstItem(1, order_to)]
        ).do()
    elif mode == 'transfer_multiple_medium':
        TransferMultipleMedium(session,
                               [{'ServiceID': order.service_id,
                                 'ServiceOrderID': order.service_order_id,
                                 'QtyOld': D(310),
                                 'QtyNew': D(300)
                                 }],
                               [{'ServiceID': order_to.service_id,
                                 'ServiceOrderID': order_to.service_order_id,
                                 'QtyDelta': 1}]
                               ).do()
        assert order.consume_qty == D(300)
        TransferMultipleMedium(session,
                               [{'ServiceID': order.service_id,
                                 'ServiceOrderID': order.service_order_id,
                                 'QtyOld': D(300),
                                 'AllQty': True
                                 }],
                               [{'ServiceID': order_to.service_id,
                                 'ServiceOrderID': order_to.service_order_id,
                                 'QtyDelta': 1}]
                               ).do()

    assert order.consume_qty == D(0)
