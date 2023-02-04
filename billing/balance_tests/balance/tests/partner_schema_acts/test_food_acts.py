# -*- coding: utf-8 -*-
__author__ = 'pelmeshka'

import decimal

import attr
from dateutil.relativedelta import relativedelta

import pytest
from hamcrest import empty
import balance.balance_db as db
import btestlib.reporter as reporter
from balance.balance_objects import Context
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import FoodProductType, TransactionType, Products, InvoiceType, Product, Paysyses, NdsNew
from btestlib.data.partner_contexts import FOOD_COURIER_CONTEXT, FOOD_COURIER_BY_CONTEXT, \
    FOOD_COURIER_KZ_CONTEXT, FOOD_RESTAURANT_CONTEXT, FOOD_RESTAURANT_BY_CONTEXT, FOOD_RESTAURANT_KZ_CONTEXT, \
    FOOD_SHOPS_CONTEXT, FOOD_PICKER_CONTEXT, \
    FOOD_PICKER_BUILD_ORDER_CONTEXT, REST_SITES_CONTEXT, FOOD_COURIER_BY_TAXI_BV_CONTEXT, \
    FOOD_RESTAURANT_BY_TAXI_BV_CONTEXT, EDA_HELP_CONTEXT, FOOD_COURIER_BY_FOODTECH_DELIVERY_BV_CONTEXT, \
    FOOD_RESTAURANT_BY_FOODTECH_DELIVERY_BV_CONTEXT, LAVKA_COURIER_FR_EUR_CONTEXT, LAVKA_COURIER_GB_GBP_CONTEXT, \
    FOOD_MERCURY_CONTEXT, FOOD_FULL_MERCURY_CONTEXT
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries

Decimal = decimal.Decimal

pytestmark = [reporter.feature(Features.FOOD, Features.PARTNER, Features.ACT)]

_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


def split_by_parts(total, parts, quant=decimal.Decimal('0.01')):
    # это можно сделать через rounded_delta в balance.muzzle_util
    ts = sum(parts, decimal.Decimal(0))
    prev_sum = decimal.Decimal(0)
    cur_sum = decimal.Decimal(0)
    for d in parts:
        cur_sum += decimal.Decimal(d)
        y_delta = ((total * cur_sum / ts).quantize(quant, decimal.ROUND_HALF_UP) -
                   (total * prev_sum / ts).quantize(quant, decimal.ROUND_HALF_UP))
        yield y_delta.quantize(quant, decimal.ROUND_HALF_UP)
        prev_sum = cur_sum


RESTAURANT_CONTEXTS = [
    FOOD_RESTAURANT_BY_TAXI_BV_CONTEXT,
    FOOD_RESTAURANT_BY_FOODTECH_DELIVERY_BV_CONTEXT,
    FOOD_RESTAURANT_CONTEXT,
    FOOD_RESTAURANT_BY_CONTEXT,
    FOOD_RESTAURANT_KZ_CONTEXT,
    FOOD_SHOPS_CONTEXT,
    # REST_SITES_CONTEXT, не запущено в проде, раскомментировать после настройки
    FOOD_MERCURY_CONTEXT,
    FOOD_FULL_MERCURY_CONTEXT,
]

COURIER_CONTEXTS = [
    FOOD_COURIER_BY_TAXI_BV_CONTEXT,
    FOOD_COURIER_CONTEXT,
    FOOD_COURIER_BY_CONTEXT,
    FOOD_COURIER_KZ_CONTEXT,
    FOOD_PICKER_CONTEXT,
    FOOD_PICKER_BUILD_ORDER_CONTEXT,
    EDA_HELP_CONTEXT,
    FOOD_COURIER_BY_FOODTECH_DELIVERY_BV_CONTEXT,
]

CONTEXTS = RESTAURANT_CONTEXTS + COURIER_CONTEXTS

