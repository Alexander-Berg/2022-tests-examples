# coding=utf-8

import datetime
from decimal import Decimal as D

import pytest
from dateutil.relativedelta import relativedelta

from balance import constants as cst, muzzle_util as ut
from tests import object_builder as ob
from tests.test_xmlrpc_test.common import create_order, create_request, create_invoice, create_cashback

_CLIENT_ID = 123


@pytest.fixture(autouse=True)
def client(session):
    return ob.ClientBuilder(id=_CLIENT_ID).build(session).obj


def test_charges_client(session, test_xmlrpc_srv, client):
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_ID)

    request = create_request(session, client, orders=[(order, 1)])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()

    cashback_1 = create_cashback(client, bonus=10, finish_dt=None)
    finish_dt = ut.trunc_date(datetime.datetime.now()) + relativedelta(days=60)
    cashback_2 = create_cashback(client, bonus=10, finish_dt=finish_dt)

    res = test_xmlrpc_srv.ChargeClientCashback({
        "client_id": client.id,
        "service_id": cst.DIRECT_SERVICE_ID,
    })

    assert res == 'OK'
    assert sum([co.current_sum for co in invoice.consumes]) == D('30')
    assert sum([co.current_qty for co in invoice.consumes]) == D('1.666666')
    assert sum([co.current_cashback_bonus for co in invoice.consumes]) == D('0.666666')
    assert cashback_1.bonus == D('0.00001')
    assert cashback_2.bonus == D('0.00001')
