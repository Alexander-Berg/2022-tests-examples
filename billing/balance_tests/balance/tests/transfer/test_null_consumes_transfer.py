# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

import pytest

import balance.balance_api as api
import balance.balance_steps as steps
import btestlib.data.defaults as defaults
import btestlib.reporter as reporter
from balance.features import Features

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.TRANSFER)
              ]

dt = datetime.datetime(2015, 6, 24, 11, 0, 0)

SERVICE_ID = 7
PERSON_TYPE = 'ur'
PAYSYS_ID = 1003

NON_CURRENCY_PRODUCT_ID = 1475
CURRENCY_PRODUCT_ID = 503162

NON_CURRENCY_ZERO_CONSUME_QTY = 0.00001
CURRENCY_ZERO_CONSUME_QTY = 0.001

NON_CURRENCY_CLIENT_PARAMS = {}
CURRENCY_CLIENT_PARAMS = {'REGION_ID': '225',
                          'CURRENCY': 'RUB',
                          'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
                          'SERVICE_ID': SERVICE_ID,
                          'CURRENCY_CONVERT_TYPE': 'COPY'}
NON_ZERO_CONSUME_QTY = 10


def params_depends_on_currency(is_currency):
    if is_currency:
        client_params = CURRENCY_CLIENT_PARAMS
        product_id = CURRENCY_PRODUCT_ID
        zero_consume_qty = CURRENCY_ZERO_CONSUME_QTY
    else:
        client_params = NON_CURRENCY_CLIENT_PARAMS
        product_id = NON_CURRENCY_PRODUCT_ID
        zero_consume_qty = NON_CURRENCY_ZERO_CONSUME_QTY
    return {'client_params': client_params, 'product_id': product_id, 'zero_consume_qty': zero_consume_qty}


def create_order(service_id, client_id, product_id):
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=product_id,
                                       service_id=service_id)
    return service_order_id


def create_transfer_multiple(dpt_service_order_id, qty_old, qty_new, dst_service_order_id, all_qty=0):
    api.medium().CreateTransferMultiple(defaults.PASSPORT_UID,
                                        [
                                            {"ServiceID": SERVICE_ID,
                                             "ServiceOrderID": dpt_service_order_id,
                                             "QtyOld": qty_old, "QtyNew": qty_new, "AllQty": all_qty}
                                        ],
                                        [
                                            {"ServiceID": SERVICE_ID,
                                             "ServiceOrderID": dst_service_order_id,
                                             "QtyDelta": 1}
                                        ], 1, None)


def create_consumes(consume_order_list, service_order_id, client_id, person_id, product_id, zero_consume_qty,
                    the_same_invoice):
    order_take_from_qty = 100
    qty_old = order_take_from_qty
    request_was_created_flag = False
    dpt_service_order_id = create_order(SERVICE_ID, client_id, product_id)
    if the_same_invoice:
        dpt_request_id = create_request(client_id, dpt_service_order_id, order_take_from_qty)
        create_invoice_and_pay(dpt_request_id, person_id)
    for consume_type in consume_order_list:
        if consume_type == 0:
            if the_same_invoice:
                create_transfer_multiple(dpt_service_order_id=dpt_service_order_id, qty_old=qty_old,
                                         qty_new=qty_old - zero_consume_qty, dst_service_order_id=service_order_id)
                qty_old += -zero_consume_qty
            else:
                dpt_request_id = create_request(client_id, dpt_service_order_id, order_take_from_qty)
                create_invoice_and_pay(dpt_request_id, person_id)
                create_transfer_multiple(dpt_service_order_id=dpt_service_order_id, qty_old=qty_old,
                                         qty_new=qty_old - zero_consume_qty, dst_service_order_id=service_order_id)
                qty_old += order_take_from_qty - zero_consume_qty
        else:
            if not request_was_created_flag:
                request_id_for_non_zero_consume = create_request(client_id, service_order_id, NON_ZERO_CONSUME_QTY)
                request_was_created_flag = True
            create_invoice_and_pay(request_id_for_non_zero_consume, person_id)


def counting_sum(consume_order_list, zero_consume_qty):
    sum = 0
    for consume_type in consume_order_list:
        if consume_type == 0:
            sum = sum + zero_consume_qty
        else:
            sum = sum + NON_ZERO_CONSUME_QTY
    return sum


def create_request(client_id, service_order_id, qty):
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}
    ]
    return steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))


def create_invoice_and_pay(request_id, person_id):
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    return invoice_id


