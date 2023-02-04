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
##service_id = 11; product_id = 2136
##service_id = 70; product_id = 503273
##service_id = 77; product_id = 2584
##service_id = 99; product_id = 504596 ##504697

qty = 100
qty2 = 200
paysys_id = 1017

after = datetime.datetime(2015, 7, 6, 11, 0, 0)  # datetime.datetime.now()
disc_dt = datetime.datetime(2015, 7, 6, 11, 0, 0)

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

client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})
order_owner = client_id
invoice_owner = agency_id
if order_owner == invoice_owner: agency_id = None
mtl.link_client_uid(invoice_owner, 'clientuid32')
person_id = None or mtl.create_person(invoice_owner, 'ua', {'phone': '234'})

# Export any object to OEBS
##    test_rpc.ExportObject('OEBS', 'Person', person_id)
# Give overdraft to client
##    mtl.get_force_overdraft(client_id, service_id, 1000, 1, after, None)
# Create contract
contract_id = None
contract_id = mtl.create_contract2('ukr_opt_ag_prem', {'client_id': invoice_owner, 'person_id': person_id,
                                                       'dt': '2015-04-30T00:00:00',
                                                       'FINISH_DT': '2016-06-30T00:00:00',
                                                       'is_signed': '2015-01-01T00:00:00'})
# Create collateral to contract
collateral_id = mtl.create_collateral2(1033, {'contract2_id': contract_id, 'dt': '2015-04-30T00:00:00',
                                              'is_signed': '2015-01-01T00:00:00'})
# Give direct discount to client
##    mtl.get_direct_discount(invoice_owner, datetime.datetime(2015,7,1), pct = 16, budget = 33333, currency = None)

client_id2 = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
mtl.merge_clients(client_id2, client_id)
client_id3 = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
mtl.merge_clients(client_id2, client_id3)

service_order_id = mtl.get_next_service_order_id(service_id)
##    service_order_id = 12345678
order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
##service_order_id2 = mtl.get_next_service_order_id(service_id)
##order_id2 = mtl.create_or_update_order (client_id2, product_id, service_id, service_order_id2,
##    {'TEXT':'Py_Test order'}, agency_id = agency_id, manager_uid = manager_uid)
orders_list = [
    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ##  , {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': qty2, 'BeginDT': begin_dt}
]
request_id = mtl.create_request(invoice_owner, orders_list, request_dt)
invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=1, contract_id=contract_id, overdraft=0,
                                endbuyer_id=None)

external_id = \
    test_rpc.ExecuteSQL('balance', "select amount, external_id from t_invoice where id = :invoice_id", {'invoice_id': invoice_id})[
        0][
        'external_id']
amount = test_rpc.ExecuteSQL('balance', "select total_sum from t_invoice where id = :invoice_id", {'invoice_id': invoice_id})[0][
    'total_sum']
max_cash_id = test_rpc.ExecuteSQL('balance', "select max(xxar_cash_fact_id) as max_id from t_oebs_cash_payment_fact")[0]['max_id']
sql = '''Insert into t_oebs_cash_payment_fact (XXAR_CASH_FACT_ID,AMOUNT,RECEIPT_NUMBER,RECEIPT_DATE,LAST_UPDATED_BY,LAST_UPDATE_DATE,LAST_UPDATE_LOGIN,CREATED_BY,CREATION_DATE,CASH_RECEIPT_NUMBER,ACC_NUMBER,PAYMENT_NUMBER,PAYMENT_DATE,OPERATION_TYPE,SOURCE_TYPE,COMISS_DATE)
values (:max_cash_id,:amount,:external_id,trunc(sysdate),'-1',trunc(sysdate),'-1','-1',trunc(sysdate),:external_id_mod,null,null,null,null,null,null)'''
sql_params = {'max_cash_id': max_cash_id + 1, 'amount': amount, 'external_id': external_id,
              'external_id_mod': '{0}-99999'.format(external_id)}
test_rpc.ExecuteSQL('balance', sql, sql_params)
time.sleep(15)
sql = "select state as val from T_EXPORT where type = 'PROCESS_PAYMENTS' and object_id = :invoice_id"
sql_params = {'invoice_id': invoice_id}
mtl.wait_for(sql, sql_params, value=1)

