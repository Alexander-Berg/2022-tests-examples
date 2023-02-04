__author__ = 'aikawa'

import datetime
import xmlrpclib

from balance import balance_steps as steps

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                   service_order_id=service_order_id)

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
from_order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                        service_order_id=service_order_id)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
]

request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)

OPCODE = 1

try:
    steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 100, 'qty_new': 0, 'all_qty': 0}],
                              [{'order_id': from_order_id, 'qty_delta': 1}])
except xmlrpclib.Fault:
    steps.CommonSteps.wait_and_get_notification(1, order_id, number=1)
    print steps.CommonSteps.parse_notification(OPCODE, order_id)
    # actual = [item['args'][0] for item in steps.CommonSteps.parse_notification(OPCODE, order_id)]
    # print actual
