#-*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

import balance.balance_steps as steps

dt = datetime.datetime(2015,10,30,11,0,0)

SERVICE_ID = 98
PERSON_TYPE = 'ur'

PRODUCT_ID = 506526

# PAYSYS_ID = 1003
PAYSYS_ID = 1201003


qty = 100

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
steps.ClientSteps.link(2759382, 'aikawa-test-0')

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id=steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}
            ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, invoice_dt=dt)

invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                              credit=0, contract_id=None, overdraft=0,endbuyer_id=None)


# steps.ActsSteps.create(42959353)

# 3700473, 3700469, 3700474, 3700470, 3700471, 3700472, 3700457