# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib.data.partner_contexts import *
from btestlib.matchers import has_entries_casted
from btestlib.data.simpleapi_defaults import DEFAULT_PRICE
import btestlib.reporter as reporter
from balance.features import Features

pytestmark = [reporter.feature(Features.TRUST)]

CONTEXTS = [
    pytest.mark.smoke(BUSES_RU_CONTEXT),
    GAS_STATION_RU_CONTEXT,
    # TAXI_RU_CONTEXT, # BALANCE-39255 - отключено проведение платежей
    TELEMEDICINE_CONTEXT,
    STATION_PAYMENTS_CONTEXT,
    SUPERCHECK_CONTEXT,
    # TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT,
    # TAXI_ZA_USD_CONTEXT, # https://st.yandex-team.ru/TRUSTDUTY-803 - терминалов нет, не проверялось
    # TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT
    # TAXI_YANDEX_GO_SRL_CONTEXT
]

CONTEXTS_REFUND = [
    GAS_STATION_RU_CONTEXT,
    # TAXI_RU_CONTEXT,  # BALANCE-39255 - отключено проведение платежей
    TELEMEDICINE_CONTEXT,
    # TAXI_ISRAEL_CONTEXT,  # BALANCE-39255 - отключено проведение платежей
    # TAXI_AZARBAYCAN_CONTEXT, # BALANCE-39255 - отключено проведение платежей
    # TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT, # BALANCE-39255 - отключено проведение платежей
    SUPERCHECK_CONTEXT,
    # TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT,
    # TAXI_ZA_USD_CONTEXT, # https://st.yandex-team.ru/TRUSTDUTY-803 терминалов нет, не проверялось
    # TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT
    # TAXI_YANDEX_GO_SRL_CONTEXT
]

ORDER_DT = utils.Date.moscow_offset_dt() - relativedelta(hours=10)
CONTRACT_START_DT = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(days=1)
TODAY = utils.Date.nullify_time_of_date(datetime.now())


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@pytest.mark.parametrize('context', CONTEXTS, ids=lambda c: c.name)
@pytest.mark.parametrize(
    'partner_integration_params',
    [
        pytest.param(steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT,
                     id='PARTNER_INTEGRATION'),
        pytest.param(None, id='WO_PARTNER_INTEGRATION'),
    ])
def test_compensation(context, partner_integration_params, switch_to_trust):
    if partner_integration_params and context.partner_integration is None:
        pytest.skip('Partner integration not configured for context, test skipped')
    switch_to_trust(service=context.service)
    partner_id = steps.SimpleApi.create_partner(context.service)
    service_product_id = steps.SimpleApi.create_service_product(context.service, partner_id)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            context, client_id=partner_id, additional_params={'start_dt': CONTRACT_START_DT},
            partner_integration_params=partner_integration_params)

    # создаем компенсацию
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_compensation(context.service, service_product_id,
                                            currency=context.payment_currency, export_payment=True,
                                            order_dt=ORDER_DT if context.service == Services.TAXI else None)

    amount = DEFAULT_PRICE
    if context.currency != context.payment_currency:
        currency_rate = steps.CurrencySteps.get_currency_rate(
            TODAY, context.currency.char_code,
            context.payment_currency.char_code, context.currency_rate_src.id)
        amount = utils.dround(amount / currency_rate, 2)

    # проверяем платеж
    payment_data = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.PAYMENT)[0]

    expected_template = steps.SimpleApi.create_expected_tpt_row_compensation(context, partner_id, contract_id,
                                                                             person_id, trust_payment_id, payment_id,
                                                                             amount=amount)
    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, has_entries_casted(expected_template),
                     'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize('context', CONTEXTS_REFUND, ids=lambda c: c.name)
@pytest.mark.parametrize(
    'partner_integration_params',
    [
        pytest.param(steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT,
                     id='PARTNER_INTEGRATION'),
        pytest.param(None, id='WO_PARTNER_INTEGRATION'),
    ])
def test_compensation_refund(context, partner_integration_params, switch_to_trust):
    if partner_integration_params and context.partner_integration is None:
        pytest.skip('Partner integration not configured for context, test skipped')
    switch_to_trust(service=context.service)
    partner_id = steps.SimpleApi.create_partner(context.service)
    service_product_id = steps.SimpleApi.create_service_product(context.service, partner_id)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            context, client_id=partner_id, additional_params={'start_dt': CONTRACT_START_DT},
            partner_integration_params=partner_integration_params)

    # создаем компенсацию
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_compensation(context.service, service_product_id,
                                            currency=context.payment_currency, export_payment=True,
                                            order_dt=ORDER_DT if context.service == Services.TAXI else None)
    trust_refund_id, refund_id = \
        steps.SimpleApi.create_refund(context.service, service_order_id, trust_payment_id, export_payment=True)

    amount = DEFAULT_PRICE
    if context.currency != context.payment_currency:
        currency_rate = steps.CurrencySteps.get_currency_rate(
            TODAY, context.currency.char_code,
            context.payment_currency.char_code, context.currency_rate_src.id)
        amount = utils.dround(amount / currency_rate, 2)

    # проверяем платеж
    payment_data = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)[0]

    expected_template = steps.SimpleApi.create_expected_tpt_row_compensation(context, partner_id, contract_id,
                                                                             person_id, trust_payment_id, payment_id,
                                                                             trust_refund_id=trust_refund_id,
                                                                             amount_fee=None, amount=amount)
    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, has_entries_casted(expected_template),
                     'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize('context, uid, login', [
                        (CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED, 436363578, 'yb-atst-user-5'),
                        (CORP_TAXI_KZ_CONTEXT_SPENDABLE_MIGRATED, 675282951, 'yb-atst-user-32')
                        ], ids=lambda c, u, l: c.name)
def test_compensation_with_user_specified(context, uid, login, switch_to_trust, get_free_user):
    switch_to_trust(service=context.service)
    # user = User(uid, login, None)
    user = get_free_user()

    taxi_client_id, service_product_id = steps.SimpleApi.create_partner_and_product(context.service)
    taxi_person_partner_id = steps.PersonSteps.create(taxi_client_id, context.person_type.code, {'is-partner': '1'})

    # создаем клиента и плательщика для корпоративного клиента
    corp_client_id = steps.ClientSteps.create()

    # привязываем логин к корпоративному клиенту
    steps.UserSteps.link_user_and_client(user, corp_client_id)

    # расходный с таксопарком
    _, _, taxi_contract_spendable_id, _ = steps.ContractSteps.create_partner_contract(
            context,
            client_id=taxi_client_id, person_id=taxi_person_partner_id)

    # создаем платеж компенсацию
    _, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_compensation(context.service, service_product_id, user=user,
                                            order_dt=utils.Date.moscow_offset_dt(),
                                            currency=context.currency)

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    additional_params = {'client_id': corp_client_id, 'client_amount': Decimal('0')}
    # формируем шаблон для сравнения
    expected_template = steps.SimpleApi.create_expected_tpt_row_compensation(context, taxi_client_id, taxi_contract_spendable_id,
                                                                             taxi_person_partner_id, trust_payment_id, payment_id,
                                                                             **additional_params)

    # проверяем платеж
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)[0]

    utils.check_that(payment_data, has_entries_casted(expected_template), 'Сравниваем платеж компенсацию с шаблоном')
