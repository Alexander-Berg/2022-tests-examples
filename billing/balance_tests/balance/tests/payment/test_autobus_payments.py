# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import equal_to, empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import PaymentType, TransactionType, PaysysType, Collateral
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import BUSES_RU_CONTEXT
from btestlib.matchers import has_entries_casted, contains_dicts_with_entries
from simpleapi.common.payment_methods import Cash

COMMISSION_CONTRACT = D('3.3')
COMMISSION_CONTRACT_DS = D('14.3')
COMMISSION_CATEGORY_TRUST = D('420')
DEFAULT_PRICE = simpleapi_defaults.DEFAULT_PRICE
SERVICE_FEE_AMOUNT = D('100.1')
PRICE = D('0.05')

PAYMENT = TransactionType.PAYMENT.name
REFUND = TransactionType.REFUND.name

pytestmark = [reporter.feature(Features.AUTOBUS, Features.PAYMENT, Features.TRUST),
              pytest.mark.usefixtures('switch_to_pg'),
              pytest.mark.tickets('BALANCE-23987', 'BALANCE-28901', 'BALANCE-28757', 'BALANCE-28858', 'BALANCE-28901')]


def create_ids_multiple_products(context, commission_pct, products_number=1, service_fee=None):
    partner_id = steps.SimpleApi.create_partner(context.service)
    service_product_id_list = [steps.SimpleApi.create_service_product(context.service, partner_id)
                               for _ in xrange(products_number)]

    if service_fee:
        service_product_id_list.append(steps.SimpleApi.create_service_product(context.service, partner_id, service_fee=1))

    additional_params = {'partner_commission_pct2': commission_pct} if commission_pct else None

    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            context, client_id=partner_id,
            additional_params=additional_params)

    return partner_id, person_id, contract_id, service_product_id_list


@utils.memoize
def create_client_and_contract_for_payments(context, commission_pct=COMMISSION_CONTRACT):
    partner_id, person_id, contract_id, service_product_id_list = \
        create_ids_multiple_products(context, commission_pct)
    return partner_id, person_id, contract_id, service_product_id_list[0]


def calc_yandex_reward(context, price, commission):
    return round(price / D('100') * commission, 2) or context.min_commission


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@pytest.mark.parametrize(
    'context, payment_type, commission, price, is_cash',
    [
        pytest.param(BUSES_RU_CONTEXT, PAYMENT, COMMISSION_CONTRACT, DEFAULT_PRICE, 0,
                     id='Buses1: Payment with commission in contract Russia',
                     marks=pytest.mark.smoke),
        pytest.param(BUSES_RU_CONTEXT, REFUND, COMMISSION_CONTRACT, DEFAULT_PRICE, 0,
                     id='Buses1: Refund with commission in contract Russia'),
        pytest.param(BUSES_RU_CONTEXT, PAYMENT, None, DEFAULT_PRICE, 0,
                     id='Buses1: Payment w/o commission in contract Russia'),
        # должно в этом случае 0.01
        pytest.param(BUSES_RU_CONTEXT, PAYMENT, COMMISSION_CONTRACT, PRICE, 0,
                     id='Buses1: Payment with yandex_reward < kopeyka Russia'),

        ]
)
def test_autobus_payment(context, payment_type, commission, price, is_cash, switch_to_pg):
    # создаем партнера, плательщика и договор, в котором подставляем комиссию
    partner_id, person_id, contract_id, service_product_id = create_client_and_contract_for_payments(context, commission)

    # если комиссии в договоре нет, то в расчете будет использована трастовская COMMISSION_CATEGORY_TRUST
    # делим на 100, т.к. в траст передается % * 100

    if commission is None or commission == '0':
        commission = COMMISSION_CATEGORY_TRUST / D('100')

    # вычисляем потенциальное вознаграждение
    yandex_reward_initial = max(round(price / D('100') * D(commission), 2), D('0.01'))

    # создаем клиента и payment_method для субагентской схемы
    PAYMENT_METHOD = None
    cash_params = {}
    if is_cash:
        subagency_id = steps.SimpleApi.create_partner(context.service)
        PAYMENT_METHOD = Cash(subagency_id)
        cash_params = {'payment_type': PaymentType.CASH, 'paysys_type_cc': PaysysType.SUBPARTNER,
                       'paysys_partner_id': subagency_id}

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service,
                                             service_product_id,
                                             commission_category=COMMISSION_CATEGORY_TRUST,
                                             price=price,
                                             paymethod=PAYMENT_METHOD,
                                             currency=context.currency)

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    trust_refund_id = None
    if payment_type == 'refund':
        # создаем рефанд
        trust_refund_id, refund_id = steps.SimpleApi.create_refund(context.service, service_order_id, trust_payment_id)
        steps.CommonPartnerSteps.export_payment(refund_id)

    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            payment_id, TransactionType.REFUND if payment_type == 'refund' else TransactionType.PAYMENT)

    expected_template = [steps.SimpleApi.create_expected_tpt_row(context, partner_id, contract_id, person_id, trust_payment_id,
                                                                 payment_id, trust_refund_id,
                                                                 yandex_reward=None if trust_refund_id else yandex_reward_initial,
                                                                 amount=price,
                                                                 **cash_params)]
    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries(expected_template), 'Сравниваем платеж с шаблоном')



