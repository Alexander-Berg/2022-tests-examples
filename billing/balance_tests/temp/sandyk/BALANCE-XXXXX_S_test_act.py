#-*- coding: utf-8 -*-

from MTestlib import MTestlib as mtl
import xmlrpclib
import pprint
import datetime
import urlparse

def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')

rpc = mtl.rpc
test_rpc = mtl.test_rpc

print rpc
print test_rpc

##Клиент
uid = 'clientuid34'
is_agency = False

service_id = 7;
product_id = 1475;

def test_client():

############################### Выставление счета на конкретного клиента (БЕЗ овердрафта)##########################################
##    client_id = mtl.create_client({'IS_AGENCY': 0})
##    person_id = mtl.create_person(client_id, 'ph')

    qty= 20
    begin_dt     = datetime.datetime.now()
    invoice_dt = datetime.datetime.now()
    payment_dt =datetime.datetime.now()
    campaigns_dt=datetime.datetime.now()

    paysys_id    = 1001
    is_credit=0
    overdraft=0

    print '1'
##    service_order_id = mtl.get_next_service_order_id(service_id)
    client_id = 29221801
    person_id = 4435234
    service_order_id = 397276665
    print '2'
    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
    print '3'
    orders_list = []
    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
    request_id = mtl.create_request (client_id, orders_list, begin_dt)
##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit, overdraft)
##    mtl.OEBS_payment(invoice_id, None, None)
##    print '1'
##    mtl.do_campaigns(service_id, service_order_id, {'Bucks': 15, 'Money': 0}, 0, campaigns_dt)
##    print '2'
##    mtl.create_act(invoice_id, campaigns_dt)
##    print '3'
##    act_id = test_rpc.ExecuteSQL('balance', 'select id from t_act where INVOICE_ID = :invoice_id' , {'invoice_id': invoice_id})[0]['id']
##    print '4'
##    url = '[MT]: https://balance-admin.greed-tm1f.yandex.ru/act.xml?act_id='+ str(act_id)
##    return url


##url =
test_client()
##print url




