# -*- coding: utf-8 -*-

import pprint
import datetime

from temp.MTestlib import MTestlib as mtl


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


rpc = mtl.rpc
test_rpc = mtl.test_rpc

service_id = 7;
product_id = 503162

qty = 100.123400
paysys_id = 1003

after = datetime.datetime(2015, 9, 23, 12, 0, 0)  # datetime.datetime.now()
disc_dt = datetime.datetime(2015, 9, 13, 12, 0, 0)

begin_dt = after
request_dt = after  ##не меняется
invoice_dt = after
payment_dt = after
campaigns_dt = after
act_dt = after
migrate_dt = after

manager_uid = None
uid = 'clientuid33'
##------------------------------------------------------------------------------

client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})
order_owner = client_id
invoice_owner = client_id
if order_owner == invoice_owner: agency_id = None
mtl.link_client_uid(invoice_owner, 'clientuid32')
person_id = None or mtl.log(mtl.create_person)(invoice_owner, 'ur', {'phone': '234'})

contract_id = None

mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB',
                   'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7,
                   'CURRENCY_CONVERT_TYPE': 'MODIFY'})

service_order_id = mtl.get_next_service_order_id(service_id)
order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
service_order_id2 = mtl.get_next_service_order_id(service_id)
order_id2 = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id2,
                                       {'TEXT': 'Py_Test order', 'unmoderated': 0}, agency_id=agency_id,
                                       manager_uid=manager_uid)
service_order_id3 = mtl.get_next_service_order_id(service_id)
order_id3 = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id3,
                                       {'TEXT': 'Py_Test order', 'unmoderated': 0}, agency_id=agency_id,
                                       manager_uid=manager_uid)

orders_list = [
    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    # , {'ServiceID': service_id, 'ServiceOrderID': service_order_id3, 'Qty': qty2, 'BeginDT': begin_dt}
]
request_id = mtl.create_request(invoice_owner, orders_list, request_dt)
invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                endbuyer_id=None)

mtl.OEBS_payment(invoice_id)
# result = rpc.Balance.CreateTransferMultiple(mtl.passport_uid,[{"ServiceID": service_id, "ServiceOrderID": service_order_id, "QtyOld":qty, "QtyNew":qty-0.000001, 'AllQty': 1}],
#                 [{"ServiceID": service_id, "ServiceOrderID": service_order_id2, "QtyDelta": 1}
#                , {"ServiceID": service_id, "ServiceOrderID": service_order_id3, "QtyDelta": 3}], 1)
# print result
