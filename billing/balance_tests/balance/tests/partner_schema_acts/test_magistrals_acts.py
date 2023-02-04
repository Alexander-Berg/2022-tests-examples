# -*- coding: utf-8 -*-

from collections import defaultdict
from decimal import Decimal as D

import pytest
from hamcrest import empty

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import InvoiceType, Products, Currencies, Services, NdsNew, ServiceCode

from btestlib.matchers import contains_dicts_with_entries
from btestlib.data.partner_contexts import MAGISTRALS_CARRIER_RU_CONTEXT, MAGISTRALS_SENDER_RU_CONTEXT

SERVICE_CURRENCY_PRODUCTS_MAP = {
    Services.MAGISTRALS_SENDER_COMMISSION: {
        Currencies.RUB: [Products.MAGISTRALS_SENDER_MAIN_RUB, ]
    },
    Services.MAGISTRALS_CARRIER_COMMISSION: {
        Currencies.RUB: [Products.MAGISTRALS_CARRIER_MAIN_RUB, ]
    },
}


def merge_sums_dicts(result_defaultdict, dict_for_merge):
    for k, v in dict_for_merge.items():
        result_defaultdict[k] += v
    return result_defaultdict


def create_oebs_completions(context, contract_id, client_id, dt, payment_sum=D('0'), refund_sum=D('0')):
    sum_w_nds = D('0')
    sums_by_products = defaultdict(lambda: D('0'))
    compls_dicts = []
    for idx, product in enumerate(SERVICE_CURRENCY_PRODUCTS_MAP[context.service][context.currency]):
        compls_dicts.append(
            {
                'service_id': context.service.id,
                'amount': (payment_sum - refund_sum),
                'product_id': product.id,
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
                'accounting_period': dt
            }
        )
        amount_w_nds = payment_sum - refund_sum
        sum_w_nds += amount_w_nds
        sums_by_products[product.id] += amount_w_nds
    steps.CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_dicts)
    return sum_w_nds, sums_by_products


def check_partner_balance(context, contract_id):
    expected_balance_data = []
    for service_code in context.service_codes_params.keys():
        pa_id, pa_external_id, pa_service_code = \
            steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id, service_code=service_code)
        expected_balance_data.append({
            'external_id': pa_external_id,
            'id': pa_id,
            'service_code': pa_service_code
        })
    balance = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    utils.check_that(expected_balance_data,
                     contains_dicts_with_entries(balance[0]['PersonalAccounts']),
                     'Сравниваем данные лицевых счетов в партнерских балансах')


def create_expected_invoice_data(context, person_id, contract_id, dt, amount, service_code):
    nds_pct = context.service_codes_params[service_code]['nds'].pct_on_dt(dt)
    nds_flag = int(bool(nds_pct))
    return steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id, person_id, amount,
        nds_pct=nds_pct, nds=nds_flag, dt=dt, paysys_id=context.service_codes_params[service_code]['paysys'].id)


