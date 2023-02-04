# -*- coding: utf-8 -*-

from collections import defaultdict
from datetime import datetime
from decimal import Decimal as D
from dateutil.relativedelta import relativedelta

import pytest
from hamcrest import empty

import balance.balance_db as db

from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as tsteps
import balance.tests.promocode_new.promocode_commons as promo_steps
import btestlib.reporter as reporter
from btestlib import utils
from btestlib.constants import (
    InvoiceType, Products, PromocodeClass, Export, Currencies, LogisticsClientsOrderType,
    CorpTaxiOrderType, Firms,
)
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries
from btestlib.data.partner_contexts import (
    LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL,
    LOGISTICS_PAYMENTS_RU_CONTEXT_GENERAL,
    LOGISTICS_CLIENTS_ISRAEL_CONTEXT_GENERAL,
    LOGISTICS_CLIENTS_BY_CONTEXT_GENERAL,
    DELIVERY_DISPATCHING_BY_CONTEXT,
    LOGISTICS_CLIENTS_KZ_CONTEXT_GENERAL,
    LOGISTICS_CLIENTS_YANGO_DELIVERY_ISRAEL_CONTEXT_GENERAL,
    LOGISTICS_CLIENTS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_GENERAL,
    LOGISTICS_CLIENTS_YANGO_CHILE_SPA_CONTEXT_GENERAL,
    LOGISTICS_CLIENTS_YANDEX_LOG_OZB_USD_CONTEXT_GENERAL,
    LOGISTICS_CLIENTS_YANDEX_LOG_OZB_RUB_CONTEXT_GENERAL,
    LOGISTICS_CLIENTS_YANDEX_LOG_OZB_UZS_CONTEXT_GENERAL
)

LOGISTICS_CLIENTS_CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP = {
    Currencies.RUB: [
        (LogisticsClientsOrderType.delivery, Products.LOGISTICS_CLIENTS_DELIVERY_RUB.id),
        (LogisticsClientsOrderType.cargo, Products.LOGISTICS_CLIENTS_CARGO_RUB.id),
    ],
    Currencies.ILS: [
        (LogisticsClientsOrderType.delivery, Products.LOGISTICS_CLIENTS_DELIVERY_ILS.id),
        (LogisticsClientsOrderType.cargo, Products.LOGISTICS_CLIENTS_CARGO_ILS.id),
    ],
    Currencies.BYN: [
        (LogisticsClientsOrderType.delivery, Products.LOGISTICS_CLIENTS_DELIVERY_BYN.id),
        (LogisticsClientsOrderType.cargo, Products.LOGISTICS_CLIENTS_CARGO_BYN.id),
    ],
    Currencies.KZT: [
        (LogisticsClientsOrderType.delivery, Products.LOGISTICS_CLIENTS_DELIVERY_KZT.id),
        (LogisticsClientsOrderType.cargo, Products.LOGISTICS_CLIENTS_CARGO_KZT.id),
    ],
    Currencies.RSD: [
        (LogisticsClientsOrderType.delivery, Products.DELIVERY_CLIENT_B2B_LOGISTICS_PAYMENT.id),
        (LogisticsClientsOrderType.cargo, Products.CARGO_CLIENT_B2B_LOGISTICS_PAYMENT.id),
    ],
    Currencies.CLP: [
        (LogisticsClientsOrderType.delivery, Products.DELIVERY_CLIENT_B2B_LOGISTICS_PAYMENT_CLP.id),
        (LogisticsClientsOrderType.revenue, Products.B2B_AGENT_LOGISTICS_REVENUE_CLP.id),
    ],
    # меппинга продуктов пока нет, см. BALANCE-39776
    # Currencies.USD: [
    #     (LogisticsClientsOrderType.delivery, Products.DELIVERY_CLIENT_B2B_LOGISTICS_PAYMENT_CLP.id),
    #     (LogisticsClientsOrderType.revenue, Products.B2B_AGENT_LOGISTICS_REVENUE_CLP.id),
    # ],
    # Currencies.UZS: [
    #     (LogisticsClientsOrderType.delivery, Products.DELIVERY_CLIENT_B2B_LOGISTICS_PAYMENT_CLP.id),
    #     (LogisticsClientsOrderType.revenue, Products.B2B_AGENT_LOGISTICS_REVENUE_CLP.id),
    # ],
}

