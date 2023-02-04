# -*- coding: utf-8 -*-

import pprint
import datetime
import time

from temp.MTestlib import MTestlib as mtl, proxy_provider
from btestlib.temp.MTestlib import proxy_provider as proxy_provider


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


rpc = mtl.rpc
test_rpc = mtl.test_rpc

# service_id = 7; product_id = 1475 ##503162
# service_id = 11; product_id = 2136
##service_id = 70; product_id = 503273
##service_id = 77; product_id = 2584
service_id = 99;
product_id = 505123  ##504697
##service_id = 102; product_id = 504654

qty = 100
qty2 = 200
paysys_id = 1091

after = datetime.datetime.now()
disc_dt = datetime.datetime(2015, 9, 18, 12, 0, 0)

begin_dt = after
request_dt = after  ##не меняется
invoice_dt = after
payment_dt = after
campaigns_dt = after
act_dt = after
migrate_dt = after

manager_uid = None
##manager_uid = '96446401'
##manager_uid = '176005458'
uid = 'clientuid33'


##------------------------------------------------------------------------------

def gen_client(x):
    rpc = proxy_provider.GetServiceProxy('greed-ts1f', 0)
    # test_rpc = mtl.test_rpc()

    start = time.time()

    client_id = 7302137 or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})  # 29680771
    agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})
    order_owner = client_id
    invoice_owner = client_id
    if order_owner == invoice_owner: agency_id = None
    mtl.link_client_uid(invoice_owner, 'clientuid32')
    person_id = None or mtl.log(mtl.create_person)(invoice_owner, 'ur_autoru', {'phone': '234'})
    contract_id = None

    mtl.get_force_overdraft(client_id, service_id, 1000, 10, after, 'RUB')
    # mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 99})

    order_ids = []
    service_order_ids = []
    for i in range(10):
        service_order_ids.append(mtl.get_next_service_order_id(service_id))
        # service_order_ids.append(99999999)
        order_ids.append(mtl.create_or_update_order(order_owner, product_id, service_id, service_order_ids[i],
                                                    {'TEXT': 'Py_Test order'}, agency_id=agency_id,
                                                    manager_uid=manager_uid))

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_ids[0], 'Qty': qty, 'BeginDT': begin_dt}
        # , {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': qty2, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, request_dt)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)

    quantum = 3
    old_qty = qty
    new_qty = old_qty - quantum
    for item in service_order_ids[1:]:
        rpc.Balance.CreateTransferMultiple(mtl.passport_uid, [
            {"ServiceID": service_id, "ServiceOrderID": service_order_ids[0], "QtyOld": old_qty, "QtyNew": new_qty}],
                                           [{"ServiceID": service_id, "ServiceOrderID": item, "QtyDelta": 1}], 1)
        print 'Transfered from {0} to {1}: {2}'.format(service_order_ids[0], item, quantum)
        old_qty = new_qty
        new_qty -= quantum

    end = time.time()
    print 'ttl: {0}'.format(end - start)


from multiprocessing import Pool

if __name__ == '__main__':
    gen_client(1)
    pool = Pool(2)
    pool.map(gen_client, range(2))



    # rpc.Balance2.CreateOrUpdateOrdersBatch(mtl.passport_uid, [{'ServiceID': service_id, 'ServiceOrderID': service_order_id2,'ProductID': product_id, 'ClientID': client_id, 'GroupServiceOrderID': service_order_id, 'GroupWithoutTransfer': 1}
    #                                                          ,{'ServiceID': service_id, 'ServiceOrderID': service_order_id3,'ProductID': product_id, 'ClientID': client_id, 'GroupServiceOrderID': service_order_id, 'GroupWithoutTransfer': 1}
    #                                                          ,{'ServiceID': service_id, 'ServiceOrderID': service_order_id4,'ProductID': product_id, 'ClientID': client_id, 'GroupServiceOrderID': service_order_id, 'GroupWithoutTransfer': 1}])
