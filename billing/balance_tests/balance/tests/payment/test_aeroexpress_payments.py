# -*- coding: utf-8 -*-
""" Частично перенесено из temp/alshkit/aeroexpress.py """

__author__ = 'el-yurchito'

from decimal import Decimal

import pytest
from pytest import param

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import constants as const, utils
from btestlib.data.partner_contexts import AEROEXPRESS_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [reporter.feature(Features.PAYMENT, Features.TRUST)]

YANDEX_REWARD = Decimal('40.77')


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


def create_client_contract(use_service_fee=False):
    if use_service_fee:
        client_id, _, service_product_id = steps.SimpleApi.create_partner_product_and_fee(AEROEXPRESS_CONTEXT.service)
    else:
        client_id, service_product_id = steps.SimpleApi.create_partner_and_product(AEROEXPRESS_CONTEXT.service)

    # создаем договор
    _, person_id, contract_id, external_id = steps.ContractSteps.create_partner_contract(
        AEROEXPRESS_CONTEXT,
        client_id=client_id,
        additional_params={'partner_commission_sum': YANDEX_REWARD})

    return client_id, person_id, contract_id, external_id, service_product_id


# Тесты на платежи------------------------------------------------------------------------------------------------------
@pytest.mark.smoke
@pytest.mark.parametrize('context', [
    param(utils.aDict({'service_fee': False,
                       'reward_expected': YANDEX_REWARD}), id='YANDEX_REWARD_FOR_PRODUCT'),
    param(utils.aDict({'service_fee': True,
                       'reward_expected': None}), id='NO_YANDEX_REWARD_FOR_SERVICE_FEE1'),
])
def test_payment(context):
    client_id, person_id, contract_id, contract_eid, service_product_id = \
        create_client_contract(use_service_fee=context['service_fee'])

    _, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_trust_payment(AEROEXPRESS_CONTEXT.service, service_product_id)

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(AEROEXPRESS_CONTEXT, client_id, contract_id,
                                                               person_id, trust_payment_id, payment_id,
                                                               yandex_reward=context['reward_expected'])
    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')
