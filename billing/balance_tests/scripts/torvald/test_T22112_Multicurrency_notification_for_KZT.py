# -*- coding: utf-8 -*-

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps

SERVICE_ID = 7
# PRODUCT_ID = 502953 ##502918
PRODUCT_ID = 1475
PAYSYS_ID = 1020
QTY = 10
BASE_DT = datetime.datetime.now()


def test_deny_kzt_nonres_offerta_for_direct():
    client_id = None or steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    # db.balance().execute("Update t_client set REGION_ID = :region_id where ID = :client_id",
    #       {'client_id': client_id, 'region_id': 159})
    agency_id = None

    order_owner = client_id
    invoice_owner = agency_id or client_id

    person_id = None or steps.PersonSteps.create(invoice_owner, 'kzu', {'phone': '234'})
    contract_id = None

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
                   ]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    for item in api.test_balance().GetNotification(10, client_id): reporter.log(item)


if __name__ == "__main__":
    pytest.main('-v')