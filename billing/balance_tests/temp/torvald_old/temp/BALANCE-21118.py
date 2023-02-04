# -*- coding: utf-8 -*-
import pprint
import datetime

from temp.MTestlib import MTestlib as mtl


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


rpc = mtl.rpc
test_rpc = mtl.test_rpc

# service_id = 7; product_id = 1475 ##503162
# service_id = 11; product_id = 2136
##service_id = 70; product_id = 503273
##service_id = 77; product_id = 2584
service_id = 99;
product_id = 505123  ##504534, 504697 505123
##service_id = 102; product_id = 504654

qty = 100
qty2 = 20
paysys_id = 1091

after = datetime.datetime(2015, 9, 28, 12, 0, 0)
disc_dt = datetime.datetime(2015, 9, 28, 12, 0, 0)

begin_dt = after
request_dt = after  ##РЅРµ РјРµРЅСЏРµС‚СЃСЏ
invoice_dt = after
payment_dt = after
campaigns_dt = after
act_dt = after
migrate_dt = after

manager_uid = None
##manager_uid = '96446401'
##manager_uid = '176005458'
uid = 'clientuid33'
##------------------------------------------------------------------------------

client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})  # 29680771
agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})
order_owner = client_id
invoice_owner = client_id
if order_owner == invoice_owner: agency_id = None
mtl.link_client_uid(invoice_owner, 'clientuid32')
person_id = None or mtl.log(mtl.create_person)(invoice_owner, 'ur_autoru', {'phone': '234'})
contract_id = None

# mtl.get_force_overdraft(client_id, service_id, 1000, 10, after, 'RUB')
# mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now(), 'SERVICE_ID': 99})
mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB',
                   'MIGRATE_TO_CURRENCY': datetime.datetime(2000, 1, 1), 'SERVICE_ID': 99})

service_order_id = mtl.get_next_service_order_id(service_id)
##    service_order_id = 12345678
order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
service_order_id2 = mtl.get_next_service_order_id(service_id)
order_id2 = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id2,
                                       {'TEXT': 'Py_Test order', 'unmoderated': 0}, agency_id=agency_id,
                                       manager_uid=manager_uid)
orders_list = [
    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    , {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': qty2, 'BeginDT': begin_dt}
]

after = datetime.datetime(2015, 7, 28, 12, 0, 0)

request_id = mtl.create_request(invoice_owner, orders_list, after)
invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                endbuyer_id=None)
mtl.OEBS_payment(invoice_id)


# test.Balance.ResetOverdraftInvoices([9902659])
# test.Balance.RefundOrders([9902659])
# Balance.BanOverdraft([30251626])
