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

dt = datetime.datetime.now()

def test_client():

    client_id = mtl.create_client({'IS_AGENCY': 0})
##    mtl.link_client_uid(client_id, uid)
##    mtl.get_overdraft(client_id,service_id,120,2,datetime.datetime.now(),'UAH','UAH')
##    mtl.get_overdraft(client_id,service_id,120,1)
##    client_id =29165036
##    person_id = 4407960
##    person_id = mtl.create_person(client_id, 'ph')
    qty= 20
####
    campaigns_dt = datetime.datetime.now()
    begin_dt = datetime.datetime.now()
    paysys_id    = 1001
    is_credit=0
    overdraft=1
##валютный
    mtl.get_overdraft(client_id,service_id,2000,1,datetime.datetime.now(),'RUB','RUB')

####
##    service_order_id = mtl.get_next_service_order_id(service_id)
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
##    request_id = mtl.create_request (client_id, orders_list, begin_dt)
##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit,None, overdraft)
##    service_order_id = 394239992
##(request_id, person_id, paysys_id, credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
##    mtl.do_campaigns(service_id, service_order_id, {'Bucks': 7, 'Money': 0}, 0, campaigns_dt)

test_client()