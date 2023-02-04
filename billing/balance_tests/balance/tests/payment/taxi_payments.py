# -*- coding: utf-8 -*-

__author__ = 'a-vasin'

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import equal_to, empty
from itertools import chain

import btestlib.utils
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import *
from btestlib.matchers import contains_dicts_with_entries
from btestlib.data.simpleapi_defaults import DEFAULT_PRICE
from simpleapi.common.payment_methods import YandexAccountWithdraw, YandexAccountTopup


# !!!!!!!!! BALANCE-39255 - отключено проведение платежей !!!!!!!!!!


pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT, Features.TAXI),
    pytest.mark.tickets('BALANCE-21765'),
]

CONTEXTS = [
    # запускаем только тесты для РФ, чтобы проверить интеграционно, остальные фирмы и валюты проверяются в тестах без траста
    # tests/payment/test_payments_wo_trust.py

    (TAXI_RU_CONTEXT, Services.TAXI),
    (TAXI_RU_CONTEXT, Services.UBER),
    (TAXI_RU_CONTEXT, Services.TAXI_VEZET),
    (TAXI_RU_CONTEXT, Services.TAXI_RUTAXI),

    # (TAXI_RU_CONTEXT, Services.UBER_ROAMING), # отключаем UBER_ROAMING https://wiki.yandex-team.ru/users/atkaya/obsoleteservices/

    # (TAXI_BV_GEO_USD_CONTEXT, Services.TAXI),
    #
    # (TAXI_BV_LAT_EUR_CONTEXT, Services.TAXI),
    #
    # (TAXI_UBER_BV_AZN_USD_CONTEXT, Services.UBER),
    # (TAXI_UBER_BV_AZN_USD_CONTEXT, Services.UBER_ROAMING),
    #
    # (TAXI_UBER_BV_BY_BYN_CONTEXT, Services.TAXI),
    # (TAXI_UBER_BV_BY_BYN_CONTEXT, Services.UBER),
    # (TAXI_UBER_BV_BY_BYN_CONTEXT, Services.UBER_ROAMING),
    #
    # (TAXI_KZ_CONTEXT, Services.TAXI),
    # (TAXI_KZ_CONTEXT, Services.UBER),
    # (TAXI_KZ_CONTEXT, Services.UBER_ROAMING),
    #
    # (TAXI_ARM_CONTEXT, Services.TAXI),
    #
    # (TAXI_ISRAEL_CONTEXT, Services.TAXI),
    #
    # (TAXI_AZARBAYCAN_CONTEXT, Services.TAXI),
    # (TAXI_AZARBAYCAN_CONTEXT, Services.UBER),
    # (TAXI_GHANA_USD_CONTEXT, Services.TAXI),
    # (TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT, Services.TAXI),  # терминалы в https://st.yandex-team.ru/TRUSTDUTY-637
    # (TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, Services.TAXI),
    # (TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, Services.UBER),
    # (TAXI_ZA_USD_CONTEXT, Services.TAXI),  # терминалов в тестинге нет! https://st.yandex-team.ru/TRUSTDUTY-803
    # (TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT, Services.TAXI)
    # (TAXI_YANDEX_GO_SRL_CONTEXT, Services.TAXI)
]

