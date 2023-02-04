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

##Клиент
uid = 'clientuid34'
is_agency = False

service_id = 11;
product_id = 2136;

def test_client():

############################### Выставление счета на конкретного клиента (БЕЗ овердрафта)##########################################
##    client_id = mtl.create_client({'IS_AGENCY': 0})
##    person_id = mtl.create_person(client_id, 'usp')

    qty= 20
    begin_dt     = datetime.datetime.now()
    invoice_dt = datetime.datetime.now()
    payment_dt =datetime.datetime.now()
    campaigns_dt=datetime.datetime.now()

    paysys_id    =1011
##    1001
##    1011
    is_credit=0
    overdraft=0

##    service_order_id = mtl.get_next_service_order_id(service_id)
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
##    request_id = mtl.create_request (client_id, orders_list, begin_dt)
##    invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, is_credit, overdraft)
    invoice_id = 38985422
    service_order_id = 46162023
    mtl.OEBS_payment(invoice_id, None, None)
    mtl.do_campaigns(service_id, service_order_id, {'Bucks': 15, 'Money': 0}, 0, campaigns_dt)
    mtl.create_act(invoice_id, campaigns_dt)
    act_id = test_rpc.ExecuteSQL('balance', 'select id from t_act where INVOICE_ID = :invoice_id' , {'invoice_id': invoice_id})[0]['id']
    url = '[MT]: https://balance-admin.greed-tm1f.yandex.ru/act.xml?act_id='+ str(act_id)
    return url

url =test_client()
print url




