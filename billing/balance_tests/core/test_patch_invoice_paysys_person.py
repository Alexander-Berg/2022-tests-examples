# -*- coding: utf-8 -*-
import pytest
import decimal
import datetime
import mock
D = decimal.Decimal

from balance import exc
from balance import mapper
from balance.constants import InvoiceTransferStatus

from tests import object_builder as ob
from tests.balance_tests.core.core_common import (
    _init_invoice,
    _patch_discount,
    _create_contract,
    _create_person,
    create_y_invoice,
    AUCTION_UNIT_ID
)


@pytest.mark.parametrize(
    'new_paysys_id',
    [1033],
    ids=['bank->card']
)
def test_change_paysys(session, core_obj, client, paysys, new_paysys_id):
    invoice = _init_invoice(
        session, client, paysys,
        unit_id=AUCTION_UNIT_ID,
        price=123,
        orders_qtys=[D('66.666666')],
        discount_pct=10,
    )

    with _patch_discount(10):
        core_obj.patch_invoice_paysys_person(invoice.id, new_paysys_id, invoice.person_id, None)

    assert new_paysys_id == invoice.paysys_id
    assert D('7380') == invoice.effective_sum

    invoice_order, = invoice.invoice_orders
    assert D('7380') == invoice_order.amount
    assert 123 == invoice_order.price
    assert 10 == invoice_order.discount_pct
    assert D('66.666666') == invoice_order.quantity
    assert D('66.666666') == invoice_order.initial_quantity


@pytest.mark.parametrize(
    'new_paysys_id',
    [1011, 1025],
    ids=[
        'currency',
        'residence'
    ]
)
def test_change_paysys_forbidden(session, core_obj, client, paysys, new_paysys_id):
    invoice = _init_invoice(session, client, paysys)

    with pytest.raises(exc.CANNOT_PATCH_INVOICE_CURRENCY_OR_NDS):
        core_obj.patch_invoice_paysys_person(invoice.id, new_paysys_id, invoice.person_id, None)


def test_change_person(session, core_obj, client, paysys):
    invoice = _init_invoice(
        session, client, paysys,
        unit_id=AUCTION_UNIT_ID,
        price=123,
        orders_qtys=[D('66.666666')],
        discount_pct=10,
    )

    new_person = _create_person(client)
    with _patch_discount(10):
        core_obj.patch_invoice_paysys_person(invoice.id, invoice.paysys_id, new_person.id, None)

    assert new_person.id == invoice.person_id
    assert D('7380') == invoice.effective_sum

    invoice_order, = invoice.invoice_orders
    assert D('7380') == invoice_order.amount
    assert 123 == invoice_order.price
    assert 10 == invoice_order.discount_pct
    assert D('66.666666') == invoice_order.quantity
    assert D('66.666666') == invoice_order.initial_quantity


def test_set_contract(session, core_obj, client, paysys):
    invoice = _init_invoice(
        session, client, paysys,
        unit_id=AUCTION_UNIT_ID,
        price=123,
        orders_qtys=[D('66.666666')],
        discount_pct=10,
    )

    contract = _create_contract(session, invoice.person)

    with _patch_discount(10):
        core_obj.patch_invoice_paysys_person(invoice.id, invoice.paysys_id, invoice.person_id, contract.id)

    assert contract.id == invoice.contract_id
    assert D('7380') == invoice.effective_sum

    invoice_order, = invoice.invoice_orders
    assert D('7380') == invoice_order.amount
    assert 123 == invoice_order.price
    assert 10 == invoice_order.discount_pct
    assert D('66.666666') == invoice_order.quantity
    assert D('66.666666') == invoice_order.initial_quantity


def test_unset_contract(session, core_obj, client, paysys):
    person = _create_person(client)
    contract = _create_contract(session, person)
    invoice = _init_invoice(
        session, client, paysys,
        contract=contract,
        unit_id=AUCTION_UNIT_ID,
        price=123,
        orders_qtys=[D('66.666666')],
        discount_pct=10,
    )

    with _patch_discount(10):
        core_obj.patch_invoice_paysys_person(invoice.id, invoice.paysys_id, invoice.person_id, None)

    assert invoice.contract_id is None
    assert D('7380') == invoice.effective_sum

    invoice_order, = invoice.invoice_orders
    assert D('7380') == invoice_order.amount
    assert 123 == invoice_order.price
    assert 10 == invoice_order.discount_pct
    assert D('66.666666') == invoice_order.quantity
    assert D('66.666666') == invoice_order.initial_quantity


@pytest.mark.parametrize(
    'qty, old_price, old_discount, new_price, new_discount, req_qty, req_sum',
    [
        pytest.param(D('6.666667'), D('66.67'), 15, D('66.67'), 15, D('6.666667'), D('377.8'), id='new-unchanged'),
        pytest.param(D('6.666667'), D('66.67'), 15, D('42'), 15, D('10.582540'), D('377.8'), id='new-price'),
        pytest.param(D('6.666667'), D('66.67'), 15, D('66.67'), 10, D('6.296297'), D('377.8'), id='new-discount'),
        pytest.param(D('6.666667'), D('66.67'), 15, D('42'), 10, D('9.994621'), D('377.8'), id='new-price-discount'),
    ]
)
def test_change_params(session, core_obj, client, paysys,
                       qty, old_price, old_discount,
                       new_price, new_discount,
                       req_qty, req_sum):
    person = _create_person(client)
    contract_old = _create_contract(session, person)
    contract_new = _create_contract(session, person)

    invoice = _init_invoice(
        session, client, paysys,
        contract=contract_old,
        unit_id=AUCTION_UNIT_ID,
        price=old_price,
        orders_qtys=[qty],
        discount_pct=old_discount,
    )
    invoice_order, = invoice.invoice_orders
    price_obj, = invoice_order.product.prices
    price_obj.price = new_price
    invoice.dt = invoice.dt + datetime.timedelta(1)  # обманываем кеширование цены
    session.flush()

    with _patch_discount(new_discount):
        core_obj.patch_invoice_paysys_person(invoice.id, invoice.paysys_id, invoice.person_id, contract_new.id)
    session.expire_all()

    assert contract_new == invoice.contract
    assert person == invoice.person
    assert req_sum == invoice.effective_sum
    assert req_sum == invoice_order.amount
    assert new_price == invoice_order.price
    assert new_discount == invoice_order.discount_pct
    assert req_qty == invoice_order.quantity
    assert req_qty == invoice_order.initial_quantity


