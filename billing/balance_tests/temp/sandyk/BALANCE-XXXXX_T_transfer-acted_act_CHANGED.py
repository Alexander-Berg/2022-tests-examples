# -*- coding: utf-8 -*-

import datetime
##import MTestlib as mtl
import pprint
import time

from MTestlib import MTestlib as mtl


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


rpc = mtl.rpc
test_rpc = mtl.test_rpc

auto_prefix = '[MT]: '

uid = 'clientuid33'
##Заказ  / Реквест
service_id = 7;
product_id = 1475  ##503162
##service_id = 11; product_id = 2136
##service_id = 70; product_id = 502761
##service_id = 77; product_id = 504083
##service_id = 99; product_id = 504850
qty = 100

##today =
##after = datetime.datetime.now()
##after = datetime.datetime(2014,12,29)
after = datetime.datetime(2015, 2, 26, 11, 0, 0)

begin_dt = after
request_dt = after  ##не меняется
invoice_dt = after
paysys_id = 1001
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
def test_client():
    client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})  ##mass
    order_owner = client_id
    invoice_owner = client_id
    if order_owner == invoice_owner: agency_id = None

    person_id = None or mtl.create_person(invoice_owner, 'ph', {'phone': '234'})

    service_order_id = mtl.get_next_service_order_id(service_id)

    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)

    service_order_id2 = mtl.get_next_service_order_id(service_id)
    order_id2 = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id2,
                                           {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
        ##        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': qty, 'BeginDT': begin_dt}
        ##        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id3, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, request_dt)
    print '111'
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, overdraft=0)
    print '222'
    mtl.OEBS_payment(invoice_id, None, None)
    mtl.do_campaigns(7, service_order_id, {'Bucks': 30, 'Money': 0}, 0, campaigns_dt)
    mtl.act_accounter(client_id, 1, campaigns_dt)
    mtl.do_campaigns(7, service_order_id, {'Bucks': 20, 'Money': 0}, 0, campaigns_dt)
    print '... (3)';
    time.sleep(3)
    rpc.Balance.CreateTransferMultiple(16571028, [
        {"QtyOld": "100.000000", "ServiceOrderID": service_order_id, "ServiceID": 7, "QtyNew": "20.000000"}],
                                       [{"QtyDelta": "92.158000", "ServiceOrderID": service_order_id2, "ServiceID": 7}])
    print '... (3)';
    time.sleep(3)
    mtl.do_campaigns(7, service_order_id2, {'Bucks': 79, 'Money': 0}, 0, campaigns_dt)
    print '!!!'
    print client_id
    print campaigns_dt
    mtl.act_accounter(client_id, 1, campaigns_dt)
    print '1111'
    act_id = test_rpc.ExecuteSQL('balance', 'select id from t_act where invoice_id = :invoice_id', {'invoice_id': invoice_id})[1][
        'id']
    print '2222'
    print '%shttps://balance-admin.%s.yandex.ru/act.xml?act_id=%s&mt-login=yb-adm&mt-password=get_secret(*UsersPwd.YANDEX_TEAM_REG_CQR5_PWD)' % (
    auto_prefix, mtl.host, act_id)


test_client()

