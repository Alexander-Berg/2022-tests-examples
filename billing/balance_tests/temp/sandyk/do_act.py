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
##product_id = 503162

service_id = 7
##product_id = 1475

##service_id = 99
##product_id =504561

##service_id = 35;
product_id = 1475
qty= 300


##service_id = 99
##product_id = 504613

##dt = datetime.datetime.now()
##paysys_id    = 1001
##is_credit=0
##overdraft=0

##504445		Пополнение лицевого счета по договору (без НДС)
##504446     	Пополнение лицевого счета по договору (с НДС)

def test_client():
##
##    client_id = mtl.create_client({'IS_AGENCY': 1})
##
##    order_owner   = client_id
##    invoice_owner = client_id
##
##    manager_uid = None
##
##    if order_owner == invoice_owner: agency_id = None
##
##    person_id = None or mtl.create_person(invoice_owner, 'ph', {'phone':'234'})
##
##    service_order_id = mtl.get_next_service_order_id(service_id)
##
##    order_id = mtl.create_or_update_order (order_owner, product_id, service_id, service_order_id,
##        {'TEXT':'Py_Test order'}, agency_id = agency_id, manager_uid = manager_uid)
##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': datetime.datetime(2015,4,16)})
##    request_id = mtl.create_request (invoice_owner, orders_list, datetime.datetime(2015,4,16))
##    invoice_id = mtl.create_invoice (request_id, person_id, 1001, 0, None,overdraft = 0)
##    mtl.OEBS_payment(42415895, None, datetime.datetime(2015,4,    16))
    service_order_id =17491587
    mtl.do_campaigns(service_id, service_order_id, {'Bucks': 20, 'Money': 0}, 0, datetime.datetime(2015,4,16))


##
##    person_id = 4502325
##    client_id = mtl.create_client({'IS_AGENCY': 0})
##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': 225, 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': service_id, 'CURRENCY_CONVERT_TYPE': 'COPY'})

##    mtl.link_client_uid(client_id, uid)
##    person_id = mtl.create_person(client_id, 'ph',{'email': 'test-balance-notify@yandex-team.ru'})
##    contract_id = mtl.create_contract2('no_agency',{'client_id': client_id, 'person_id': person_id,'is_signed': '2015-03-01T00:00:00'})

##    service_order_id = mtl.get_next_service_order_id(service_id)
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': datetime.datetime.now()})
##    request_id = mtl.create_request (29562335, orders_list, datetime.datetime.now())

##
##    invoice_id = mtl.create_invoice (request_id, person_id, 1001, 0, None,overdraft = 0)
##    mtl.OEBS_payment(42412152, None, datetime.datetime(2015,4,16))
##    service_order_id  =17490242
##    mtl.do_campaigns(service_id, service_order_id, {'Bucks': 150, 'Money': 0}, 0, datetime.datetime(2015,4,16))
##    mtl.act_accounter(client_id, 1, datetime.datetime(2015,2,15))
##
##    service_order_id  =417393078
##    mtl.do_campaigns(service_id, service_order_id, {'Bucks': 200, 'Money': 0}, 0, datetime.datetime.now())
##    mtl.act_accounter(client_id, 1, datetime.datetime.now())


##    mtl.act_accounter (29331209, 1, datetime.datetime(2015,4,30))
##
##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit, overdraft)
##    mtl.OEBS_payment(39423342, 61.5, None)


##
##    test_rpc.TestBalance.OldCampaigns({'Bucks': 2, 'stop': '0', 'service_id': 7, 'service_order_id': 412372485}, datetime.datetime(2015,5,16))
##    test_rpc.TestBalance.OldAct(39369079, datetime.datetime(2015,5,16))






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