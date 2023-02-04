#-*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

import balance.balance_api as api
import balance.balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
CURRENCY_PRODUCT_ID = 503162
NON_CURRENCY_PRODUCT_ID = 1475
NON_CURRENCY_MSR = 'Bucks'
CURRENCY_MSR = 'Money'

order_id_list = []

def creating_order(order_params, client_id, person_id, service_id, product_id, msr):
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=product_id, service_id=service_id)
    if 'with_shipment' in order_params:
       steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {msr: order_params['with_shipment']}, 0, dt)
    if 'with_payment' in order_params:
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': order_params['with_payment'], 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, invoice_dt=dt)
        invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                              credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)
    return order_id

def main(non_currency_order_params_list, currency_order_params_list=False):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    for order_params in non_currency_order_params_list:
        order_id = creating_order(order_params, client_id, person_id, SERVICE_ID, NON_CURRENCY_PRODUCT_ID, NON_CURRENCY_MSR)
        order_id_list.append(order_id)
    if currency_order_params_list:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='MODIFY', dt=dt)
        for order_params in currency_order_params_list:
            order_id = creating_order(order_params, client_id, person_id, SERVICE_ID, CURRENCY_PRODUCT_ID, CURRENCY_MSR)
            order_id_list.append(order_id)
    for order_params in non_currency_order_params_list+currency_order_params_list:
        if 'Parent' in order_params:
            parent_order_id = order_id_list.pop()


    steps.OrderSteps.merge(parent_order_id, sub_orders=order_id_list, group_without_transfer = 1)
    api.test_balance().ua_transfer_queue([client_id])

    query= "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :client_id"
    sql_params = {'client_id': client_id}
    steps.CommonSteps.wait_for(query, sql_params)

main([
                 {'Parent':True, 'with_payment': 10000, 'after_group_shipment': 1000}
             ],
             [
                 {'with_shipment': 199}
             ])
