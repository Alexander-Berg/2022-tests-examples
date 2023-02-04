# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import TELEMEDICINE_CONTEXT, TELEMEDICINE_SPENDABLE_CONTEXT
from btestlib.matchers import has_entries_casted
from simpleapi.common.payment_methods import Coupon

PRICE = simpleapi_defaults.DEFAULT_PRICE

contract_start_dt = datetime.datetime.today().replace(day=1, hour=0, minute=0) - datetime.timedelta(days=200)

AGENT_REWARD_PCT = D('24.87')
COMMISSION_PCT = D('25.39')


@utils.memoize
def create_client_persons_contracts(agent_reward_pct, commission_pct):
    partner_id, product_id = steps.SimpleNewApi.create_partner_with_product(TELEMEDICINE_SPENDABLE_CONTEXT.service)

    params = {'medicine_pay_commission': agent_reward_pct, 'medicine_pay_commission2': commission_pct}

    _, person_id, general_contract_id, _ = steps.ContractSteps.create_partner_contract(TELEMEDICINE_CONTEXT,
                                                                                       client_id=partner_id,
                                                                                       additional_params=params)

    _, partner_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        TELEMEDICINE_SPENDABLE_CONTEXT, client_id=partner_id,
        additional_params={'link_contract_id': general_contract_id})
    return partner_id, spendable_contract_id, partner_person_id, product_id


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@reporter.feature(Features.TRUST, Features.PAYMENT, Features.MEDICINE, Features.REFUND)
@pytest.mark.tickets('BALANCE-26070')
@pytest.mark.parametrize(
    'is_refund, amount, agent_reward_pct, commission_pct',
    [
        pytest.param(False, PRICE, AGENT_REWARD_PCT, COMMISSION_PCT, marks=pytest.mark.smoke,
                     id='Payment'),
        pytest.param(True, PRICE, AGENT_REWARD_PCT, COMMISSION_PCT,
                     id='Refund'),
        pytest.param(False, D('0.02'), AGENT_REWARD_PCT, COMMISSION_PCT,
                     id='Payment for kopeika check'),
        pytest.param(True, D('0.02'), AGENT_REWARD_PCT, COMMISSION_PCT,
                     id='Refund for kopeika check'),
        pytest.param(False, PRICE, D('0'), D('100'),
                     id='Payment with amount = 0'),
        pytest.param(True, PRICE, D('0'), D('100'),
                     id='Refund with amount = 0'),
    ])
def test_create_payment(is_refund, amount, agent_reward_pct, commission_pct, switch_to_pg):
    # создаем данные
    partner_id, spendable_contract_id, partner_person_id, product_id = create_client_persons_contracts(agent_reward_pct,
                                                                                                       commission_pct)

    # создаем платеж
    trust_payment_id, payment_id, purchase_token = steps.SimpleNewApi.create_payment(
        TELEMEDICINE_SPENDABLE_CONTEXT.service,
        product_id,
        paymethod=Coupon(),
        amount=amount)

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    trust_refund_id = None
    if is_refund:
        # создаем рефанд и обрабатываем его
        trust_refund_id, refund_id = steps.SimpleNewApi.create_refund(TELEMEDICINE_SPENDABLE_CONTEXT.service,
                                                                      purchase_token)
        steps.CommonPartnerSteps.export_payment(refund_id)

    pct_sum = agent_reward_pct + commission_pct

    amount = max(round(amount / D('100') * (D('100') - pct_sum), 2), D('0.01')) if pct_sum < D('100') else 0

    expected_template = steps.SimpleApi.create_expected_tpt_row(TELEMEDICINE_SPENDABLE_CONTEXT, partner_id,
                                                                spendable_contract_id, partner_person_id,
                                                                trust_payment_id,
                                                                payment_id,
                                                                trust_refund_id,
                                                                amount=amount)

    # проверяем платеж или рефанд
    transaction_type = TransactionType.REFUND if is_refund else TransactionType.PAYMENT
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, transaction_type)[0]

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, has_entries_casted(expected_template),
                     'Сравниваем платеж с шаблоном')
