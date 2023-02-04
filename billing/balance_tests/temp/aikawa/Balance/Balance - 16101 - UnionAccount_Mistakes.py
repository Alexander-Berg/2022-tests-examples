# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

from temp.MTestlib import MTestlib as mtl

rpc = mtl.rpc
test_rpc = mtl.test_rpc

after = datetime.datetime(2015, 6, 24, 11, 0, 0)
disc_dt = datetime.datetime(2015, 6, 24, 11, 0, 0)

begin_dt = after
invoice_dt = after
campaigns_dt = after

agency_id = None
contract_id = None
manager_uid = None

# # -------- firm_id = 1 -------
person_type = 'ur'  # ЮЛ резидент РФ
paysys_id = 1003  # Банк для юридических лиц
service_id = 7  # Директ
product_id = 1475  # Рекламная кампания
msr = 'Bucks'

qty = 100

ServiceOrderIdList = []


def UA_Tree():
    client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    person_id = mtl.create_person(client_id, person_type)
    mtl.link_client_uid(client_id, 'aikawa-test-0')

    # Создаем заказ
    for x in range(0, 4):
        service_order_id = mtl.get_next_service_order_id(service_id)
        order_id = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id,
                                              {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
        ]
        request_id = mtl.create_request(client_id, orders_list, disc_dt)
        invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id,
                                        overdraft=0, endbuyer_id=None)
        mtl.OEBS_payment(invoice_id)
        mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
        ServiceOrderIdList.append(service_order_id)
        print '_____________________'


    # #Объединяем заказы
    rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
        {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[0], 'ProductID': product_id,
         'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[3],
         'GroupWithoutTransfer': 1}
        , {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[1], 'ProductID': product_id,
           'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[3],
           'GroupWithoutTransfer': 1}
        , {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[2], 'ProductID': product_id,
           'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[3],
           'GroupWithoutTransfer': 1}
    ])

    test_rpc.UATransferQueue([client_id])
    # group_order_id = test_rpc.ExecuteSQL("select id from t_order where service_order_id = :service_order_id" , {'service_order_id': ServiceOrderIdList[1] })[0]['id']
    # sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :order_id"
    # sql_params = {'order_id': group_order_id}
    # mtl.wait_for(sql, sql_params, value = 1)
    #
    sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :order_id"
    sql_params = {'order_id': client_id}
    mtl.wait_for(sql, sql_params, value=1)

    rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
        {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[0], 'ProductID': product_id,
         'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[2],
         'GroupWithoutTransfer': 1}
        , {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[1], 'ProductID': product_id,
           'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[2],
           'GroupWithoutTransfer': 1}

    ])

    test_rpc.UATransferQueue([client_id])

    sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :order_id"
    sql_params = {'order_id': client_id}
    mtl.wait_for(sql, sql_params, value=1)

UA_Tree()
def OwnAndAgency():
    agency_id = None

    client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
    order_owner = client_id
    invoice_owner = client_id
    mtl.link_client_uid(client_id, 'aikawa-test-0')

    # Создаем заказ клиента
    for x in range(0, 2):
        person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})
        service_order_id = mtl.get_next_service_order_id(service_id)
        order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                              {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
        ]
        request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
        invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id,
                                        overdraft=0, endbuyer_id=None)
        mtl.OEBS_payment(invoice_id)
        mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
        ServiceOrderIdList.append(service_order_id)

    # Создаем заказ агентства
    agency_id = mtl.create_client({'IS_AGENCY': 1, 'NAME': u'LastMan'})
    invoice_owner = agency_id
    person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)
    mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
    ServiceOrderIdList.append(service_order_id)

    print rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
        {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[0], 'ProductID': product_id,
         'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1],
         'GroupWithoutTransfer': 1}
        , {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[2], 'ProductID': product_id,
           'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1],
           'GroupWithoutTransfer': 1}

    ])

    print (test_rpc.ExecuteSQL("select group_order_id from t_order where service_order_id = :service_order_id",
                               {'service_order_id': ServiceOrderIdList[0]})[0]['group_order_id'])
    print (test_rpc.ExecuteSQL("select group_order_id from t_order where service_order_id = :service_order_id",
                               {'service_order_id': ServiceOrderIdList[2]})[0]['group_order_id'])
    test_rpc.UATransferQueue([client_id])

    test_rpc.UATransferQueue([agency_id])

    sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :order_id"
    sql_params = {'order_id': client_id}
    mtl.wait_for(sql, sql_params, value=1)