@pytest.mark.parametrize("amount_list, action_list, context", [
    pytest.param([DEFAULT_PRICE / 3], ['clear'], BUSES_RU_CONTEXT,
                 id='Buses1: CLEAR'),
    pytest.param([DEFAULT_PRICE / 3, DEFAULT_PRICE / 4], ['clear', 'clear'], BUSES_RU_CONTEXT,
                 id='Buses1: TWO_CLEARS'),
    pytest.param([DEFAULT_PRICE / 3, DEFAULT_PRICE / 4], ['cancel', 'clear'], BUSES_RU_CONTEXT,
                 id='Buses1: CLEAR_AND_CANCEL'),
    pytest.param([0, DEFAULT_PRICE / 4], ['clear', 'clear'], BUSES_RU_CONTEXT,
                 id='Buses1: ZERO_CLEAR_AND_CLEAR'),
])
def test_partial_reversal(amount_list, action_list, context, switch_to_pg):
    amount_list = [utils.dround(amount, 2) for amount in amount_list]

    partner_id, person_id, contract_id, service_product_id_list = \
        create_ids_multiple_products(context, COMMISSION_CONTRACT, len(amount_list))

    commission_category_list = [COMMISSION_CATEGORY_TRUST] * len(amount_list)
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service, service_product_id_list,
                                                       commission_category_list=commission_category_list,
                                                       need_postauthorize=False,
                                                       wait_for_export_from_bs=False,
                                                       currency=context.currency)

    steps.SimpleApi.postauthorize(context.service, trust_payment_id, service_order_id_list,
                                  amounts=amount_list, actions=action_list)

    payment_id = steps.SimpleApi.wait_for_payment(trust_payment_id)

    steps.CommonPartnerSteps.export_payment(payment_id)

    expected_template = [steps.SimpleApi.create_expected_tpt_row(context, partner_id, contract_id,
                                                                 person_id, trust_payment_id, payment_id,
                                                                 yandex_reward=utils.dround(amount * COMMISSION_CONTRACT / D('100'), 2),
                                                                 amount=amount)
                         for amount, action in zip(amount_list, action_list) if action == 'clear' and amount > 0]

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    utils.check_that(payment_data, contains_dicts_with_entries(expected_template), 'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize(
    "action_list, context, service_fee",
    [
        pytest.param(['clear'], BUSES_RU_CONTEXT, 0, id='BUSES1: CLEAR'),
        pytest.param(['cancel'], BUSES_RU_CONTEXT, 0, id='BUSES1: CANCEL'),
        pytest.param(['clear', 'cancel'], BUSES_RU_CONTEXT, 0, id='BUSES1: CLEAR_AND_CANCEL'),
        pytest.param(['clear', 'clear'], BUSES_RU_CONTEXT, 1, id='BUSES1 with service fee: CLEAR_AND_CLEAR'),
        pytest.param(['cancel', 'cancel'], BUSES_RU_CONTEXT, 1, id='BUSES1 with service fee: CANCEL_AND_CANCEL'),
        pytest.param(['cancel', 'clear'], BUSES_RU_CONTEXT, 1, id='BUSES1 with service fee: CANCEL_AND_CLEAR'),
    ])