DISPATCHING_CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP = {
    Currencies.BYN: [
        (CorpTaxiOrderType.dispatching_commission, Products.DELIVERY_DISPATCHING_B2B_TRIPS_ACCESS_PAYMENT.id),
    ],
}

DISCOUNT_BONUS_SUM = D('0')
APX_SUM = D('0')


@utils.memoize
def get_product_mapping():
    return tsteps.TaxiSteps.get_product_mapping()


@utils.memoize
def get_ua_root_product_mapping():
    with reporter.step(u"Получаем маппинг из service_id + currency + order_type -> product_id"):
        query = "SELECT pp.service_id, pp.currency_iso_code, p.id product_id, pp.unified_account_root " \
                "FROM bo.t_partner_product pp left join bo.t_product p on p.mdh_id = pp.product_mdh_id"
        rows = db.balance().execute(query)

        return {(row['service_id'], row['currency_iso_code']):
                    row['product_id'] for row in rows if row['unified_account_root'] == 1}


@utils.memoize
def get_product_id_service_mapping():
    return {product_id: service_id for (service_id, _, _), product_id in get_product_mapping().items()}


def create_oebs_completions(context, contract_id, client_id, dt, payment_sum=D('0'), refund_sum=D('0')):
    sum_w_nds = D('0')
    sums_by_products = defaultdict(lambda: D('0'))
    compls_dicts = []
    for idx, (order_type, product_id) in enumerate(LOGISTICS_CLIENTS_CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP[context.currency]):
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


def create_dispatching_oebs_completions(context, contract_id, client_id, dt, payment_sum=D('0'), refund_sum=D('0')):
    sum_w_nds = D('0')
    sums_by_products = defaultdict(lambda: D('0'))
    compls_dicts = []
    for idx, (order_type, product_id) in enumerate(DISPATCHING_CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP[context.currency]):
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


def prepare_expected_balance_data(context, contract_id, client_id, invoice_eid, is_postpay, personal_account_pay_sum,
                                  total_compls_sum, cur_month_charge, act_sum):
    if is_postpay:
        expected_balance = {
            'ClientID': client_id,
            'ContractID': contract_id,
            'Currency': context.currency.iso_code,
            'CommissionToPay': cur_month_charge,
            'ActSum': act_sum,
            'BonusLeft': D('0'),
            'CurrMonthBonus': D('0'),
        }
    else:
        expected_balance = {
            'ClientID': client_id,
            'ContractID': contract_id,
            'PersonalAccountExternalID': invoice_eid,
            'ReceiptSum': personal_account_pay_sum,
            'DiscountBonusSum': DISCOUNT_BONUS_SUM,
            'TotalCharge': total_compls_sum,
            'ApxSum': APX_SUM,
            'Currency': context.currency.iso_code,
            'Balance': personal_account_pay_sum - total_compls_sum,
            'CurrMonthCharge': cur_month_charge,
            'ActSum': act_sum,
            'BonusLeft': D('0'),
            'CurrMonthBonus': D('0'),
        }
    return expected_balance


def check_balance(descr, context, contract_id, client_id, invoice_eid, is_postpay, personal_account_pay_sum,
                  total_compls_sum, cur_month_charge, act_sum):
    partner_balance = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    expected_balance = prepare_expected_balance_data(context, contract_id, client_id, invoice_eid,
                                                     is_postpay, personal_account_pay_sum,
                                                     total_compls_sum, cur_month_charge, act_sum)
    utils.check_that(partner_balance, contains_dicts_with_entries([expected_balance]), descr)


def merge_sums_dicts(result_defaultdict, dict_for_merge):
    for k, v in dict_for_merge.items():
        result_defaultdict[k] += v
    return result_defaultdict


