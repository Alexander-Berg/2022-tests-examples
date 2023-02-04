# -*- coding: utf-8 -*-

##from temp.MTestlib import mtl
import datetime
import pprint
import time

from temp.MTestlib import MTestlib as mtl


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


rpc = mtl.rpc
test_rpc = mtl.test_rpc

uid = 'clientuid33'
##Заказ  / Реквест
service_id = 7;
product_id = 1475  ##503162
##service_id = 11; product_id = 2136
##service_id = 70; product_id = 502761
qty = 6.641929

##today =
##after = datetime.datetime.now()
##after = datetime.datetime(2014,12,29)
after = datetime.datetime(2015, 1, 1, 11, 0, 0)

begin_dt = after
request_dt = after  ##не меняется
invoice_dt = after
paysys_id = 1003
##Оплата счета
payment_dt = after
##Дата открутки
qty2 = 200
campaigns_dt = after
act_dt = after
migrate_dt = after
##manager_uid = '241593318'
manager_uid = None


##------------------------------------------------------------------------------

##def test_client(client_params=None, person_params=None):#client_id=None, type_ = "pu", lname='Ivan'):
def test_client():
    ## Клиент
    ##    client_id = 27882241 or mtl.create_client({'IS_AGENCY': 0})
    client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    ##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})  ##mass
    order_owner = client_id
    invoice_owner = client_id
    if order_owner == invoice_owner: agency_id = None
    mtl.link_client_uid(invoice_owner, 'clientuid33')
    person_id = None or mtl.create_person(invoice_owner, 'ur', {'phone': '234'})
    ## Service_order_id
    ##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '187', 'CURRENCY': 'UAH', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})

    ## Счет одним вызовом (+agency_id, external_id)
    campaigns_list = [
        {'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt}
    ]
    invoice_id, orders_list = mtl.create_force_invoice(client_id, person_id, campaigns_list, paysys_id,
                                                       invoice_dt, agency_id=agency_id, manager_uid=manager_uid)

    mtl.OEBS_payment(invoice_id, None, None)
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)

    mtl.do_campaigns(7, orders_list[0]['ServiceOrderID'], {'Bucks': 3.57, 'Money': 0}, 0, campaigns_dt)
    print '... (1)';
    time.sleep(1)
    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB',
                       'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7,
                       'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    print '... (5)';
    time.sleep(5)
    mtl.do_campaigns(7, orders_list[0]['ServiceOrderID'], {'Bucks': 3.57, 'Money': 1}, 0, campaigns_dt)
    print '... (1)';
    time.sleep(1)
    mtl.do_campaigns(7, orders_list[0]['ServiceOrderID'], {'Bucks': 3.57, 'Money': 0}, 0, campaigns_dt)
    print '... (1)';
    time.sleep(1)

    rpc.Balance.CreateTransferMultiple(16571028, [
        {"QtyOld": "199.260000", "ServiceOrderID": orders_list[0]['ServiceOrderID'], "ServiceID": 7,
         "QtyNew": "107.100000"}], [{"QtyDelta": "92.158000", "ServiceOrderID": service_order_id, "ServiceID": 7}])

    rpc.Balance.CreateTransferMultiple(16571028, [
        {"QtyOld": "199.260000", "ServiceOrderID": orders_list[0]['ServiceOrderID'], "ServiceID": 7,
         "QtyNew": "107.100000"}], [{"QtyDelta": "92.158000", "ServiceOrderID": service_order_id, "ServiceID": 7}])

    ##    service_order_id = 329488068
    ## Заказ (+manager_code)


    ##    service_order_id2 = mtl.get_next_service_order_id(service_id)
    ##    order_id2 = mtl.create_or_update_order (order_owner, product_id, service_id, service_order_id2,
    ##        {'TEXT':'Py_Test order'}, agency_id = agency_id, manager_uid = manager_uid)
    ##
    ##    service_order_id3 = mtl.get_next_service_order_id(service_id)
    ##    order_id3 = mtl.create_or_update_order (client_id3, product_id, service_id, service_order_id3,
    ##        {'TEXT':'Py_Test order'}, agency_id = agency_id, manager_uid = manager_uid)

    ##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7})

    ##    service_order_id3 = mtl.get_next_service_order_id(service_id)
    ##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id,
    ##        {'TEXT':'Py_Test order'}, agency_id = agency_id, manager_uid = manager_uid)
    ## Реквест
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
        ##        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': qty, 'BeginDT': begin_dt}
        ##        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id3, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, request_dt)
    ##    request_id2 = mtl.create_request (agency_id, orders_list, request_dt)
    ##    request_id3 = mtl.create_request (agency_id, orders_list, request_dt)
    ## Счет (+agency_id, external_id)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, overdraft=0)


##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, credit = 1, contract_id=contract_id['ID'])

##------------------------------------------------------------------------------

test_client()