PAYMENT_PRODUCTS_MAP = {
    FOOD_COURIER_BY_TAXI_BV_CONTEXT.name: [Products.FOOD_COURIER_BYN, Products.FOOD_REST_PAYMENTS_BYN],
    FOOD_RESTAURANT_CONTEXT.name: [Products.FOOD_REST_PAYMENTS_RUB, Products.FOOD_REST_PAYMENTS_RUB_CORP],
    FOOD_RESTAURANT_BY_CONTEXT.name: [Products.FOOD_REST_PAYMENTS_BYN],
    FOOD_RESTAURANT_BY_TAXI_BV_CONTEXT.name: [Products.FOOD_REST_PAYMENTS_BYN],
    FOOD_RESTAURANT_KZ_CONTEXT.name: [Products.FOOD_REST_PAYMENTS_KZT],
    FOOD_COURIER_CONTEXT.name: [Products.FOOD_COURIER_RUB, Products.FOOD_COURIER_RUB_CORP],
    FOOD_COURIER_BY_CONTEXT.name: [Products.FOOD_COURIER_BYN],
    FOOD_COURIER_KZ_CONTEXT.name: [Products.FOOD_COURIER_KZT],
    FOOD_SHOPS_CONTEXT.name: [Products.FOOD_SHOPS_PAYMENTS_RUB],
    EDA_HELP_CONTEXT.name: [Products.EDA_HELP_PAYMENTS_RUB],
    FOOD_PICKER_CONTEXT.name: [Products.FOOD_PICKER_RUB, Products.FOOD_PICKER_RUB_CORP],
    FOOD_PICKER_BUILD_ORDER_CONTEXT.name: [Products.FOOD_PICKER_BUILD_ORDER_RUB,
                                           Products.FOOD_PICKER_BUILD_ORDER_RUB_CORP],
    REST_SITES_CONTEXT.name: [Products.REST_SITES_PAYMENTS_RUB, Products.REST_SITES_PAYMENTS_RUB_CORP],
    FOOD_COURIER_BY_FOODTECH_DELIVERY_BV_CONTEXT.name: [Products.FOOD_COURIER_BYN, Products.FOOD_REST_PAYMENTS_BYN],
    FOOD_RESTAURANT_BY_FOODTECH_DELIVERY_BV_CONTEXT.name: [Products.FOOD_REST_PAYMENTS_BYN],
    FOOD_MERCURY_CONTEXT.name: [Products.FOOD_MERCURY_PAYMENTS_RUB],
    FOOD_FULL_MERCURY_CONTEXT.name: [Products.FOOD_MERCURY_PAYMENTS_RUB],
}


@attr.s
class ProductInfo(object):
    product = attr.ib(type=Product)
    order_type = attr.ib(type=FoodProductType, default=FoodProductType.GOODS)


RESTAURANT_PRODUCT_MAP = {
    FOOD_RESTAURANT_BY_TAXI_BV_CONTEXT.name: [ProductInfo(Products.FOOD_REST_SERVICES_BYN)],
    FOOD_RESTAURANT_BY_FOODTECH_DELIVERY_BV_CONTEXT.name: [ProductInfo(Products.FOOD_REST_SERVICES_BYN)],
    FOOD_RESTAURANT_CONTEXT.name: [ProductInfo(Products.FOOD_REST_SERVICES_PICKUP_RUB,
                                               order_type=FoodProductType.PICKUP),
                                   ProductInfo(Products.FOOD_REST_SERVICES_RUB)],
    FOOD_RESTAURANT_BY_CONTEXT.name: [ProductInfo(Products.FOOD_REST_SERVICES_BYN)],
    FOOD_RESTAURANT_KZ_CONTEXT.name: [ProductInfo(Products.FOOD_REST_SERVICES_KZT)],
    FOOD_SHOPS_CONTEXT.name: [ProductInfo(Products.FOOD_SHOPS_SERVICES_RUB, order_type=FoodProductType.RETAIL)],
    REST_SITES_CONTEXT.name: [ProductInfo(Products.REST_SITES_SERVICES_RUB,
                                          order_type=FoodProductType.TP_ORDER_PROCESSING)],
    FOOD_MERCURY_CONTEXT.name: [ProductInfo(Products.FOOD_MERCURY_SERVICES_RUB)],
    FOOD_FULL_MERCURY_CONTEXT.name: [ProductInfo(Products.FOOD_MERCURY_SERVICES_RUB)],
}

COURIER_PAYMENT_AMOUNT = Decimal('100.22')
RESTAURANT_PAYMENT_AMOUNT = Decimal('32.11')
RESTAURANT_SERVICE_AMOUNT = Decimal('42.42')


