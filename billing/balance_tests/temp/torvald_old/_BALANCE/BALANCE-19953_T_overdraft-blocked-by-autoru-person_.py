# -*- coding: utf-8 -*-

import pprint
import datetime

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
qty = 100

##today =
##after = datetime.datetime.now()
##after = datetime.datetime(2014,12,29)
after = datetime.datetime(2015, 4, 15, 11, 0, 0)
disc_dt = datetime.datetime(2015, 4, 15, 11, 0, 0)

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
    ##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})  ##mass
    order_owner = client_id
    invoice_owner = client_id
    if order_owner == invoice_owner: agency_id = None
    mtl.link_client_uid(invoice_owner, 'clientuid32')
    person_id = None or mtl.create_person(invoice_owner, 'ph', {'phone': '234'})

    ##    contract_id = mtl.create_contract2('ukr_opt_ag_prem',{'client_id': agency_id, 'person_id': person_id, 'dt': '2015-02-28T00:00:00'})
    ##    contract_id = None

    mtl.get_overdraft(client_id, 7, 1000)

    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, request_dt)


test_client()
