__author__ = 'aikawa'
import datetime

from balance import balance_steps as steps

SERVICE_ID = 42
REGION_ID = 126
PERSON_TYPE = 'sw_ur'
PAYSYS_ID = 1601044
PRODUCT_ID = 506619
dt = datetime.datetime.now()

client_id = steps.ClientSteps.create({'REGION_ID': REGION_ID})
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
order_id = order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
        ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                              credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)
print steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)