def product_info_and_part_amounts(product_infos, full_amount):
    return [(product_info, part_amount)
            for product_info, part_amount in zip(product_infos, split_by_parts(full_amount, (1,) * len(product_infos)))]


@attr.s
class PATestCase(object):
    context = attr.ib(type=Context)
    w_nds_paysys = attr.ib(type=Paysyses.constant_type)
    w_nds_nds = attr.ib(type=NdsNew)
    wo_nds_paysys = attr.ib(type=Paysyses.constant_type)
    wo_nds_nds = attr.ib(type=NdsNew)


# Во Франции, Великобритании и Израиле создаются только договоры с 2мя лицевыми счетами - с НДС и БЕЗ.
# На Израиль тест в test_lavka_commission с закрытием. Если великобритания и франция начнут тоже закрываться,
# допилить их и перенсти туда.
@pytest.mark.parametrize('test_case', [PATestCase(LAVKA_COURIER_FR_EUR_CONTEXT,
                                                  Paysyses.BANK_FR_UR_EUR_BANK_NDS, NdsNew.FR,
                                                  Paysyses.BANK_FR_UR_EUR_BANK_NO_NDS, NdsNew.ZERO),
                                       PATestCase(LAVKA_COURIER_GB_GBP_CONTEXT,
                                                  Paysyses.BANK_GB_UR_GBP_BANK_NDS, NdsNew.GB,
                                                  Paysyses.BANK_GB_UR_GBP_BANK_NO_NDS, NdsNew.ZERO)],
                         ids=lambda test_case: test_case.context.name)
def test_lavka_couriers_contracts_and_personal_accounts(test_case):
    context = test_case.context
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': first_month_start_dt},
        full_person_params=True
    )
    nds_invoice_id, _, _ = get_invoice(contract_id)
    no_nds_invoice_id, _, _ = get_invoice(contract_id, service_code='YANDEX_SERVICE_WO_VAT')

    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(
            context, contract_id,
            person_id,
            0,
            dt=first_month_start_dt,
            paysys_id=test_case.w_nds_paysys.id,
            nds_pct=test_case.w_nds_nds.pct_on_dt(first_month_start_dt),
            nds=1
        ),
        steps.CommonData.create_expected_invoice_data_by_context(
            context, contract_id,
            person_id,
            0,
            dt=first_month_start_dt,
            paysys_id=test_case.wo_nds_paysys.id,
            nds_pct=test_case.wo_nds_nds.pct_on_dt(first_month_start_dt),
            nds=0
        ),
    ]
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data, same_length=True),
                     u'Сравниваем данные из лицевых счетов с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('context', CONTEXTS, ids=lambda context: context.name)
def test_food_acts_wo_data(context):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': first_month_start_dt}
    )

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(act_data, empty(), 'Проверяем, что акты не сгенерились и генерация не упала')


