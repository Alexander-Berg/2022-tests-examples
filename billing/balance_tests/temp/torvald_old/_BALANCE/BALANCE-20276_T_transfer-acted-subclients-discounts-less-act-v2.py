# -*- coding: utf-8 -*-

##from temp.MTestlib import mtl
import pprint
import datetime
import time

from temp.MTestlib import MTestlib as mtl


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
qty2 = 200
paysys_id = 1003

##after = datetime.datetime.now()
after = datetime.datetime(2015, 8, 26, 11, 0, 0)

begin_dt = after
request_dt = after  ##не меняется
invoice_dt = after
payment_dt = after
campaigns_dt = after
act_dt = after
migrate_dt = after
##manager_uid = '241593318'
manager_uid = None


##------------------------------------------------------------------------------
def data_generator():
    client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    client_id2 = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    client_id3 = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    client_id4 = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    client_id5 = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})  ##mass
    order_owner = client_id
    order_owner2 = client_id2
    order_owner3 = client_id3
    order_owner4 = client_id4
    order_owner5 = client_id5
    invoice_owner = agency_id
    if order_owner == invoice_owner: agency_id = None

    person_id = None or mtl.create_person(invoice_owner, 'ur', {'phone': '234'})

    service_order_id = mtl.get_next_service_order_id(service_id)

    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)

    service_order_id2 = mtl.get_next_service_order_id(service_id)
    order_id2 = mtl.create_or_update_order(order_owner2, product_id, service_id, service_order_id2,
                                           {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)

    service_order_id3 = mtl.get_next_service_order_id(service_id)
    order_id3 = mtl.create_or_update_order(order_owner3, product_id, service_id, service_order_id3,
                                           {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)

    service_order_id4 = mtl.get_next_service_order_id(service_id)
    order_id4 = mtl.create_or_update_order(order_owner4, product_id, service_id, service_order_id4,
                                           {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)

    service_order_id5 = mtl.get_next_service_order_id(service_id)
    order_id5 = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id5,
                                           {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)

    service_order_id6 = mtl.get_next_service_order_id(service_id)
    order_id5 = mtl.create_or_update_order(order_owner5, product_id, service_id, service_order_id6,
                                           {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': qty, 'BeginDT': begin_dt}
        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id5, 'Qty': qty, 'BeginDT': begin_dt}
        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id6, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, request_dt)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, overdraft=0)

    mtl.get_direct_discount(client_id2, datetime.datetime(2015, 8, 1), pct=3)

    mtl.OEBS_payment(invoice_id, None, None)
    mtl.do_campaigns(7, service_order_id, {'Bucks': 30, 'Money': 0}, 0, campaigns_dt)
    mtl.act_accounter(invoice_owner, 1, campaigns_dt)

    mtl.do_campaigns(7, service_order_id2, {'Bucks': 30, 'Money': 0}, 0, campaigns_dt)
    mtl.act_accounter(invoice_owner, 1, campaigns_dt)
    mtl.do_campaigns(7, service_order_id, {'Bucks': 20, 'Money': 0}, 0, campaigns_dt)
    mtl.do_campaigns(7, service_order_id2, {'Bucks': 20, 'Money': 0}, 0, campaigns_dt)
    print '... (3)';
    time.sleep(3)
    rpc.Balance.CreateTransferMultiple(16571028, [
        {"QtyOld": "100.000000", "ServiceOrderID": service_order_id2, "ServiceID": 7, "QtyNew": "20.000000",
         'AllQty': 1}], [{"QtyDelta": "92.158000", "ServiceOrderID": service_order_id4, "ServiceID": 7}])
    rpc.Balance.CreateTransferMultiple(16571028, [
        {"QtyOld": "100.000000", "ServiceOrderID": service_order_id, "ServiceID": 7, "QtyNew": "20.000000",
         'AllQty': 1}], [{"QtyDelta": "92.158000", "ServiceOrderID": service_order_id3, "ServiceID": 7}])
    print '... (3)';
    time.sleep(3)
    mtl.do_campaigns(7, service_order_id5, {'Bucks': 10, 'Money': 0}, 0, campaigns_dt)
    mtl.do_campaigns(7, service_order_id6, {'Bucks': 10, 'Money': 0}, 0, campaigns_dt)
    mtl.get_direct_discount(client_id3, datetime.datetime(2015, 8, 1), pct=3)
    mtl.do_campaigns(7, service_order_id3, {'Bucks': 5.8, 'Money': 0}, 0, campaigns_dt)
    mtl.get_direct_discount(client_id4, datetime.datetime(2015, 8, 1), pct=5)
    mtl.do_campaigns(7, service_order_id4, {'Bucks': 4.4, 'Money': 0}, 0, campaigns_dt)
    mtl.act_accounter(invoice_owner, 1, campaigns_dt)
    mtl.act_accounter(invoice_owner, 1, campaigns_dt)
    ##    mtl.do_campaigns(7, service_order_id3, {'Bucks': 20, 'Money': 0}, 0, campaigns_dt)
    ##    mtl.act_accounter(invoice_owner, 1, campaigns_dt)

    mtl.Print(dict(mtl.objects))


# Test_1 [Transfer_acted]_transfer_acted_with_groupping_by_discounts
def init_transfer_acted_with_groupping_by_discounts():
    data_generator()


if __name__ == '__main__':
    init_transfer_acted_with_groupping_by_discounts()
    pass
