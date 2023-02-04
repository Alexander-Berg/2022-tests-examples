# -*- coding: utf-8 -*-

import xmlrpclib
import pprint
import datetime
import subprocess

from temp.MTestlib import MTestlib


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


TM_url = "http://greed-tm1f.yandex.ru:8002/xmlrpc"
TS_url = "http://greed-ts1f.yandex.ru:8002/xmlrpc"
TEST_url = 'http://xmlrpc.balance.greed-tm1f.yandex.ru:30702/xmlrpc'

XMLRPC_URL = TM_url
XMLRPC_URL_1 = TEST_url

tm = xmlrpclib.ServerProxy(XMLRPC_URL, allow_none=1, use_datetime=1)
test = xmlrpclib.ServerProxy(XMLRPC_URL_1, allow_none=1, use_datetime=1)
##------------------------------------------------------------------------------

uid = 'ulrich666'
##Заказ  / Реквест
service_id = 7
product_id = 1475
qty = 100
begin_dt = datetime.datetime.now()
request_dt = datetime.datetime.now()  ##не меняется
invoice_dt = datetime.datetime.now()
paysys_id = 1001
##Оплата счета
payment_dt = datetime.datetime(2014, 10, 2)
##Дата открутки
qty2 = 200
campaigns_dt = datetime.datetime(2014, 10, 13)
act_dt = datetime.datetime(2014, 10, 4)
migrate_dt = datetime.datetime(2014, 10, 5)


##------------------------------------------------------------------------------

##def test_client(client_params=None, person_params=None):#client_id=None, type_ = "pu", lname='Ivan'):
def test_client():
    ## Клиент
    client_id = MTestlib.create_client({'IS_AGENCY': 0})
    ##agency_id = create_client({'IS_AGENCY' = 1})
    ## Привязка к UID
    MTestlib.link_client_uid(client_id, uid)
    ## Плательщки
    person_id = MTestlib.create_person(client_id, 'ph', {'phone': '234'})
    ## Service_order_id
    ##    agency_id = 10165447
    ##    client_id = 10165453
    ##    contract_id = 220258
    ##    person_id = 2848516
    service_order_id = MTestlib.get_next_service_order_id(service_id)
    ## Заказ (+manager_code)
    order_id = MTestlib.create_or_update_order(client_id, product_id, service_id, service_order_id,
                                               {'TEXT': 'Py_Test order'})
    ## Реквест
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = MTestlib.create_request(client_id, orders_list, request_dt)
    ## Счет (+agency_id, external_id)
    invoice_id = MTestlib.create_invoice(request_id, person_id, paysys_id)
    ## Счет одним вызовом (+agency_id, external_id)
    ##    campaigns_list = []
    ##    campaigns_list.append({'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt})
    ##    campaigns_list.append({'service_id': service_id, 'product_id': product_id, 'qty': qty2, 'begin_dt': begin_dt})
    ##    invoice_id, orders_list = MTestlib.create_force_invoice(client_id, person_id, campaigns_list, paysys_id, invoice_dt)
    ## Оплата счета
    MTestlib.OEBS_payment(invoice_id, None, None)
    ##    MTestlib.get_overdraft(0, service_order_id, invoice_id, service_id, client_id, 50)
    ## Открутки
    ##    MTestlib.do_campaigns(service_id, service_order_id, {'Bucks': 10, 'Money': 0}, 0, campaigns_dt)
    ## Открутки для случая с несколькими заказами в счёте
    ##    MTestlib.do_campaigns(orders_list[0]['ServiceID'], orders_list[0]['ServiceOrderID'], {'Bucks': 10, 'Money': 0}, 0, None)
    ## Делаем акт
    ##    act_id = MTestlib.create_act(invoice_id, None)
    ## Прцедура генерации актов
    ##    MTestlib.generate_acts(client_id, 1, campaigns_dt)
    ##------------------------------------------------------------------------------
    ##Путь до браузера
    opera = 'C:\Program Files (x86)\Opera\launcher.exe'
    opera_args = 'https://balance-admin.greed-tm1f.yandex.ru/passports.xml?tcl_id=%s' % client_id
    spOpera = subprocess.Popen(opera + ' ' + opera_args)
    print opera_args


test_client()
