#-*- coding: utf-8 -*-

##from MTest import mtl
from MTestlib import MTestlib as mtl
import xmlrpclib
import pprint
import datetime
import time
import urlparse
import webbrowser
import subprocess
import os

def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')

rpc = mtl.rpc
test_rpc = mtl.test_rpc

##Клиент
uid = 'clientuid34'
is_agency = False

##paysys_id
##1001
##1003
##1029 USA

##product_id = 503162; ## RUB

##Дата открутки
##qty2 = 200
##campaigns_dt = datetime.datetime(2014,10,13)
##act_dt       = datetime.datetime(2014,10,4)
##migrate_dt   = datetime.datetime(2014,10,5)
################################

####Дата перехода на мультивалютность
##migration_date = datetime.datetime(2014,1,2)
####Заказ  / Реквест
##qty = 600
##request_dt =datetime.datetime.now() ##не меняется
##begin_dt     = datetime.datetime(2014,1,5)
####(2014,1,5)
##invoice_dt = datetime.datetime(2014,1,5)
####(2014,1,5)
##
####Оплата счета
##payment_dt =datetime.datetime(2014,1,6)
####(2014,1,6)
##paysys_id    = 1001
##is_credit=0
##overdraft = 0
##Сервис+продукт

##request_dt =datetime.datetime(2014,1,5) ##не меняется
service_id = 7;
product_id = 1475;
##product_id = 503162;
##product_id =503165; ## UAH
##503162; // RUB

##service_id = 50;
##product_id = 504446;

##service_id = 11;
##product_id = 2136;

##service_id = 70;
##product_id = 503829;
def test_client():

##########################################  Фишечный овердрафт ########################################################
######   Заказ  / Реквест
##    uid = 'clientuid34'
##    is_agency = False
##    qty = 2000
##    begin_dt     = datetime.datetime(2014,1,5)
##    invoice_dt = datetime.datetime(2014,1,5)
##    request_dt =datetime.datetime.now()
##    ##Оплата счета
##    payment_dt =datetime.datetime(2014,1,6)
##    ##(2014,1,6)
##    paysys_id    = 1001
##    is_credit=0
##    overdraft = 0
#########################
##    client_id = mtl.create_client({'IS_AGENCY': 0})
####  Перепривязка uid
##    mtl.link_client_uid(client_id, uid)
##    ##Плательщик-физик
##    person_id = mtl.create_person(client_id, 'ph')
##    ##Service_Order_ID
####    client_id = 27765120
####    person_id = 2936579
##    service_order_id = mtl.get_next_service_order_id(service_id)
##   ########Заказ и счет на клиента
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
##    request_id = mtl.create_request (client_id, orders_list,overdraft, invoice_dt)
##     ## Счет обычный
##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit)
##    mtl.OEBS_payment(invoice_id, None, None)
####Получение овердрафта
##    mtl.get_overdraft(0,service_order_id,invoice_id,service_id,client_id,120)

######################################### Валютный овердрафт ##############################################################
####
##    migration_date = datetime.datetime(2014,1,2)
##    ##Заказ  / Реквест
##    qty = 60000
##    begin_dt     = datetime.datetime(2014,1,5)
##    ##(2014,1,5)
##    invoice_dt = datetime.datetime(2014,1,5)
##    ##(2014,1,5)
##
##    ##Оплата счета
##    payment_dt =datetime.datetime(2014,1,6)
##    ##(2014,1,6)
##    paysys_id    = 1001
##    is_credit=0
##    overdraft = 0
####################################
##    client_id = mtl.create_client({'IS_AGENCY': 0})
##  #####  client_id = 10852542
##    tm.Balance.CreateClient(205303367,{'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB',
##	'MIGRATE_TO_CURRENCY': datetime.datetime.now(), 'SERVICE_ID': 7})
##    ##Меняем дату миграции
##    test.ExecuteSQL('update (select * from t_client_service_data where class_id= :client_id ) set migrate_to_currency = to_date(:migration_date,\'DD.MM.YYYY HH24:MI:SS\')', {'client_id': client_id, 'migration_date': migration_date })
######    client_id =10487992
######    person_id = 2859155
####  Перепривязка uid
##    mtl.link_client_uid(client_id, uid)
##    ##Плательщик-физик
##    person_id = mtl.create_person(client_id, 'ph')
##    ##Service_Order_ID
##    service_order_id = mtl.get_next_service_order_id(service_id)
####
##########Заказ и счет на клиента
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
##    request_id = mtl.create_request (client_id, orders_list,overdraft, invoice_dt)
##     ## Счет обычный
##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit)
##    mtl.OEBS_payment(invoice_id, None, None)
####Получение овердрафта
##    mtl.get_overdraft(1,service_order_id,invoice_id,service_id,client_id,1500)



