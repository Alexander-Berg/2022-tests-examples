# -*- coding: utf-8 -*-

import datetime

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features

DT = datetime.datetime.now()


def generate_data_test_transfer_acted_is_true():
    return [
        {'first_campaign': 100, 'second_campaign': 0, 'qty_old': 100, 'qty_new': 0},
        {'first_campaign': 100, 'second_campaign': 50, 'qty_old': 100, 'qty_new': 50},
        {'first_campaign': 80, 'second_campaign': 50, 'qty_old': 100, 'qty_new': 50},
    ]


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('params',
                         generate_data_test_transfer_acted_is_true()

                         )
def test_transfer_acted_is_true(params):
    reporter.log(params)
    PERSON_TYPE = 'ph'
    PAYSYS_ID = 1001
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    invoice = db.get_invoice_by_id(invoice_id)[0]
    transfer_acted_value = invoice['transfer_acted']
    # проверяем, что признак проставился
    # utils.check_that(transfer_acted_value, hamcrest.equal_to(1))
    # на заказе есть только заакченные свободные средства
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': params['first_campaign']}, 0, DT)
    steps.ActsSteps.generate(client_id, force=1, date=DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': params['second_campaign']}, 0, DT)
    service_order_id_2 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, PRODUCT_ID, SERVICE_ID)
    steps.OrderSteps.transfer(
        [{'order_id': order_id, 'qty_old': params['qty_old'], 'qty_new': params['qty_new'], 'all_qty': 0}],
        [{'order_id': order_id_2, 'qty_delta': 1}]
    )


def generate_data_test_transfer_acted_is_false():
    return [
        {'first_campaign': 100, 'second_campaign': 0, 'qty_old': 100, 'qty_new': 0, 'TariffingQuantity': 100},
        {'first_campaign': 100, 'second_campaign': 50, 'qty_old': 100, 'qty_new': 30, 'TariffingQuantity': 70},
        {'first_campaign': 80, 'second_campaign': 50, 'qty_old': 100, 'qty_new': 30,
         'TariffingQuantity': 50.0000000000},
    ]


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('params',
                         generate_data_test_transfer_acted_is_false()

                         )
def test_transfer_acted_is_false_acted_free_funds(params):
    PERSON_TYPE = 'ur'
    PAYSYS_ID = 1003
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    invoice = db.get_invoice_by_id(invoice_id)[0]
    transfer_acted_value = invoice['transfer_acted']
    # проверяем, что признак не проставился
    # utils.check_that(transfer_acted_value, hamcrest.equal_to(0))
    # на заказе есть заакченные свободные средства
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, DT)
    steps.ActsSteps.generate(client_id, force=1, date=DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 0}, 0, DT)
    service_order_id_2 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, PRODUCT_ID, SERVICE_ID)
    try:
        steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 100, 'qty_new': 0, 'all_qty': 0}],
                                  [{'order_id': order_id_2, 'qty_delta': 1}])
    except Exception, exc:
        expected_error_content = 'Cannot make reverse qty=100 for 7-{0}: not enough free funds 100.000000'\
            .format(service_order_id)
        utils.check_that(expected_error_content,
                         hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'contents')))


@reporter.feature(Features.TO_UNIT)
def test_transfer_acted_is_false_acted_free_funds_acted_non_free_funds():
    PERSON_TYPE = 'ur'
    PAYSYS_ID = 1003
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    invoice = db.get_invoice_by_id(invoice_id)[0]
    transfer_acted_value = invoice['transfer_acted']
    # проверяем, что признак не проставился
    # utils.check_that(transfer_acted_value, hamcrest.equal_to(0))
    # на заказе есть заакченные свободные средства
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, DT)
    steps.ActsSteps.generate(client_id, force=1, date=DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50}, 0, DT)
    service_order_id_2 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, PRODUCT_ID, SERVICE_ID)
    try:
        steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 100, 'qty_new': 30, 'all_qty': 0}],
                                  [{'order_id': order_id_2, 'qty_delta': 1}])
    except Exception, exc:
        reporter.log(steps.CommonSteps.get_exception_code(exc, 'contents'))
        expected_error_content = 'Cannot make reverse qty=70 for 7-{0}: not enough free funds 70.000000'.format(
            service_order_id)
        utils.check_that(expected_error_content,
                         hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'contents')))


@reporter.feature(Features.TO_UNIT)
def test_transfer_acted_is_false_acted_free_funds_non_acted_free_funds_acted_non_free_funds():
    PERSON_TYPE = 'ur'
    PAYSYS_ID = 1003
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    invoice = db.get_invoice_by_id(invoice_id)[0]
    transfer_acted_value = invoice['transfer_acted']
    # проверяем, что признак не проставился
    # utils.check_that(transfer_acted_value, hamcrest.equal_to(0))
    # на заказе есть свободные средства и заакченные, и незаакченные
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 80}, 0, DT)
    steps.ActsSteps.generate(client_id, force=1, date=DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50}, 0, DT)
    service_order_id_2 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, PRODUCT_ID, SERVICE_ID)
    try:
        steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 100, 'qty_new': 30, 'all_qty': 0}],
                                  [{'order_id': order_id_2, 'qty_delta': 1}])
    except Exception, exc:
        reporter.log(steps.CommonSteps.get_exception_code(exc, 'contents'))
        qty_regex = r'50\.0*'
        expected_error_content = \
            'Cannot make reverse qty=70 for 7-{0}: not enough free funds {1}QTY'.format(service_order_id, qty_regex)
        utils.check_that(steps.CommonSteps.get_exception_code(exc, 'contents'),
                         hamcrest.matches_regexp(expected_error_content))


if __name__ == "__main__":
    pytest.main("test_transfer_depend_on_invoice_transfer_acted_value.py -v")
