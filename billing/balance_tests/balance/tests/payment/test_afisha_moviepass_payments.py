# coding: utf-8
__author__ = 'a-vasin'

from decimal import Decimal

import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType, Regions, Currencies
from btestlib.data.partner_contexts import TICKETS_MOVIEPASS_CONTEXT, TICKETS_MOVIEPASS_TARIFFICATOR_CONTEXT
from btestlib.matchers import contains_dicts_with_entries
import btestlib.reporter as reporter
from balance.features import Features

pytestmark = [reporter.feature(Features.TRUST, Features.PAYMENT)]

parametrize_prodcut_prices = pytest.mark.parametrize('prices', [
    [{'region_id': Regions.RU.id, 'dt': 1347521693, 'price': '2000', 'currency': Currencies.RUB.iso_code}],
    [{'region_id': Regions.RU.id, 'dt': 1347521693, 'price': '10', 'currency': Currencies.RUB.iso_code}]
])

parametrize_price = pytest.mark.parametrize('price', [
    pytest.param(20000, id='Non zero reward'),
    pytest.param(10, id='Zero reward')
])


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


# a-vasin: тут подписки, поэтому price не передаем в платеже, а заводится в самом продукте
@parametrize_prodcut_prices
def test_payment(prices):
    context = TICKETS_MOVIEPASS_CONTEXT
    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context, prices=prices)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id, price=None)

    amount = Decimal(prices[0]['price'])

    steps.CommonPartnerSteps.export_payment(payment_id)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                               person_id, trust_payment_id, payment_id,
                                                               amount=amount,
                                                               yandex_reward=get_yandex_reward(amount))
    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')


@parametrize_price
def test_payment_tarifficator(price):
    context = TICKETS_MOVIEPASS_TARIFFICATOR_CONTEXT
    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id, price=price)

    steps.CommonPartnerSteps.export_payment(payment_id)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                               person_id, trust_payment_id, payment_id,
                                                               amount=price,
                                                               yandex_reward=get_yandex_reward(price),
                                                               service_id=TICKETS_MOVIEPASS_CONTEXT.service.id
                                                               )
    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')


@parametrize_prodcut_prices
def test_refund(prices):
    context = TICKETS_MOVIEPASS_CONTEXT
    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context, prices=prices)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id, price=None)

    price = Decimal(prices[0]['price'])
    process_refund(context, contract_id, client_id, person_id, payment_id,
                   trust_payment_id, service_order_id, price)


@parametrize_price
def test_refund_tarifficator(price):
    context = TICKETS_MOVIEPASS_TARIFFICATOR_CONTEXT
    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id, price=price)

    process_refund(context, contract_id, client_id, person_id, payment_id,
                   trust_payment_id, service_order_id, price)


# -----------------------------------------
# Utils

def create_client_and_contract(context, prices=None):
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(context.service,
                                                                               prices)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                               client_id=client_id)
    return client_id, person_id, contract_id, service_product_id


def get_yandex_reward(amount):
    return utils.dround(amount * Decimal('0.01') / 100, 2)


def process_refund(context, contract_id, client_id, person_id, payment_id,
                   trust_payment_id, service_order_id, amount):
    trust_refund_id, refund_id = \
        steps.SimpleApi.create_refund(context.service, service_order_id, trust_payment_id,
                                      delta_amount=amount)

    steps.CommonPartnerSteps.export_payment(refund_id)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                               person_id, trust_payment_id, payment_id, trust_refund_id,
                                                               amount=amount,
                                                               amount_fee=0,
                                                               yandex_reward=get_yandex_reward(amount),
                                                               # оба сервиса маппятся в один
                                                               service_id=TICKETS_MOVIEPASS_CONTEXT.service.id,
                                                               )
    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')
