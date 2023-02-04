__author__ = 'aikawa'

import datetime

from balance import balance_steps as steps

PERSON_TYPE = 'usu'
PAYSYS_ID = 1028
SERVICE_ID = 7
PRODUCT_ID_2 = 1475
PRODUCT_ID = 1475
MSR = 'Bucks'

dt = datetime.datetime(2016, 1, 22, 11, 0, 0)

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                   service_order_id=service_order_id)

# service_order_id_2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
# order_id_2 = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID_2, service_id=SERVICE_ID, service_order_id=service_order_id_2)

orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 1, 'BeginDT': dt}
    # ,{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id_2, 'Qty': 1, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                       additional_params=dict(InvoiceDesireDT=dt))

invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 0.99}, 0, dt)

# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id_2, {'Bucks': 0.01}, 0, dt)

print steps.ActsSteps.generate(client_id, force=1, date=dt)