# на данный момент логика одинаковая для отправителя и перевозчика. Если начнет расходиться - тесты разбить
@pytest.mark.parametrize('context', [
    pytest.param(MAGISTRALS_SENDER_RU_CONTEXT, id=MAGISTRALS_SENDER_RU_CONTEXT.name),
    pytest.param(MAGISTRALS_CARRIER_RU_CONTEXT, id=MAGISTRALS_CARRIER_RU_CONTEXT.name),
])
@pytest.mark.parametrize('is_postpay', [
    pytest.param(0, id='prepay'),
    pytest.param(1, id='postpay'),
])
def test_act_sender_wo_data(context, is_postpay):
    _, _, start_dt_1, end_dt_1, start_dt_2, end_dt_2 = utils.Date.previous_three_months_start_end_dates()
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': start_dt_1})
    check_partner_balance(context, contract_id)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, end_dt_1,
                                                                   manual_export=False)

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

    # проверяем данные в счете
    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data_agent = [
        create_expected_invoice_data(context, person_id, contract_id, start_dt_1, D('0'), service_code)
        for service_code, params in context.service_codes_params.items() if params['nds'] == NdsNew.ZERO
    ]
    expected_invoice_data_commission = create_expected_invoice_data(
        context, person_id, contract_id, start_dt_1, D('0'), ServiceCode.YANDEX_SERVICE)
    # проверяем данные в акте
    act_data_1 = steps.ActsSteps.get_act_data_by_client(client_id)
    # готовим ожидаемые данные для счёта
    expected_invoice_data_1 = expected_invoice_data_agent + [expected_invoice_data_commission]

    utils.check_that(invoice_data_1, contains_dicts_with_entries(expected_invoice_data_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, empty(), 'Сравниваем данные из акта с шаблоном')
    utils.check_that(consume_data_1, empty(), 'Проверяем, что конзюмов нет')


@pytest.mark.parametrize('personal_account_payment_sum', [
    pytest.param(D('10000'), id='pay-sum-10000'),
    pytest.param(D('0'), id='no-pay-sum'),
])
@pytest.mark.parametrize('is_postpay', [
    pytest.param(0, id='prepay'),
    pytest.param(1, id='postpay'),
])
@pytest.mark.parametrize('context', [
    pytest.param(MAGISTRALS_SENDER_RU_CONTEXT, id=MAGISTRALS_SENDER_RU_CONTEXT.name),
    pytest.param(MAGISTRALS_CARRIER_RU_CONTEXT, id=MAGISTRALS_CARRIER_RU_CONTEXT.name),
])
def test_act_sender_2_months(context, is_postpay, personal_account_payment_sum):
    month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
        utils.Date.previous_two_months_dates()

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': month_minus2_start_dt})

    expected_invoice_data_agent = [
        create_expected_invoice_data(context, person_id, contract_id, month_minus2_start_dt, D('0'), service_code)
        for service_code, params in context.service_codes_params.items() if params['nds'] == NdsNew.ZERO
    ]

    invoice_id, external_invoice_id, service_code = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id, service_code=ServiceCode.YANDEX_SERVICE)

    if personal_account_payment_sum:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_account_payment_sum,
                               payment_dt=month_minus2_start_dt)
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

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

    # проверяем данные в счете
    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    expected_invoice_data_commission_1 = create_expected_invoice_data(
        context, person_id, contract_id, month_minus2_start_dt, total_compls_sum, ServiceCode.YANDEX_SERVICE)
    expected_invoice_data_1 = expected_invoice_data_agent + [expected_invoice_data_commission_1]

    # проверяем данные в акте
    act_data_1 = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_consumes_1 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_1.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_act_data_1 = steps.CommonData.create_expected_act_data(total_compls_sum_1, month_minus2_end_dt)
    utils.check_that(consume_data_1, contains_dicts_with_entries(expected_consumes_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_1, contains_dicts_with_entries(expected_invoice_data_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, contains_dicts_with_entries([expected_act_data_1]),
                     'Сравниваем данные из акта с шаблоном')

    total_compls_sum_2 = D('0')
    compls_sum, sums_by_products = \
        create_oebs_completions(context, contract_id, client_id, month_minus1_start_dt, payment_sum_1, refund_sum_1)
    total_compls_sum += compls_sum
    total_compls_sum_2 += compls_sum
    total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus1_end_dt)

    consume_data_2 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

    # проверяем данные в счете
    invoice_data_2 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data_2 = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_consumes_2 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_2.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_commission_2 = create_expected_invoice_data(
        context, person_id, contract_id, month_minus2_start_dt, total_compls_sum, ServiceCode.YANDEX_SERVICE)
    expected_invoice_data_2 = expected_invoice_data_agent + [expected_invoice_data_commission_2]

    expected_act_data_2 = steps.CommonData.create_expected_act_data(total_compls_sum_2, month_minus1_end_dt)
    utils.check_that(consume_data_2, contains_dicts_with_entries(expected_consumes_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_2, contains_dicts_with_entries(expected_invoice_data_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_2, contains_dicts_with_entries([expected_act_data_1, expected_act_data_2]),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('is_postpay', [
    pytest.param(0, id='prepay'),
    pytest.param(1, id='postpay'),
])
@pytest.mark.parametrize('context', [
    pytest.param(MAGISTRALS_SENDER_RU_CONTEXT, marks=pytest.mark.smoke, id=MAGISTRALS_SENDER_RU_CONTEXT.name),
    pytest.param(MAGISTRALS_CARRIER_RU_CONTEXT, marks=pytest.mark.smoke, id=MAGISTRALS_CARRIER_RU_CONTEXT.name),
])
def test_acts_sender_on_finished_contract(context, is_postpay):
    personal_account_payment_sum = D('10000')

    month1_start_dt, month1_end_dt, \
    month2_start_dt, month2_end_dt,\
    month3_start_dt, month3_end_dt = \
        utils.Date.previous_three_months_start_end_dates()

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': month1_start_dt,
                                                                       'finish_dt': month2_start_dt})
    invoice_id, external_invoice_id, service_code = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id, service_code=ServiceCode.YANDEX_SERVICE)

    steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_account_payment_sum,
                           payment_dt=month1_start_dt)
    expected_invoice_data_agent = [
        create_expected_invoice_data(context, person_id, contract_id, month1_start_dt, D('0'), service_code)
        for service_code, params in context.service_codes_params.items() if params['nds'] == NdsNew.ZERO
    ]

    payment_sum_1 = D('420.69')
    refund_sum_1 = D('120.15')

    total_compls_sum = D('0')
    total_compls_sum_1 = D('0')
    total_sums_by_products = defaultdict(lambda: D('0'))

    compls_sum, sums_by_products = create_oebs_completions(
        context, contract_id, client_id, month1_start_dt, payment_sum_1, refund_sum_1
    )
    total_compls_sum += compls_sum
    total_compls_sum_1 += compls_sum
    total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    # запускаем конец месяца
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month1_end_dt)

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

    # проверяем данные в счете
    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data_1 = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_consumes_1 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_1.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_commission_1 = create_expected_invoice_data(
        context, person_id, contract_id, month1_start_dt, total_compls_sum, ServiceCode.YANDEX_SERVICE)
    expected_invoice_data_1 = expected_invoice_data_agent + [expected_invoice_data_commission_1]

    expected_act_data_1 = steps.CommonData.create_expected_act_data(total_compls_sum_1, month1_end_dt)
    utils.check_that(consume_data_1, contains_dicts_with_entries(expected_consumes_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_1, contains_dicts_with_entries(expected_invoice_data_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, contains_dicts_with_entries([expected_act_data_1]),
                     'Сравниваем данные из акта с шаблоном')

    total_compls_sum_2 = D('0')
    compls_sum, sums_by_products = create_oebs_completions \
        (context, contract_id, client_id, month2_start_dt, payment_sum_1, refund_sum_1)
    total_compls_sum += compls_sum
    total_compls_sum_2 += compls_sum
    total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_end_dt)

    consume_data_2 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

    # проверяем данные в счете
    invoice_data_2 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data_2 = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_consumes_2 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_2.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_commission_2 = create_expected_invoice_data(
        context, person_id, contract_id, month1_start_dt, total_compls_sum, ServiceCode.YANDEX_SERVICE)
    expected_invoice_data_2 = expected_invoice_data_agent + [expected_invoice_data_commission_2]

    expected_act_data_2 = steps.CommonData.create_expected_act_data(total_compls_sum_2, month2_end_dt)
    utils.check_that(consume_data_2, contains_dicts_with_entries(expected_consumes_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_2, contains_dicts_with_entries(expected_invoice_data_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_2, contains_dicts_with_entries([expected_act_data_1, expected_act_data_2]),
                     'Сравниваем данные из акта с шаблоном')


    total_compls_sum_3 = D('0')
    compls_sum, sums_by_products = create_oebs_completions \
        (context, contract_id, client_id, month3_start_dt, payment_sum_1, refund_sum_1)
    total_compls_sum += compls_sum
    total_compls_sum_3 += compls_sum
    total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month3_end_dt)

    consume_data_3 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

    # проверяем данные в счете
    invoice_data_3 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data_3 = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_consumes_3 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_3.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_commission_3 = create_expected_invoice_data(
        context, person_id, contract_id, month1_start_dt, total_compls_sum, ServiceCode.YANDEX_SERVICE)
    expected_invoice_data_3 = expected_invoice_data_agent + [expected_invoice_data_commission_3]

    expected_act_data_3 = steps.CommonData.create_expected_act_data(total_compls_sum_3, month3_end_dt)
    utils.check_that(consume_data_3, contains_dicts_with_entries(expected_consumes_3),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_3, contains_dicts_with_entries(expected_invoice_data_3),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_3, contains_dicts_with_entries([expected_act_data_1, expected_act_data_2,
                                                              expected_act_data_3]),
                     'Сравниваем данные из акта с шаблоном')
