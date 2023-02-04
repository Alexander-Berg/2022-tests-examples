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
product_id = 1475  ##503162
##service_id = 99; product_id = 504596 ##504697
##service_id = 11; product_id = 2136
##service_id = 70; product_id = 503273
##service_id = 77; product_id = 2584
##service_id = 120; product_id = 504690
##service_id = 114; product_id = 502981
##service_id = 111; product_id = 503352

qty = 80

##today =
after = datetime.datetime.now()
disc_dt = after
##after = datetime.datetime(2014,12,29)
##after = datetime.datetime(2015,5,14,11,0,0)
##disc_dt = datetime.datetime(2015,5,18,11,0,0)

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
    ##    client_id = 29449220 or mtl.create_client({'IS_AGENCY': 0})
    client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    client_id2 = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    client_id3 = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    ##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})  ##mass
    order_owner = client_id
    invoice_owner = client_id
    if order_owner == invoice_owner: agency_id = None
    mtl.link_client_uid(invoice_owner, 'clientuid32')
    person_id = None or mtl.create_person(invoice_owner, 'ur', {'phone': '234'})

    ##    person_id =
    ##    person_id = None or mtl.create_person(invoice_owner, 'yt', {'phone':'234'})

    ##    contract_id = mtl.create_contract2('',{'client_id': invoice_owner, 'person_id': person_id, 'dt': '2015-04-30T00:00:00', 'FINISH_DT': '2016-06-30T00:00:00', 'is_signed': '2015-01-01T00:00:00', 'SERVICES': [7]})
    ##    contract_id2 = mtl.create_contract2('shv_client',{'client_id': client_id2, 'person_id': person_id2, 'dt': '2015-04-30T00:00:00', 'FINISH_DT': '2016-06-30T00:00:00', 'is_signed': '2015-01-01T00:00:00', 'SERVICES': [7]})
    ##    contract_id = mtl.create_contract2('auto_ru_post',{'client_id': invoice_owner, 'person_id': person_id, 'FINISH_DT': '2016-06-30T00:00:00', 'is_signed': '2015-05-01T00:00:00', 'SERVICES': [7]})
    contract_id = None

    ##    amt = 30000
    ##    sql = "Insert into t_client_direct_budget (ID,CLIENT_ID,END_DT,CLASSNAME,BUDGET,CURRENCY,UPDATE_DT) values (s_client_direct_budget_id.nextval,'{0}',date'2015-06-01','DirectDiscountCalculator','{1}',null,sysdate)".format(
    ##    client_id, amt)
    ##    test_rpc.ExecuteSQL(sql);

    print(rpc.Balance.MergeClients(16571028, client_id, client_id2))
    print(rpc.Balance.MergeClients(16571028, client_id, client_id3))

    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, request_dt)

    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)
    ##    mtl.do_campaigns(7,service_order_id , {'Bucks': 13.666666, 'Money': 0}, 0, campaigns_dt)

    ##    service_order_id = mtl.get_next_service_order_id(service_id)
    ##    order_id = mtl.create_or_update_order (order_owner, product_id, service_id, service_order_id,
    ##        {'TEXT':'Py_Test order'}, agency_id = agency_id, manager_uid = manager_uid)
    ##    orders_list = [
    ##        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ##    ]
    ##    request_id = mtl.create_request (invoice_owner, orders_list, request_dt)
    ##
    ##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, credit = 0, contract_id = contract_id, overdraft = 0, endbuyer_id = None)
    ##
    ##    mtl.OEBS_payment(invoice_id)
    ##    mtl.do_campaigns(7,service_order_id , {'Bucks': 13.666666, 'Money': 0}, 0, campaigns_dt)
    ##    test_rpc.ActEnqueuer([invoice_owner], datetime.datetime.today(), 1)

    # ---------- For multicurrency cases: ------------
    ##    mtl.create_client({'CLIENT_ID': client_id2, 'REGION_ID': '187', 'CURRENCY': 'UAH', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'COPY'})
    mtl.create_client({'CLIENT_ID': client_id2, 'REGION_ID': '225', 'CURRENCY': 'RUB',
                       'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7,
                       'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    test_rpc.ExecuteSQL(
        "update T_EXPORT set priority = -1 where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id",
        {'client_id': client_id2})
    time.sleep(15)
    while 1 == 1:
        state = \
            test_rpc.ExecuteSQL(
                "select state from T_EXPORT where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id",
                {'client_id': client_id2})[0]['state']
        print(state)
        time.sleep(3)
        if state == 1: break


##    print '{0}, {1}'.format (order_id, service_order_id)
test_client()