@pytest.mark.smoke
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('context', COURIER_CONTEXTS, ids=lambda context: context.name)
def test_food_couriers_acts(context):
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
    month_minus2_start_dt, month_minus2_end_dt, \
    month_minus1_start_dt, month_minus1_end_dt, \
    migration_dt = \
        get_dates_for_migration(context)

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': month_migration_minus2_start_dt}
    )

    invoice_eid = None

    create_payments(context, client_id, person_id, contract_id, month_migration_minus2_start_dt, COURIER_PAYMENT_AMOUNT,
                    coef=1, invoice_eid=invoice_eid)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_end_dt)

    create_payments(context, client_id, person_id, contract_id, month_migration_minus1_start_dt, COURIER_PAYMENT_AMOUNT,
                    coef=2, invoice_eid=invoice_eid)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus1_end_dt)

    products = PAYMENT_PRODUCTS_MAP[context.name]
    products_coef = len(products)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id,
        person_id,
        3 * products_coef * COURIER_PAYMENT_AMOUNT,
        dt=month_migration_minus2_start_dt)
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data], same_length=False),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [
        steps.CommonData.create_expected_act_data(products_coef * COURIER_PAYMENT_AMOUNT, month_migration_minus2_end_dt),
        steps.CommonData.create_expected_act_data(2 * products_coef * COURIER_PAYMENT_AMOUNT, month_migration_minus1_end_dt)
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Сравниваем данные из акта с шаблоном')

    consume_data = steps.ConsumeSteps.get_consumes_by_client_id(client_id)
    expected_consume_data = [
        steps.CommonData.create_expected_consume_data(
            product.id,
            3 * COURIER_PAYMENT_AMOUNT,
            InvoiceType.PERSONAL_ACCOUNT
        ) for product in products
    ]
    utils.check_that(consume_data, contains_dicts_with_entries(expected_consume_data), u'Проверяем данные консьюмов')

    if migration_dt:
        create_payments(context, client_id, person_id, contract_id, month_minus2_start_dt, COURIER_PAYMENT_AMOUNT,
                        coef=1, invoice_eid=invoice_eid)
        create_payment_oebs_completions(context, contract_id, client_id, month_minus2_start_dt, COURIER_PAYMENT_AMOUNT,
                                        coef=1)
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus2_end_dt)

        create_payments(context, client_id, person_id, contract_id, month_minus1_start_dt, COURIER_PAYMENT_AMOUNT,
                        coef=2, invoice_eid=invoice_eid)
        create_payment_oebs_completions(context, contract_id, client_id, month_minus1_start_dt, COURIER_PAYMENT_AMOUNT,
                                        coef=2)
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus1_end_dt)

        products = PAYMENT_PRODUCTS_MAP[context.name]
        products_coef = len(products)

        invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
        expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
            context, contract_id,
            person_id,
            6 * products_coef * COURIER_PAYMENT_AMOUNT,
            dt=month_migration_minus2_start_dt)
        utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data], same_length=False),
                         u'Сравниваем данные из счета с шаблоном')

        act_data = steps.ActsSteps.get_act_data_by_client(client_id)
        expected_act_data = [
            steps.CommonData.create_expected_act_data(products_coef * COURIER_PAYMENT_AMOUNT,
                                                      month_migration_minus2_end_dt),
            steps.CommonData.create_expected_act_data(2 * products_coef * COURIER_PAYMENT_AMOUNT,
                                                      month_migration_minus1_end_dt),
            steps.CommonData.create_expected_act_data(products_coef * COURIER_PAYMENT_AMOUNT,
                                                      month_minus2_end_dt),
            steps.CommonData.create_expected_act_data(2 * products_coef * COURIER_PAYMENT_AMOUNT,
                                                      month_minus1_end_dt)
        ]
        utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                         u'Сравниваем данные из акта с шаблоном')

        consume_data = steps.ConsumeSteps.get_consumes_by_client_id(client_id)
        expected_consume_data = [
            steps.CommonData.create_expected_consume_data(
                product.id,
                6 * COURIER_PAYMENT_AMOUNT,
                InvoiceType.PERSONAL_ACCOUNT
            ) for product in products
        ]
        utils.check_that(consume_data, contains_dicts_with_entries(expected_consume_data),
                         u'Проверяем данные консьюмов')



