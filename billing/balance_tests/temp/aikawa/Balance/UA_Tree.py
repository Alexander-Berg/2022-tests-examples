# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

import balance.balance_steps as steps

after = datetime.datetime(2015, 6, 24, 11, 0, 0)
dt = after

PERSON_TYPE = 'ur'
SERVICE_ID = 7
PRODUCT_ID = 1475
CURRENCY_PRODUCT_ID = 503162
PAYSYS_ID = 1003

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

service_order_id_list = []
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
service_order_id_list.append(service_order_id)
child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, dt)

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
service_order_id_list.append(service_order_id)
parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
service_order_id_list.append(service_order_id)
parent_parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
steps.OrderSteps.make_optimized(parent_order_id)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, invoice_dt=dt)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)

steps.OrderSteps.merge(parent_parent_order_id, sub_orders=[parent_order_id], group_without_transfer=1)
steps.OrderSteps.merge(parent_order_id, sub_orders=[child_order_id], group_without_transfer=1)
steps.OrderSteps.ua_enqueue([client_id])

query = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :client_id"
sql_params = {'client_id': client_id}
steps.CommonSteps.wait_for(query, sql_params)

query = "select result as result from t_operation where id = (select operation_id from t_consume where parent_order_id = {0})".format()
steps.CommonSteps.get_pickled_value()

orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id_list[1], 'Qty': 200, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, invoice_dt=dt)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)

# query = 'select result from t_operation where type_id = 1 and id in (select operation_id from t_consume where parent_order_id = {0})'.format(
#     parent_order_id)
# result = steps.CommonSteps.get_pickled_value(query, 'result')
# print result



#
#
#
# orders_list = [
#             {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
#         ]
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, invoice_dt=dt)
# invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                                               credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)
#
#
# def main(order_params_list):
#     client_id = steps.ClientSteps.create()
#     person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
#     for order_params in order_params_list:
#         if not order_params['Currency']:
#             order_id = creating_order(order_params, client_id, person_id, datetime.datetime.now())
#             order_id_list.append(order_id)
# #     steps.ClientSteps.create({
# #                           'CLIENT_ID':client_id,
# #                           'REGION_ID': '225',
# #                           'CURRENCY': 'RUB',
# #                           'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
# #                           'SERVICE_ID': SERVICE_ID,
# #                           'CURRENCY_CONVERT_TYPE': 'COPY'})
# #     query= "select state as val from T_EXPORT where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id"
# #     sql_params = {'client_id': client_id}
# #     steps.CommonSteps.wait_for(query, sql_params)
#
#     steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY',
#                                   dt=datetime.datetime.now() + datetime.timedelta(seconds=5),
#                                   service_id=SERVICE_ID, region_id='225', currency='RUB')
#
#     for order_params in order_params_list:
#         if order_params['Currency']:
#             order_id=creating_order(order_params, client_id, person_id, datetime.datetime.now())
#             order_id_list.append(order_id)
#
#     for order_params in order_params_list:
#         if order_params['Parent']:
#             parent_order_id = order_id_list.pop()
#     # steps.OrderSteps.make_optimized(parent_order_id)
#
#     steps.OrderSteps.merge(parent_order_id, sub_orders=order_id_list, group_without_transfer = 1)
#     api.test_balance().ua_transfer_queue([client_id])
#
#     #обновляем дату, на которую будут учитываться открутки в переносах по единому счету (надо текущую дату)
#     input = {'use_completion_history': True, 'for_dt': datetime.datetime(2015, 11, 13, 23, 59, 59)}
#     db.balance().execute("update t_export set input = {0} where type = 'UA_TRANSFER' and classname = 'Client' and object_id = {1}" .format(steps.CommonSteps.set_input_value(input), client_id))
#     db.balance().execute("commit")
#
#     query= "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :client_id"
#     sql_params = {'client_id': client_id}
#     steps.CommonSteps.wait_for(query, sql_params)
#
# main([
#     {'service_id': SERVICE_ID, 'product_id': CURRENCY_PRODUCT_ID, 'with_shipment':False, 'Currency':True, 'Parent':True, 'msr': 'Money', 'with_payment': True, 'payment_qty': 10000}
#     ,{'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'with_shipment':True, 'shipment_qty':100.44444, 'Currency': False, 'Parent':False, 'msr': 'Bucks','with_payment': False }
#     ,{'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'with_shipment':True, 'shipment_qty':100, 'Currency': False, 'Parent':False, 'msr': 'Bucks', 'with_payment': False}]
#     )
#
#
# # main([
# #     {'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'with_shipment':False, 'Currency':True, 'Parent':True, 'msr': 'Money', 'with_payment': True, 'payment_qty': 10000}
# #     ,{'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'with_shipment':True, 'shipment_qty':100.44444, 'Currency': False, 'Parent':False, 'msr': 'Bucks','with_payment': False }
# #     ,{'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'with_shipment':True, 'shipment_qty':100, 'Currency': False, 'Parent':False, 'msr': 'Bucks', 'with_payment': False}]
# #     )
