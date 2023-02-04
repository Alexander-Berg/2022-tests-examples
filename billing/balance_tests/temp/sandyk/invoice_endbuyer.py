#-*- coding: utf-8 -*-

from MTestlib import MTestlib as mtl
import xmlrpclib
import pprint
import time
import urlparse
import os
import subprocess
import datetime
from datetime import date,timedelta

def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')

rpc = mtl.rpc
test_rpc = mtl.test_rpc

##Клиент
uid = 'clientuid34'
is_agency = False

##request_dt =datetime.datetime(2014,1,5) ##не меняется
service_id = 7;
product_id = 1475;
##product_id = 503162;
##product_id =503165; ## UAH
##503162; // RUB

##service_id = 11;
##product_id = 2136;

##service_id = 70;
##product_id = 503829;
def test_client():

############################# Выставление счета на конкретного клиента (БЕЗ овердрафта)##########################################

############
##    client_id = 10283299
##    person_id = 2852084
############
    qty= 450
    begin_dt     = datetime.datetime.now()
##    (2014,12,15)
    invoice_dt = datetime.datetime.now()
     ####Оплата счета
    payment_dt =datetime.datetime.now()
    ####(2014,1,6)
    paysys_id    = 1001
    is_credit=0
    overdraft=0
##
##################
##

    client_id = mtl.create_client({'IS_AGENCY': 1})
    mtl.oebs_export('Client', client_id )
######  Перепривязка uid
    mtl.link_client_uid(client_id, uid)
#####   Плательщик-физик
    person_id = mtl.create_person(client_id, 'ur', {'name': 'ООО "Сидоров индастриз"'})
    mtl.oebs_export('Person', person_id )
##    ##Service_Order_ID
    contract_id = mtl.create_contract(client_id, person_id, service_id, 'commiss')
    mtl.oebs_export('Contract', contract_id )

    person_id = mtl.create_person(client_id, 'endbuyer_ur')
##    , {'inn': '890202368050', 'name': 'ООО "Сидоров индастриз"'})
##    invoice_id = mtl.create_invoice (request_id, person_id, 1001, is_credit, overdraft, contract_id)
    service_order_id = mtl.get_next_service_order_id(service_id)
    ##Заказ и счет на клиента
    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
    orders_list = []
    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
    request_id = mtl.create_request (client_id, orders_list, begin_dt)
##    mtl.OEBS_payment(invoice_id, None, None)



test_client()





