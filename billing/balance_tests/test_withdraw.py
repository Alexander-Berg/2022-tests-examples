# -*- coding: utf-8 -*-

import decimal

import pytest
import hamcrest as hm

from balance import exc
from balance.actions.withdraw import Withdraw
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.actions.consumption import reverse_consume
from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
    TransferMode,
)

from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_invoice,
    create_order,
)

D = decimal.Decimal
PAYSYS_ID_YAMONEY = 1000
PAYSYS_ID_BANK = 1001


@pytest.fixture
def client(session):
    return ob.ClientBuilder.construct(session)


@pytest.mark.parametrize(
    'paysys_id, receipt_changed',
    [
        pytest.param(PAYSYS_ID_YAMONEY, 1, id='instant'),
        pytest.param(PAYSYS_ID_BANK, 0, id='bank'),
    ]
)
def test_receipt_sum(session, paysys_id, receipt_changed):
    invoice = ob.InvoiceBuilder.construct(session, paysys_id=paysys_id)
    InvoiceTurnOn(invoice, manual=True).do()

    Withdraw(invoice).do(100, forced=True)

    assert invoice.consume_sum == invoice.effective_sum - 100
    assert invoice.receipt_sum == invoice.effective_sum - (100 if receipt_changed else 0)


def test_receipt_sum_instant_returned(session):
    invoice = ob.InvoiceBuilder.construct(session, paysys_id=PAYSYS_ID_YAMONEY)
    invoice.turn_on_rows()
    invoice.create_receipt(40)

    Withdraw(invoice).do(100, forced=True)

    assert invoice.consume_sum == invoice.effective_sum - 100
    assert invoice.receipt_sum == 0


def test_full_with_completions(session):
    invoice = create_invoice(session, qty=10, product_id=DIRECT_PRODUCT_RUB_ID)
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows()
    consume, = invoice.consumes

    consume.order.do_process_completion(D('0.004'))
    session.flush()

    Withdraw(invoice).do()

    assert invoice.consume_sum == 0
    assert consume.current_qty == D('0.004')


def test_full_with_completions_iterative(session):
    # Проверка что снятиями до откруток нельзя бесконечно увеличивать зачисленное количество без смены зачисленной суммы
    # https://st.yandex-team.ru/BALANCE-24212

    invoice = create_invoice(session, qty=10, product_id=DIRECT_PRODUCT_RUB_ID)
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows()
    consume, = invoice.consumes

    consume.order.do_process_completion(D('0.004'))
    session.flush()

    Withdraw(invoice).do()
    invoice.transfer(consume.order, TransferMode.all)
    consume.order.do_process_completion(D('0.006'))
    session.flush()
    Withdraw(invoice).do()
    invoice.transfer(consume.order, TransferMode.all)
    consume.order.do_process_completion(D('0.016'))
    session.flush()

    Withdraw(invoice).do()

    hm.assert_that(
        invoice,
        hm.has_properties(
            consume_sum=D('0.02'),
            receipt_sum=10,
        )
    )
    hm.assert_that(
        consume,
        hm.has_properties(
            current_qty=D('0.016'),
            current_sum=D('0.02'),
            completion_qty=D('0.016'),
            completion_sum=D('0.02'),
        )
    )


@pytest.mark.parametrize(
    'consume_qty, completion_qty, withdraw_amount, res_sum, res_qty',
    [
        pytest.param(10, 1, D('3.34'), D('6.66'), D('6.66'), id='by_amount'),
        pytest.param(10, D('6.6633'), D('3.34'), D('6.66'), D('6.6633'), id='to_completions'),
    ]
)
def test_part_with_completions(session, consume_qty, completion_qty, withdraw_amount, res_sum, res_qty):
    invoice = create_invoice(session, qty=consume_qty, product_id=DIRECT_PRODUCT_RUB_ID)
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows()
    consume, = invoice.consumes

    consume.order.do_process_completion(completion_qty)
    session.flush()

    Withdraw(invoice).do(amount=withdraw_amount)

    assert invoice.consume_sum == res_sum
    assert consume.current_qty == res_qty


def test_part_with_completions_not_enough(session):
    invoice = create_invoice(session, qty=10, product_id=DIRECT_PRODUCT_RUB_ID)
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows()
    consume, = invoice.consumes

    consume.order.do_process_completion(6)
    session.flush()

    with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_WITHDRAW):
        Withdraw(invoice).do(amount=5)


@pytest.mark.parametrize(
    'feature, res_qty',
    [
        pytest.param(False, 0, id='wo_check'),
        pytest.param(True, 10, id='w_check'),
    ]
)
def test_full_overact(session, feature, res_qty):
    session.config.__dict__['CHECK_SUBCLIENTS_OVERACT_WITHDRAW'] = feature

    invoice = create_invoice(session, qty=10, product_id=DIRECT_PRODUCT_RUB_ID)
    invoice.create_receipt(666)
    invoice.turn_on_rows()
    overact_consume, = invoice.consumes

    overact_consume.order.do_process_completion(10)
    invoice.generate_act(force=1)
    reverse_consume(overact_consume, None, 10)

    order = create_order(invoice.client)
    consume = invoice.transfer(order, TransferMode.dst, 17).consume
    order.do_process_completion(1)

    Withdraw(invoice).do()

    assert invoice.consume_sum == res_qty
    assert consume.current_qty == res_qty


@pytest.mark.parametrize(
    'feature, withdraw_sum, is_ok',
    [
        pytest.param(False, 7, True, id='wo_check_ok'),
        pytest.param(False, 8, True, id='wo_check_overact'),
        pytest.param(False, 18, False, id='wo_check_very_overact'),
        pytest.param(True, 7, True, id='w_check_ok'),
        pytest.param(True, 8, False, id='w_check_overact'),
    ]
)
def test_partial_overact(session, feature, withdraw_sum, is_ok):
    session.config.__dict__['CHECK_SUBCLIENTS_OVERACT_WITHDRAW'] = feature

    invoice = create_invoice(session, qty=10, product_id=DIRECT_PRODUCT_RUB_ID)
    invoice.create_receipt(666)
    invoice.turn_on_rows()
    overact_consume, = invoice.consumes

    overact_consume.order.do_process_completion(10)
    invoice.generate_act(force=1)
    reverse_consume(overact_consume, None, 10)

    order = create_order(invoice.client)
    consume = invoice.transfer(order, TransferMode.dst, 17).consume

    if is_ok:
        Withdraw(invoice).do(withdraw_sum)

        assert invoice.consume_sum == 17 - withdraw_sum
        assert consume.current_qty == 17 - withdraw_sum
    else:
        with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_WITHDRAW):
            Withdraw(invoice).do(withdraw_sum)


@pytest.mark.parametrize(
    'feature',
    [
        pytest.param(False, id='wo_check'),
        pytest.param(True, id='w_check'),
    ]
)
def test_overact_multiple_subclients(session, feature):
    session.config.__dict__['CHECK_SUBCLIENTS_OVERACT_WITHDRAW'] = feature

    invoice = create_invoice(session, qty=10, product_id=DIRECT_PRODUCT_RUB_ID)
    invoice.create_receipt(666)
    invoice.turn_on_rows()
    overact_consume, = invoice.consumes

    overact_consume.order.do_process_completion(10)
    invoice.generate_act(force=1)
    reverse_consume(overact_consume, None, 10)

    other_client = ob.ClientBuilder.construct(session)
    other_order = create_order(other_client)
    other_consume = invoice.transfer(other_order, TransferMode.dst, 100).consume

    Withdraw(invoice).do()

    assert invoice.consume_sum == 0
    assert other_consume.current_qty == 0
