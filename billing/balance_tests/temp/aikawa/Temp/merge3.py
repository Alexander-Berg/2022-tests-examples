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
manager_id = None
manager_uid = None
contract_id = None

person_type = 'ur'  # ЮЛ резидент РФ
paysys_id = 1003  # Банк для юридических лиц
service_id = 7
product_id = 1475

ServiceOrderIdList = []

client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
service_order_id = mtl.get_next_service_order_id(service_id)
order_id = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
ServiceOrderIdList.append(service_order_id)

client_id2 = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
service_order_id = mtl.get_next_service_order_id(service_id)
order_id = mtl.create_or_update_order(client_id2, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
ServiceOrderIdList.append(service_order_id)

print ServiceOrderIdList

rpc.Balance.MergeClients(16571028, client_id, client_id2)

class_id = test_rpc.ExecuteSQL("select class_id from t_client where id = :client_id", {'client_id': client_id2})[0][
    'class_id']
if class_id == client_id:
    print ('MergeClients Done! Main client: ' + str(client_id) + ', related client: ' + str(client_id2))

print rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
    {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[1], 'ProductID': product_id, 'ClientID': client_id2,
     'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[0], 'GroupWithoutTransfer': 1}
])

print (test_rpc.ExecuteSQL("select group_order_id from t_order where service_order_id = :service_order_id",
                           {'service_order_id': ServiceOrderIdList[1]})[0]['group_order_id'])

# test_rpc.UATransferQueue([client_id])
#
# while 1==1:
#         state = test_rpc.ExecuteSQL("select state from T_EXPORT where type = 'UA_TRANSFER' and object_id = :client_id" , {'client_id': client_id })[0]['state']
#         print('waiting...')
#         time.sleep(3)
#         if state == 1:
#             print ('Transfer done!')
#             break
