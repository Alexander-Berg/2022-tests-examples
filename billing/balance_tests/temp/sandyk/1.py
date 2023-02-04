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

uid = 'clientuid34'

##Клиент
uid = 'clientuid34'
is_agency = False

##Заказ  / Реквест
qty = 1000
request_dt =datetime.datetime.now() ##не меняется
begin_dt     = datetime.datetime.now()
invoiceDate = datetime.datetime.now()
payment_dt =datetime.datetime.now()

service_id = 7;
product_id = 1475;

def test_client():
    client_id = mtl.create_client({'IS_AGENCY': 0},)
    person_id = mtl.create_person(client_id, 'usp')
##########
    qty= 20
    begin_dt     = datetime.datetime.now()
    invoice_dt = datetime.datetime.now()
######Оплата счета
    payment_dt =datetime.datetime.now()
    paysys_id    = 1029
    is_credit=0
    overdraft=0

##################
    service_order_id = mtl.get_next_service_order_id(service_id)
    ##Заказ и счет на клиента
    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
    orders_list = []
    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
    request_id = mtl.create_request (client_id, orders_list, begin_dt)
##    contract_id = mtl.create_contract(client_id, person_id, service_id ,'usa')
##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit, overdraft, contract_id)
##    mtl.OEBS_payment(invoice_id, None, None)
##    url = '[MT]: https://balance-admin.greed-tm1f.yandex.ru/invoice-publish.xml?ft=html&object_id='+ str(invoice_id)
##    return url
##
##url =
test_client()
##print url
