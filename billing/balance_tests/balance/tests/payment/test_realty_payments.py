# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import REALTY_CONTEXT
from btestlib.matchers import has_entries_casted

pytestmark = [
    reporter.feature(Features.REALTY, Features.PAYMENT, Features.TRUST),
    pytest.mark.tickets('BALANCE-22481'),
    # Больше не модифицируем технического партнёра в БД.
    # Потому можем теперь просто разбирать платежи на настоящий технический договор
    # pytest.mark.no_parallel('realty', write=False)
]

SERVICE = REALTY_CONTEXT.service


def create_client_and_contract_for_payments():
    service_product_id = steps.SimpleApi.create_service_product(SERVICE)
    # client_id, person_id, realty_contract_id = steps.CommonPartnerSteps.get_tech_ids(SERVICE)
    client_id, person_id, realty_contract_id = steps.CommonPartnerSteps.get_active_tech_ids(SERVICE)
    return client_id, person_id, realty_contract_id, service_product_id


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


# проверка платежа
@pytest.mark.smoke
def test_realty_payment():
    client_id, person_id, realty_contract_id, service_product_id = create_client_and_contract_for_payments()

    # создаем платеж в трасте
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id)

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # формируем шаблон для сравнения
    expected_template = steps.SimpleApi.create_expected_tpt_row(REALTY_CONTEXT, client_id, realty_contract_id,
                                                                person_id,
                                                                trust_payment_id, payment_id,
                                                                yandex_reward=simpleapi_defaults.DEFAULT_PRICE,
                                                                internal=1,
                                                                invoice_commission_sum=0,
                                                                row_paysys_commission_sum=0)

    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)[0]

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, has_entries_casted(expected_template), 'Сравниваем платеж с шаблоном')


# проверка рефанда
@reporter.feature(Features.REFUND)
def test_realty_refund():
    client_id, person_id, realty_contract_id, service_product_id = create_client_and_contract_for_payments()

    # создаем платеж в трасте
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id)

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # создаем рефанд
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(SERVICE, service_order_id, trust_payment_id)

    # запускаем обработку рефанда
    steps.CommonPartnerSteps.export_payment(refund_id)

    # формируем шаблон для сравнения
    expected_template = steps.SimpleApi.create_expected_tpt_row(REALTY_CONTEXT, client_id, realty_contract_id,
                                                                person_id,
                                                                trust_payment_id, payment_id, trust_refund_id,
                                                                yandex_reward=simpleapi_defaults.DEFAULT_PRICE,
                                                                internal=1,
                                                                invoice_commission_sum=0,
                                                                row_paysys_commission_sum=0)

    # получаем данные по рефанду
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
        payment_id, TransactionType.REFUND)[0]

    # сравниваем рефанд с шаблоном
    utils.check_that(payment_data, has_entries_casted(expected_template), 'Сравниваем платеж с шаблоном')