@pytest.mark.parametrize('context', [
    pytest.param(LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL, id=LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_PAYMENTS_RU_CONTEXT_GENERAL, id=LOGISTICS_PAYMENTS_RU_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_ISRAEL_CONTEXT_GENERAL, id=LOGISTICS_CLIENTS_ISRAEL_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_BY_CONTEXT_GENERAL, id=LOGISTICS_CLIENTS_BY_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_KZ_CONTEXT_GENERAL, id=LOGISTICS_CLIENTS_KZ_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_YANGO_DELIVERY_ISRAEL_CONTEXT_GENERAL,
                 id=LOGISTICS_CLIENTS_YANGO_DELIVERY_ISRAEL_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_GENERAL,
                 id=LOGISTICS_CLIENTS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_YANGO_CHILE_SPA_CONTEXT_GENERAL,
                 id=LOGISTICS_CLIENTS_YANGO_CHILE_SPA_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_YANDEX_LOG_OZB_USD_CONTEXT_GENERAL,
                 id=LOGISTICS_CLIENTS_YANDEX_LOG_OZB_USD_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_YANDEX_LOG_OZB_RUB_CONTEXT_GENERAL,
                 id=LOGISTICS_CLIENTS_YANDEX_LOG_OZB_RUB_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_YANDEX_LOG_OZB_UZS_CONTEXT_GENERAL,
                 id=LOGISTICS_CLIENTS_YANDEX_LOG_OZB_UZS_CONTEXT_GENERAL.name),
])
@pytest.mark.parametrize('is_postpay', [
    pytest.param(0, id='prepay'),
    pytest.param(1, id='postpay'),
])
def test_act_wo_data(context, is_postpay):
    _, _, start_dt_1, end_dt_1, start_dt_2, end_dt_2 = utils.Date.previous_three_months_start_end_dates()

    # Костыль (т.к. фирма на тестовые даты еще не заведена). Удалить в октябре 2022 или позднее
    if context.firm == Firms.YANGO_DELIVERY_BEOGRAD_1898 and datetime.now() < datetime(2022, 10, 1):
        start_dt_1 = datetime(2022, 6, 15)
    # Костыль (т.к. tax_policy_pct на тестовые даты еще не заведен). Удалить в ноябре 2022 или позднее
    if context.firm == Firms.YANDEX_LOG_OZB and datetime.now() < datetime(2022, 11, 1):
        start_dt_1 = datetime(2022, 7, 15)

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': start_dt_1})

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


@pytest.mark.parametrize('personal_account_payment_sum', [
    pytest.param(D('10000'), id='pay-sum-10000'),
    pytest.param(D('0'), id='no-pay-sum'),
])
@pytest.mark.parametrize('is_postpay', [
    pytest.param(0, id='prepay'),
    pytest.param(1, id='postpay'),
])
@pytest.mark.parametrize('context', [
    pytest.param(LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL,
                 marks=pytest.mark.smoke,
                 id=LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_PAYMENTS_RU_CONTEXT_GENERAL, id=LOGISTICS_PAYMENTS_RU_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_ISRAEL_CONTEXT_GENERAL, id=LOGISTICS_CLIENTS_ISRAEL_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_BY_CONTEXT_GENERAL, id=LOGISTICS_CLIENTS_BY_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_KZ_CONTEXT_GENERAL, id=LOGISTICS_CLIENTS_KZ_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_YANGO_DELIVERY_ISRAEL_CONTEXT_GENERAL,
                 id=LOGISTICS_CLIENTS_YANGO_DELIVERY_ISRAEL_CONTEXT_GENERAL.name),
    # меппинга на продукты нет, такси придет отдельно для тестирования закрытия
    # pytest.param(LOGISTICS_CLIENTS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_GENERAL,
    #              id=LOGISTICS_CLIENTS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_CLIENTS_YANGO_CHILE_SPA_CONTEXT_GENERAL,
                 id=LOGISTICS_CLIENTS_YANGO_CHILE_SPA_CONTEXT_GENERAL.name),
    # меппинга на продукты нет, такси придет отдельно для тестирования закрытия (BALANCE-39776)
    # pytest.param(LOGISTICS_CLIENTS_YANDEX_LOG_OZB_USD_CONTEXT_GENERAL,
    #              id=LOGISTICS_CLIENTS_YANDEX_LOG_OZB_USD_CONTEXT_GENERAL.name),
    # pytest.param(LOGISTICS_CLIENTS_YANDEX_LOG_OZB_RUB_CONTEXT_GENERAL,
    #              id=LOGISTICS_CLIENTS_YANDEX_LOG_OZB_RUB_CONTEXT_GENERAL.name),
    # pytest.param(LOGISTICS_CLIENTS_YANDEX_LOG_OZB_UZS_CONTEXT_GENERAL,
    #              id=LOGISTICS_CLIENTS_YANDEX_LOG_OZB_UZS_CONTEXT_GENERAL.name),
])
def test_act_2_months(context, is_postpay, personal_account_payment_sum):
    month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
        utils.Date.previous_two_months_dates()

    # Костыль (т.к. фирма на тестовые даты еще не заведена). Удалить в октябре 2022 или позднее
    if context.firm == Firms.YANGO_DELIVERY_BEOGRAD_1898 and datetime.now() < datetime(2022, 10, 1):
        month_minus2_start_dt = datetime(2022, 6, 15)
        month_minus2_end_dt = datetime(2022, 6, 30)
        month_minus1_start_dt = datetime(2022, 6, 15)
        month_minus1_end_dt = datetime(2022, 6, 30)

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': month_minus2_start_dt})
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)

    check_balance(u'Проверяем баланс до платежа', context, contract_id, client_id, external_invoice_id,
                  is_postpay, personal_account_pay_sum=D('0'), total_compls_sum=D('0'), cur_month_charge=D('0'),
                  act_sum=D('0'))

    if personal_account_payment_sum:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_account_payment_sum,
                               payment_dt=month_minus2_start_dt)
    check_balance(u'Проверяем баланс после платежа', context, contract_id, client_id, external_invoice_id,
                  is_postpay, personal_account_pay_sum=personal_account_payment_sum, total_compls_sum=D('0'),
                  cur_month_charge=D('0'), act_sum=D('0'))

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

    check_balance(u'Проверяем баланс после создания откруток в 1м месяце', context, contract_id, client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=total_compls_sum_1, act_sum=D('0'))

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus2_end_dt)

    check_balance(u'Проверяем баланс после создания актов в 1м месяце', context, contract_id, client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=D('0'), act_sum=total_compls_sum_1)

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

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

    check_balance(u'Проверяем баланс после создания откруток во 2м месяце', context, contract_id, client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=total_compls_sum_2,
                  act_sum=total_compls_sum_1)

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus1_end_dt)

    check_balance(u'Проверяем баланс после создания акта во 2м месяце', context, contract_id, client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=D('0'), act_sum=total_compls_sum)

    consume_data_2 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

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