mtl.OEBS_payment(invoice_id)
mtl.do_campaigns(service_id, service_order_id, {'Bucks': 10, 'Money': 0}, 0, campaigns_dt)
##
##qty_new = qty - 13
##mtl.log(rpc.Balance.CreateTransferMultiple)(16571028,[{"ServiceID":service_id, "ServiceOrderID": service_order_id, "QtyOld": qty, "QtyNew": qty_new}],[{"ServiceID":service_id, "ServiceOrderID": service_order_id2, "QtyDelta":"1.000000"}])
##qty = qty_new
##qty_new -= 2
##mtl.log(rpc.Balance.CreateTransferMultiple)(16571028,[{"ServiceID":service_id, "ServiceOrderID": service_order_id, "QtyOld": qty, "QtyNew": qty_new}],[{"ServiceID":service_id, "ServiceOrderID": service_order_id2, "QtyDelta":"1.000000"}])
##qty = qty_new
##qty_new -= 5
##mtl.log(rpc.Balance.CreateTransferMultiple)(16571028,[{"ServiceID":service_id, "ServiceOrderID": service_order_id, "QtyOld": qty, "QtyNew": qty_new}],[{"ServiceID":service_id, "ServiceOrderID": service_order_id2, "QtyDelta":"1.000000"}])
##qty = qty_new
##qty_new -= 4
##mtl.log(rpc.Balance.CreateTransferMultiple)(16571028,[{"ServiceID":service_id, "ServiceOrderID": service_order_id, "QtyOld": qty, "QtyNew": qty_new}],[{"ServiceID":service_id, "ServiceOrderID": service_order_id2, "QtyDelta":"1.000000"}])
##qty = qty_new
##qty_new -= 13
##mtl.log(rpc.Balance.CreateTransferMultiple)(16571028,[{"ServiceID":service_id, "ServiceOrderID": service_order_id, "QtyOld": qty, "QtyNew": qty_new}],[{"ServiceID":service_id, "ServiceOrderID": service_order_id2, "QtyDelta":"1.000000"}])
##
##mtl.do_campaigns(service_id,service_order_id2 , {'Bucks': 233, 'Money': 0}, 0, campaigns_dt)
##
##mtl.do_campaigns(7,service_order_id , {'Bucks': 13.666666, 'Money': 0}, 0, campaigns_dt)
##    test_rpc.ActEnqueuer([invoice_owner], datetime.datetime.today(), 1)
mtl.act_accounter(invoice_owner, 1, datetime.datetime.today())

##    # ---------- For multicurrency cases: ------------
##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})
##    test_rpc.ExecuteSQL('balance', "update T_EXPORT set priority = -1 where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id" , {'client_id': client_id })
##    while 1==1:
##        state = test_rpc.ExecuteSQL('balance', "select state from T_EXPORT where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id" , {'client_id': client_id })[0]['state']
##        print(state)
##        time.sleep(3)
##        if state == 1: break
##
##    print '{0}, {1}'.format (order_id, service_order_id)
##
##    service_order_id = mtl.get_next_service_order_id(service_id)
##    order_id = mtl.create_or_update_order (order_owner, product_id, service_id, service_order_id,
##        {'TEXT':'Py_Test order'}, agency_id = agency_id, manager_uid = manager_uid)


##    for info in scenario:
##        locals().update(info)
##        client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
##        scenario['client_id'] = client_id
##        campaigns_list = [
##          {'client_id': client_id, 'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt}
####        , {'client_id': client_id2, 'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt}
##        ]
##        invoice_id, orders_list = mtl.create_force_invoice(client_id, person_id, campaigns_list, paysys_id,
##        invoice_dt, agency_id = agency_id, credit = 1, contract_id = contract_id, overdraft = 0, manager_uid = manager_uid)
##
##        mtl.OEBS_payment(invoice_id, None, None)
##    Print(scenario)
##
##
##header = 'service_id', 'product_id', 'qty', 'completions'
##scenario = [dict(zip(header, line)) for line in scenario]
##Print(scenario)
