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
qty= 5

service_id = 99
product_id = 504613

dt = datetime.datetime.now()
paysys_id    = 1001
is_credit=0
overdraft=0

##504445		Пополнение лицевого счета по договору (без НДС)
##504446     	Пополнение лицевого счета по договору (с НДС)

def test_client():
##    client_id = mtl.create_client({'IS_AGENCY': 0})
##    mtl.link_client_uid(client_id, uid)''
##    person_id = mtl.create_person(client_id, 'ur_autoru',{'email': 'test-balance-notify@yandex-team.ru'})
    client_id = 29352328
##    person_id =
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
    orders_list = []
    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': datetime.datetime.now()})
    request_id = mtl.create_request (client_id, orders_list, datetime.datetime.now())




test_client()