@pytest.mark.parametrize('is_postpay', [
    pytest.param(0, id='prepay'),
    pytest.param(1, id='postpay'),
])
@pytest.mark.parametrize('context', [
    pytest.param(DELIVERY_DISPATCHING_BY_CONTEXT, id=DELIVERY_DISPATCHING_BY_CONTEXT.name),
])
def test_dispatching_act_2_months(context, is_postpay):
    month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
        utils.Date.previous_two_months_dates()

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': month_minus2_start_dt})
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)

    payment_sum_1 = D('420.69')
    refund_sum_1 = D('120.15')

    total_compls_sum = D('0')
    total_compls_sum_1 = D('0')
    total_sums_by_products = defaultdict(lambda: D('0'))

    compls_sum, sums_by_products = create_dispatching_oebs_completions(
        context, contract_id, client_id, month_minus2_start_dt, payment_sum_1, refund_sum_1
    )
    total_compls_sum += compls_sum
    total_compls_sum_1 += compls_sum
    total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    # запускаем конец месяца
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus2_end_dt)

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

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
    compls_sum, sums_by_products = create_dispatching_oebs_completions \
        (context, contract_id, client_id, month_minus1_start_dt, payment_sum_1, refund_sum_1)
    total_compls_sum += compls_sum
    total_compls_sum_2 += compls_sum
    total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    # запускаем конец
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus1_end_dt)

    consume_data_2 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

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


################ ПРОМОКОДЫ ####################################
def create_new_promo(client_id, context, promo_sum, adjust_quantity=True, apply_on_create=True):
    start_dt = datetime.now() - relativedelta(years=1)
    end_dt = datetime.now() + relativedelta(years=1)
    minimal_amounts = {context.currency.iso_code: 1}

    calc_params = promo_steps.fill_calc_params_fixed_sum(currency_bonuses={context.currency.iso_code: promo_sum},
                                                         reference_currency=context.currency.iso_code,
                                                         adjust_quantity=adjust_quantity,
                                                         apply_on_create=apply_on_create)

    code = steps.PromocodeSteps.generate_code()
    promo_code_id, promo_code_code = promo_steps.import_promocode(calc_class_name=PromocodeClass.FIXED_SUM,
                                                                  start_dt=start_dt, end_dt=end_dt,
                                                                  calc_params=calc_params,
                                                                  firm_id=context.firm.id,
                                                                  promocodes=[code],
                                                                  service_ids=context.contract_services,
                                                                  minimal_amounts=minimal_amounts)[0]
    if client_id:
        promo_steps.reserve(client_id, promo_code_id)

    return promo_code_id, promo_code_code


