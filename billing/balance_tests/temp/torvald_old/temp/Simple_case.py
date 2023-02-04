# -*- coding: utf-8 -*-

import pprint
import datetime
import time

from temp.MTestlib import MTestlib as mtl


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


rpc = mtl.rpc
test_rpc = mtl.test_rpc

service_id = 7;
product_id = 1475  ##503162
# service_id = 11; product_id = 2136
##service_id = 70; product_id = 503273
##service_id = 77; product_id = 2584
# service_id = 99; product_id = 505285 ##504697
##service_id = 102; product_id = 504654

qty = 100
qty2 = 200
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
##manager_uid = '96446401'
##manager_uid = '176005458'
uid = 'clientuid33'
##------------------------------------------------------------------------------

##client_id = 29683097 or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'}) #29680771
##agency_id = 29691238 or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})
client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})  # 29680771
agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})
##agency_id = 29648530 or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})
order_owner = client_id
invoice_owner = client_id
if order_owner == invoice_owner: agency_id = None
mtl.link_client_uid(invoice_owner, 'clientuid32')
##person_id = 3627411 or mtl.create_person(invoice_owner, 'ur', {'phone':'234'})
person_id = None or mtl.log(mtl.create_person)(invoice_owner, 'ur', {'phone': '234'})
# Export any object to OEBS
##    test_rpc.ExportObject('OEBS', 'Person', person_id)
# Give overdraft to client
mtl.get_force_overdraft(client_id, service_id, 1000, 1, after, None)
# mtl.get_overdraft(client_id, service_id, 1000,1, after, None)
# Create contract
contract_id = None
# contract_id = mtl.create_contract2('comm_post',{'client_id': invoice_owner, 'person_id': person_id,
#                                                    'dt'       : '2015-04-30T00:00:00',
#                                                    'FINISH_DT': '2016-06-30T00:00:00',
#                                                    'is_signed': '2015-01-01T00:00:00',
#                                                    'NON_RESIDENT_CLIENTS': 0,
#                                                    'REPAYMENT_ON_CONSUME': 1})
# Create collateral to contract
##collateral_id = mtl.create_collateral2(1033, {'contract2_id': contract_id, 'dt' : '2015-04-30T00:00:00', 'is_signed': '2015-01-01T00:00:00'})
# Give direct discount to client
##mtl.get_direct_discount(invoice_owner, datetime.datetime(2015,7,1), pct = 16, budget = 33333, currency = None)

# mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})
# sql = "select state as val from T_EXPORT where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id"
# sql_params = {'client_id': client_id}
# mtl.wait_for(sql, sql_params, value = 1)

# mtl.merge_clients(client_id, client_id2)

# test_rpc.ExecuteSQL('Insert into t_extprops (ID,OBJECT_ID,CLASSNAME,ATTRNAME,KEY,VALUE_STR,VALUE_NUM,VALUE_DT,VALUE_CLOB,UPDATE_DT,PASSPORT_ID) \
#         values (s_extprops.nextval,:client_id,\'Client\',\'acts_most_valuable_priority\',null,null,\'1\',null, EMPTY_CLOB(),sysdate,\'125122388\')',
#         {'client_id': invoice_owner})

service_order_id = mtl.get_next_service_order_id(service_id)
##service_order_id = 17760741
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

mtl.do_campaigns(service_id, service_order_id, {'Bucks': 100, 'Money': 0}, 0, after)
mtl.act_accounter(invoice_owner, 1, datetime.datetime.today())
mtl.do_campaigns(service_id, service_order_id, {'Bucks': 50, 'Money': 0}, 0, after)
result = rpc.Balance.CreateTransferMultiple(mtl.passport_uid, [
    {"ServiceID": service_id, "ServiceOrderID": service_order_id, "QtyOld": qty, "QtyNew": qty - 50, 'AllQty': 1}],
                                            [{"ServiceID": service_id, "ServiceOrderID": service_order_id2,
                                              "QtyDelta": 1}], 1)

mtl.do_campaigns(service_id, service_order_id2, {'Bucks': 50, 'Money': 0}, 0, after)

print result

mtl.do_campaigns(service_id, service_order_id, {'Bucks': 0, 'Money': 0.000060}, 0, after)
# ------------------------------------------------------------------
orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 10.123450, 'BeginDT': begin_dt}]
request_id = mtl.create_request(invoice_owner, orders_list, request_dt)
invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                endbuyer_id=None)
mtl.OEBS_payment(invoice_id)
mtl.do_campaigns(service_id, service_order_id, {'Bucks': 0, 'Money': 0.000060 + 10.123450}, 0, after)

