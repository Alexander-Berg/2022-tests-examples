# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)
contract_dt = datetime.datetime.now() + datetime.timedelta(days=7)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'
QTY = 100
CONTRACT_TYPE = 'no_agency_post'

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
contract_id, _ = steps.ContractSteps.create_contract(CONTRACT_TYPE, {'PERSON_ID': person_id, 'CLIENT_ID': client_id,
                                                                     'SERVICES': [SERVICE_ID],
                                                                     'FINISH_DT': contract_dt})
invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)

steps.ActsSteps.enqueue([client_id], force=1)
steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)