def create_request_taxi(client_id, service_order_id, qty, dt, service_id, promo=None):
    begin_dt = dt

    additional_params = {}
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': begin_dt}]
    additional_params.update({'TurnOnRows': 1, 'InvoiceDesireType': 'charge_note'})
    if promo:
        additional_params.update({'PromoCode': promo})

    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=additional_params)  # 'InvoiceDesireType': 'charge_note'})
    return request_id


def calculate_discount_pct_from_fixed_sum(fixed_sum, nds, qty, sum_before=None, adjust_quantity=None):
    internal_price = 1
    if sum_before:
        total_sum = sum(sum_before)
    else:
        total_sum = qty * D(internal_price).quantize(D('0.001'))
    bonus_with_nds = promo_steps.add_nds_to_amount(fixed_sum, nds)
    return promo_steps.calculate_static_discount_sum(total_sum=total_sum, bonus_with_nds=bonus_with_nds,
                                                     adjust_quantity=adjust_quantity)


@pytest.mark.parametrize('context', [
    LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL,
], ids=lambda c: c.name)
def test_promocode(context):
    month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
        utils.Date.previous_two_months_dates()

    # корп.такси применяет промокоды только FixedSumBonusPromoCodeGroup, т.е. на сумму оплаты докидываются лишние qty
    adjust_quantity = True
    # сумма промо без НДС
    promo_sum = D('50000')
    # сумма платежа с НДС
    pa_payment_sum = D('100000')

    # суммы откруток
    payment_sum_1 = D('420')
    refund_sum_1 = D('160')

    params = {'start_dt': month_minus2_start_dt}
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        is_postpay=False,
        additional_params=params)

    service_order_id = steps.OrderSteps.get_order_id_by_contract(contract_id, context.service.id)

    # создаю промокод, привязанный к клиенту
    create_new_promo(client_id, context, promo_sum, adjust_quantity=adjust_quantity)

    # создаю реквест и счет-квитанцию
    request_id = create_request_taxi(client_id, service_order_id, pa_payment_sum,
                                     month_minus2_start_dt, context.service.id)
    invoice_id_charge_note, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=False,
                                                             contract_id=contract_id)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    payment_id = steps.InvoiceSteps.get_payment_id_for_charge_note(invoice_id_charge_note)

    # рассчитываю скидку и сумму к оплате
    discount = calculate_discount_pct_from_fixed_sum(promo_sum, context.nds.pct_on_dt(month_minus2_start_dt),
                                                     pa_payment_sum, adjust_quantity=adjust_quantity)

    # оплачиваю счет на сумму с учетом скидки
    steps.TaxiSteps.create_cash_payment_fact(external_invoice_id, pa_payment_sum, month_minus2_start_dt,
                                             'INSERT', payment_id)
    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

    # делаю открутки
    dt = month_minus2_start_dt + relativedelta(days=5)

    total_compls_qty = D('0')
    cur_compls_qty_1 = D('0')
    total_qty_by_products = defaultdict(lambda: D('0'))
    cur_qty_by_products_1 = defaultdict(lambda: D('0'))

    compls_qty_sum, qty_by_products = create_oebs_completions(
        context, contract_id, client_id, dt, payment_sum_1, refund_sum_1)
    total_compls_qty += compls_qty_sum
    cur_compls_qty_1 += compls_qty_sum
    total_qty_by_products = merge_sums_dicts(total_qty_by_products, qty_by_products)
    cur_qty_by_products_1 = merge_sums_dicts(cur_qty_by_products_1, qty_by_products)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus2_end_dt)

    main_product_id = get_ua_root_product_mapping()[context.service.id, context.currency.iso_code]
    total_sum_by_products = {product_id: (product_sum * (D('1') - discount * D('0.01')))
                             for (product_id, product_sum) in total_qty_by_products.items()}
    cur_sum_by_products_1 = {product_id: (product_sum * (D('1') - discount * D('0.01')))
                             for (product_id, product_sum) in cur_qty_by_products_1.items()}
    total_compls_sum = sum(_sum for (product_id, _sum) in total_sum_by_products.items())
    cur_compls_sum_1 = sum(_sum for (product_id, _sum) in cur_sum_by_products_1.items())

    expected_invoice_data_1 = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                       contract_id, person_id,
                                                                                       pa_payment_sum,
                                                                                       total_act_sum=total_compls_sum,
                                                                                       dt=month_minus2_start_dt)

    expected_act_data_1 = steps.CommonData.create_expected_act_data(amount=cur_compls_sum_1,
                                                                    act_date=month_minus2_end_dt)
    # на главном заказе сумма будет равна: общее зачисления со счета - сумма всех заказов (включая главный)
    # + сумма откруток по самому заказу
    consume_sum_main = pa_payment_sum - total_compls_sum + (payment_sum_1 - refund_sum_1) * (D('100') - discount) * D('0.01')
    consume_qty_main = consume_sum_main / (1 - discount * D('0.01'))

    expected_order_data_1 = []
    for product_id, consume_sum in total_sum_by_products.items():
        cs = consume_sum_main if product_id == main_product_id else consume_sum
        cq = consume_qty_main if product_id == main_product_id else total_qty_by_products[product_id]
        expected_order_data_1.append(
            steps.CommonData.create_expected_order_data(get_product_id_service_mapping()[product_id],
                                                        product_id, contract_id,
                                                        consume_sum=cs,
                                                        completion_qty=total_qty_by_products[product_id],
                                                        consume_qty=cq),
        )

    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    act_data_1 = steps.ActsSteps.get_act_data_by_client(client_id)
    order_data_1 = steps.OrderSteps.get_order_data_by_client(client_id)
    # отфильтруем нулевые заказы, (сюда попадают добивочные заказы, на которые ничего не зачисляется и не откручивается)
    order_data_1 = filter(lambda o: (o['completion_qty'] > 0 or o['consume_sum'] > 0 or o['consume_qty'] > 0), order_data_1)
    # тут есть ещё charge_note
    utils.check_that(invoice_data_1, contains_dicts_with_entries([expected_invoice_data_1], same_length=False),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, contains_dicts_with_entries([expected_act_data_1]),
                     'Сравниваем данные из акта с шаблоном')
    utils.check_that(order_data_1, contains_dicts_with_entries(expected_order_data_1),
                     'Сравниваем данные из заказа с шаблоном')

    # Периодически в рассчетах будет умножение на 2 - т.к. общая сумма зачислений и откруток по заказам, которые были
    # в первом месяце - повторятся. Открутки по новым добавленным сервисам будут в одном экземпляре

    # создаю промокод, привязанный к клиенту
    create_new_promo(client_id, context, promo_sum, adjust_quantity=adjust_quantity)

    # создаю реквест и счет-квитанцию
    request_id = create_request_taxi(client_id, service_order_id, pa_payment_sum,
                                     month_minus1_start_dt, context.service.id)
    invoice_id_charge_note, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=False,
                                                             contract_id=contract_id)
    payment_id = steps.InvoiceSteps.get_payment_id_for_charge_note(invoice_id_charge_note)
    # оплачиваю счет на сумму с учетом скидки
    steps.TaxiSteps.create_cash_payment_fact(external_invoice_id, pa_payment_sum, month_minus1_start_dt,
                                             'INSERT', payment_id)
    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

    # делаю открутки
    dt = month_minus1_start_dt + relativedelta(days=5)

    cur_compls_qty_2 = D('0')
    cur_qty_by_products_2 = defaultdict(lambda: D('0'))
    compls_qty_sum, qty_by_products = create_oebs_completions(
        context, contract_id, client_id, dt, payment_sum_1, refund_sum_1)
    total_compls_qty += compls_qty_sum
    cur_compls_qty_2 += compls_qty_sum
    total_qty_by_products = merge_sums_dicts(total_qty_by_products, qty_by_products)
    cur_qty_by_products_2 = merge_sums_dicts(cur_qty_by_products_2, qty_by_products)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   month_minus1_end_dt)

    total_sum_by_products = {product_id: (product_sum * (D('1') - discount * D('0.01')))
                             for (product_id, product_sum) in total_qty_by_products.items()}
    cur_sum_by_products_2 = {product_id: (product_sum * (D('1') - discount * D('0.01')))
                             for (product_id, product_sum) in cur_qty_by_products_2.items()}
    total_compls_sum = sum(_sum for (product_id, _sum) in total_sum_by_products.items())
    cur_compls_sum_2 = sum(_sum for (product_id, _sum) in cur_sum_by_products_2.items())

    expected_invoice_data_2 = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                       contract_id, person_id,
                                                                                       pa_payment_sum * 2,
                                                                                       total_act_sum=total_compls_sum,
                                                                                       dt=month_minus2_start_dt)

    expected_act_data_2 = steps.CommonData.create_expected_act_data(amount=cur_compls_sum_2,
                                                                    act_date=month_minus1_end_dt)
    # на главном заказе сумма будет равна: общее зачисления со счета - сумма всех заказов (включая главный)
    # + сумма откруток по самому заказу
    consume_sum_main = 2 * pa_payment_sum - total_compls_sum \
                       + 2 * (payment_sum_1 - refund_sum_1) * (D('100') - discount) * D('0.01')
    consume_qty_main = consume_sum_main / (1 - discount * D('0.01'))

    expected_order_data_2 = []
    for product_id, consume_sum in total_sum_by_products.items():
        cs = consume_sum_main if product_id == main_product_id else consume_sum
        cq = consume_qty_main if product_id == main_product_id else total_qty_by_products[product_id]
        expected_order_data_2.append(
            steps.CommonData.create_expected_order_data(get_product_id_service_mapping()[product_id],
                                                        product_id, contract_id,
                                                        consume_sum=cs,
                                                        completion_qty=total_qty_by_products[product_id],
                                                        consume_qty=cq),
        )

    invoice_data_2 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    act_data_2 = steps.ActsSteps.get_act_data_by_client(client_id)
    order_data_2 = steps.OrderSteps.get_order_data_by_client(client_id)
    # отфильтруем нулевые заказы, (сюда попадают добивочные заказы, на которые ничего не зачисляется и не откручивается)
    order_data_2 = filter(lambda o: (o['completion_qty'] > 0 or o['consume_sum'] > 0 or o['consume_qty'] > 0), order_data_2)
    # тут есть ещё charge_note
    utils.check_that(invoice_data_2, contains_dicts_with_entries([expected_invoice_data_2], same_length=False),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_2, contains_dicts_with_entries([expected_act_data_1,
                                                              expected_act_data_2]),
                     'Сравниваем данные из акта с шаблоном')
    utils.check_that(order_data_2, contains_dicts_with_entries(expected_order_data_2),
                     'Сравниваем данные из заказа с шаблоном')