def test_full_reversal(action_list, context, service_fee, switch_to_pg):
    if service_fee:
        if action_list == ['cancel', 'clear']:
            amount_list = [0, SERVICE_FEE_AMOUNT]
        else:
            amount_list = [0, 0]
    else:
        amount_list = [0] * len(action_list)

    partner_id, person_id, contract_id, service_product_id_list = \
        create_ids_multiple_products(context,
                                     COMMISSION_CONTRACT,
                                     1 if service_fee else len(action_list),
                                     service_fee=service_fee)

    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service,
                                                       service_product_id_list,
                                                       prices_list=[DEFAULT_PRICE, SERVICE_FEE_AMOUNT],
                                                       need_postauthorize=False,
                                                       wait_for_export_from_bs=False,
                                                       currency=context.currency)

    steps.SimpleApi.postauthorize(context.service, trust_payment_id, service_order_id_list,
                                  amounts=amount_list, actions=action_list)

    payment_id = steps.SimpleApi.wait_for_payment(trust_payment_id)

    response = steps.CommonPartnerSteps.export_payment(payment_id)

    if not (service_fee and action_list == ['cancel', 'clear']):
        expected_output = "TrustPayment({}) skipped: payment has been completely cancelled".format(payment_id)
        utils.check_that(response['output'], equal_to(expected_output), u'Проверяем, что платеж пропущен')

        payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
        utils.check_that(payment_data, empty(), u'Проверяем, что транзакции не созданы')
    else:
        payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

        tech_client_id, tech_person_id, tech_contract_id = \
            steps.CommonPartnerSteps.get_active_tech_ids(BUSES_RU_CONTEXT.service)

        expected_service_fee = steps.SimpleApi.create_expected_tpt_row(BUSES_RU_CONTEXT,
                                                                       tech_client_id, tech_contract_id,
                                                                       tech_person_id, trust_payment_id,
                                                                       payment_id,
                                                                       internal=1,
                                                                       yandex_reward=SERVICE_FEE_AMOUNT,
                                                                       amount=SERVICE_FEE_AMOUNT)

        expected_service_fee.update({
            'transaction_type': PAYMENT
        })

        utils.check_that(payment_data[0], has_entries_casted(expected_service_fee), 'Сравниваем платеж с шаблоном')


# Тесты платежей с сервисным сбором
def test_autobus_payment_with_service_fee():
    partner_id, person_id, contract_id, service_product_id_list = \
        create_ids_multiple_products(BUSES_RU_CONTEXT, commission_pct=COMMISSION_CONTRACT, service_fee=1)
    tech_client_id, tech_person_id, tech_contract_id = \
        steps.CommonPartnerSteps.get_active_tech_ids(BUSES_RU_CONTEXT.service)

    # создаем платеж
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(BUSES_RU_CONTEXT.service,
                                                       service_product_id_list,
                                                       prices_list=[DEFAULT_PRICE, SERVICE_FEE_AMOUNT],
                                                       currency=BUSES_RU_CONTEXT.currency)
    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            payment_id, TransactionType.PAYMENT)

    # сравниваем платеж с шаблоном
    expected_payment = steps.SimpleApi.create_expected_tpt_row(BUSES_RU_CONTEXT,
                                                               partner_id, contract_id, person_id, trust_payment_id,
                                                               payment_id,
                                                               yandex_reward=max(round(DEFAULT_PRICE / D('100')
                                                                                       * COMMISSION_CONTRACT, 2), D('0.01')),
                                                               amount=DEFAULT_PRICE)

    expected_service_fee = steps.SimpleApi.create_expected_tpt_row(BUSES_RU_CONTEXT,
                                                                   tech_client_id, tech_contract_id,
                                                                   tech_person_id, trust_payment_id,
                                                                   payment_id,
                                                                   internal=1,
                                                                   yandex_reward=SERVICE_FEE_AMOUNT,
                                                                   amount=SERVICE_FEE_AMOUNT)

    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment, expected_service_fee],
                                                                same_length=False), 'Сравниваем платеж с шаблоном')


