#-*- coding: utf-8 -*-
import datetime
##from MTestlib import MTestlib as mtl
import pprint

import MTestlib as mtl


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')

rpc = mtl.rpc
test_rpc = mtl.test_rpc

##Клиент
uid = 'clientuid34'
is_agency = False

service_id = 99;
product_id = 505123;

def test_client():

############################### Выставление счета на конкретного клиента (БЕЗ овердрафта)##########################################
    client_id = mtl.create_client({'IS_AGENCY': 0})
    person_id = mtl.create_person(client_id, 'ur_autoru')

    begin_dt = datetime.datetime.now()
    campaigns_dt=datetime.datetime.now()

    mtl.get_force_overdraft(client_id, service_id, 1000, 10, datetime.datetime.now(), 'RUB')
    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=1), 'SERVICE_ID': 99})

    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id,
        {'TEXT':'Py_Test order'})

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 300, 'BeginDT': begin_dt}
##        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': 600, 'BeginDT': begin_dt}
####        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id3, 'Qty': qty, 'BeginDT': begin_dt}
    ]

    request_id = mtl.create_request (client_id, orders_list, begin_dt)
    invoice_id = mtl.create_invoice (request_id, person_id, 1091, 1, None, 1)


print '[MT]: https://balance-admin.greed-tm1f.yandex.ru/invoice-publish.xml?ft=html&object_id=' + str(
    invoice_id) + '&mt-login=yb-adm&mt-password=get_secret(*UsersPwd.YANDEX_TEAM_REG_CQR5_PWD)'

test_client()