def SeveralClients():
    client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
    order_owner = client_id
    invoice_owner = client_id
    mtl.link_client_uid(client_id, 'aikawa-test-0')

    # Создаем заказ клиента
    for x in range(0, 2):
        person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})
        service_order_id = mtl.get_next_service_order_id(service_id)
        order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                              {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
        ]
        request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
        invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id,
                                        overdraft=0, endbuyer_id=None)
        mtl.OEBS_payment(invoice_id)
        mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
        ServiceOrderIdList.append(service_order_id)
    client_id1 = client_id
    client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
    order_owner = client_id
    invoice_owner = client_id
    mtl.link_client_uid(client_id, 'aikawa-test-0')

    # Создаем заказ клиента
    person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)
    mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
    ServiceOrderIdList.append(service_order_id)

    print rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
        {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[0], 'ProductID': product_id,
         'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1],
         'GroupWithoutTransfer': 1}
        , {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[2], 'ProductID': product_id,
           'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1],
           'GroupWithoutTransfer': 1}

    ])
    print (test_rpc.ExecuteSQL("select group_order_id from t_order where service_order_id = :service_order_id",
                               {'service_order_id': ServiceOrderIdList[0]})[0]['group_order_id'])
    print (test_rpc.ExecuteSQL("select group_order_id from t_order where service_order_id = :service_order_id",
                               {'service_order_id': ServiceOrderIdList[2]})[0]['group_order_id'])
    test_rpc.UATransferQueue([client_id])

    sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :order_id"
    sql_params = {'order_id': client_id}
    mtl.wait_for(sql, sql_params, value=1)


def Equivalentclients():
    # Создаем заказ клиента
    client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
    order_owner = client_id
    invoice_owner = client_id
    mtl.link_client_uid(client_id, 'aikawa-test-0')
    person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    ServiceOrderIdList.append(service_order_id)
    client_id1 = client_id

    # Еще один
    client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
    order_owner = client_id
    invoice_owner = client_id
    mtl.link_client_uid(client_id, 'aikawa-test-0')
    person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    ServiceOrderIdList.append(service_order_id)

    rpc.Balance.MergeClients(16571028, client_id1, client_id)
    while 1 == 1:
        class_id = \
            test_rpc.ExecuteSQL("select class_id from t_client where id = :client_id", {'client_id': client_id})[0][
                'class_id']
        if class_id == client_id1:
            print ('MergeClients Done! Main client: ' + str(client_id) + ', related client: ' + str(client_id1))
            break

    print rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
        {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[1], 'ProductID': product_id,
         'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[0],
         'GroupWithoutTransfer': 1}
        # ,  {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[2],'ProductID': product_id, 'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1], 'GroupWithoutTransfer': 1}

    ])
    print (test_rpc.ExecuteSQL("select group_order_id from t_order where service_order_id = :service_order_id",
                               {'service_order_id': ServiceOrderIdList[0]})[0]['group_order_id'])

    test_rpc.UATransferQueue([client_id])

    sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :order_id"
    sql_params = {'order_id': client_id}
    mtl.wait_for(sql, sql_params, value=1)


def nonexistingorder():
    # Создаем заказ клиента
    client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
    order_owner = client_id
    invoice_owner = client_id
    mtl.link_client_uid(client_id, 'aikawa-test-0')
    person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    ServiceOrderIdList.append(service_order_id)
    client_id1 = client_id

    # Еще один
    ServiceOrderIdList.append(service_order_id + 10)

    print rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
        {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[0], 'ProductID': product_id,
         'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1],
         'GroupWithoutTransfer': 1}
        # ,  {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[2],'ProductID': product_id, 'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1], 'GroupWithoutTransfer': 1}

    ])
    print (test_rpc.ExecuteSQL("select group_order_id from t_order where service_order_id = :service_order_id",
                               {'service_order_id': ServiceOrderIdList[0]})[0]['group_order_id'])

    test_rpc.UATransferQueue([client_id])

    sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :order_id"
    sql_params = {'order_id': client_id}
    mtl.wait_for(sql, sql_params, value=1)


