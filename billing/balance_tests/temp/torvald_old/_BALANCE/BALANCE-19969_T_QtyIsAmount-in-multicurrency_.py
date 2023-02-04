# -*- coding: utf-8 -*-

import pprint
import datetime
import time

from temp.MTestlib import MTestlib as mtl


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


rpc = mtl.rpc
test_rpc = mtl.test_rpc

uid = 'clientuid33'
##Заказ  / Реквест
service_id = 7;
product_id = 503162
##service_id = 11; product_id = 2136
##service_id = 70; product_id = 502761
qty = 100

##today =
##after = datetime.datetime.now()
##after = datetime.datetime(2014,12,29)
after = datetime.datetime(2015, 3, 15, 11, 0, 0)
disc_dt = datetime.datetime(2015, 4, 21, 11, 0, 0)

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
manager_uid = None


##manager_uid = '96446401'
##manager_uid2 = '27116496'
##------------------------------------------------------------------------------

##def test_client(client_params=None, person_params=None):#client_id=None, type_ = "pu", lname='Ivan'):
def test_client():
    ## Клиент
    ##    client_id = 27882241 or mtl.create_client({'IS_AGENCY': 0})
    client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB',
                       'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7,
                       'CURRENCY_CONVERT_TYPE': 'MODIFY'})

    time.sleep(3)
    test_rpc.ExecuteSQL(
        "update T_EXPORT set priority = -1 where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id",
        {'client_id': client_id})
    while 1 == 1:
        state = \
            test_rpc.ExecuteSQL(
                "select state from T_EXPORT where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id",
                {'client_id': client_id})[0]['state']
        print(state)
        time.sleep(3)
        if state == 1: break

    agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})  ##mass
    order_owner = client_id
    invoice_owner = client_id
    if order_owner == invoice_owner: agency_id = None
    mtl.link_client_uid(invoice_owner, 'clientuid32')
    person_id = None or mtl.create_person(invoice_owner, 'ur', {'phone': '234'})

    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': disc_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt, {'QtyIsAmount': 1})

    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': disc_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt, {'QtyIsAmount': 0})

    res = mtl.log(rpc.Balance.EstimateDiscount)(
        {'ClientID': agency_id, 'PaysysID': paysys_id, 'ContractID': contract_id}, [
            {'ProductID': product_id, 'ClientID': client_id, 'Qty': qty, 'ID': 1, 'BeginDT': disc_dt, 'RegionID': 1,
             'discard_agency_discount': 0}])
    print res
    print "rpc.Balance.EstimateDiscount({'ClientID': %d, 'PaysysID': %d, 'ContractID': %d}, [{'ProductID': %d, 'ClientID': %d, 'Qty': %d, 'ID': 1, 'BeginDT': %s, 'RegionID': 1, 'discard_agency_discount': 0}])" % (
        agency_id, paysys_id, contract_id, product_id, client_id, qty, 'datetime.datetime(2015,4,21,0,0,0)')

    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=1, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)


test_client()
