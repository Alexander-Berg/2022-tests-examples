# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import pytest

from balance import exc
from balance import mapper
from balance import constants as cst
from balance.actions import promocodes

from tests import object_builder as ob
from tests.balance_tests.promocode.common import (
    create_order,
    create_invoice,
    create_promocode,
)

pytestmark = [
    pytest.mark.promo_code,
]


@pytest.fixture
def invoice(session, invoice, promocode):
    invoice.promo_code = promocode
    session.flush()
    return invoice


def extract_consumes(invoice):
    return [
        (
            q.parent_order_id,
            q.current_qty, q.current_sum,
            q.completion_qty, q.completion_sum,
            q.act_qty, q.act_sum,
            q.discount_obj
        )
        for q in invoice.consumes
    ]


def complete(order, qty):
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})
    order.session.flush()


def act(invoice):
    res = invoice.generate_act(force=1, backdate=datetime.datetime.now())
    invoice.session.flush()
    return res


def test_wo_promocode(session, invoice):
    invoice.promo_code = None
    invoice.turn_on_rows()

    with pytest.raises(exc.CANT_TEAR_PC_OFF_NO_PC) as exc_info:
        promocodes.tear_promocode_off(session, invoice)

    assert exc_info.value.msg == "Can't tear promocode off: PC_TEAR_OFF_NO_PC"


def test_all_wo_discount(session, invoice, order):
    invoice.transfer(order, discount_pct=0)

    with pytest.raises(exc.CANT_TEAR_PC_OFF_NO_PC) as exc_info:
        promocodes.tear_promocode_off(session, invoice)

    req_consumes = [
        (order.id, 1000, 1000, 0, 0, 0, 0, mapper.DiscountObj()),
    ]
    assert exc_info.value.msg == "Can't tear promocode off: PC_TEAR_OFF_NO_PC"
    assert extract_consumes(invoice) == req_consumes


def test_allcompleted(session, invoice, order):
    invoice.transfer(order, discount_obj=mapper.DiscountObj(0, 75, invoice.promo_code))
    complete(order, order.consume_qty)

    with pytest.raises(exc.CANT_TEAR_PC_OFF_NO_FREE_CONSUMES) as exc_info:
        promocodes.tear_promocode_off(session, invoice)

    req_consumes = [
        (order.id, 4000, 1000, 4000, 1000, 0, 0, mapper.DiscountObj(0, 75, invoice.promo_code)),
    ]
    assert exc_info.value.msg == "Can't tear promocode off: PC_TEAR_OFF_NO_FREE_CONSUMES"
    assert extract_consumes(invoice) == req_consumes


def test_all_overacted_single_consume(session, invoice, order):
    invoice.transfer(order, discount_obj=mapper.DiscountObj(0, 75, invoice.promo_code))
    complete(order, order.consume_qty)
    act(invoice)
    complete(order, 0)

    with pytest.raises(exc.CANT_TEAR_PC_OFF_NO_FREE_CONSUMES) as exc_info:
        promocodes.tear_promocode_off(session, invoice)

    req_consumes = [
        (order.id, 4000, 1000, 0, 0, 4000, 1000, mapper.DiscountObj(0, 75, invoice.promo_code)),
    ]
    assert exc_info.value.msg == "Can't tear promocode off: PC_TEAR_OFF_NO_FREE_CONSUMES"
    assert extract_consumes(invoice) == req_consumes


def test_all_overacted_other_consume(session, invoice, order):
    invoice.transfer(order, discount_obj=mapper.DiscountObj(0, 75, invoice.promo_code))
    complete(order, order.consume_qty)
    act(invoice)
    complete(order, 0)
    order_alt = create_order(session, order.client, mapper.DIRECT_PRODUCT_RUB_ID)
    order.transfer(order_alt)
    session.flush()

    with pytest.raises(exc.CANT_TEAR_PC_OFF_NO_FREE_CONSUMES) as exc_info:
        promocodes.tear_promocode_off(session, invoice)

    req_consumes = [
        (order.id, 0, 0, 0, 0, 4000, 1000, mapper.DiscountObj(0, 75, invoice.promo_code)),
        (order_alt.id, 4000, 1000, 0, 0, 0, 0, mapper.DiscountObj(0, 75, invoice.promo_code)),
    ]
    assert exc_info.value.msg == "Can't tear promocode off: PC_TEAR_OFF_NO_FREE_CONSUMES"
    assert extract_consumes(invoice) == req_consumes


def test_w_discount(session, invoice, order):
    invoice.transfer(order, cst.TransferMode.dst, 1000, discount_obj=mapper.DiscountObj(0, 50, invoice.promo_code))
    complete(order, 250)

    promocodes.tear_promocode_off(session, invoice)

    req_consumes = [
        (order.id, 250, 125, 250, 125, 0, 0, mapper.DiscountObj(0, 50, invoice.promo_code)),
        (order.id, 375, 375, 0, 0, 0, 0, mapper.DiscountObj()),
    ]
    assert extract_consumes(invoice) == req_consumes
    assert invoice.consume_sum == 500