####################################################### Реквест с 2мя заказами ###############
##
    ##Заказ  / Реквест
##    qty_1 = 200
##    qty_2 = 300
##    begin_dt     = datetime.datetime.now()
##    invoice_dt = datetime.datetime.now()
##     ####Оплата счета
##    payment_dt =datetime.datetime.now()
##    ####(2014,1,6)
##    paysys_id    = 1001
##    is_credit=0
##    overdraft = 1
####
############## Счет в офердрафт
##    client_id = 10189683
##    person_id = 2850464
##    ####Service_Order_ID
##    service_order_id_1 = mtl.get_next_service_order_id(service_id)
##    service_order_id_2 = mtl.get_next_service_order_id(service_id)
##    ####Заказ и счет на клиента
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id_1,  {'TEXT':'Py_Test order'})
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id_2,  {'TEXT':'Py_Test order'})
##    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id_1, 'Qty': qty_1, 'BeginDT': begin_dt},
##    {'ServiceID': service_id, 'ServiceOrderID': service_order_id_2, 'Qty': qty_2, 'BeginDT': begin_dt}
##        ]
##    request_id = mtl.create_request (client_id, orders_list, overdraft, invoice_dt)
######      Счет в овердрафт
##    invoice_id = mtl.create_invoice_in_oferdraft(uid, service_id, service_order_id_1, service_order_id_2, qty, paysys_id, overdraft)
##


####################################################### Счет на клиента в овердрафт ###############
##
    ##Заказ  / Реквест
##    qty = 5
####    begin_dt     = datetime.datetime(2014,10,5)
####    invoice_dt = datetime.datetime(2014,10,5)
####    payment_dt =datetime.datetime(2014,10,5)
##
##    begin_dt     = datetime.datetime.now()
##    invoice_dt = datetime.datetime.now()
##    payment_dt =datetime.datetime.now()
##    ####(2014,1,6)
##    paysys_id    = 1001
##    is_credit=0
##    overdraft = 1
##
####
######    client_id = mtl.create_client({'IS_AGENCY': 0})
######  Перепривязка uid
######    mtl.link_client_uid(client_id, uid)
####    ##Плательщик-физик
######    person_id = mtl.create_person(client_id, 'ph')
############## Счет в офердрафт
##    client_id = 27997571
##    person_id = 3092938
##    ####Service_Order_ID
##    service_order_id = mtl.get_next_service_order_id(service_id)
##    ####Заказ и счет на клиента
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id,  {'TEXT':'Py_Test order'})
##    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}]
##    request_id = mtl.create_request (client_id, orders_list, overdraft, invoice_dt)
########    ### Счет в овердрафт
########   invoice_id = mtl.create_invoice_in_oferdraft(uid, service_id, service_order_id, qty, paysys_id, overdraft)
##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit, overdraft)
####  mtl.OEBS_payment(invoice_id, None, None)


############################### Выставление счета на конкретного клиента (БЕЗ овердрафта)##########################################
##    client_id = mtl.create_client({'IS_AGENCY': 0})
 ##########  Перепривязка uid
##    mtl.link_client_uid(client_id, uid)
#########   Плательщик-физик
##    person_id = mtl.create_person(client_id, 'ph')
######
##############
    client_id = 28067808
##    person_id = mtl.create_person(client_id, 'ph')
    person_id = 3120122
############
    qty= 1
    begin_dt     = datetime.datetime.now()
    invoice_dt = datetime.datetime.now()
     ####Оплата счета
    payment_dt =datetime.datetime.now()
    ####(2014,1,6)
    paysys_id    = 1003
    is_credit=0
    overdraft=1

##################

    ##Service_Order_ID
    service_order_id = mtl.get_next_service_order_id(service_id)
    ##Заказ и счет на клиента
    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
    orders_list = []
    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
    request_id = mtl.create_request (client_id, orders_list, begin_dt)
    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit,None, overdraft )
    mtl.OEBS_payment(invoice_id, None, None)

 #######Договор
####    contract_id = mtl.create_contract(client_id,person_id,'comm')

## Оплата счета
##    mtl.OEBS_payment(invoice_id, None, None)



 ###Заказ на клиента, счет на агентство
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,agency_id,   {'TEXT':'Py_Test order'})
##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
##    request_id = mtl.create_request (agency_id, orders_list, request_dt)
##     ## Счет обычный
##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit)

test_client()





