# -*- coding: utf-8 -*-

__author__ = 'a-vasin'

import pytest
from decimal import Decimal
from hamcrest import empty

import balance.balance_db as db
import balance.balance_steps as steps
from balance.features import Features
from btestlib import reporter, utils
from btestlib.constants import Services, ServiceCode, Firms, PaysysType, TransactionType, PaymentType
from btestlib.data.partner_contexts import *
from btestlib.matchers import contains_dicts_equal_to, contains_dicts_with_entries

_, _, FIRST_MONTH_START_DT, FIRST_MONTH_END_DT, SECOND_MONTH_START_DT, SECOND_MONTH_END_DT = \
    utils.Date.previous_three_months_start_end_dates()

PAYMENT_AMOUNT = Decimal('100.1')
REFUND_AMOUNT = Decimal('93.13')
TOTAL_AMOUNT = PAYMENT_AMOUNT - REFUND_AMOUNT

pytestmark = [
    reporter.feature(Features.TAXI, Features.ACT)
]

PAYMENT_SERVICES = [
    Services.TAXI.id,
    Services.UBER.id,
]
PAYMENT_SERVICES_WO_ACTS = [
    Services.TAXI_VEZET.id,
    Services.TAXI_RUTAXI.id,
]

CONTEXTS = [
    TAXI_RU_CONTEXT_CLONE,
    TAXI_KZ_CONTEXT,
    TAXI_ARM_CONTEXT,
    TAXI_BV_GEO_USD_CONTEXT,
    TAXI_BV_LAT_EUR_CONTEXT,
    TAXI_UBER_BV_AZN_USD_CONTEXT,
    TAXI_UBER_BV_BYN_AZN_USD_CONTEXT,
    TAXI_UBER_BV_BY_BYN_CONTEXT,
    TAXI_UBER_BV_BYN_BY_BYN_CONTEXT,
    TAXI_ISRAEL_CONTEXT,
    TAXI_YANGO_ISRAEL_CONTEXT,
    TAXI_AZARBAYCAN_CONTEXT,
    TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT,
    TAXI_GHANA_USD_CONTEXT,
    TAXI_BOLIVIA_USD_CONTEXT,
    TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT,
    TAXI_ZA_USD_CONTEXT,
    TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT,
    TAXI_YANDEX_GO_SRL_CONTEXT,
    TAXI_RU_INTERCOMPANY_CONTEXT,
    TAXI_BV_NOR_NOK_CONTEXT,
    TAXI_BV_COD_EUR_CONTEXT,
    TAXI_YA_TAXI_CORP_KZ_KZT_PLUS_AV_CONTEXT,
    TAXI_UBER_BV_BY_BYN_PLUS_AV_CONTEXT,
    TAXI_MLU_EUROPE_SWE_SEK_CONTEXT,
    TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT,
    TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT,
    TAXI_ARM_GEO_USD_CONTEXT,
    TAXI_ARM_KGZ_USD_CONTEXT,
    TAXI_ARM_GHA_USD_CONTEXT,
    TAXI_ARM_ZAM_USD_CONTEXT,
    TAXI_ARM_UZB_USD_CONTEXT,
    TAXI_ARM_CMR_EUR_CONTEXT,
    TAXI_ARM_SEN_EUR_CONTEXT,
    TAXI_ARM_CIV_EUR_CONTEXT,
    TAXI_ARM_ANG_EUR_CONTEXT,
    TAXI_ARM_MD_EUR_CONTEXT,
    TAXI_ARM_RS_EUR_CONTEXT,
    TAXI_ARM_LT_EUR_CONTEXT,
    TAXI_ARM_FIN_EUR_CONTEXT,
    TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT,
    # TAXI_ARM_BY_BYN_CONTEXT,
    # TAXI_ARM_NOR_NOK_CONTEXT,
]


