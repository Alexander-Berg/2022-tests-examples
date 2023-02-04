# -*- coding: utf-8 -*-
__author__ = 'alshkit'

import json

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from balance.features import Features
from btestlib import reporter
from btestlib import utils
from btestlib.constants import NdsNew as Nds, TransactionType, Export, PaymentType
from btestlib.data import simpleapi_defaults
from btestlib.matchers import has_entries_casted, contains_dicts_with_entries
from simpleapi.common.payment_methods import Subsidy
from btestlib.data.partner_contexts import BLUE_MARKET_SUBSIDY
from .test_blue_market_payments import check_payment_fail_in_flag

# constants ============================================================================================================
CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
PREVIUS_MONTH_START_DT, PREVIUS_MONTH_END_DT = utils.Date.previous_month_first_and_last_days(datetime.today())
PRICE = Decimal('98.03893')
REFUND_AMOUNT = Decimal('9.99')
context = BLUE_MARKET_SUBSIDY

pytestmark = [pytest.mark.usefixtures('switch_to_pg')]


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


# utils ================================================================================================================
def create_client_persons_contracts(start_dt=None, nds=Nds.DEFAULT):
    partner_id, product_id = steps.SimpleNewApi.create_partner_with_product(context.service)
    partner_person_id = steps.PersonSteps.create(partner_id, context.person_type.code, {'is-partner': '1'})

    _, spendable_person_id, spendable_contract_id, spendable_contract_eid = \
        steps.ContractSteps.create_partner_contract(context, client_id=partner_id, person_id=partner_person_id,
                                                    additional_params={'start_dt': start_dt, 'nds': nds.nds_id})

    return partner_id, spendable_contract_id, spendable_contract_eid, partner_person_id, product_id


# tests=================================================================================================================
@pytest.mark.smoke
@reporter.feature(Features.TRUST, Features.PAYMENT, Features.MARKET)
def test_subsidy():
    partner_id, contract_id, _, partner_person_id, product_id = create_client_persons_contracts()

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, product_id, paymethod=Subsidy(), price=PRICE)

    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                     TransactionType.PAYMENT)[0]

    expected_data = steps.SimpleApi.create_expected_tpt_row(context, partner_id, contract_id, partner_person_id,
                                                            trust_payment_id, payment_id,
                                                            amount=utils.dround2(PRICE),
                                                            service_order_id_str=str(service_order_id),
                                                            )

    utils.check_that(payment_data, has_entries_casted(expected_data), step=u'Сравним платеж с ожидаемым.')


@reporter.feature(Features.TRUST, Features.PAYMENT, Features.MARKET)
def test_refund():
    partner_id, contract_id, _, partner_person_id, product_id = create_client_persons_contracts()

    # создадим платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, product_id, paymethod=Subsidy(), price=PRICE)

    steps.CommonPartnerSteps.export_payment(payment_id)

    # сделаем возврат
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(context.service, service_order_id,
                                                               trust_payment_id,
                                                               delta_amount=REFUND_AMOUNT)

    steps.CommonPartnerSteps.export_payment(refund_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)[0]

    expected_data = steps.SimpleApi.create_expected_tpt_row(context, partner_id, contract_id, partner_person_id,
                                                            trust_payment_id, payment_id, trust_refund_id,
                                                            amount=utils.dround2(REFUND_AMOUNT),
                                                            transaction_type=TransactionType.REFUND.name,
                                                            service_order_id_str=str(service_order_id),
                                                            )

    utils.check_that(payment_data, has_entries_casted(expected_data), step=u'Сравним платеж с ожидаемым.')


def test_through_yt_flag():
    # (_, _, _, _, product_id) = create_client_persons_contracts()
    # так как по цепочке платежа идти не будем, то не обязательно и существование всех балансовых сущностей
    _, product_id = steps.SimpleNewApi.create_partner_with_product(context.service)

    service_order_id, trust_payment_id, _, payment_id = steps.SimpleApi.create_trust_payment(
        context.service,
        product_id,
        paymethod=Subsidy(),
        price=PRICE,
        developer_payload_basket=json.dumps({'ProcessThroughYt': 1})
    )

    check_payment_fail_in_flag(payment_id)
    return service_order_id, trust_payment_id  # to refund test


def test_through_yt_flag_refund():
    service_order_id, trust_payment_id = test_through_yt_flag()

    trust_refund_id, refund_id = steps.SimpleApi.create_refund(context.service,
                                                               service_order_id, trust_payment_id,
                                                               delta_amount=REFUND_AMOUNT)
    check_payment_fail_in_flag(refund_id)


# SidePayment тесты вынесены в файл test_market_sidepayments.py