def transfer(consume_order_list, client_params, product_id, zero_consume_qty, stay_on_sum, all_qty, the_same_invoice):
    client_id = steps.ClientSteps.create(params=client_params)
    person_id = steps.PersonSteps.create(client_id=client_id, type_=PERSON_TYPE)
    service_order_id = create_order(service_id=SERVICE_ID, client_id=client_id, product_id=product_id)
    create_consumes(consume_order_list, service_order_id=service_order_id, client_id=client_id, person_id=person_id,
                    product_id=product_id, zero_consume_qty=zero_consume_qty, the_same_invoice=the_same_invoice)
    sum = counting_sum(consume_order_list, zero_consume_qty)
    service_order_id_transfer_to = create_order(SERVICE_ID, client_id, product_id)
    create_transfer_multiple(dpt_service_order_id=service_order_id, qty_old=sum, qty_new=stay_on_sum,
                             dst_service_order_id=service_order_id_transfer_to, all_qty=all_qty)


def transfer_all_using_all_qty(consume_order_list, is_currency, the_same_invoice=0):
    params = params_depends_on_currency(is_currency)
    transfer(consume_order_list=consume_order_list, client_params=params['client_params'],
             product_id=params['product_id'], zero_consume_qty=params['zero_consume_qty'],
             all_qty=1, stay_on_sum=0, the_same_invoice=the_same_invoice)


def transfer_all(consume_order_list, is_currency, the_same_invoice=0):
    params = params_depends_on_currency(is_currency)
    transfer(consume_order_list=consume_order_list, client_params=params['client_params'],
             product_id=params['product_id'], zero_consume_qty=params['zero_consume_qty'],
             all_qty=0, stay_on_sum=0, the_same_invoice=0)


def transfer_partly(consume_order_list, is_currency, stay_on_sum):
    params = params_depends_on_currency(is_currency)
    transfer(consume_order_list=consume_order_list, client_params=params['client_params'],
             product_id=params['product_id'], zero_consume_qty=params['zero_consume_qty'],
             all_qty=0, stay_on_sum=stay_on_sum, the_same_invoice=0)


def transfer_number_of_consumes(consume_order_list, is_currency, count_of_consumes_to_transfer):
    params = params_depends_on_currency(is_currency)
    stay_on_sum = counting_sum(consume_order_list[1:count_of_consumes_to_transfer], params['zero_consume_qty'])
    transfer(consume_order_list=consume_order_list, client_params=params['client_params'],
             product_id=params['product_id'], zero_consume_qty=params['zero_consume_qty'],
             all_qty=0, stay_on_sum=stay_on_sum, the_same_invoice=0)


# transfer_number_of_consumes([1, 1, 1], is_currency=1, count_of_consumes_to_transfer=2)


@pytest.mark.parametrize('consume_order_list, currency_client, the_same_invoice',
                         [
                             ([0], 0, 0),
                             ([0], 0, 1),
                             ([1, 0], 0, 0),
                             ([0, 0], 0, 0)
                         ]
                         )
def test_transfer_all_using_all_qty(consume_order_list, currency_client, the_same_invoice):
    transfer_all_using_all_qty(consume_order_list, currency_client, the_same_invoice)
    assert 1 == 1


@pytest.mark.parametrize('consume_order_list, currency_client', [
    ([0], 0),
    ([1, 0], 0),
    ([0, 0], 0),
    ([0, 1], 0)
]
                         )
def test_transfer_all(consume_order_list, currency_client):
    transfer_all(consume_order_list, currency_client)
    assert 1 == 1


@pytest.mark.parametrize('consume_order_list, currency_client, count_of_consumes_to_transfer',
                         [
                             ([0, 0, 0], 0, 1),
                             ([0, 0, 0], 1, 2),
                             ([1, 0, 0], 0, 2)
                         ]
                         )
def test_transfer_number_of_consumes(consume_order_list, currency_client, count_of_consumes_to_transfer):
    transfer_number_of_consumes(consume_order_list, currency_client, count_of_consumes_to_transfer)
    assert 1 == 1


@pytest.mark.slow
@pytest.mark.parametrize('consume_order_list, currency_client, stay_on_sum',
                         [

                             ([0, 0, 0], 1, CURRENCY_ZERO_CONSUME_QTY / 2),
                             ([0, 0, 0], 0, NON_CURRENCY_ZERO_CONSUME_QTY / 2),
                             ([1, 0], 1,
                              (CURRENCY_ZERO_CONSUME_QTY + NON_ZERO_CONSUME_QTY) - CURRENCY_ZERO_CONSUME_QTY * 1.1),
                             ([1, 0], 0, (
                                     NON_CURRENCY_ZERO_CONSUME_QTY + NON_ZERO_CONSUME_QTY) - NON_CURRENCY_ZERO_CONSUME_QTY * 1.1),
                             ([1, 0, 0], 1, CURRENCY_ZERO_CONSUME_QTY / 2)
                         ]
                         )
def test_transfer_partly(consume_order_list, currency_client, stay_on_sum):
    transfer_partly(consume_order_list, currency_client, stay_on_sum)
    assert 1 == 1


if __name__ == "__main__":
    pytest.main("test_null_consumes_transfer.py -v")