def test_change_paysys_with_promocode(session, core_obj, client, paysys):
    invoice = _init_invoice(session, client, paysys)
    promocode, = ob.PromoCodeGroupBuilder.construct(session).promocodes
    invoice.promo_code = promocode
    session.flush()

    core_obj.patch_invoice_paysys_person(invoice.id, 1033, invoice.person_id, None)

    assert invoice.paysys_id == 1033
    assert invoice.promo_code == promocode


def test_turnon(session, core_obj, client):
    paysys = session.query(mapper.Paysys).get(1000)  # yamoney

    invoice = _init_invoice(session, client, paysys, price=1, orders_qtys=[100])
    invoice.receipt_sum_1c = 100
    session.flush()

    core_obj.patch_invoice_paysys_person(invoice.id, 1001, invoice.person_id, None)

    assert invoice.paysys_id == 1001
    assert invoice.receipt_sum == 100
    assert invoice.consume_sum == 100


def test_patch_y_invoice_contract(session, core_obj, client):
    """Проверяем, что у ы счетов нельзя менять договор"""
    person = _create_person(client)
    invoice = create_y_invoice(session, person, qty=19)
    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        core_obj.patch_invoice_paysys_person(invoice.id, invoice.paysys_id, invoice.person_id, None)
    assert exc_info.value.msg == u"Invalid parameter for function: Can't alter person or contract of y_invoice"


def test_patch_y_invoice_person(session, core_obj, client):
    """Проверяем, что у ы счетов нельзя менять плательщика"""
    person = _create_person(client)
    another_person = _create_person(client)
    invoice = create_y_invoice(session, person, qty=19)
    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        core_obj.patch_invoice_paysys_person(invoice.id, invoice.paysys_id, another_person.id, invoice.contract.id)
    assert exc_info.value.msg == u"Invalid parameter for function: Can't alter person or contract of y_invoice"


def test_patch_y_invoice_paysys(session, core_obj, client):
    """Проверяем, что у ы счетов можно менять способ оплаты, но при этом не происходит пересчет сумм"""
    person = _create_person(client)
    invoice = create_y_invoice(session, person, qty=19, paysys_id=1003)
    invoice.update_head_attributes = mock.MagicMock()
    invoice.update_from_calculator = mock.MagicMock()
    invoice.update_bank_id = mock.MagicMock()
    core_obj.patch_invoice_paysys_person(invoice.id, 1033, invoice.person.id, invoice.contract.id)
    assert invoice.paysys_id == 1033
    invoice.update_head_attributes.assert_not_called()
    invoice.update_from_calculator.assert_not_called()
    invoice.update_bank_id.assert_not_called()


@pytest.mark.parametrize(
    'status, res',
    [
        (InvoiceTransferStatus.not_exported, False),
        (InvoiceTransferStatus.exported, False),
        (InvoiceTransferStatus.export_failed, False),
        (InvoiceTransferStatus.in_progress, False),
        (InvoiceTransferStatus.successful, True),
        (InvoiceTransferStatus.failed_unlocked, True),
    ]
)
def test_patch_invoice_transfer_person_src(session, core_obj, client, paysys, status, res):
    """Проверяем, что для счетов, участвующих в переносе средств, менять плательщика нельзя"""
    person_1 = _create_person(client)
    person_2 = _create_person(client)
    src_invoice = _init_invoice(session, client, paysys)
    dst_invoice = _init_invoice(session, client, paysys)
    dst_invoice.person = src_invoice.person
    session.flush()

    invoice_transfer = ob.InvoiceTransferBuilder(
        src_invoice=src_invoice,
        dst_invoice=dst_invoice,
        amount=1
    ).build(session).obj
    invoice_transfer.set_status(status)
    session.flush()

    if res:
        core_obj.patch_invoice_paysys_person(src_invoice.id, src_invoice.paysys_id, person_1.id, 0)
        assert src_invoice.person == person_1
        core_obj.patch_invoice_paysys_person(dst_invoice.id, dst_invoice.paysys_id, person_2.id, 0)
        assert dst_invoice.person == person_2
    else:
        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            core_obj.patch_invoice_paysys_person(src_invoice.id, src_invoice.paysys_id, person_1.id, 0)
        assert exc_info.value.msg == u"Invalid parameter for function: Can't alter person due to invoice transfers"
        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            core_obj.patch_invoice_paysys_person(dst_invoice.id, dst_invoice.paysys_id, person_2.id, 0)
        assert exc_info.value.msg == u"Invalid parameter for function: Can't alter person due to invoice transfers"