CONTRACT_DT = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(days=1)
ORDER_DT = utils.Date.moscow_offset_dt() - relativedelta(hours=10)
DEFAULT_COMMISSION_PCT = Decimal('5.1')


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@pytest.mark.parametrize('context, service', CONTEXTS, ids=lambda c, s: c.name + '_' + Services.name(s))
def test_taxi_payment(context, service, switch_to_trust):
    switch_to_trust(service=service)

    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context, service)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(service, service_product_id[0], currency=context.payment_currency,
                                             order_dt=ORDER_DT)

    steps.CommonPartnerSteps.export_payment(payment_id)

    expected_data = create_expected_data(context, service, client_id, person_id, contract_id, payment_id,
                                         trust_payment_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize('context, service', CONTEXTS, ids=lambda c, s: c.name + '_' + Services.name(s))
def test_taxi_refund(context, service, switch_to_trust):
    switch_to_trust(service=service)

    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context, service)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(service, service_product_id[0], currency=context.payment_currency,
                                             order_dt=ORDER_DT)
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(service, service_order_id, trust_payment_id)

    steps.CommonPartnerSteps.export_payment(refund_id)

    expected_data = create_expected_data(context, service, client_id, person_id, contract_id, payment_id,
                                         trust_payment_id, trust_refund_id=trust_refund_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


def test_taxi_new_billing_client(switch_to_trust):
    context, service = TAXI_RU_CONTEXT, Services.TAXI
    switch_to_trust(service=service)

    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context, service)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(service, service_product_id[0], currency=context.payment_currency,
                                             order_dt=ORDER_DT)
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(service, service_order_id, trust_payment_id)
    steps.CommonPartnerSteps.export_payment(payment_id)
    steps.CommonPartnerSteps.export_payment(refund_id)
    # проверим, что до добавления пользователя в смигрировавшие флаг не устанавливается
    expected_data = list(chain.from_iterable([
        create_expected_data(context, service, client_id, person_id, contract_id, payment_id,
                             trust_payment_id, additional_params={'internal': None}),
        create_expected_data(context, service, client_id, person_id, contract_id, payment_id,
                             trust_payment_id, trust_refund_id=trust_refund_id, additional_params={'internal': None})
    ]))
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, transaction_type=None)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')
    # добавим клиента в смигрировавшие но в будущем
    steps.CommonPartnerSteps.migrate_client('taxi', 'Client', client_id, datetime(2030, 1, 1))
    # переэкспортим, проверим что не изменилось
    steps.CommonPartnerSteps.export_payment(payment_id)
    steps.CommonPartnerSteps.export_payment(refund_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, transaction_type=None)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')
    # перенесем дату миграции клиента до postauth_dt
    steps.CommonPartnerSteps.cancel_migrate_client('taxi', 'Client', client_id)
    steps.CommonPartnerSteps.migrate_client('taxi', 'Client', client_id, datetime(2020, 1, 1))
    # переэкспортим, проверим что не изменилось
    steps.CommonPartnerSteps.export_payment(payment_id)
    steps.CommonPartnerSteps.export_payment(refund_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, transaction_type=None)
    for expected_row in expected_data:
        expected_row['internal'] = 1

    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')
    steps.CommonPartnerSteps.cancel_migrate_client('taxi', 'Client', client_id)


