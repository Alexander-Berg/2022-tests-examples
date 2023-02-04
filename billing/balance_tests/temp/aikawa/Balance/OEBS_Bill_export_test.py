__author__ = 'aikawa'

import datetime

import temp.aikawa.Balance.OEBS_Bill_export as obe
from balance import balance_steps as steps

dt = datetime.datetime.now()
SERVICE_ID = 7
PAYSYS_ID  = 1003
PRODUCT_ID = 1475

client_id = steps.ClientSteps.create()
person_id =  steps.PersonSteps.create(client_id, 'ur')
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
        ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                              credit=0, contract_id=None, overdraft=0,endbuyer_id=None)

steps.InvoiceSteps.pay(invoice_id)

print steps.CommonSteps.get_extprops(classname='Person', object_id=person_id, attrname='invalid_bankprops')
print steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)

oebs_person_data = obe.Person(person_id, 'OEBS')
balance_person_data = obe.Person(person_id)

print oebs_person_data.id
print balance_person_data.id