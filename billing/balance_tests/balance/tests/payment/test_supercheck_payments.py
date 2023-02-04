# -*- coding: utf-8 -*-

__author__ = 'mindlin'

from decimal import Decimal as D

import pytest
from hamcrest import equal_to, empty, none
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import SUPERCHECK_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

COMMISSION_CATEGORY_TRUST = D('420')
DEFAULT_PRICE = simpleapi_defaults.DEFAULT_PRICE
PRICE = D('0.05')
PAYOUT_READY_DT = utils.Date.first_day_of_month() + relativedelta(months=1)

PAYMENT = TransactionType.PAYMENT.name
REFUND = TransactionType.REFUND.name

pytestmark = [reporter.feature(Features.SUPERCHECK, Features.PAYMENT, Features.TRUST),
              pytest.mark.tickets('BALANCE-32085')]

parametrize_integration_params = pytest.mark.parametrize(
    'partner_integration_params',
    [
        pytest.param(steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT,
                     id='PARTNER_INTEGRATION'),
        pytest.param(None,
                     id='WO_PARTNER_INTEGRATION')
    ])


def create_client_and_contract_for_payments(context, partner_integration_params):
    partner_id, service_product_id = steps.SimpleApi.create_partner_and_product(context.service)

    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            context, client_id=partner_id, partner_integration_params=partner_integration_params
    )

    return partner_id, person_id, contract_id, service_product_id


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@pytest.mark.parametrize('payment_type, price, commission_category',
                         [
                             pytest.mark.smoke((PAYMENT, DEFAULT_PRICE, COMMISSION_CATEGORY_TRUST)),
                             (REFUND, DEFAULT_PRICE, COMMISSION_CATEGORY_TRUST),
                             (PAYMENT, PRICE, COMMISSION_CATEGORY_TRUST),
                             (PAYMENT, DEFAULT_PRICE, 0)
                         ],
                         ids=[
                             'Supercheck: Payment with commision category Russia',
                             'Supercheck: Refund with commision category Russia',
                             'Supercheck: Payment with yandex_reward < kopeyka Russia',
                             'Supercheck: Payment with COMMISSION_CATEGORY=0'
                         ]
                         )
@parametrize_integration_params
def test_supercheck_payment(payment_type, price, commission_category, partner_integration_params):
    context = SUPERCHECK_CONTEXT

    # создаем партнера, плательщика и договор, в котором подставляем комиссию
    partner_id, person_id, contract_id, service_product_id = \
        create_client_and_contract_for_payments(context, partner_integration_params)

    commission = commission_category / D('100')

    # вычисляем потенциальное вознаграждение
    yandex_reward_initial = round(price / D('100') * D(commission), 2)

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service,
                                             service_product_id,
                                             commission_category=commission_category,
                                             price=price,
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
                                                                 yandex_reward=yandex_reward_initial,
                                                                 amount=price)]
    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries(expected_template), 'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize(
    "action_list",
    [
        pytest.param(['clear'], id='SUPERCHECK: CLEAR'),
        pytest.param(['cancel'], id='SUPERCHECK: CANCEL')
    ])
@parametrize_integration_params
def test_full_reversal(action_list, partner_integration_params):
    context = SUPERCHECK_CONTEXT

    amount_list = [0]

    partner_id, person_id, contract_id, service_product_id = \
        create_client_and_contract_for_payments(context, partner_integration_params)

    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service, [service_product_id],
                                                       need_postauthorize=False,
                                                       wait_for_export_from_bs=False,
                                                       currency=context.currency)

    steps.SimpleApi.postauthorize(context.service, trust_payment_id, service_order_id_list,
                                  amounts=amount_list, actions=action_list)

    payment_id = steps.SimpleApi.wait_for_payment(trust_payment_id)

    response = steps.CommonPartnerSteps.export_payment(payment_id)

    expected_output = "TrustPayment({}) skipped: payment has been completely cancelled".format(payment_id)
    utils.check_that(response['output'], equal_to(expected_output), u'Проверяем, что платеж пропущен')

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data, empty(), u'Проверяем, что транзакции не созданы')


@parametrize_integration_params
def test_partial_reversal(partner_integration_params):
    context = SUPERCHECK_CONTEXT

    amount = DEFAULT_PRICE / D('3')
    # вычисляем потенциальное вознаграждение
    commission = COMMISSION_CATEGORY_TRUST / D('100')
    yandex_reward = round(amount / D('100') * D(commission), 2)

    partner_id, person_id, contract_id, service_product_id = \
        create_client_and_contract_for_payments(context, partner_integration_params)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service,
                                             service_product_id,
                                             price=DEFAULT_PRICE,
                                             commission_category=COMMISSION_CATEGORY_TRUST,
                                             need_postauthorize=False,
                                             wait_for_export_from_bs=False,
                                             currency=context.currency)

    steps.SimpleApi.postauthorize(context.service, trust_payment_id, [service_order_id],
                                  amounts=[amount], actions=['clear'])

    payment_id = steps.SimpleApi.wait_for_payment(trust_payment_id)

    steps.CommonPartnerSteps.export_payment(payment_id)

    # сравниваем платеж с шаблоном
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    expected_template = [
        steps.SimpleApi.create_expected_tpt_row(context, partner_id, contract_id, person_id, trust_payment_id,
                                                payment_id,
                                                yandex_reward=yandex_reward,
                                                amount=amount)]
    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries(expected_template), 'Сравниваем платеж с шаблоном')


@parametrize_integration_params
def test_update_payment(partner_integration_params):
    context = SUPERCHECK_CONTEXT

    partner_id, person_id, contract_id, service_product_id = \
        create_client_and_contract_for_payments(context, partner_integration_params)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id)

    steps.CommonPartnerSteps.export_payment(payment_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data[0]['payout_ready_dt'], none(), u'Проверяем payout_ready_dt')

    steps.PaymentSteps.update_payment(trust_payment_id, PAYOUT_READY_DT)
    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data[0]['payout_ready_dt'], equal_to(PAYOUT_READY_DT), u'Проверяем payout_ready_dt')