@reporter.feature(Features.PLUS, Features.TRUST)
@pytest.mark.plus_2_0
@pytest.mark.smoke
@pytest.mark.parametrize('context, service', [
    (TAXI_RU_CONTEXT, Services.TAXI),
    (TAXI_RU_CONTEXT, Services.UBER),
    (TAXI_UBER_BV_BY_BYN_CONTEXT, Services.TAXI),
    (TAXI_UBER_BV_BYN_BY_BYN_CONTEXT, Services.TAXI),
    (TAXI_KZ_CONTEXT, Services.TAXI)
], ids=lambda c, s: c.name + '_' + Services.name(s))
def test_account_wallet_payment(context, service, switch_to_trust):
    plus_part_key = dict(currency=context.currency.iso_code)
    plus_part_config = steps.ConfigSteps.get_plus_part_config(plus_part_key)
    switch_to_trust(service=service)
    topup_amount, withdraw_amount = Decimal('200'), Decimal('100')
    user = simpleapi_defaults.USER_NEW_API
    account = steps.payments_api_steps.Account.create(service, user)
    _, _, _, service_product_id = create_client_and_contract(context, service)
    topup_payment_id, topup_trust_payment_id, _ = create_topup_payment(context, service, topup_amount, account, user,
                                                                       service_product_id[0])
    withdraw_payment_id, withdraw_trust_payment_id, _ = create_withdraw_payment(context, service, withdraw_amount,
                                                                                account, user, service_product_id[0])

    topup_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(topup_payment_id)
    withdraw_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(withdraw_payment_id)
    if plus_part_config.get('skip_plus_2_0_payment_processing'):
        utils.check_that(topup_payment_data, empty(), u'Проверяем, что topup платежи не созданы')
        utils.check_that(withdraw_payment_data, empty(), u'Проверяем, что withdraw платежи не созданы')
        return

    plus_contract = steps.CommonPartnerSteps.get_active_plus_ids_by_service(context.service, plus_part_key)
    expected_topup_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, topup_payment_id, topup_trust_payment_id,
        additional_params={'amount': topup_amount,
                           'internal': 1, 'yandex_reward': None, 'payment_type': PaymentType.YANDEX_ACCOUNT_TOPUP})
    expected_withdraw_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, withdraw_payment_id, withdraw_trust_payment_id,
        additional_params={'amount': withdraw_amount,
                           'internal': 1, 'yandex_reward': None, 'payment_type': PaymentType.YANDEX_ACCOUNT_WITHDRAW})

    utils.check_that(topup_payment_data, contains_dicts_with_entries(expected_topup_data),
                     u'Сравниваем пополнение кошелька с шаблоном')
    utils.check_that(withdraw_payment_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


@reporter.feature(Features.PLUS, Features.TRUST)
@pytest.mark.plus_2_0
@pytest.mark.smoke
@pytest.mark.parametrize('context, service', [
    (TAXI_RU_CONTEXT, Services.TAXI),
    (TAXI_RU_CONTEXT, Services.UBER),
    (TAXI_UBER_BV_BY_BYN_CONTEXT, Services.TAXI),
    (TAXI_UBER_BV_BYN_BY_BYN_CONTEXT, Services.TAXI),
    (TAXI_KZ_CONTEXT, Services.TAXI)
], ids=lambda c, s: c.name + '_' + Services.name(s))
def test_account_wallet_refund(context, service, switch_to_trust):
    plus_part_key = dict(currency=context.currency.iso_code)
    plus_part_config = steps.ConfigSteps.get_plus_part_config(plus_part_key)
    switch_to_trust(service=service)
    topup_amount, withdraw_amount = Decimal('200'), Decimal('100')
    user = simpleapi_defaults.USER_NEW_API
    account = steps.payments_api_steps.Account.create(service, user)
    _, _, _, service_product_id = create_client_and_contract(context, service)
    topup_payment_id, topup_trust_payment_id, topup_purchase_token = create_topup_payment(
        context, service, topup_amount, account, user, service_product_id[0])
    withdraw_payment_id, withdraw_trust_payment_id, withdraw_purchase_token = create_withdraw_payment(
        context, service, withdraw_amount, account, user, service_product_id[0])

    topup_trust_refund_id, topup_refund_id = steps.SimpleNewApi.create_account_refund(service, topup_purchase_token)
    withdraw_trust_refund_id, withdraw_refund_id = steps.SimpleNewApi.create_account_refund(service, withdraw_purchase_token)

    steps.CommonPartnerSteps.export_payment(topup_refund_id)
    steps.CommonPartnerSteps.export_payment(withdraw_refund_id)
    topup_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(topup_payment_id,
                                                                                          TransactionType.REFUND)
    withdraw_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(withdraw_payment_id,
                                                                                             TransactionType.REFUND)
    if plus_part_config.get('skip_plus_2_0_payment_processing'):
        utils.check_that(topup_refund_data, empty(), u'Проверяем, что topup рефанды не созданы')
        utils.check_that(withdraw_refund_data, empty(), u'Проверяем, что withdraw рефанды не созданы')
        return

    plus_contract = steps.CommonPartnerSteps.get_active_plus_ids_by_service(context.service, plus_part_key)
    expected_topup_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, topup_payment_id, topup_trust_payment_id,
        trust_refund_id=topup_trust_refund_id,
        additional_params={'amount': topup_amount,
                           'internal': 1, 'yandex_reward': None, 'payment_type': PaymentType.YANDEX_ACCOUNT_TOPUP})
    expected_withdraw_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, withdraw_payment_id, withdraw_trust_payment_id,
        trust_refund_id=withdraw_trust_refund_id,
        additional_params={'amount': withdraw_amount,
                           'internal': 1, 'yandex_reward': None, 'payment_type': PaymentType.YANDEX_ACCOUNT_WITHDRAW})

    utils.check_that(topup_refund_data, contains_dicts_with_entries(expected_topup_data),
                     u'Сравниваем пополнение кошелька с шаблоном')
    utils.check_that(withdraw_refund_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


@pytest.mark.parametrize("amount_list, action_list, service", [
    pytest.param([DEFAULT_PRICE / 3], ['clear'], Services.TAXI, id='CLEAR'),
    pytest.param([DEFAULT_PRICE / 3, DEFAULT_PRICE / 4], ['clear', 'clear'], Services.TAXI, id='TWO_CLEARS'),
    pytest.param([DEFAULT_PRICE / 3, DEFAULT_PRICE / 4], ['cancel', 'clear'], Services.TAXI, id='CLEAR_AND_CANCEL'),
    pytest.param([0, DEFAULT_PRICE / 4], ['clear', 'clear'], Services.TAXI, id='ZERO_CLEAR_AND_CLEAR'),
])
def test_taxi_partial_reversal(amount_list, action_list, service, switch_to_trust):
    switch_to_trust(service=service)
    context = TAXI_RU_CONTEXT
    amount_list = [utils.dround(amount, 2) for amount in amount_list]

    client_id, person_id, contract_id, service_product_id_list = create_client_and_contract(context, service, len(amount_list))

    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(service, service_product_id_list,
                                                       order_dt=ORDER_DT,
                                                       need_postauthorize=False,
                                                       wait_for_export_from_bs=False,
                                                       currency=context.payment_currency)

    steps.SimpleApi.postauthorize(service, trust_payment_id, service_order_id_list,
                                  amounts=amount_list, actions=action_list)

    payment_id = steps.SimpleApi.wait_for_payment(trust_payment_id)

    steps.CommonPartnerSteps.export_payment(payment_id)

    expected_template = [steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                                 person_id, trust_payment_id, payment_id,
                                                                 yandex_reward=get_yandex_reward(context, amount),
                                                                 amount=amount, service_id=service.id)
                         for amount, action in zip(amount_list, action_list) if action == 'clear' and amount > 0]

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    utils.check_that(payment_data, contains_dicts_with_entries(expected_template), 'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize("action_list, service",
                         [
                             pytest.param(['clear'], Services.TAXI, id='CLEAR'),
                             pytest.param(['cancel'], Services.TAXI, id='CANCEL'),
                             pytest.param(['clear', 'cancel'], Services.TAXI, id='CLEAR_AND_CANCEL'),
                         ])
