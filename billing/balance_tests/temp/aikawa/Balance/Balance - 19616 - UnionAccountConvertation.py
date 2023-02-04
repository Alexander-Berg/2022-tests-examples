# -*- coding: utf-8 -*-

import datetime
import time

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
non_currency_product_id = 1475  # Рекламная кампания
currency_product_id = 503162
currency_msr = 'Money'
non_currency_msr = 'Bucks'


def CreateOrder(invoice_owner, order_owner, product_id, with_money=None):
    print '********'
    qty = 100
    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                          {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
    ]
    if with_money == 1:
        request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
        invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id,
                                        overdraft=0, endbuyer_id=None)
        mtl.OEBS_payment(invoice_id)
    return service_order_id, order_id


def union_account(child_service_order_id, parent_service_order_id, child_product_id):
    print rpc.Balance2.CreateOrUpdateOrdersBatch(16571028, [
        {'ServiceID': service_id, 'ServiceOrderID': child_service_order_id, 'ProductID': child_product_id,
         'ClientID': client_id, 'AgencyID': None, 'GroupServiceOrderID': parent_service_order_id,
         'GroupWithoutTransfer': 1}
    ])


client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
order_owner = client_id
invoice_owner = client_id
mtl.link_client_uid(client_id, 'aikawa-test-0')
person_id = None or mtl.create_person(invoice_owner, person_type, {'phone': '234'})

non_currency_service_order_id, non_currency_order_id = CreateOrder(invoice_owner, order_owner, non_currency_product_id,
                                                                   with_money=1)

steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=dt)

currency_service_order_id, currency_order_id = CreateOrder(invoice_owner, order_owner, currency_product_id,
                                                           with_money=0)

# Присылаем открутку на момент, в который клиент уже перешел на мультивалютность
# дочерний заказ валютный, родительский - фишечный
# mtl.do_campaigns(service_id, currency_service_order_id, {currency_msr: 50}, 0, datetime.datetime.now())
# union_account(currency_service_order_id, non_currency_service_order_id, currency_product_id)

# дочерний заказ фишечный, родительский - валютный
mtl.do_campaigns(service_id, non_currency_service_order_id, {non_currency_msr: 50}, 0, datetime.datetime.now())
union_account(non_currency_service_order_id, currency_service_order_id, non_currency_product_id)

test_rpc.UATransferQueue([client_id])

# обновляем дату, на которую будут учитываться открутки в переносах по единому счету (надо текущую дату)
input = {'use_completion_history': True, 'for_dt': datetime.datetime(2015, 8, 31, 23, 59, 59)}
test_rpc.ExecuteSQL(
    "update t_export set input = {0} where type = 'UA_TRANSFER' and classname = 'Client' and object_id = {1}".format(
        mtl.set_input_value(input), client_id))
test_rpc.ExecuteSQL("commit")

sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :object_id"
sql_params = {'object_id': client_id}
mtl.wait_for(sql, sql_params, value=1)
