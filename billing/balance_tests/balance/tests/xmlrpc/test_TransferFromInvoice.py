# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
from decimal import Decimal

import pytest
import hamcrest

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import Services, Paysyses, PersonTypes, Products
from btestlib.matchers import contains_dicts_equal_to
import balance.balance_db as db
import btestlib.config as balance_config

PERSON_TYPE = PersonTypes.UR.code
PAYSYS_ID = Paysyses.BANK_UR_RUB.id
SERVICE_ID = Services.DIRECT.id
PRODUCT_ID = Products.DIRECT_FISH.id
DT = datetime.datetime.now()

ORDER_QTY = Decimal('100')
CAMPAIGN_QTY = Decimal('30')
PAYMENT_SUM = 10000

pytestmark = [pytest.mark.tickets('BALANCE-21236'), reporter.feature(Features.ORDER, Features.INVOICE, Features.TO_UNIT)]


def test_transfer_all_funds():
    invoice_id, service_order_id, client_id = create_data()
    trans_sum = (PAYMENT_SUM - ORDER_QTY * 30)
    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    steps.InvoiceSteps.free_funds_to_order(invoice_id, SERVICE_ID, service_order_id)
    check_order_db(trans_sum, client_id)


def test_transfer_part_funds():
    invoice_id, service_order_id, client_id = create_data()
    trans_sum = PAYMENT_SUM / 2
    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    steps.InvoiceSteps.free_funds_to_order(invoice_id, SERVICE_ID, service_order_id, mode=1, sum=trans_sum)
    check_order_db(trans_sum, client_id)


def test_transfer_more_funds():
    invoice_id, service_order_id, client_id = create_data()
    trans_sum = PAYMENT_SUM * 2
    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    with pytest.raises(Exception) as exc:
        steps.InvoiceSteps.free_funds_to_order(invoice_id, SERVICE_ID, service_order_id, mode=1, sum=trans_sum)
    expected_error_message_regex = "Cannot make transfer to 7-{0}: not enough free funds {1}".format(
        service_order_id,
        r'{0}(\.0*)?FISH'.format(trans_sum - (PAYMENT_SUM - ORDER_QTY * 30), )
    )
    utils.check_that(steps.CommonSteps.get_exception_code(exc.value, 'msg'),
                     hamcrest.matches_regexp(expected_error_message_regex))


def create_data():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    campaigns_list = [{'client_id': client_id, 'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'qty': ORDER_QTY}]
    invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id, person_id, campaigns_list,
                                                                            PAYSYS_ID)
    steps.InvoiceSteps.pay(invoice_id, PAYMENT_SUM)
    service_order_id = orders_list[0]['ServiceOrderID']
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, campaigns_params={'Bucks': CAMPAIGN_QTY},
                                      campaigns_dt=DT)
    return invoice_id, service_order_id, client_id


def check_order_db(trans_sum, client_id):
    expected_data = [{'completion_qty': CAMPAIGN_QTY,
                      'consume_qty': (ORDER_QTY + Decimal(trans_sum) / 30).quantize(Decimal('.000001')),
                      'consume_sum': ORDER_QTY * 30 + trans_sum,
                      'contract_id': None,
                      'service_code': PRODUCT_ID,
                      'service_id': SERVICE_ID}]
    fact_data = steps.OrderSteps.get_order_data_by_client(client_id)
    utils.check_that(expected_data, contains_dicts_equal_to(fact_data), u'Проверяем соответствие данных заказа')