@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('context', RESTAURANT_CONTEXTS, ids=lambda context: context.name)
def test_food_restaurant_acts(context):
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
    month_minus2_start_dt, month_minus2_end_dt, \
    month_minus1_start_dt, month_minus1_end_dt, \
    migration_dt = \
        get_dates_for_migration(context)

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': month_migration_minus2_start_dt}
    )

    create_payments(context, client_id, person_id, contract_id, month_migration_minus2_start_dt,
                    RESTAURANT_PAYMENT_AMOUNT, coef=1)
    create_restaurant_completions(context, client_id, month_migration_minus2_start_dt,
                                  RESTAURANT_SERVICE_AMOUNT, coef=1)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_end_dt)

    create_payments(context, client_id, person_id, contract_id, month_migration_minus1_start_dt,
                    RESTAURANT_PAYMENT_AMOUNT, coef=2)
    create_restaurant_completions(context, client_id, month_migration_minus1_start_dt,
                                  RESTAURANT_SERVICE_AMOUNT, coef=2)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus1_end_dt)

    products_payments_count = len(PAYMENT_PRODUCTS_MAP[context.name])
    products_commission_count = len(RESTAURANT_PRODUCT_MAP[context.name])
    total_month_sum = (products_payments_count * RESTAURANT_PAYMENT_AMOUNT
                       + products_commission_count * RESTAURANT_SERVICE_AMOUNT)

    # https://st.yandex-team.ru/BALANCE-37807
    # Сервис использует 662 сервис не по назначению, только для плюса по фиктивному клиенту.
    # Поэтому генерация актов отключена. После вводных - поправить
    #
    if context.name == 'FOOD_SHOPS_CONTEXT':
        total_month_sum -= RESTAURANT_PAYMENT_AMOUNT

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id,
        person_id,
        3 * total_month_sum,
        dt=month_migration_minus2_start_dt)
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [
        steps.CommonData.create_expected_act_data(total_month_sum, month_migration_minus2_end_dt),
        steps.CommonData.create_expected_act_data(2 * total_month_sum, month_migration_minus1_end_dt)
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Сравниваем данные из акта с шаблоном')

    consume_data = steps.ConsumeSteps.get_consumes_by_client_id(client_id)
    expected_consume_data = []
    # https://st.yandex-team.ru/BALANCE-37807
    # Сервис использует 662 сервис не по назначению, только для плюса по фиктивному клиенту.
    # Поэтому генерация актов отключена. После вводных - поправить
    #
    if context.name != 'FOOD_SHOPS_CONTEXT':
        expected_consume_data += [
            steps.CommonData.create_expected_consume_data(
                product.id,
                3 * RESTAURANT_PAYMENT_AMOUNT,
                InvoiceType.PERSONAL_ACCOUNT
            ) for product in PAYMENT_PRODUCTS_MAP[context.name]
        ]
    expected_consume_data += [
        steps.CommonData.create_expected_consume_data(
            product_info.product.id,
            3 * RESTAURANT_SERVICE_AMOUNT,
            InvoiceType.PERSONAL_ACCOUNT
        )
        for product_info in RESTAURANT_PRODUCT_MAP[context.name]
    ]
    utils.check_that(consume_data, contains_dicts_with_entries(expected_consume_data), u'Проверяем данные консьюмов')

    # Кейс после миграции
    if migration_dt:
        # старые открутки с датой после миграции не учитываются
        create_payments(context, client_id, person_id, contract_id, month_minus2_start_dt,
                        RESTAURANT_PAYMENT_AMOUNT, coef=1)
        create_restaurant_completions(context, client_id, month_minus2_start_dt, RESTAURANT_SERVICE_AMOUNT,
                                      coef=1)

        create_payment_oebs_completions(context, contract_id, client_id, month_minus2_start_dt,
                                        RESTAURANT_PAYMENT_AMOUNT, coef=1)
        create_commission_oebs_completions(context, contract_id, client_id, month_minus2_start_dt,
                                           RESTAURANT_SERVICE_AMOUNT, coef=1)

        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                       month_minus2_end_dt)

        create_payment_oebs_completions(context, contract_id, client_id, month_minus1_start_dt,
                                        RESTAURANT_PAYMENT_AMOUNT, coef=2)
        create_commission_oebs_completions(context, contract_id, client_id, month_minus1_start_dt,
                                           RESTAURANT_SERVICE_AMOUNT, coef=2)

        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus1_end_dt)

        invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
        expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
            context, contract_id,
            person_id,
            6 * total_month_sum,
            dt=month_migration_minus2_start_dt)
        utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                         u'Сравниваем данные из счета с шаблоном')

        act_data = steps.ActsSteps.get_act_data_by_client(client_id)
        expected_act_data = [
            steps.CommonData.create_expected_act_data(total_month_sum, month_migration_minus2_end_dt),
            steps.CommonData.create_expected_act_data(2 * total_month_sum, month_migration_minus1_end_dt),
            steps.CommonData.create_expected_act_data(total_month_sum, month_minus2_end_dt),
            steps.CommonData.create_expected_act_data(2 * total_month_sum, month_minus1_end_dt),
        ]
        utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                         u'Сравниваем данные из акта с шаблоном')

        consume_data = steps.ConsumeSteps.get_consumes_by_client_id(client_id)
        expected_consume_data = []
        # https://st.yandex-team.ru/BALANCE-37807
        # Сервис использует 662 сервис не по назначению, только для плюса по фиктивному клиенту.
        # Поэтому генерация актов отключена. После вводных - поправить
        #
        if context.name != 'FOOD_SHOPS_CONTEXT':
            expected_consume_data += [
                steps.CommonData.create_expected_consume_data(
                    product.id,
                    6 * RESTAURANT_PAYMENT_AMOUNT,
                    InvoiceType.PERSONAL_ACCOUNT
                ) for product in PAYMENT_PRODUCTS_MAP[context.name]
            ]
        expected_consume_data += [
            steps.CommonData.create_expected_consume_data(
                product_info.product.id,
                6 * RESTAURANT_SERVICE_AMOUNT,
                InvoiceType.PERSONAL_ACCOUNT
            )
            for product_info in RESTAURANT_PRODUCT_MAP[context.name]
        ]
        utils.check_that(consume_data, contains_dicts_with_entries(expected_consume_data),
                         u'Проверяем данные консьюмов')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('context', RESTAURANT_CONTEXTS, ids=lambda context: context.name)
