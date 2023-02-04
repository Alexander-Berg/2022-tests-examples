# -*- coding: utf-8 -*-
import decimal
import datetime
import pytest

from xmlrpclib import Fault
from balance import mapper, exc, core
import tests.object_builder as ob
from balance.actions.invoice_turnon import InvoiceTurnOn
import balance.actions.process_completions as action_completion


def test_InvoiceRollback(xmlrpcserver, session):
    passport_id = ob.PassportBuilder().build(session).obj.passport_id
    inv = ob.InvoiceBuilder().build(session).obj
    InvoiceTurnOn(inv, manual=True).do()
    with pytest.raises(Fault):
        xmlrpcserver.RollbackInvoice(passport_id, inv.id)

    pc = ob.PayOnCreditCase(session)
    prod = pc.get_product_hierarchy()
    cont = pc.get_contract(
        commission=1,
        payment_type=3,
        credit_type=1,
        payment_term=30,
        services=set([77]),
        is_signed=1,
        firm=1,
        discount_policy_type=7,
        turnover_forecast={
            prod[0].activity_type.id: 235000000,
            prod[1].activity_type.id: 235000000,
        },
    )
    prod[1]._other.price.b.tax = 0
    prod[1]._other.price.b.price = 1000

    basket = ob.BasketBuilder(rows=[
        ob.BasketItemBuilder(quantity=c[2], order=ob.OrderBuilder(product=c[1], client=c[0], agency=c[0].agency,
                                                                  service=ob.Getter(mapper.Service, 77)).build(
            session)) for c in [(cont.client, prod[1], 1000)]
        ])
    inv = pc.pay_on_credit(basket, cont)[0]
    assert inv.receipt_sum == decimal.Decimal(xmlrpcserver.RollbackInvoice(passport_id, inv.id))
    assert inv.receipt_sum == 0

    basket = ob.BasketBuilder(rows=[
        ob.BasketItemBuilder(quantity=c[2], order=ob.OrderBuilder(product=c[1], client=c[0], agency=c[0].agency,
                                                                  service=ob.Getter(mapper.Service, 77)).build(
            session)) for c in [(cont.client, prod[1], 1000)]
        ])
    inv = pc.pay_on_credit(basket, cont)[0]

    pr_compl = action_completion.ProcessCompletions(inv.consumes[0].order,
                                                    on_dt=datetime.datetime.today() - datetime.timedelta(1))
    pr_compl.process_completions(qty=10)

    assert sum(i.completion_qty for i in inv.consumes) == 10

    req = mapper.Request(passport_id=0,
                         basket=mapper.Basket(rows=[mapper.BasketItem(order=inv.consumes[0].order, quantity=1000)],
                                              client=inv.client))
    session.flush()

    session.oper_id = passport_id
    inv2 = core.Core(session).pay_on_credit(
        request_id=req.id,
        paysys_id=inv.paysys.id,
        person_id=inv.contract.person.id,
        contract_id=inv.contract.id)[0]

    assert xmlrpcserver.RollbackInvoice(passport_id, inv.id) == str(1188000)
    xmlrpcserver.RollbackInvoice(passport_id, inv2.id)
    with pytest.raises(Fault):
        xmlrpcserver.RollbackInvoice(passport_id, inv2.id)
        # self.assertFault("WITHDRAW_NOT_POSSIBLE", self.xmlrpcserver.RollbackInvoice, passport_id, inv2.id)
