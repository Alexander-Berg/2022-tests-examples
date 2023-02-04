__author__ = 'aikawa'

import datetime

from btestlib import balance_steps as steps

dt = datetime.datetime.now()

PERSON_TYPE = 'sw_yt'
PAYSYS_ID = 1003
PAYSYS_ID = 1046
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'


client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)


service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
        ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))


invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                            credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)

print steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)