def test_food_restaurant_incomplete_month_acts(context):
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
    month_minus2_start_dt, month_minus2_end_dt, \
    month_minus1_start_dt, month_minus1_end_dt, \
    migration_dt = \
        get_dates_for_migration(context)

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(
            context,
            additional_params={'start_dt': month_migration_minus2_start_dt.replace(day=10),
                               'finish_dt': month_migration_minus2_start_dt.replace(day=20)}
        )
    dts = [
        month_migration_minus2_start_dt.replace(day=9),
        month_migration_minus2_start_dt.replace(day=10),
        month_migration_minus2_start_dt.replace(day=19),
        month_migration_minus2_start_dt.replace(day=20)
    ]
    for num, dt in enumerate(dts):
        coef = pow(2, num)
        create_payments(context, client_id, person_id, contract_id, dt, RESTAURANT_PAYMENT_AMOUNT, coef=coef)
        create_restaurant_completions(context, client_id, dt, RESTAURANT_SERVICE_AMOUNT, coef=coef)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_end_dt)

    products_payments_count = len(PAYMENT_PRODUCTS_MAP[context.name])
    products_commission_count = len(RESTAURANT_PRODUCT_MAP[context.name])
    total_amount = 6 * (products_payments_count * RESTAURANT_PAYMENT_AMOUNT
                       + products_commission_count * RESTAURANT_SERVICE_AMOUNT)

    # https://st.yandex-team.ru/BALANCE-37807
    # Сервис использует 662 сервис не по назначению, только для плюса по фиктивному клиенту.
    # Поэтому генерация актов отключена. После новых вводных - поправить
    #
    if context.name == 'FOOD_SHOPS_CONTEXT':
        total_amount -= 6 * RESTAURANT_PAYMENT_AMOUNT

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context, contract_id,
                                                                                     person_id, total_amount,
                                                                                     dt=month_migration_minus2_start_dt)
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [
        steps.CommonData.create_expected_act_data(total_amount, month_migration_minus2_end_dt),
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Сравниваем данные из акта с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('context', COURIER_CONTEXTS, ids=lambda context: context.name)
def test_food_courier_incomplete_month_acts(context):
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
    month_minus2_start_dt, month_minus2_end_dt, \
    month_minus1_start_dt, month_minus1_end_dt, \
    migration_dt = \
        get_dates_for_migration(context)
    REWARDS = [Decimal(1000.1), Decimal(200.2), Decimal(30.3), Decimal(4.4)]

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(
            context,
            additional_params={'start_dt': month_migration_minus2_start_dt.replace(day=10),
                               'finish_dt': month_migration_minus2_start_dt.replace(day=20)}
        )

    invoice_eid = None
    product = PAYMENT_PRODUCTS_MAP[context.name][0]
    steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id,
                                        dt=month_migration_minus2_start_dt.replace(day=9),
                                        yandex_reward=REWARDS[0], transaction_type=TransactionType.PAYMENT,
                                        product_id=product.id, invoice_eid=invoice_eid)
    steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id,
                                        dt=month_migration_minus2_start_dt.replace(day=10),
                                        yandex_reward=REWARDS[1], transaction_type=TransactionType.PAYMENT,
                                        product_id=product.id, invoice_eid=invoice_eid)
    steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id,
                                        dt=month_migration_minus2_start_dt.replace(day=19),
                                        yandex_reward=REWARDS[2], transaction_type=TransactionType.PAYMENT,
                                        product_id=product.id, invoice_eid=invoice_eid)
    steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id,
                                        dt=month_migration_minus2_start_dt.replace(day=20),
                                        yandex_reward=REWARDS[3], transaction_type=TransactionType.PAYMENT,
                                        product_id=product.id, invoice_eid=invoice_eid)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_start_dt)

    # договор действует с 10 по 20 число. Учитываем только открутки за 10 и 19 число
    month_sum = utils.dround2(REWARDS[1] + REWARDS[2])

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    # создаем шаблон для сравнения
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(
            context, contract_id,
            person_id,
            month_sum,
            dt=month_migration_minus2_start_dt),
    ]

    expected_act_data = [
        steps.CommonData.create_expected_act_data(month_sum, month_migration_minus2_end_dt),
    ]

    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data, same_length=False),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# ---------------UTILS----------------------------------------------------