def diffservices():
    service_id = 7  # Директ
    product_id = 1475  # Рекламная кампания

    # Создаем заказ
    client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
    order_owner = client_id
    invoice_owner = client_id
    mtl.link_client_uid(client_id, 'aikawa-test-0')
    person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)
    mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
    ServiceOrderIdList.append(service_order_id)

    service_id = 11
    product_id = 2136
    # Создаем еще один заказ
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    ServiceOrderIdList.append(service_order_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)
    mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
    ServiceOrderIdList.append(service_order_id)

    # Объединяем
    service_id = 7  # Директ
    product_id = 1475
    print rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
        {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[0], 'ProductID': product_id,
         'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1],
         'GroupWithoutTransfer': 0}
        # ,  {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[2],'ProductID': product_id, 'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1], 'GroupWithoutTransfer': 1}
    ])
    print ServiceOrderIdList
    print (test_rpc.ExecuteSQL("select group_order_id from t_order where service_order_id = :service_order_id",
                               {'service_order_id': ServiceOrderIdList[0]})[0]['group_order_id'])

    test_rpc.UATransferQueue([client_id])

    sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :order_id"
    sql_params = {'order_id': client_id}
    mtl.wait_for(sql, sql_params, value=1)


# diffservices()

def parent_already():
    # Создаем заказ клиента
    client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
    order_owner = client_id
    invoice_owner = client_id
    mtl.link_client_uid(client_id, 'aikawa-test-0')
    person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)
    mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
    ServiceOrderIdList.append(service_order_id)

    print ServiceOrderIdList
    print (test_rpc.ExecuteSQL("select group_order_id from t_order where service_order_id = :service_order_id",
                               {'service_order_id': ServiceOrderIdList[0]})[0]['group_order_id'])

    print rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
        {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[0], 'ProductID': product_id,
         'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[0],
         'GroupWithoutTransfer': 1}
        # ,  {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[2],'ProductID': product_id, 'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1], 'GroupWithoutTransfer': 1}
    ])
    test_rpc.UATransferQueue([client_id])

    sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :order_id"
    sql_params = {'order_id': client_id}
    mtl.wait_for(sql, sql_params, value=1)


def diffClientsEqualAgency():
    # Создаем заказ клиента
    client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
    agency_id = mtl.create_client({'IS_AGENCY': 1, 'NAME': u'LastMan'})
    agency_id1 = agency_id
    order_owner = client_id
    invoice_owner = agency_id
    mtl.link_client_uid(client_id, 'aikawa-test-0')
    person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)
    mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
    ServiceOrderIdList.append(service_order_id)

    client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
    agency_id = mtl.create_client({'IS_AGENCY': 1, 'NAME': u'LastMan'})
    order_owner = client_id
    invoice_owner = agency_id
    mtl.link_client_uid(client_id, 'aikawa-test-0')
    person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)
    mtl.do_campaigns(service_id, service_order_id, {msr: 50}, 0, campaigns_dt)
    ServiceOrderIdList.append(service_order_id)

    rpc.Balance.MergeClients(16571028, agency_id1, agency_id)
    while 1 == 1:
        class_id = \
            test_rpc.ExecuteSQL("select class_id from t_client where id = :client_id", {'client_id': agency_id})[0][
                'class_id']
        if class_id == agency_id1:
            print ('MergeClients Done! Main client: ' + str(agency_id) + ', related client: ' + str(agency_id1))
            break

    print ServiceOrderIdList
    print (test_rpc.ExecuteSQL("select group_order_id from t_order where service_order_id = :service_order_id",
                               {'service_order_id': ServiceOrderIdList[0]})[0]['group_order_id'])

    print rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
        {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[0], 'ProductID': product_id,
         'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[0],
         'GroupWithoutTransfer': 1}
        # ,  {'ServiceID': service_id, 'ServiceOrderID': ServiceOrderIdList[2],'ProductID': product_id, 'ClientID': client_id, 'AgencyID': agency_id, 'GroupServiceOrderID': ServiceOrderIdList[1], 'GroupWithoutTransfer': 1}
    ])
    test_rpc.UATransferQueue([client_id])

    sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :order_id"
    sql_params = {'order_id': client_id}
    mtl.wait_for(sql, sql_params, value=1)


    # parent_already()
