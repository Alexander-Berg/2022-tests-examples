__author__ = 'aikawa'
import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)
PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'

def order_to_invoice(service_order_id):
    orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    return invoice_id

client_id = steps.ClientSteps.create()
# steps.ClientSteps.negative_reverse_allow(client_id)
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

dst_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
dst_order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=dst_service_order_id)
order_to_invoice(dst_service_order_id)

dpt_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
dpt_order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=dpt_service_order_id)
order_to_invoice(dpt_service_order_id)

order_to_invoice(dst_service_order_id)
steps.OrderSteps.transfer([{'order_id':dpt_order_id, 'qty_old': 200, 'qty_new': 100, 'all_qty': 0}],[{'order_id':dst_order_id, 'qty_delta': 1}])
steps.OrderSteps.transfer([{'order_id':dpt_order_id, 'qty_old': 100, 'qty_new': 0, 'all_qty': 0}],[{'order_id':dst_order_id, 'qty_delta': 1}])