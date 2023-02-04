#-*- coding: utf-8 -*-

##from MTest import mtl
from MTestlib import MTestlib as mtl
import xmlrpclib
import pprint
import datetime
import time
import urlparse

def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')

rpc = mtl.rpc
test_rpc = mtl.test_rpc

uid = 'clientuid34'

service_id = 7;
product_id = 1475
qty= 3000

dt = datetime.datetime.now()
paysys_id    = 1001
is_credit=0
overdraft=0

def test_client():

##
    client_id = 29227420
    person_id = 4437131
##    service_order_id =402278689
##    order_id = 15262459

##    test_rpc.ExecuteSQL('Insert into t_client_overdraft (CLIENT_ID,SERVICE_ID,OVERDRAFT_LIMIT,FIRM_ID,START_DT,UPDATE_DT,CURRENCY) values (client_id :client_id, 7, 120, 1, to_date(migration_dt:migration_dt,\'DD.MM.YYYY HH24:MI:SS\'), to_date(current_dt:current_dt,\'DD.MM.YYYY HH24:MI:SS\'),null)', {'migration_dt':migration_dt.strftime(sql_date_format),'client_id': client_id,'external_id':external_id})
##    service_order_id = mtl.get_next_service_order_id(service_id)
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': datetime.datetime(2015,4,5)})
##    request_id = mtl.create_request (client_id, orders_list, invoice_dt=datetime.datetime.now(), props={'QtyIsAmount': 1})



####
####
##    test_rpc.TestBalance.OldCampaigns({'Bucks': 2, 'stop': '0', 'service_id': service_id, 'service_order_id': orders_list[0]['ServiceOrderID']}, datetime.datetime(2015,4,5))
##
##    rpc.Balance.CreateClient(205303367,{'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB',
##	'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'currency_convert_type':'MODIFY'})
##    ##Меняем дату миграции
##    test_rpc.ExecuteSQL('update (select * from t_client_service_data where class_id= :client_id ) set migrate_to_currency = to_date(:migration_date,\'DD.MM.YYYY HH24:MI:SS\')', {'client_id': client_id, 'migration_date': datetime.datetime(2015,4,14) })
    service_order_id=402278823

##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
##
    orders_list = []
    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 1000, 'BeginDT': datetime.datetime(2015,4,5)})


    request_id = mtl.create_request (client_id, orders_list, invoice_dt=datetime.datetime.now(), props={'QtyIsAmount': 1})


##    mtl.link_client_uid(client_id, uid)
##    mtl.get_overdraft(client_id,service_id,120,2,datetime.datetime.now(),'UAH','UAH')


##    mtl.get_overdraft(client_id,service_id,120,1)
##    client_id, service_id, limit, firm_id=1, start_dt=datetime.datetime.now(), currency=None, invoice_currency=None):

##    client_id =29123602
##    person_id = 4386177
####    person_id = mtl.create_person(client_id, 'ph')
##    qty= 20
####
##    campaigns_dt = datetime.datetime.now()
##    begin_dt = datetime.datetime.now()
####
##    service_order_id = mtl.get_next_service_order_id(service_id)
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
##    request_id = mtl.create_request (client_id, orders_list, begin_dt)
##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit,None, overdraft)
##    service_order_id = 394239882
##
##    mtl.do_campaigns(service_id, service_order_id, {'Bucks': 7, 'Money': 0}, 0, campaigns_dt)

test_client()