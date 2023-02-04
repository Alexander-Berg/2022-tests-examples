#-*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1002
QTY = 100
BASE_DT = datetime.datetime.now()

def test_client():


    client_id = steps.ClientSteps.create()
    agency_id = None
    order_owner = client_id
    invoice_owner = client_id
    person_id = steps.PersonSteps.create(invoice_owner, 'ph')

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                           params={'AgencyID': agency_id})
    orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0,
                                                                contract_id=None,
                                                                overdraft=0, endbuyer_id=None)
    print  'https://balance.greed-tm1f.yandex.ru/invoice.xml?invoice_id=%s'%invoice_id

test_client()