def test_wo_discount(session, invoice, order):
    invoice.transfer(order, cst.TransferMode.dst, 500, discount_pct=0)
    invoice.transfer(order, cst.TransferMode.dst, 500, discount_obj=mapper.DiscountObj(0, 75, invoice.promo_code))

    promocodes.tear_promocode_off(session, invoice)

    req_consumes = [
        (order.id, 500, 500, 0, 0, 0, 0, mapper.DiscountObj()),
        (order.id, 0,   0,   0, 0, 0, 0, mapper.DiscountObj(0, 75, invoice.promo_code)),
        (order.id, 125, 125, 0, 0, 0, 0, mapper.DiscountObj()),
    ]
    assert extract_consumes(invoice) == req_consumes
    assert invoice.consume_sum == 625


def test_zero_sum(session, invoice, order):
    invoice.transfer(order, cst.TransferMode.dst, 1, discount_pct=0)
    invoice.transfer(
        order,
        cst.TransferMode.dst,
        D('0.001'),
        discount_obj=mapper.DiscountObj(0, 75, invoice.promo_code)
    )

    promocodes.tear_promocode_off(session, invoice)

    req_consumes = [
        (order.id, 1,           1, 0, 0, 0, 0, mapper.DiscountObj()),
        (order.id, 0,           0, 0, 0, 0, 0, mapper.DiscountObj(0, 75, invoice.promo_code)),
        (order.id, D('0.0003'), 0, 0, 0, 0, 0, mapper.DiscountObj()),
    ]
    assert extract_consumes(invoice) == req_consumes
    assert invoice.consume_sum == 1


def test_w_add_discount(session, invoice, order):
    invoice.transfer(
        order,
        cst.TransferMode.dst,
        1000,
        discount_obj=mapper.DiscountObj(10, 50, invoice.promo_code)
    )

    promocodes.tear_promocode_off(session, invoice)

    req_consumes = [
        (order.id, 0, 0, 0, 0, 0, 0, mapper.DiscountObj(10, 50, invoice.promo_code)),
        (order.id, 500, 450, 0, 0, 0, 0, mapper.DiscountObj(10)),
    ]
    assert extract_consumes(invoice) == req_consumes
    assert invoice.consume_sum == 450


@pytest.mark.permissions
@pytest.mark.parametrize(
    'inv_firm_id, role_firm_id, res',
    (
            (cst.FirmId.YANDEX_OOO, None, False),
            (cst.FirmId.YANDEX_OOO, [], True),
            (cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO, True),
            (cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET, False),
    ),
    ids=[
        'user does not have permission',
        'user has permission without constraints',
        'user has permission with right constraint',
        'user has permission with wrong constraint',
    ],
)
def test_check_access_tearing_promocode_off(session, inv_firm_id, role_firm_id, res):
    """Проверяем права пользователя с ограничениями по фирме для отрыва промокода"""
    roles = [ob.create_role(session, cst.PermissionCode.VIEW_INVOICES)]
    if role_firm_id is not None:
        role = ob.create_role(
            session,
            (cst.PermissionCode.TEAR_PROMOCODE_OFF, {cst.ConstraintTypes.firm_id: None}),
        )
        pc_role = (role, {cst.ConstraintTypes.firm_id: role_firm_id}) if role_firm_id else role
        roles.append(pc_role)

    ob.set_roles(session, session.passport, roles)

    invoice = create_invoice(session)
    promocode = create_promocode(session)
    invoice.promo_code = promocode
    invoice.firm_id = inv_firm_id
    session.flush()

    allowed = True
    try:
        promocodes.check_access_tearing_promocode_off(session.passport, invoice)
    except exc.PERMISSION_DENIED:
        allowed = False
    assert allowed == res


@pytest.mark.permissions
def test_check_access_tearing_promocode_off_own_client(session, invoice):
    """У пользователя нет прав, и счёт ему не принадлежит"""
    client_role = session.query(mapper.Role).getone(cst.RoleName.CLIENT)
    ob.set_roles(session, session.passport, [client_role])
    with pytest.raises(exc.PERMISSION_DENIED):
        promocodes.check_access_tearing_promocode_off(session.passport, invoice)


@pytest.mark.cashback
def test_tear_off_w_cashback(session, order, invoice):
    """Отрываем промокод, а кешбэк должен остаться на заказе.
    НО! пока что получается, что заоднем отрываем часть кешбэка...
    """
    cashback = ob.ClientCashbackBuilder.construct(
        session,
        client=order.client,
        service_id=order.service_id,
        iso_currency=order.product_iso_currency,
        bonus=D('500'),
    )
    cashback_usage = ob.CashbackUsageBuilder.construct(session, client_cashback=cashback)
    discount_obj = mapper.DiscountObj(0, 50, invoice.promo_code, cashback_base=D(1), cashback_bonus=D(1), cashback_usage_id=cashback_usage.id)

    invoice.transfer(
        order,
        cst.TransferMode.dst,
        sum=1000,
        discount_obj=discount_obj,
    )
    session.expire_all()

    primary_consumes = [
        (order.id, D('1000'), D('250'), 0, 0, 0, 0, discount_obj),
    ]
    assert extract_consumes(invoice) == primary_consumes
    assert cashback.bonus == D('0')  # весь кешбэк на заказе

    promocodes.tear_promocode_off(session, invoice)
    session.expire_all()

    req_consumes = [
        (order.id, 0, 0, 0, 0, 0, 0, discount_obj),
        (order.id, D('500'), D('250'), 0, 0, 0, 0, discount_obj.without_promo_code()),
    ]
    assert extract_consumes(invoice) == req_consumes
    assert invoice.consume_sum == 250
    assert cashback.bonus == D('250')  # и часть кешбека пока что тоже отрываем
