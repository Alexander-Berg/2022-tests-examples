# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

from temp.MTestlib import MTestlib as mtl

rpc = mtl.rpc
test_rpc = mtl.test_rpc

after = datetime.datetime(2015, 6, 24, 11, 0, 0)
disc_dt = datetime.datetime(2015, 6, 24, 11, 0, 0)

begin_dt = after
invoice_dt = after
campaigns_dt = after

agency_id = None
contract_id = None
manager_uid = None

paysys_person_list = [
    # ['1006','ur']
    # ,['1007', '']
    # ,['1094', '']
    ['11048', 'tru']
]

service_id = 7  # Директ
product_id = 1475  # Рекламная кампания
msr = 'Bucks'

qty = 100

client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
mtl.link_client_uid(client_id, 'aikawa-test-0')

# Создаем заказ

service_order_id = mtl.get_next_service_order_id(service_id)
order_id = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
orders_list = [
    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
]
request_id = mtl.create_request(client_id, orders_list, disc_dt)
for paysys_id, person_type in paysys_person_list:
    person_id = mtl.create_person(client_id, person_type)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)
