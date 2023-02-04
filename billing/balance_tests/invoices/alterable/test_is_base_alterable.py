# -*- coding: utf-8 -*-
import pytest
from balance import mapper
from balance.providers.invoice_alterable import _is_base_alterable
from tests.balance_tests.invoices.alterable.alterable_common import create_invoice


def test_prepayment_invoice(session):
    """предоплатный нескрытый счет можно менять"""
    invoice = create_invoice(session)
    assert invoice.type == 'prepayment'
    assert _is_base_alterable(invoice)


def test_hidden_invoice(session):
    """скрытые счета менять нельзя"""
    invoice = create_invoice(session, hidden=2)
    assert _is_base_alterable(invoice) is False


def test_invoice_status_temp(session):
    """временные счета нельзя менять"""
    invoice = create_invoice(session)
    invoice.status_id = 5
    session.flush()
    assert _is_base_alterable(invoice) is False


def test_repayment(session, invoice):
    """Счета на погашение менять нельзя"""
    invoice.type = 'repayment'
    invoice.credit = 1
    invoice.__class__ = mapper.RepaymentInvoice
    session.flush()
    session.expire_all()
    assert _is_base_alterable(invoice) is False


@pytest.mark.parametrize('w_repayment', [True, False])
def test_fictive(session, invoice, w_repayment):
    """Счета на погашение менять нельзя"""
    invoice.type = 'fictive'
    invoice.credit = 1
    invoice.__class__ = mapper.FictiveInvoice
    if w_repayment:
        repayment_invoice = create_invoice(session)
        invoice.type = 'repayment'
        invoice.credit = 1
        invoice.__class__ = mapper.RepaymentInvoice
        invoice.repayments = [repayment_invoice]

    session.flush()
    session.expire_all()
    if w_repayment:
        assert _is_base_alterable(invoice) is False
    else:
        assert _is_base_alterable(invoice) is True
