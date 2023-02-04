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

# # -------- firm_id = 1 -------
person_type = 'ur'  # ЮЛ резидент РФ
paysys_id = 1003  # Банк для юридических лиц
service_id = 7  # Директ
product_id = 1475  # Рекламная кампания
msr = 'Bucks'

qty = 100

ServiceOrderIdList = []

client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
person_id = mtl.create_person(client_id, person_type)
mtl.link_client_uid(client_id, 'torvald-test-0')

# Заказ с перекруткой
service_order_id = mtl.get_next_service_order_id(service_id)
order_id = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
# orders_list = [
#         {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
#     ]
# request_id = mtl.create_request (client_id, orders_list, disc_dt)
# invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, credit = 0, contract_id = contract_id, overdraft = 0, endbuyer_id = None)
# mtl.OEBS_payment(invoice_id)
mtl.do_campaigns(service_id, service_order_id, {msr: 86}, 0, campaigns_dt)
ServiceOrderIdList.append(service_order_id)

print '_____________________'

# Родительский заказ
service_order_id = mtl.get_next_service_order_id(service_id)
order_id = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
orders_list = [
    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
]
request_id = mtl.create_request(client_id, orders_list, disc_dt)
invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                endbuyer_id=None)
mtl.OEBS_payment(invoice_id)
ServiceOrderIdList.append(service_order_id)


# Дочернему заказу с перекруткой и родительскому устанавливаем признак "Непромодерирован"
rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
    {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[0], 'ProductID': product_id, 'ClientID': client_id,
     'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1], 'GroupWithoutTransfer': 0}
    , {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[1], 'ProductID': product_id, 'ClientID': client_id,
       'AgencyID': agency_id, 'unmoderated': '1'}

    # ,{'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[1],'ProductID': product_id, 'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[2], 'GroupWithoutTransfer': 1}
    # ,{'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[2],'ProductID': product_id, 'ClientID': client_id, 'AgencyID': agency_id}
])
print (test_rpc.ExecuteSQL("select unmoderated from t_order where service_order_id = :service_order_id",
                           {'service_order_id': ServiceOrderIdList[0]})[0]['unmoderated'])

unmoderated_order_id = test_rpc.ExecuteSQL("select id from t_order where service_order_id = :service_order_id",
                                           {'service_order_id': ServiceOrderIdList[1]})[0]['id']
print unmoderated_order_id

sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :order_id"
sql_params = {'order_id': unmoderated_order_id}
mtl.wait_for(sql, sql_params, value=1)
