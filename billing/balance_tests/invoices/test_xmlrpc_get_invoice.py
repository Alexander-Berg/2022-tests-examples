# -*- coding: utf-8 -*-
import decimal
import tests.object_builder as ob


def test_get_invoice(xmlrpcserver, session):
    session.oper_id = ob.PassportBuilder().build(session).obj.passport_id
    inv = ob.InvoiceBuilder().build(session).obj

    result = xmlrpcserver.GetInvoice(session.oper_id, inv.id)
    assert inv.external_id == result['EXTERNAL_ID']
    assert inv.total_sum == decimal.Decimal(result['TOTAL_SUM'])
    assert 0 == result['CANCELLED']
