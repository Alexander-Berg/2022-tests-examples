#-*- coding: utf-8 -*-

##from MTest import mtl
from MTestlib import MTestlib as mtl
import xmlrpclib
import pprint
import datetime
import time
import urlparse

def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')

rpc = mtl.rpc
test_rpc = mtl.test_rpc


uid = 'clientuid34'

service_id = 7;
product_id = 1475
##product_id =503162


sql_date_format = "%d.%m.%Y %H:%M:%S"
def test_client():


    campaigns_dt = datetime.datetime.now()
    dt           = mtl.add_months_to_date(datetime.datetime.now(),-1)
    paysys_id    = 1001
    is_credit=0
    overdraft=1
    qty=2000

###################
    client_id = mtl.create_client({'IS_AGENCY': 0})
    mtl.get_overdraft(client_id,service_id,160,1,datetime.datetime.now())
##    client_id = 29524101
##    person_id = mtl.create_person(client_id, 'ph',{'email': 'test-balance-notify@yandex-team.ru'})
##
##    service_order_id = mtl.get_next_service_order_id(service_id)
##    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
##    orders_list = []
##    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': datetime.datetime.now()})
##    request_id = mtl.create_request (client_id, orders_list, datetime.datetime.now())
##
##    invoice_id = mtl.create_invoice (request_id, person_id, 1001, 0, None,overdraft =1)
##    test_rpc.ExecuteSQL('update (select * from T_INVOICE where ID = :invoice_id ) set dt = to_date(:dt,\'DD.MM.YYYY HH24:MI:SS\'), payment_term_dt= to_date(:dt,\'DD.MM.YYYY HH24:MI:SS\')',
##         {'invoice_id': invoice_id, 'dt': dt.strftime(sql_date_format)})
##    mtl.do_campaigns(service_id, service_order_id, {'Bucks': 0, 'Money': 1000}, 0, datetime.datetime.now())
##    test_rpc.ActEnqueuer([client_id],datetime.datetime.today(), 0)

test_client()