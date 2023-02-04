# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

import btestlib.balance_steps as steps

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'

PAYSYS_ID = 1003

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
SERVICE_ID = 70
PRODUCT_ID = 506337
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                   service_order_id=service_order_id)
# SERVICE_ID2 = 7
# PRODUCT_ID2 = 1475
SERVICE_ID2 = 11
PRODUCT_ID2 = 2136
service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID2)
order_id2 = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID2, service_id=SERVICE_ID2,
                                    service_order_id=service_order_id2)
orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt},
               {'ServiceID': SERVICE_ID2, 'ServiceOrderID': service_order_id2, 'Qty': 200, 'BeginDT': dt}
               ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                       additional_params=dict(InvoiceDesireDT=dt))
# invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                                               credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Days': 200}, 0, dt)
#
# print steps.ActsSteps.generate(client_id, force=1, date=dt)