def test_autobus_payment_service_fee_refund_with_2_rows():
    partner_id, person_id, contract_id, service_product_id_list = \
        create_ids_multiple_products(BUSES_RU_CONTEXT, commission_pct=COMMISSION_CONTRACT, service_fee=1)
    tech_client_id, tech_person_id, tech_contract_id = \
        steps.CommonPartnerSteps.get_active_tech_ids(BUSES_RU_CONTEXT.service)

    # создаем платеж
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(BUSES_RU_CONTEXT.service,
                                                       service_product_id_list,
                                                       prices_list=[DEFAULT_PRICE, SERVICE_FEE_AMOUNT],
                                                       currency=BUSES_RU_CONTEXT.currency)

    # делаем возврат
    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(BUSES_RU_CONTEXT.service,
                                                                         service_order_id_list,
                                                                         trust_payment_id,
                                                                         [DEFAULT_PRICE, SERVICE_FEE_AMOUNT])

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(refund_id)

    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            payment_id, TransactionType.REFUND)

    # сравниваем платеж с шаблоном
    expected_payment = steps.SimpleApi.create_expected_tpt_row(BUSES_RU_CONTEXT,
                                                               partner_id, contract_id, person_id, trust_payment_id,
                                                               payment_id, trust_refund_id=trust_refund_id,
                                                               yandex_reward=max(round(DEFAULT_PRICE / D('100')
                                                                                       * COMMISSION_CONTRACT, 2), D('0.01')),
                                                               amount=DEFAULT_PRICE)

    expected_payment.update({
        'transaction_type': REFUND,
        'yandex_reward': None
    })

    expected_service_fee = steps.SimpleApi.create_expected_tpt_row(BUSES_RU_CONTEXT,
                                                                   tech_client_id, tech_contract_id,
                                                                   tech_person_id, trust_payment_id,
                                                                   payment_id,  trust_refund_id=trust_refund_id,
                                                                   internal=1,
                                                                   yandex_reward=SERVICE_FEE_AMOUNT,
                                                                   amount=SERVICE_FEE_AMOUNT)

    expected_service_fee.update({
        'transaction_type': REFUND
    })

    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment, expected_service_fee],
                                                                same_length=False), 'Сравниваем платеж с шаблоном')


def test_autobus_payment_service_fee_refund_with_1_row():
    partner_id, person_id, contract_id, service_product_id_list = \
        create_ids_multiple_products(BUSES_RU_CONTEXT, commission_pct=COMMISSION_CONTRACT, service_fee=1)

    # создаем платеж
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(BUSES_RU_CONTEXT.service,
                                                       service_product_id_list,
                                                       prices_list=[DEFAULT_PRICE, SERVICE_FEE_AMOUNT],
                                                       currency=BUSES_RU_CONTEXT.currency)

    # делаем возврат
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(BUSES_RU_CONTEXT.service,
                                                               service_order_id_list[0],
                                                               trust_payment_id)

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(refund_id)

    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            payment_id, TransactionType.REFUND)

    # сравниваем платеж с шаблоном
    expected_payment = steps.SimpleApi.create_expected_tpt_row(BUSES_RU_CONTEXT,
                                                               partner_id, contract_id, person_id, trust_payment_id,
                                                               payment_id, trust_refund_id=trust_refund_id,
                                                               yandex_reward=max(round(DEFAULT_PRICE / D('100')
                                                                                       * COMMISSION_CONTRACT, 2), D('0.01')),
                                                               amount=DEFAULT_PRICE)

    expected_payment.update({
        'transaction_type': REFUND,
        'yandex_reward': None
    })

    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment],
                                                                same_length=False), 'Сравниваем платеж с шаблоном')