@pytest.mark.parametrize('context', CONTEXTS, ids=lambda c: c.name)
def test_taxi_act_wo_data(context):
    partner_integration_params = (None if context.partner_integration is None else
                                  steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT)
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': FIRST_MONTH_START_DT},
        partner_integration_params=partner_integration_params,)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, FIRST_MONTH_START_DT,
                                                                   manual_export=False)

    invoice_data = steps.InvoiceSteps.get_personal_accounts_with_service_codes(client_id)
    expected_invoice_data = create_expected_invoice_data(context, contract_id, person_id, amount_payments=Decimal('0'),
                                                         invoice_dt=FIRST_MONTH_START_DT)
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    utils.check_that(act_data, empty(), 'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize(
    'context, need_act', [
        (cntx, False if cntx.firm == Firms.TAXI_13 and not cntx.client_intercompany else True)
        for cntx in CONTEXTS
    ],
    ids=lambda c: c.name
)
def test_taxi_payments_acts(context, need_act):
    migration_dt, \
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
    month_minus2_start_dt, month_minus2_end_dt, \
    month_minus1_start_dt, month_minus1_end_dt = steps.CommonPartnerSteps.generate_dates_for_taxi_payments_oebs_migration()
    partner_integration_params = (None if context.partner_integration is None else
                                  steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT)
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': month_migration_minus2_start_dt},
        partner_integration_params=partner_integration_params,
    )

    payment_services, act_services = get_payment_and_act_services_for(context)

    for service in payment_services:
        create_completions(context, service, client_id, person_id, contract_id, month_migration_minus2_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id, contract_id, month_migration_minus2_start_dt, manual_export=need_act
    )

    for service in payment_services:
        create_completions(context, service, client_id, person_id, contract_id, month_migration_minus2_start_dt)
        create_completions(context, service, client_id, person_id, contract_id, month_migration_minus1_start_dt, coef=2)
        # открутки ОЕБС до даты миграции не попадут в обработку
        create_partner_oebs_completions(context, service, client_id, person_id, contract_id, month_migration_minus1_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id, contract_id, month_migration_minus1_start_dt, manual_export=need_act
    )

    invoice_data = steps.InvoiceSteps.get_personal_accounts_with_service_codes(client_id)

    amount_payments = Decimal('0')

    if need_act:
        amount_payments = 4 * len(act_services) * TOTAL_AMOUNT

    expected_invoice_data = create_expected_invoice_data(context, contract_id, person_id, amount_payments, month_migration_minus2_start_dt)
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    if need_act:
        expected_act_data = [
            steps.CommonData.create_expected_act_data(len(act_services) * TOTAL_AMOUNT, month_migration_minus2_end_dt),
            steps.CommonData.create_expected_act_data(3 * len(act_services) * TOTAL_AMOUNT, month_migration_minus1_end_dt)
        ]
        expected_result = contains_dicts_with_entries(expected_act_data)
    else:
        expected_result = empty()

    utils.check_that(act_data, expected_result, u'Сравниваем данные из акта с шаблоном')

    if migration_dt:
        for service in payment_services:
            # открутки в thirdparty после даты миграции не будут учитываться
            create_completions(context, service, client_id, person_id, contract_id, month_minus2_start_dt)
            create_partner_oebs_completions(context, service, client_id, person_id, contract_id, month_minus2_start_dt)
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
            client_id, contract_id, month_minus2_end_dt, manual_export=need_act
        )

        for service in payment_services:
            create_partner_oebs_completions(context, service, client_id, person_id, contract_id, month_minus2_start_dt)
            create_partner_oebs_completions(context, service, client_id, person_id, contract_id, month_minus1_start_dt,
                                            coef=2)
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
            client_id, contract_id, month_minus1_end_dt, manual_export=need_act
        )

        invoice_data = steps.InvoiceSteps.get_personal_accounts_with_service_codes(client_id)

        amount_payments = Decimal('0')

        if need_act:
            amount_payments = 8 * len(act_services) * TOTAL_AMOUNT

        expected_invoice_data = create_expected_invoice_data(context, contract_id, person_id,
                                                             amount_payments,
                                                             month_migration_minus2_start_dt)
        utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                         u'Сравниваем данные из счета с шаблоном')

        act_data = steps.ActsSteps.get_act_data_by_client(client_id)

        if need_act:
            expected_act_data = [
                steps.CommonData.create_expected_act_data(
                    len(act_services) * TOTAL_AMOUNT, month_migration_minus2_end_dt),
                steps.CommonData.create_expected_act_data(
                    3 * len(act_services) * TOTAL_AMOUNT,
                    month_migration_minus1_end_dt),
                steps.CommonData.create_expected_act_data(
                    len(act_services) * TOTAL_AMOUNT, month_minus2_end_dt),
                steps.CommonData.create_expected_act_data(
                    3 * len(act_services) * TOTAL_AMOUNT,
                    month_minus1_end_dt)
            ]
            expected_result = contains_dicts_with_entries(expected_act_data)
        else:
            expected_result = empty()

        utils.check_that(act_data, expected_result, u'Сравниваем данные из акта с шаблоном')


@pytest.mark.smoke
def test_taxi_agent_report():
    migration_dt, \
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
    month_minus2_start_dt, month_minus2_end_dt, \
    month_minus1_start_dt, month_minus1_end_dt = steps.CommonPartnerSteps.generate_dates_for_taxi_payments_oebs_migration()

    context = TAXI_KZ_CONTEXT
    payment_services, act_services = get_payment_and_act_services_for(context)

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': month_migration_minus2_start_dt}
    )

    for service in payment_services:
        create_completions(context, service, client_id, person_id, contract_id, month_migration_minus2_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_start_dt)

    agent_report_data = steps.CommonPartnerSteps.get_data_from_agent_rep(contract_id)

    act_id = get_act_id(client_id, month_migration_minus2_end_dt)
    invoice_id = get_personal_account_id(client_id)
    expected_agent_report = [
        steps.CommonData.create_expected_agent_report(contract_id, act_id, invoice_id, service, TOTAL_AMOUNT,
                                                      month_migration_minus2_end_dt, context.currency)
        for service in act_services
    ]

    utils.check_that(agent_report_data, contains_dicts_equal_to(expected_agent_report), u'Проверяем отчет агента')

    if migration_dt:
        for service in payment_services:
            create_partner_oebs_completions(context, service, client_id, person_id, contract_id, month_minus2_start_dt)
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus2_start_dt)

        agent_report_data = steps.CommonPartnerSteps.get_data_from_agent_rep(contract_id)

        act_id = get_act_id(client_id, month_minus2_end_dt)
        invoice_id = get_personal_account_id(client_id)
        expected_agent_report.extend([
            steps.CommonData.create_expected_agent_report(contract_id, act_id, invoice_id, service, TOTAL_AMOUNT,
                                                          month_minus2_end_dt, context.currency)
            for service in act_services
        ])

        utils.check_that(agent_report_data, contains_dicts_equal_to(expected_agent_report), u'Проверяем отчет агента')


