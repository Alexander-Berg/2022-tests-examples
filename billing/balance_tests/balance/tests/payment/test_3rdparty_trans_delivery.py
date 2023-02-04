# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
from decimal import Decimal

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.data.partner_contexts import DOSTAVKA_CONTEXT
from btestlib.matchers import contains_dicts_with_entries
from simpleapi.common.payment_methods import Cash

START_DT = utils.Date.first_day_of_month(datetime.datetime.now())
PAYSYS_PARTNER_ID = 2237685
PAYMENT_METHOD = Cash(PAYSYS_PARTNER_ID)
SERVICE = DOSTAVKA_CONTEXT.service
MIN_COMMISSION = Decimal('3.5')


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@pytest.mark.smoke
@pytest.mark.slow
@pytest.mark.priority('low')
@reporter.feature(Features.TRUST, Features.PAYMENT)
@pytest.mark.tickets('BALANCE-21203')
def test_delivery(switch_to_pg):
    client_id, service_product_id, service_product_fee_id = steps.SimpleApi.create_partner_product_and_fee(SERVICE)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(DOSTAVKA_CONTEXT, client_id=client_id,
                                                                               is_postpay=0,
                                                                               additional_params={'start_dt': START_DT,
                                                                                                  'minimal_payment_commission': MIN_COMMISSION})

    service_order_id, trust_payment_id, _, payment_id = steps.SimpleApi.create_trust_payment(
        SERVICE, service_product_id,
        paymethod=PAYMENT_METHOD)

    steps.CommonPartnerSteps.export_payment(payment_id)

    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    expected_template = [
        steps.SimpleApi.create_expected_tpt_row(DOSTAVKA_CONTEXT, client_id, contract_id, person_id, trust_payment_id,
                                                payment_id,
                                                paysys_partner_id=PAYSYS_PARTNER_ID,
                                                yandex_reward=MIN_COMMISSION,
                                                service_order_id_str=str(service_order_id),
                                                )]
    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries(expected_template), 'Сравниваем платеж с шаблоном')
