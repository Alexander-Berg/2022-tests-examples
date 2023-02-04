# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

import balance.balance_api as api
import balance.balance_steps as steps
import btestlib.data.defaults as defaults

after = datetime.datetime(2015, 6, 24, 11, 0, 0)
dt = after

PERSON_TYPE = 'ur'
SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

dpt_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=dpt_service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': dpt_service_order_id, 'Qty': 200, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, invoice_dt=dt)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, dpt_service_order_id, {'Bucks': 100}, 0, dt)


dst_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
dst_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=dst_service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)

api.medium().create_transfer_multiple(defaults.PASSPORT_UID,
                                      [
                                          {"ServiceID": SERVICE_ID,
                                           "ServiceOrderID": dpt_service_order_id,
                                           "QtyOld": 200, "QtyNew": 50, "AllQty": 0}
                                      ],
                                      [
                                          {"ServiceID": SERVICE_ID,
                                           "ServiceOrderID": dst_service_order_id,
                                           "QtyDelta": 1}
                                      ], 1, None)
