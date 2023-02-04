__author__ = 'aikawa'

# -*- coding: utf-8 -*-

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003
QTY = 100
dt = datetime.datetime.now()

client_id = steps.ClientSteps.create()
agency_id = steps.ClientSteps.create({'IS_AGENCY': '1'})
person_id = steps.PersonSteps.create(agency_id, 'ur')
steps.ClientSteps.link(client_id, 'aikawa-test-0')

service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}]
request_id = steps.RequestSteps.create(agency_id, orders_list)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=None,
                                             overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)
print steps.ActsSteps.generate(agency_id, force=1, date=dt)