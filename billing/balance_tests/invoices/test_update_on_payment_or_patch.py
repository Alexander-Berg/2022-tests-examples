# -*- coding: utf-8 -*-

from tests.balance_tests.invoices.invoice_common import (
    create_invoice,
)

CARD_PAYSYS_ID = 1002
BANK_PAYSYS_ID = 1003


def test_bank_new(session):
    invoice = create_invoice(session, 100, BANK_PAYSYS_ID)
    invoice.receipt_sum_1c = 90
    session.flush()

    invoice.update_on_payment_or_patch()

    assert invoice.receipt_sum == 90
    assert invoice.consume_sum == 0


def test_bank_add_payment(session):
    invoice = create_invoice(session, 100, BANK_PAYSYS_ID)
    invoice.receipt_sum_1c = 90
    invoice.receipt_sum = 70
    session.flush()

    invoice.update_on_payment_or_patch()

    assert invoice.receipt_sum == 90
    assert invoice.consume_sum == 0


def test_bank_return(session):
    invoice = create_invoice(session, 100, BANK_PAYSYS_ID)
    invoice.receipt_sum_1c = 70
    invoice.receipt_sum = 90
    session.flush()

    invoice.update_on_payment_or_patch()

    assert invoice.receipt_sum == 70
    assert invoice.consume_sum == 0


def test_bank_exact_total_sum(session):
    invoice = create_invoice(session, 100, BANK_PAYSYS_ID)
    invoice.receipt_sum_1c = 100
    session.flush()

    invoice.update_on_payment_or_patch()

    assert invoice.receipt_sum == 100
    assert invoice.consume_sum == 100


def test_instant(session):
    invoice = create_invoice(session, 100, CARD_PAYSYS_ID)
    invoice.receipt_sum_1c = 100
    session.flush()

    invoice.update_on_payment_or_patch()

    assert invoice.receipt_sum == 0
    assert invoice.consume_sum == 0