def test_taxi_full_reversal(action_list, service, switch_to_trust):
    switch_to_trust(service=service)
    context = TAXI_RU_CONTEXT
    amount_list = [0] * len(action_list)

    client_id, person_id, contract_id, service_product_id_list = create_client_and_contract(context, service, len(amount_list))

    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(service, service_product_id_list, need_postauthorize=False,
                                                       order_dt=ORDER_DT,
                                                       wait_for_export_from_bs=False,
                                                       currency=context.payment_currency)

    steps.SimpleApi.postauthorize(service, trust_payment_id, service_order_id_list,
                                  amounts=amount_list, actions=action_list)

    payment_id = steps.SimpleApi.wait_for_payment(trust_payment_id)

    response = steps.CommonPartnerSteps.export_payment(payment_id)

    expected_output = "TrustPayment({}) skipped: payment has been completely cancelled".format(payment_id)
    utils.check_that(response['output'], equal_to(expected_output), u'Проверяем, что платеж пропущен')

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data, empty(), u'Проверяем, что транзакции не созданы')

# ------------------------------------------------------------
# Utils


def create_client_and_contract(context, service, products_number=1):
    client_id = steps.SimpleApi.create_partner(service)
    service_product_id_list = [steps.SimpleApi.create_service_product(service, client_id)
                               for _ in xrange(products_number)]
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, client_id=client_id,
                                                                               additional_params={
                                                                                   'start_dt': CONTRACT_DT,
                                                                                   'partner_commission_pct2': DEFAULT_COMMISSION_PCT
                                                                               })
    return client_id, person_id, contract_id, service_product_id_list


