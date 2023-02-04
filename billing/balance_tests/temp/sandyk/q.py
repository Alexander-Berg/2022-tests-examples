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

##service_id = 7;
##product_id = 1475
##product_id = 503162  ##rub
##product_id =503165  ##uah
service_id = 11
product_id = 2136

qty= 50

##service_id = 99
##product_id = 504613

dt = datetime.datetime.now()
paysys_id    = 1001
is_credit=0
overdraft=0

##504445		Пополнение лицевого счета по договору (без НДС)
##504446     	Пополнение лицевого счета по договору (с НДС)

def test_client():
##    client_id = 29384159
##    person_id = 4502325
##    client_id = mtl.create_client({'IS_AGENCY': 0})
##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})
##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '187', 'CURRENCY': 'UAH', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})

##    test_rpc.ExecuteSQL("update T_EXPORT set priority = -1 where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id" , {'client_id': client_id })
##    while 1==1:
##            state = test_rpc.ExecuteSQL("select state from T_EXPORT where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id" , {'client_id': client_id })[0]['state']
##            print(state)
##            time.sleep(3)
##            if state == 1: break
##    mtl.link_client_uid(client_id, uid)
##    person_id1 = mtl.create_person(client_id, 'ph',{'email': 'test-balance-notify@yandex-team.ru'})
##    person_id2 = mtl.create_person(client_id, 'usp',{'email': 'test-balance-notify@yandex-team.ru'})
####
##    service_order_id1 = mtl.get_next_service_order_id(service_id)
##    service_order_id2 = mtl.get_next_service_order_id(service_id)
##    order_id1 = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id1 ,  {'TEXT':'Py_Test order'})
##    order_id2 = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id2 ,  {'TEXT':'Py_Test order'})


##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': datetime.datetime.now()})
##    print orders_list
##    request_id1 = mtl.create_request (client_id, [{'ServiceID': service_id, 'ServiceOrderID': service_order_id1, 'Qty': 20, 'BeginDT': datetime.datetime.now()},
##    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 30, 'BeginDT': datetime.datetime.now()}], datetime.datetime.now())
##
##    request_id2 = mtl.create_request (client_id, [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 30, 'BeginDT': datetime.datetime.now()}], datetime.datetime.now())
##    mtl.act_accounter (29331209, 1, datetime.datetime(2015,4,30))
##
##    invoice_id1 = mtl.create_invoice (request_id1, person_id1, 1001, is_credit, overdraft)
##    invoice_id2 = mtl.create_invoice (request_id2, person_id2, 1029, is_credit, overdraft)
##    mtl.OEBS_payment(42595905, None, None)
##    mtl.OEBS_payment(invoice_id2, None, None)
##    test_rpc.TestBalance.OldCampaigns({'Bucks': 10, 'stop': '0', 'service_id': service_id, 'service_order_id': service_order_id}, datetime.datetime.now())

    test_rpc.TestBalance.OldCampaigns({'Days': 15, 'stop': '0', 'service_id': 99, 'service_order_id': 22381978}, datetime.datetime.now())

##

##test_rpc.TestBalance.OldAct(invoice_id, datetime.datetime(2014,10,15))
##
##    test_rpc.TestBalance.OldCampaigns({'Bucks': 150, 'stop': '0', 'service_id': service_id, 'service_order_id': orders_list[0]['ServiceOrderID']}, datetime.datetime(2014,12,15))
##    test_rpc.TestBalance.OldAct(invoice_id, datetime.datetime(2014,12,15))
##
##    test_rpc.TestBalance.OldCampaigns({'Bucks': 350, 'stop': '0', 'service_id': service_id, 'service_order_id': orders_list[0]['ServiceOrderID']}, datetime.datetime(2015,1,15))
##    test_rpc.TestBalance.OldAct(invoice_id, datetime.datetime(2015,1,15))
##
##    rpc.Balance.CreateClient(205303367,{'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB',
##	'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7})
##    ##Меняем дату миграции
##    test_rpc.ExecuteSQL('update (select * from t_client_service_data where class_id= :client_id ) set migrate_to_currency = to_date(:migration_date,\'DD.MM.YYYY HH24:MI:SS\')', {'client_id': client_id, 'migration_date': datetime.datetime(2015,1,16) })
##
##    print '1'
##    test_rpc.TestBalance.OldCampaigns({'Money': 4000, 'Bucks': 350,'stop': '0', 'service_id': service_id, 'service_order_id': orders_list[0]['ServiceOrderID']}, datetime.datetime(2015,2,15))
##    test_rpc.TestBalance.OldAct(invoice_id, datetime.datetime(2015,2,15))
##
##
##    test_rpc.TestBalance.OldCampaigns({'Money':3500 , 'Bucks': 350,'stop': '0', 'service_id': service_id, 'service_order_id': orders_list[0]['ServiceOrderID']}, datetime.datetime(2015,3,15))
##    test_rpc.TestBalance.OldAct(invoice_id, datetime.datetime(2015,3,15))

##

test_client()