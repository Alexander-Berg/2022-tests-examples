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

client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
order_owner = client_id
invoice_owner = client_id
mtl.link_client_uid(client_id, 'aikawa-test-0')
person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})

service_order_id = mtl.get_next_service_order_id(service_id)
order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
orders_list = [
    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
]
request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                endbuyer_id=None)
mtl.OEBS_payment(invoice_id)
mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
ServiceOrderIdList.append(service_order_id)

service_order_id = mtl.get_next_service_order_id(service_id)
order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
parent_order_id = order_id
orders_list = [
    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
]
request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                endbuyer_id=None)
mtl.OEBS_payment(invoice_id)
mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
ServiceOrderIdList.append(service_order_id)

# объединяем
rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
    {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[0], 'ProductID': product_id, 'ClientID': client_id,
     'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1], 'GroupWithoutTransfer': 1}
])
# print ServiceOrderIdList
# print (test_rpc.ExecuteSQL('balance', "select group_order_id from t_order where service_order_id = :service_order_id" , {'service_order_id': ServiceOrderIdList[0] })[0]['group_order_id'])


# ставим в очередь
test_rpc.UATransferQueue([client_id])

# перезапускаем разборщик очереди
test_rpc.ExecuteSQL('balance', 
    'update t_pycron_state set started = null where id = (select state_id from v_pycron where name = \'unified_account_transfer\')')
test_rpc.ExecuteSQL('balance', 'commit')

# ждем разбора
sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :client_id"
sql_params = {'client_id': order_owner}
mtl.wait_for(sql, sql_params, value=1)

output = test_rpc.ExecuteSQL('balance', 
    "select output from t_export where type = 'UA_TRANSFER' and object_id = {0}".format(client_id))
print output

query = 'select result from t_operation where type_id = 1 and id in (select operation_id from t_consume where parent_order_id = {0})'.format(
    parent_order_id)
result = mtl.get_input_value(query, 'result')
print result
