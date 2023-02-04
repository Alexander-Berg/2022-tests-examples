# coding: utf-8

import datetime

from hamcrest import has_entries

import balance.balance_db as db
import btestlib.utils as utils
from balance import balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)
ORDER_DT = dt


def test_unmoderated_transfer():
    PERSON_TYPE = 'ur'
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    PAYSYS_ID = 1003
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 300}, 0, dt)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                              service_order_id=parent_service_order_id)

    steps.OrderSteps.merge(parent_order_id, [order_id])
    steps.OrderSteps.make_unmoderated(parent_order_id)
    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    error = db.balance().execute('''
    select * from t_export_ua_errors where client_id = :client_id
    ''',
                                 {'client_id': client_id})[0]
    expected_error = {}
    error_text = 'Got exception transfer2main ({0}) Transfer to unmoderated (or from invoice={1}) dst_order_id: {2}, unmoderated_order_ids: [{3}]'.format(
        parent_order_id, invoice_id, parent_order_id, parent_order_id)
    expected_error.update({'client_id': client_id,
                           'order_id': parent_order_id,
                           'agency_id': None,
                           'error': error_text,
                           'status': 2})
    utils.check_that(error, has_entries(expected_error))


# Ошибка исправлена в https://st.yandex-team.ru/BALANCE-26277 отключил пока тест
def optimized_error():
    dt = datetime.datetime.now()
    PERSON_TYPE = 'ur'
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    PAYSYS_ID = 1003
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 800}, 0, dt)
    consume_id = db.get_consumes_by_invoice(invoice_id)[0]['id']
    db.BalanceBO().execute('update t_consume set current_qty = 200 where id = :consume_id', {'consume_id': consume_id})

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                              service_order_id=parent_service_order_id)

    steps.OrderSteps.merge(parent_order_id, [order_id])
    steps.OrderSteps.make_optimized_force(parent_order_id)
    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    error = db.balance().execute('''
    select * from t_export_ua_errors where client_id = :client_id
    ''',
                                 {'client_id': client_id})[0]
    expected_error = {}
    error_text = 'Got exception main_orders_optimized ({0})'.format(parent_order_id)
    expected_error.update({'client_id': client_id,
                           'order_id': parent_order_id,
                           'agency_id': None,
                           'error': error_text,
                           'status': 2})
    utils.check_that(error, has_entries(expected_error))
