# -*- coding: utf-8 -*-

import pytest
from contextlib import contextmanager

from balance.actions.promocodes import reserve_promo_code
from tests import object_builder as ob
from tests.balance_tests.core.core_common import _init_invoice
from balance.muzzle_util import D
from balance.exc import UNMODERATED_TRANSFER, PERMISSION_DENIED
from balance.constants import DIRECT_PRODUCT_RUB_ID


@contextmanager
def does_not_raise():
    yield


def test_pay_order_promocode(session, core_obj, client, paysys):
    pg = ob.PromoCodeGroupBuilder.construct(session)
    pc = pg.promocodes[0]
    reserve_promo_code(client, pc)

    invoice = _init_invoice(session, client, paysys)
    io = invoice.invoice_orders[0]
    order = io.order
    core_obj.pay_order_cc(io.order_id, 'co', io.quantity / order.product.unit.type_rate)

    q = order.consumes[0]

    assert not q.discount_pct, 'Promocode should not be applied'


@pytest.mark.parametrize('paysys_cc, allowed', [('co', True), ('ce', True), ('ur', False)])
def test_pay_order_unmoderated(session, core_obj, client, paysys, paysys_cc, allowed):
    invoice_main = _init_invoice(session, client, paysys)
    main_order = invoice_main.invoice_orders[0].order
    main_order.unmoderated = 1

    invoice_child = _init_invoice(session, client, paysys)
    child_order = invoice_child.invoice_orders[0].order
    child_order.unmoderated = 1
    child_order.group_order_id = main_order.id
    session.flush()

    raises = does_not_raise() if allowed else pytest.raises(UNMODERATED_TRANSFER)

    with raises:
        core_obj.pay_order_cc(child_order.id, paysys_cc, D('666') / child_order.product.unit.type_rate)

    if allowed:
        assert child_order.consumes[0].consume_qty == D('666')
        assert not child_order.consumes[0].current_qty
        assert main_order.consumes[0].current_qty == D('666')


def test_pay_order_cc_with_cashback(session, core_obj, client):
    ob.ClientCashbackBuilder.construct(session, client=client, bonus=666)
    order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_RUB_ID)
    core_obj.pay_order_cc(order.id, 'co', 100)
    q = order.consumes[0]
    assert q.cashback_usage_id is None
    assert q.cashback_bonus is None
    assert not q.discount_pct


def test_pay_order_wo_passport_perm(session, core_obj, client, app):
    passport = ob.PassportBuilder.construct(session)
    session = app.new_session(oper_id=passport.passport_id)

    order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_RUB_ID)
    with pytest.raises(PERMISSION_DENIED):
        core_obj.pay_order_cc(order.id, 'co', 100)

    assert len(order.consumes) == 0


def test_pay_order_wo_passport_perm_skip_perm_check(session, core_obj, client, app):
    passport = ob.PassportBuilder.construct(session)
    session = app.new_session(oper_id=passport.passport_id)

    order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_RUB_ID)

    core_obj.pay_order_cc(order.id, 'co', 100, check_perm=False)

    assert order.consumes[0].consume_qty == D('100')