# ---------------------------------------------------------------
# Utils

def create_completions(context, service, client_id, person_id, contract_id, dt, coef=1):
    # создаем строки в tpt для платежа (amount не важен, в акт идет сумма yandex_reward),
    # рефанда (amount не важен, в акт идет сумма yandex_reward),
    # компенсации (у нее yandex_reward = None и она в акт не должна попасть)

    steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt,
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'yandex_reward': coef * PAYMENT_AMOUNT,
                                           'service_id': service},
                                          {'transaction_type': TransactionType.REFUND,
                                           'yandex_reward': coef * REFUND_AMOUNT,
                                           'service_id': service},
                                          {'transaction_type': TransactionType.REFUND,
                                           'yandex_reward': None, 'paysys_type_cc': PaysysType.YANDEX,
                                           'payment_type': PaymentType.COMPENSATION,
                                           'service_id': service}])

@utils.memoize
def get_product_mapping(service_id, currency):
    with reporter.step(u'Получаем продукт для сервиса: {} и валюты: {}'.format(service_id, currency)):
        query = "select product_id " \
                "from bo.t_partner_product " \
                "where service_id = :service_id " \
                "   and currency_iso_code = :currency " \
                "   and order_type = 'main'"

        params = {'service_id': service_id, 'currency': currency}
        res = db.balance().execute(query, params)
        if len(res) != 1:
            raise Exception('Check product mapping for service_id: {} and currency: {}'.format(service_id, currency))
        return db.balance().execute(query, params)[0]['product_id']


def create_partner_oebs_completions(context, service, client_id, person_id, contract_id, dt, coef=1):
    product_id = get_product_mapping(service, context.currency.iso_code)
    compls_dict = {
        'service_id': service,
        'amount': (PAYMENT_AMOUNT - REFUND_AMOUNT) * coef,
        'product_id': product_id,
        'dt': dt,
        'transaction_dt': dt,
        'currency': context.currency.iso_code,
        'accounting_period': dt,
    }

    steps.CommonPartnerSteps.create_partner_oebs_compl(contract_id, client_id, **compls_dict)


def create_expected_invoice_data(context, contract_id, person_id, amount_payments, invoice_dt):
    data = [
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, amount_payments,
                                                                 dt=invoice_dt,
                                                                 service_code=ServiceCode.AGENT_REWARD)
    ]

    if Services.TAXI_111.id in context.contract_services or Services.TAXI_128.id in context.contract_services:
        data.append(
            steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, Decimal(0),
                                                                     dt=invoice_dt,
                                                                     service_code=ServiceCode.YANDEX_SERVICE))
    if context.firm == Firms.TAXI_13 or context.firm == Firms.TAXI_CORP_KZT_31 and not context.client_intercompany:
        data.append(
            steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, Decimal(0),
                                                                     dt=invoice_dt,
                                                                     service_code=ServiceCode.DEPOSITION,
                                                                     paysys_id=context.additional_paysys.id))

    return data


def get_personal_account_id(client_id):
    with reporter.step(u'Находим id ЛС такси для клиента: {}'.format(client_id)):
        query = "SELECT inv.id " \
                "FROM T_INVOICE inv LEFT JOIN T_EXTPROPS prop ON " \
                "inv.ID = prop.OBJECT_ID AND prop.CLASSNAME='PersonalAccount' AND prop.ATTRNAME='service_code' " \
                "WHERE inv.CLIENT_ID=:client_id AND prop.VALUE_STR=:service_code"

        params = {'client_id': client_id, 'service_code': ServiceCode.AGENT_REWARD}
        return db.balance().execute(query, params)[0]['id']


def get_act_id(client_id, dt):
    return steps.ActsSteps.get_all_act_data(client_id, dt)[0]['id']


def get_payment_and_act_services_for(context):
    payment_services = [service
                        for service in context.contract_services
                        if service in PAYMENT_SERVICES + PAYMENT_SERVICES_WO_ACTS]
    act_services = [service
                    for service in payment_services
                    if service not in PAYMENT_SERVICES_WO_ACTS]
    return payment_services, act_services
