# -*- coding: utf-8 -*-

from xmlrpclib import Fault
import pytest
from hamcrest import assert_that, equal_to, is_

from tests import object_builder as ob
from tests.tutils import get_exception_code


@pytest.fixture()
def overdraft_invoice(session):
    invoice = ob.InvoiceBuilder(overdraft=1).build(session).obj
    return invoice


@pytest.fixture()
def prepayment_invoice(session):
    invoice = ob.InvoiceBuilder(overdraft=0).build(session).obj
    return invoice


def test_prepayment(session, test_xmlrpc_srv, prepayment_invoice):
    actual_result = test_xmlrpc_srv.GetInvoice({'invoice_id': prepayment_invoice.id})
    assert_that(actual_result['payment_term_dt'], is_(None))


def test_overdraft(session, test_xmlrpc_srv, overdraft_invoice):
    actual_result = test_xmlrpc_srv.GetInvoice({'invoice_id': overdraft_invoice.id})
    assert_that(actual_result['payment_term_dt'], equal_to(overdraft_invoice.payment_term_dt))


def test_exceptions(session, test_xmlrpc_srv, overdraft_invoice):
    with pytest.raises(Fault) as exc_info:
        test_xmlrpc_srv.GetInvoice({'invoice_id': -1})
    assert get_exception_code(exc=exc_info.value) == 'INVOICE_NOT_FOUND'
    assert get_exception_code(exc=exc_info.value, tag_name='msg') == 'Invoice with ID -1 not found in DB'