def create_payments(context, client_id, person_id, contract_id, dt, amount, coef, invoice_eid=None):
    data = []
    for product in PAYMENT_PRODUCTS_MAP[context.name]:
        product_id = product.id
        data += [
            {
                'transaction_type': TransactionType.PAYMENT,
                'yandex_reward': coef * 2 * amount,
                'product_id': product_id,
                'invoice_eid': invoice_eid,
            },
            {
                'transaction_type': TransactionType.REFUND,
                'yandex_reward': coef * amount,
                'product_id': product_id,
                'invoice_eid': invoice_eid,
            }
        ]
    steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, data)


def create_restaurant_completions(context, client_id, dt, amount, coef):
    for product_info in RESTAURANT_PRODUCT_MAP[context.name]:
        steps.PartnerSteps.create_fake_product_completion(
            dt,
            client_id=client_id,
            service_id=context.commission_service.id,
            service_order_id=0,
            commission_sum=coef * amount,
            type=product_info.order_type
        )


def get_invoice(contract_id, service_code=None):
    with reporter.step(u'Находим eid для лицевого счета договора: {}'.format(contract_id)):
        query = "SELECT inv.id, inv.external_id FROM T_INVOICE inv LEFT OUTER JOIN T_EXTPROPS prop ON inv.ID = prop.OBJECT_ID " \
                "and prop.ATTRNAME='service_code' AND prop.CLASSNAME='PersonalAccount'" \
                "WHERE inv.CONTRACT_ID=:contract_id"
        if service_code:
            query += " AND prop.VALUE_STR = :service_code"
        else:
            query += " AND prop.VALUE_STR is null"

        invoice_data = db.balance().execute(query, {'contract_id': contract_id, 'service_code': service_code},
                                            single_row=True)

        if invoice_data == {}:
            raise Exception("No personal accounts by params")
        return invoice_data['id'], invoice_data['external_id'], service_code


def get_dates_for_migration(context):
    if getattr(context, 'migration_alias', None):
        migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    else:
        migration_params = {}
    migration_dt = migration_params and migration_params.get('migration_date') or None
    # 2 месяца до даты миграции
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
        utils.Date.previous_two_months_dates(migration_dt)
    # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
    posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
    oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt) if migration_dt else posible_oebs_compls_start_dt
    month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
        utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))
    return month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
           month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
           month_minus2_start_dt, month_minus2_end_dt, \
           month_minus1_start_dt, month_minus1_end_dt, migration_dt


def create_payment_oebs_completions(context, contract_id, client_id, dt, amount, coef):
    data = []
    for product in PAYMENT_PRODUCTS_MAP[context.name]:
        product_id = product.id
        data += [
            {
                'service_id': context.service.id,
                'amount': coef * amount,
                'product_id': product_id,
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
                'accounting_period': dt
            },
        ]
    steps.CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, data)


def create_commission_oebs_completions(context, contract_id, client_id, dt, amount, coef):
    data = []
    for product_info in RESTAURANT_PRODUCT_MAP[context.name]:
        data += [
            {
                'service_id': context.commission_service.id,
                'amount': coef * amount,
                'product_id': product_info.product.id,
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
                'accounting_period': dt
            },
        ]
    steps.CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, data)
