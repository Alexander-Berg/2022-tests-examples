#-*- coding: utf-8 -*-

from MTestlib import MTestlib as mtl
import xmlrpclib
import pprint
import datetime
import urlparse
import time

def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')

rpc = mtl.rpc
test_rpc = mtl.test_rpc

##Клиент
uid = 'clientuid34'
is_agency = False

##service_id = 11;
##product_id = 506537;

service_id = 70;
product_id = 505067;

##service_id = 50;
##product_id = 504446

def test_client():

############################### Выставление счета на конкретного клиента (БЕЗ овердрафта)##########################################
##    client_id = mtl.create_client({'IS_AGENCY': 1})
##    person_id = mtl.create_person(client_id, 'ph')
##
##    begin_dt     = datetime.datetime(2015,9 ,15)
##
##    campaigns_dt=datetime.datetime(2015,9 ,15)
##
##
##    client_id=29962480
##    person_id=3684578
##    contract_id
##    contract_id = mtl.create_contract2('mine', {'client_id':client_id, 'person_id':person_id})
##    service_order_id = mtl.get_next_service_order_id(service_id)
##    qty = 250000
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
##    request_id = mtl.create_request (client_id, orders_list, begin_dt)

##
##
##    invoice_id = mtl.create_invoice (request_id, person_id, 1033, 1,contract_id, 0)
##
##    service_order_id = 22452025
##    invoice_id = 42808252
##    mtl.do_campaigns(service_id, service_order_id, {'Shows': 250000, 'Money': 0}, 0, campaigns_dt)
    mtl.create_act(invoice_id, campaigns_dt)




##    invoice_id = 42698909
##    mtl.OEBS_payment(42808564, 90000, campaigns_dt)
##    external_id = test_rpc.ExecuteSQL("select amount, external_id from t_invoice where id = :invoice_id", {'invoice_id': invoice_id})[0]['external_id']
##    amount = test_rpc.ExecuteSQL("select total_sum from t_invoice where id = :invoice_id", {'invoice_id': invoice_id})[0]['total_sum']
##    max_cash_id = test_rpc.ExecuteSQL("select max(xxar_cash_fact_id) as max_id from t_oebs_cash_payment_fact")[0]['max_id']
##    sql = '''Insert into t_oebs_cash_payment_fact (XXAR_CASH_FACT_ID,AMOUNT,RECEIPT_NUMBER,RECEIPT_DATE,LAST_UPDATED_BY,LAST_UPDATE_DATE,LAST_UPDATE_LOGIN,CREATED_BY,CREATION_DATE,CASH_RECEIPT_NUMBER,ACC_NUMBER,PAYMENT_NUMBER,PAYMENT_DATE,OPERATION_TYPE,SOURCE_TYPE,COMISS_DATE)
##    values (:max_cash_id,:amount,:external_id,trunc(sysdate),'-1',trunc(sysdate),'-1','-1',trunc(sysdate),:external_id_mod,null,null,null,null,null,null)'''
####    print '{0}-99999'.format(external_id)
##    sql_params = {'max_cash_id': max_cash_id+1, 'amount': amount, 'external_id': external_id, 'external_id_mod':'Б-1916360-1-73862'}
##    test_rpc.ExecuteSQL(sql, sql_params)
##    time.sleep(15)
##    sql = "select state as val from T_EXPORT where type = 'PROCESS_PAYMENTS' and object_id = :invoice_id"
##    sql_params = {'invoice_id': invoice_id}
##    mtl.wait_for(sql, sql_params, value = 1)



test_client()
##print url




