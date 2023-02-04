# -*- coding: utf-8 -*-

import random
import json
import decimal

import pytest
from hamcrest import empty, anything

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import NdsNew, TransactionType, InvoiceType, K50OrderType, PartnerPaymentType
from btestlib.data.partner_contexts import (K50_UR_POSTPAY_CONTRACT_CONTEXT, K50_UR_PREPAY_CONTRACT_CONTEXT,
                                            K50_UR_POSTPAY_OFFER_CONTEXT, K50_UR_PREPAY_OFFER_CONTEXT,
                                            K50_YT_POSTPAY_CONTRACT_CONTEXT, K50_YT_PREPAY_CONTRACT_CONTEXT,
                                            K50_YT_POSTPAY_OFFER_CONTEXT, K50_YT_PREPAY_OFFER_CONTEXT,
                                            K50_YTPH_POSTPAY_CONTRACT_CONTEXT)
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries
from btestlib.data import person_defaults

pytestmark = [reporter.feature(Features.K50, Features.PARTNER, Features.ACT)]

import datetime

contexts = [
    K50_YTPH_POSTPAY_CONTRACT_CONTEXT,
    K50_UR_POSTPAY_CONTRACT_CONTEXT, K50_UR_PREPAY_CONTRACT_CONTEXT,
    K50_UR_POSTPAY_OFFER_CONTEXT, K50_UR_PREPAY_OFFER_CONTEXT,
    K50_YT_POSTPAY_CONTRACT_CONTEXT, K50_YT_PREPAY_CONTRACT_CONTEXT,
    K50_YT_POSTPAY_OFFER_CONTEXT, K50_YT_PREPAY_OFFER_CONTEXT
]

SERVICE_AMOUNT = decimal.Decimal('42.42')

_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


def create_person(context, client_id):
    return steps.PersonSteps.create(client_id, context.person_type.code,
                                    full=True,
                                    inn_type=person_defaults.InnType.RANDOM,
                                    name_type=person_defaults.NameType.RANDOM,
                                    params={'is-partner': '0'},
                                    )


def create_completions(context, client_id, dt, amount, order_type=None):
    steps.PartnerSteps.create_fake_product_completion(
        dt,
        transaction_dt=dt,
        client_id=client_id,
        service_id=context.service.id,
        service_order_id=0,
        amount=amount,
        type=order_type,
        transaction_type=TransactionType.PAYMENT.name,
        payment_type=PartnerPaymentType.WALLET,
        currency=context.currency.iso_code,
    )


def create_contract(context, start_dt):
    client_id = steps.ClientSteps.create()
    person_id = create_person(context, client_id)
    partner_integration_params = steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT
    additional_params = dict(start_dt=start_dt, **(context.special_contract_params or {}))
    return steps.ContractSteps.create_partner_contract(
        context,
        client_id=client_id, person_id=person_id,
        partner_integration_params=partner_integration_params,
        additional_params=additional_params)[:-1]


@pytest.mark.parametrize('context', contexts, ids=lambda c: c.name)
def test_k50_acts_wo_data(context):
    client_id, person_id, contract_id = create_contract(context, first_month_start_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(act_data, empty(), 'Проверяем, что акты не сгенерились и генерация не упала')


@pytest.mark.parametrize('context', contexts, ids=lambda c: c.name)
def test_k50_acts(context):
    client_id, person_id, contract_id = create_contract(context, first_month_start_dt)

    total_month_sum1 = utils.dround(SERVICE_AMOUNT * context.nds.koef_on_dt(first_month_end_dt), 2)
    total_month_sum2 = utils.dround(2 * SERVICE_AMOUNT * context.nds.koef_on_dt(second_month_end_dt), 2)
    full_total_sum = total_month_sum1 + total_month_sum2

    # invoice_id = steps.InvoiceSteps.get_invoice_data_by_client_with_ids(client_id)[0]['id']
    # steps.InvoiceSteps.pay_fair(invoice_id, payment_sum=full_total_sum)
    #
    create_completions(context, client_id, first_month_start_dt, SERVICE_AMOUNT, order_type=K50OrderType.OPTIMIZATOR)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_end_dt)

    create_completions(context,  client_id, second_month_start_dt, 2*SERVICE_AMOUNT, order_type=K50OrderType.GENERATOR)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, second_month_end_dt)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context, contract_id,
                                                                                     person_id,
                                                                                     full_total_sum,
                                                                                     dt=first_month_start_dt)
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [
        steps.CommonData.create_expected_act_data(total_month_sum1, first_month_end_dt),
        steps.CommonData.create_expected_act_data(total_month_sum2, second_month_end_dt)
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Сравниваем данные из акта с шаблоном')

    consume_data = steps.ConsumeSteps.get_consumes_by_client_id(client_id)
    expected_consume_data = [
        steps.CommonData.create_expected_consume_data(
            context.order_type2product[K50OrderType.OPTIMIZATOR].id,
            total_month_sum1,
            InvoiceType.PERSONAL_ACCOUNT,
            # act_qty=anything(),
            # completion_qty=anything(),
            # current_qty=anything()
        ),
        steps.CommonData.create_expected_consume_data(
            context.order_type2product[K50OrderType.GENERATOR].id,
            total_month_sum2,
            InvoiceType.PERSONAL_ACCOUNT,
            # act_qty=anything(),
            # completion_qty=anything(),
            # current_qty=anything()
        )
    ]
    utils.check_that(consume_data, contains_dicts_with_entries(expected_consume_data), u'Проверяем данные консьюмов')

    expetced_balance_data = [
        {'ExpiredDT': utils.anything(), 'ActSum': str(full_total_sum),
         'FirstDebtAmount': str(total_month_sum1).rstrip('0'), 'Currency': context.currency.iso_code,
         'Amount': 0, 'ExpiredDebtAmount': utils.anything(),
         'FirstDebtFromDT': utils.anything(), 'ReceiptSum': '0',
         'ConsumeSum': str(full_total_sum), 'DT': utils.anything(), 'ContractID': str(contract_id)}]
    balance_data = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    utils.check_that(balance_data, contains_dicts_with_entries(expetced_balance_data), u'Проверяем данные баланса')
