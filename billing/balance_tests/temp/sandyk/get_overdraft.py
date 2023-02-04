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
##product_id = 503162
qty= 3

##service_id = 99
##product_id = 504613

dt = datetime.datetime.now()
paysys_id    = 1001
is_credit=0
overdraft=0

##504445		Пополнение лицевого счета по договору (без НДС)
##504446     	Пополнение лицевого счета по договору (с НДС)

def test_client():
    client_id = 29384159
##    person_id = 4502325
##    client_id = mtl.create_client({'IS_AGENCY': 0})
##    mtl.link_client_uid(client_id, uid)
##    person_id = mtl.create_person(client_id, 'ph',{'email': 'test-balance-notify@yandex-team.ru'})
##
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
    orders_list = []
    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': datetime.datetime.now()})
    request_id = mtl.create_request (client_id, orders_list, datetime.datetime.now())


##    mtl.act_accounter (29331209, 1, datetime.datetime(2015,4,30))

##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit, overdraft)
##    mtl.OEBS_payment(invoice_id, None, None)
##
##    test_rpc.TestBalance.OldCampaigns({'Bucks': 5, 'stop': '0', 'service_id': service_id, 'service_order_id': orders_list[0]['ServiceOrderID']}, datetime.datetime(2014,10,15))
##    test_rpc.TestBalance.OldAct(invoice_id, datetime.datetime(2014,10,15))
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