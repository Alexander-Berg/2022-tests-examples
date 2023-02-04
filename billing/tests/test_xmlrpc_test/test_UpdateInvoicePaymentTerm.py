# -*- coding: utf-8 -*-

from xmlrpclib import Fault
import pytest
from hamcrest import assert_that, equal_to
import datetime

from tests import object_builder as ob
from tests.tutils import get_exception_code

NOW = datetime.datetime.now().date()


@pytest.fixture()
def overdraft_invoice(session):
    invoice = ob.InvoiceBuilder(overdraft=1).build(session).obj
    return invoice


@pytest.fixture()
def prepayment_invoice(session):
    invoice = ob.InvoiceBuilder(overdraft=0).build(session).obj
    return invoice


def test_prepayment(session, test_xmlrpc_srv, prepayment_invoice):
    with pytest.raises(Fault) as exc_info:
        test_xmlrpc_srv.UpdateInvoicePaymentTerm({'invoice_id': prepayment_invoice.id,
                                                  'payment_term_dt': NOW.strftime("%Y%m%dT%H:%M:%S")})
    assert get_exception_code(exc=exc_info.value) == 'INVALID_PARAM'
    exc_msg = 'Invalid parameter for function: Payment term dt is not supported for this invoice'
    assert get_exception_code(exc=exc_info.value, tag_name='msg') == exc_msg


def test_overdraft(session, test_xmlrpc_srv, overdraft_invoice):
    new_dt = NOW + datetime.timedelta(days=10)
    test_xmlrpc_srv.UpdateInvoicePaymentTerm({'invoice_id': overdraft_invoice.id,
                                              'payment_term_dt': new_dt.strftime("%Y%m%dT%H:%M:%S")})
    assert_that(overdraft_invoice.payment_term_dt.date(), equal_to(new_dt))


def test_exceptions(session, test_xmlrpc_srv, overdraft_invoice):
    new_dt = NOW + datetime.timedelta(days=10)
    with pytest.raises(Fault) as exc_info:
        test_xmlrpc_srv.UpdateInvoicePaymentTerm({'invoice_id': -1,
                                                  'payment_term_dt': new_dt.strftime("%Y%m%dT%H:%M:%S")})
    assert get_exception_code(exc=exc_info.value) == 'INVOICE_NOT_FOUND'
    assert get_exception_code(exc=exc_info.value, tag_name='msg') == 'Invoice with ID -1 not found in DB'