@pytest.mark.parametrize('is_postpay', [
    pytest.param(0, id='prepay'),
    pytest.param(1, id='postpay'),
])
@pytest.mark.parametrize('context', [
    pytest.param(LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL,
                 marks=pytest.mark.smoke,
                 id=LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL.name),
    pytest.param(LOGISTICS_PAYMENTS_RU_CONTEXT_GENERAL, id=LOGISTICS_PAYMENTS_RU_CONTEXT_GENERAL.name),
])
def test_acts_on_finished_contract(context, is_postpay):
    personal_account_payment_sum = D('10000')

    month1_start_dt, month1_end_dt, \
    month2_start_dt, month2_end_dt,\
    month3_start_dt, month3_end_dt = \
        utils.Date.previous_three_months_start_end_dates()

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': month1_start_dt,
                                                                       'finish_dt': month2_start_dt})
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)

    steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_account_payment_sum,
                           payment_dt=month1_start_dt)

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

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month1_end_dt)

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

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
        context, contract_id, person_id, total_compls_sum, dt=month1_start_dt
    )

    expected_act_data_1 = steps.CommonData.create_expected_act_data(total_compls_sum_1, month1_end_dt)
    utils.check_that(consume_data_1, contains_dicts_with_entries(expected_consumes_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_1, equal_to_casted_dict(expected_invoice_data_1),
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
        context, contract_id, person_id, total_compls_sum, dt=month1_start_dt
    )

    expected_act_data_2 = steps.CommonData.create_expected_act_data(total_compls_sum_2, month2_end_dt)
    utils.check_that(consume_data_2, contains_dicts_with_entries(expected_consumes_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_2, equal_to_casted_dict(expected_invoice_data_2),
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
    invoice_data_3 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data_3 = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_consumes_3 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_3.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_3 = steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id, person_id, total_compls_sum, dt=month1_start_dt
    )

    expected_act_data_3 = steps.CommonData.create_expected_act_data(total_compls_sum_3, month3_end_dt)
    utils.check_that(consume_data_3, contains_dicts_with_entries(expected_consumes_3),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_3, equal_to_casted_dict(expected_invoice_data_3),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_3, contains_dicts_with_entries([expected_act_data_1, expected_act_data_2,
                                                              expected_act_data_3]),
                     'Сравниваем данные из акта с шаблоном')
