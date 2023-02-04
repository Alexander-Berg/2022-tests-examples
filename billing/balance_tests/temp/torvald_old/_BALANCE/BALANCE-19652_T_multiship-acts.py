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
##service_id = 7; product_id = 1475 ##503162
##service_id = 99; product_id = 504596 ##504697
##service_id = 11; product_id = 2136
##service_id = 70; product_id = 503273
##service_id = 77; product_id = 2584
##service_id = 120; product_id = 504690
##service_id = 114; product_id = 502981
service_id = 101;
product_id = 504970
##504969
##504970
##504972
##504971
##505058
##505059
##505062
##505060
##505061
##505063
##505064

qty = 800

##today =
after = datetime.datetime.now()
disc_dt = datetime.datetime.now()
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


##------------------------------------------------------------------------------
def test_client():
    ## Клиент
    client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    ##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})  ##mass
    order_owner = client_id
    invoice_owner = client_id
    if order_owner == invoice_owner: agency_id = None
    mtl.link_client_uid(invoice_owner, 'clientuid32')
    person_id = None or mtl.create_person(invoice_owner, 'ur', {'phone': '234'})

    contract_id = mtl.create_contract2('multiship_post',
                                       {'client_id': invoice_owner, 'person_id': person_id, 'dt': '2015-01-01T00:00:00',
                                        'FINISH_DT': '2016-06-30T00:00:00', 'is_signed': '2015-01-01T00:00:00'})
    ##    contract_id = 382777
    completions_map = [(datetime.datetime(2015, 6, 14, 11, 0, 0), 504969, 1000, 0),
                       (datetime.datetime(2015, 6, 14, 11, 0, 0), 504970, 1001, 1)]
    max_entity_id = test_rpc.ExecuteSQL("select max(entity_id) from bo.T_PARTNER_MULTISHIP_STAT")
    max_id = test_rpc.ExecuteSQL("select max(id) from bo.T_PARTNER_MULTISHIP_STAT")
    for completion_dt, product_id, amount, is_correction in completions_map:
        max_id += 1
        sql = "Insert into T_PARTNER_MULTISHIP_STAT (ID,ENTITY_ID,CONTRACT_ID,DT,PRODUCT_ID,AMOUNT,IS_CORRECTION) values ('{0}','{1}','{2}',{3},'{4}','{5}','{6}')".format(
            max_id, max_entity_id + 1, contract_id, completion_dt.strftime(mtl.sql_date_format), product_id, amount,
            is_correction)
        test_rpc.ExecuteSQL(sql)
        test_rpc.ExecuteSQL("commit")

    ##    service_order_id = mtl.get_next_service_order_id(service_id)
    service_order_id = contract_id
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)

    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)
    mtl.OEBS_payment(invoice_id)
    ##    mtl.do_campaigns(7,service_order_id , {'Bucks': 13.666666, 'Money': 0}, 0, campaigns_dt)
    ##    test_rpc.ActEnqueuer([invoice_owner], datetime.datetime.today(), 1)

    ##    # ---------- For multicurrency cases: ------------
    ##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    ##    test_rpc.ExecuteSQL("update T_EXPORT set priority = -1 where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id" , {'client_id': client_id })
    ##    while 1==1:
    ##        state = test_rpc.ExecuteSQL("select state from T_EXPORT where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id" , {'client_id': client_id })[0]['state']
    ##        print(state)
    ##        time.sleep(3)
    ##        if state == 1: break
    ##
    ##    print '{0}, {1}'.format (order_id, service_order_id)
    ##
    ##    service_order_id = mtl.get_next_service_order_id(service_id)
    ##    order_id = mtl.create_or_update_order (order_owner, product_id, service_id, service_order_id,
    ##        {'TEXT':'Py_Test order'}, agency_id = agency_id, manager_uid = manager_uid)

    after = datetime.datetime(2015, 5, 14, 11, 0, 0)
    disc_dt = datetime.datetime(2015, 5, 18, 11, 0, 0)

    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)

    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=1, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)

    after = datetime.datetime(2015, 4, 14, 11, 0, 0)
    disc_dt = datetime.datetime(2015, 4, 18, 11, 0, 0)

    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)

    invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=1, contract_id=contract_id, overdraft=0,
                                    endbuyer_id=None)


test_client()

##    for info in scenario:
##        locals().update(info)
##        client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
##        scenario['client_id'] = client_id
##        campaigns_list = [
##          {'client_id': client_id, 'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt}
####        , {'client_id': client_id2, 'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt}
##        ]
##        invoice_id, orders_list = mtl.create_force_invoice(client_id, person_id, campaigns_list, paysys_id,
##        invoice_dt, agency_id = agency_id, credit = 1, contract_id = contract_id, overdraft = 0, manager_uid = manager_uid)
##
##        mtl.OEBS_payment(invoice_id, None, None)
##    Print(scenario)
##
##
##header = 'service_id', 'product_id', 'qty', 'completions'
##scenario = [dict(zip(header, line)) for line in scenario]
##Print(scenario)
