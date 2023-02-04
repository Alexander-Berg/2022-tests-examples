# -*- coding: utf-8 -*-

from collections import defaultdict
from decimal import Decimal as D
import pytest
from hamcrest import empty

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import InvoiceType, Products, Currencies
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries
from btestlib.data.partner_contexts import TAXI_SAMOKAT_RU_CONTEXT_GENERAL, \
    TAXI_SAMOKAT_MICRO_MOBILITY_RU_CONTEXT_GENERAL, TAXI_SAMOKAT_WIND_CONTEXT

TAXI_SAMOKAT_CURRENCY_TO_PRODUCTS_MAP = {
    Currencies.RUB: [
        Products.SCOOTER_RENT_FARE_RUB.id,
    ],
    Currencies.ILS: [
        Products.SCOOTER_PAYMENT_ILS.id,
        Products.SCOOTER_FINE_ILS.id
    ],
}

DISCOUNT_BONUS_SUM = D('0')
APX_SUM = D('0')


def create_oebs_completions(context, contract_id, client_id, dt, payment_sum=D('0'), refund_sum=D('0')):
    sum_w_nds = D('0')
    sums_by_products = defaultdict(lambda: D('0'))
    compls_dicts = []
    for idx, product_id in enumerate(TAXI_SAMOKAT_CURRENCY_TO_PRODUCTS_MAP[context.currency]):
        compls_dicts += [
            {
                'service_id': context.service.id,
                'amount': (payment_sum - refund_sum),
                'product_id': product_id,
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
                'accounting_period': dt
            },
        ]
        amount_w_nds = payment_sum - refund_sum
        sum_w_nds += amount_w_nds
        sums_by_products[product_id] += amount_w_nds
    steps.CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_dicts)
    return sum_w_nds, sums_by_products


def merge_sums_dicts(result_defaultdict, dict_for_merge):
    for k, v in dict_for_merge.items():
        result_defaultdict[k] += v
    return result_defaultdict


def get_consumes(client_id):
    return [c for c in steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id) if c['act_qty'] != 0]


@pytest.mark.parametrize('context', [
    TAXI_SAMOKAT_RU_CONTEXT_GENERAL,
    TAXI_SAMOKAT_MICRO_MOBILITY_RU_CONTEXT_GENERAL,
    TAXI_SAMOKAT_WIND_CONTEXT],
                         ids=lambda context: context.name)
def test_act_wo_data(context):
    _, _, start_dt_1, end_dt_1, start_dt_2, end_dt_2 = utils.Date.previous_three_months_start_end_dates()
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=False,
                                                    additional_params={'start_dt': start_dt_1},
                                                    partner_integration_params={
                                                        'link_integration_to_client': 1,
                                                        'link_integration_to_client_args': {
                                                            'integration_cc': context.partner_integration_cc,
                                                            'configuration_cc': context.partner_configuration_cc,
                                                        },
                                                        'set_integration_to_contract': 1,
                                                        'set_integration_to_contract_params': {
                                                            'integration_cc': context.partner_integration_cc,
                                                        },
                                                    })

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, end_dt_1,
                                                                   manual_export=False)

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

    # проверяем данные в счете
    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data_1 = steps.ActsSteps.get_act_data_by_client(client_id)
    # готовим ожидаемые данные для счёта
    expected_invoice_data_1 = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                       contract_id, person_id,
                                                                                       D('0'), dt=start_dt_1)

    utils.check_that(invoice_data_1, equal_to_casted_dict(expected_invoice_data_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, empty(), 'Сравниваем данные из акта с шаблоном')
    utils.check_that(consume_data_1, empty(), 'Проверяем, что конзюмов нет')


@pytest.mark.parametrize('context', [
    TAXI_SAMOKAT_RU_CONTEXT_GENERAL,
    TAXI_SAMOKAT_MICRO_MOBILITY_RU_CONTEXT_GENERAL,
    TAXI_SAMOKAT_WIND_CONTEXT],
                         ids=lambda context: context.name)
def test_act_2_months(context):
    month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
        utils.Date.previous_two_months_dates()

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=False,
                                                    additional_params={'start_dt': month_minus2_start_dt},
                                                    partner_integration_params={
                                                        'link_integration_to_client': 1,
                                                        'link_integration_to_client_args': {
                                                            'integration_cc': context.partner_integration_cc,
                                                            'configuration_cc': context.partner_configuration_cc,
                                                        },
                                                        'set_integration_to_contract': 1,
                                                        'set_integration_to_contract_params': {
                                                            'integration_cc': context.partner_integration_cc,
                                                        },
                                                    })
    payment_sum_1 = D('420.69')
    refund_sum_1 = D('120.15')

    total_compls_sum = D('0')
    total_compls_sum_1 = D('0')
    total_sums_by_products = defaultdict(lambda: D('0'))

    compls_sum, sums_by_products = create_oebs_completions(
        context, contract_id, client_id, month_minus2_start_dt, payment_sum_1, refund_sum_1
    )
    total_compls_sum += compls_sum
    total_compls_sum_1 += compls_sum
    total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus2_end_dt)

    consume_data_1 = get_consumes(client_id)

    # проверяем данные в счете
    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data_1 = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_consumes_1 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_1.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_1 = steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id, person_id, total_compls_sum, dt=month_minus2_start_dt
    )

    expected_act_data_1 = steps.CommonData.create_expected_act_data(total_compls_sum_1, month_minus2_end_dt)
    utils.check_that(consume_data_1, contains_dicts_with_entries(expected_consumes_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_1, equal_to_casted_dict(expected_invoice_data_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, contains_dicts_with_entries([expected_act_data_1]),
                     'Сравниваем данные из акта с шаблоном')

    total_compls_sum_2 = D('0')
    compls_sum, sums_by_products = create_oebs_completions \
        (context, contract_id, client_id, month_minus1_start_dt, payment_sum_1, refund_sum_1)
    total_compls_sum += compls_sum
    total_compls_sum_2 += compls_sum
    total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus1_end_dt)

    consume_data_2 = get_consumes(client_id)

    # проверяем данные в счете
    invoice_data_2 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data_2 = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_consumes_2 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_2.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_2 = steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id, person_id, total_compls_sum, dt=month_minus2_start_dt
    )

    expected_act_data_2 = steps.CommonData.create_expected_act_data(total_compls_sum_2, month_minus1_end_dt)
    utils.check_that(consume_data_2, contains_dicts_with_entries(expected_consumes_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_2, equal_to_casted_dict(expected_invoice_data_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_2, contains_dicts_with_entries([expected_act_data_1, expected_act_data_2]),
                     'Сравниваем данные из акта с шаблоном')