# ------------------------------------------------------------------
orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': 5, 'BeginDT': begin_dt}]
request_id = mtl.create_request(invoice_owner, orders_list, request_dt)
invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                endbuyer_id=None)
mtl.OEBS_payment(invoice_id)
mtl.do_campaigns(service_id, service_order_id2, {'Bucks': 0, 'Money': 4.004990}, 0, after)

mtl.act_accounter(invoice_owner, 1, datetime.datetime.today())

mtl.do_campaigns(service_id, service_order_id, {'Bucks': 5.123456, 'Money': 0}, 0, datetime.datetime.now())

pass
orders_list = [
    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty2, 'BeginDT': begin_dt}
    ##  , {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': qty2, 'BeginDT': begin_dt}
]
request_id = mtl.create_request(invoice_owner, orders_list, request_dt)
invoice_id2 = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                 endbuyer_id=None)
mtl.OEBS_payment(invoice_id2)
mtl.do_campaigns(service_id, service_order_id, {'Bucks': 0, 'Money': 25}, 0, datetime.datetime.now())

mtl.act_accounter(invoice_owner, 1, datetime.datetime.today())

test_rpc.ActEnqueuer([invoice_owner], datetime.datetime.today(), 1)

time.sleep(5)
rpc.Balance.CreateTransferMultiple(mtl.passport_uid, [
    {"ServiceID": service_id, "ServiceOrderID": service_order_id, "QtyOld": qty, "QtyNew": qty - 0.000001}],
                                   [{"ServiceID": service_id, "ServiceOrderID": service_order_id2, "QtyDelta": 1}], 1)
rpc.Balance.CreateTransferMultiple(mtl.passport_uid, [
    {"ServiceID": service_id, "ServiceOrderID": service_order_id, "QtyOld": qty - 0.000001, "QtyNew": qty - 0.000167}],
                                   [{"ServiceID": service_id, "ServiceOrderID": service_order_id2, "QtyDelta": 1}], 1)
rpc.Balance.CreateTransferMultiple(mtl.passport_uid, [
    {"ServiceID": service_id, "ServiceOrderID": service_order_id, "QtyOld": qty - 0.000167, "QtyNew": qty - 0.000267,
     'AllQty': 0}],
                                   [{"ServiceID": service_id, "ServiceOrderID": service_order_id2, "QtyDelta": 1}], 1)
rpc.Balance.CreateTransferMultiple(mtl.passport_uid, [
    {"ServiceID": service_id, "ServiceOrderID": service_order_id, "QtyOld": qty - 0.000267, "QtyNew": qty - 80.000267,
     'AllQty': 0}],
                                   [{"ServiceID": service_id, "ServiceOrderID": service_order_id2, "QtyDelta": 1}], 1)

mtl.do_campaigns(7, 27532350, {'Bucks': 200.000050, 'Money': 0}, 0, datetime.datetime.now())
mtl.act_accounter(29692003, 1, datetime.datetime.now())
mtl.do_campaigns(7, 27532350, {'Bucks': 200.000015, 'Money': 0}, 0, datetime.datetime.now())

##rpc.Balance.CreateTransferMultiple(mtl.passport_uid,[{"ServiceID": service_id, "ServiceOrderID": service_order_id2, "QtyOld":qty-0.000267, "QtyNew":qty-80.000267, 'AllQty': 1}],
##                [{"ServiceID": service_id, "ServiceOrderID": service_order_id, "QtyDelta": 1}], 1)

pass
##mtl.rpc.Balance.UpdateCampaigns([{"ServiceID": service_id, "ServiceOrderID": service_order_id, "dt": datetime.datetime.now(), "stop": 0, "Bucks": 14, "Money": 0}])
##time.sleep(15)
##sql = "select state as val from T_EXPORT where type = 'PROCESS_COMPLETION' and object_id = :order_id"
##sql_params = {'order_id': order_id}
##mtl.wait_for(sql, sql_params, value = 1)

##mtl.OEBS_payment(invoice_id, 10, datetime.datetime(2015,7,1))
##mtl.do_campaigns(service_id,service_order_id , {'Bucks': 13.688555, 'Money': 0}, 0, campaigns_dt)
##mtl.do_campaigns(service_id,service_order_id , {'Bucks': 20.688555, 'Money': 0}, 0, datetime.datetime.now())
##mtl.do_campaigns(service_id,service_order_id , {'Bucks': 20.688555, 'Money': 3}, 0, datetime.datetime.now())

##mtl.Print(dict(mtl.objects))
##
##    test_rpc.ActEnqueuer([invoice_owner], datetime.datetime.today(), 1)
##mtl.act_accounter(invoice_owner,1,datetime.datetime.today())
