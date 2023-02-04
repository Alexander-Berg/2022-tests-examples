# -*- coding: utf-8 -*-
import pytest
from decimal import Decimal as D

from balance import constants as cst

from tests import object_builder as ob




@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture(name='invoice')
def create_invoice(session, client, firm_id=cst.FirmId.YANDEX_OOO):
    order = ob.OrderBuilder(client=client).build(session).obj
    request = ob.RequestBuilder(
        firm_id=firm_id,
        basket=ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(order=order, quantity=1)]
        )
    ).build(session).obj

    invoice = ob.InvoiceBuilder(
        request=request,
        person=ob.PersonBuilder(client=client),
    ).build(session).obj
    return invoice


def test_operation_w_invoice(session, client):
    invoice = create_invoice(session, client)
    invoice.receipt_sum = D('12345.67')
    session.flush()
    invoice.turn_on_req(request=invoice.request)

    assert invoice.operations[0].invoice
