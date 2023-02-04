# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest
from hamcrest import has_entries

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import matchers as mtch
from btestlib import utils

SERVICE_ID = 81
PRODUCT_ID = 505151
PAYSYS_ID = 1201003
QTY = 100
BASE_DT = datetime.datetime.now()

def test_simple_client ():
    client_id = None or steps.ClientSteps.create()
    agency_id = None
    person_id = None or steps.PersonSteps.create(client_id, 'ur')

    order_owner = client_id
    invoice_owner = agency_id or client_id

    contract_id = None

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
        , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    expected = {'consume_qty': 100, 'current_qty': 100, 'completion_qty': 0, 'passport_id': None}

    actual = db.get_consumes_by_invoice(invoice_id)[0]
    utils.check_that(actual, has_entries(expected))

if __name__ == '__main__':
   # test_simple_client()
   pytest.main('test_matchers.py')