def create_expected_data(context, service, partner_id, person_id, contract_id, payment_id,
                         trust_payment_id, trust_refund_id=None, additional_params=None, client_id=None):
    amount, yandex_reward = None, None
    additional_params = additional_params or {}
    if 'amount' not in additional_params:
        currency_rate = steps.CurrencySteps.get_currency_rate(ORDER_DT, context.currency.char_code,
                                                              context.payment_currency.char_code,
                                                              context.currency_rate_src.id)
        amount = utils.dround(simpleapi_defaults.DEFAULT_PRICE / currency_rate, 2)
        yandex_reward = get_yandex_reward(context, amount, is_refund=trust_refund_id)

    additional_params = additional_params or {}
    additional_params = dict({
        'client_id': client_id,
        'amount': amount,
        'yandex_reward': yandex_reward,
        'service_id': service.id
    }, **additional_params)

    expected_data = steps.SimpleApi.create_expected_tpt_row(context, partner_id, contract_id, person_id,
                                                            trust_payment_id, payment_id,
                                                            trust_refund_id=trust_refund_id,
                                                            **additional_params)
    return [expected_data]


def create_topup_payment(context, service, amount, account, user, service_product_id):
    topup_payment_method = YandexAccountTopup(account['payment_method_id'])
    topup_trust_payment_id, topup_payment_id, topup_purchase_token = \
        steps.SimpleNewApi.create_topup_payment(
            service, service_product_id, user=user,
            paymethod=topup_payment_method,
            currency=context.payment_currency.iso_code,
            amount=amount
        )

    steps.CommonPartnerSteps.export_payment(topup_payment_id)
    return topup_payment_id, topup_trust_payment_id, topup_purchase_token


def create_withdraw_payment(context, service, amount, account, user, service_product_id):
    withdraw_payment_method = YandexAccountWithdraw(account['payment_method_id'])
    withdraw_service_order_id, withdraw_trust_payment_id, withdraw_purchase_token, withdraw_payment_id = \
        steps.SimpleApi.create_trust_payment(service, service_product_id, currency=context.payment_currency,
                                             order_dt=ORDER_DT, paymethod=withdraw_payment_method,
                                             price=amount, user=user)
    steps.CommonPartnerSteps.export_payment(withdraw_payment_id)
    return withdraw_payment_id, withdraw_trust_payment_id, withdraw_purchase_token


def get_yandex_reward(context, amount, is_refund=False):
    yandex_reward = max(context.min_commission,
                        round(Decimal(amount) * DEFAULT_COMMISSION_PCT / Decimal('100'), context.precision))
    if context.firm in (Firms.TAXI_13, ):
        yandex_reward = Decimal('0')

    # BALANCE-33907: С 1 мая включаем добавление НДС на АВ в Такси Казахстане и Азербайджане
    if yandex_reward and context.firm in (Firms.UBER_AZ_116, Firms.TAXI_KAZ_24, Firms.TAXI_CORP_KZT_31)\
            and datetime.now() >= datetime(2020, 5, 1):
        yandex_reward = btestlib.utils.get_sum_with_nds(yandex_reward, context.nds.pct_on_dt(datetime.now()))

    if is_refund:
        yandex_reward = None
    return yandex_reward
