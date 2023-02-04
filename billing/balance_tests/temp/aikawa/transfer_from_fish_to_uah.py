# coding: utf-8
import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'

order_id_list = []

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))

invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)

steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY', currency='UAH', region_id=225)

PRODUCT_ID = 503165
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
dst_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 100, 'qty_new': 50, 'all_qty': 0}], [{'order_id': dst_order_id, 'qty_delta': 1}])


# CreateTransferMultiple(16571028, [{'QtyNew': '-0.000001',
#   'QtyOld': 80,
#   'ServiceID': 7,
#   'ServiceOrderID': 417405989,
#   'Tolerance': 0.00001}], [{'QtyDelta': '1.000000', 'ServiceID': 7, 'ServiceOrderID': 417405990}], True)
#
# Balance.CreateTransferMultiple(16571028, [{'AllQty': 0,
#   'QtyNew': 50,
#   'QtyOld': 100,
#   'ServiceID': 7,
#   'ServiceOrderID': 52178492}], [{'QtyDelta': 1, 'ServiceID': 7, 'ServiceOrderID': 52178500}], 1, None)
# Ответ: [{'Qty': '-50', 'ServiceID': 7, 'ServiceOrderID': 52178492},
#  {'Qty': '569.92181', 'ServiceID': 7, 'ServiceOrderID': 52178460}]