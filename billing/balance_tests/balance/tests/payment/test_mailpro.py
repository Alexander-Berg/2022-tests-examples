# coding: utf-8
__author__ = 'mindlin'

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import TransactionType, Currencies, Products, Regions
from btestlib.data.partner_contexts import MAILPRO_CONTEXT, MAILPRO_NONRESIDENT_SW_CONTEXT
from btestlib.matchers import contains_dicts_with_entries
from cashmachines.data.constants import CMNds

pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT, Features.MAILPRO),
    pytest.mark.usefixtures("switch_to_pg")
]

SERVICE = MAILPRO_CONTEXT.service

START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))

# a-vasin: цена задается в продукте
PRODUCT_PRICE = Decimal('10')


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@pytest.mark.smoke
@pytest.mark.parametrize('context', [MAILPRO_NONRESIDENT_SW_CONTEXT, MAILPRO_CONTEXT])
def test_payment(context):
    currency = context.currency
    region = context.region
    fiscal_nds = CMNds.NDS_20

    client_id, person_id, contract_id, service_product_id = get_tech_ids_for_mailpro(currency)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None, price=None,
                                             fiscal_nds=fiscal_nds, currency=currency, region_id=region.id)

    additional_params = create_specific_tpt_params(service_product_id, currency, context.product)
    expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id,
                                                               **additional_params)

    export_and_check_payment(payment_id, [expected_payment])


@pytest.mark.parametrize('context', [MAILPRO_NONRESIDENT_SW_CONTEXT, MAILPRO_CONTEXT])
def test_refund(context):
    currency = context.currency
    region = context.region
    fiscal_nds = CMNds.NDS_20

    client_id, person_id, contract_id, service_product_id = get_tech_ids_for_mailpro(currency)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None, price=None,
                                             fiscal_nds=fiscal_nds, currency=currency, region_id=region.id)
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(SERVICE, service_order_id, trust_payment_id,
                                                               delta_amount=PRODUCT_PRICE)

    additional_params = create_specific_tpt_params(service_product_id, currency, context.product)
    expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id, trust_refund_id,
                                                               **additional_params)
    export_and_check_payment(refund_id, [expected_payment], payment_id, TransactionType.REFUND)


# ------------------------------------------------
# Utils

def get_tech_ids_for_mailpro(currency):
    service_product_id = steps.SimpleApi.create_service_product(SERVICE, prices=[
        {'region_id': 225, 'dt': 1347521693, 'price': '10', 'currency': 'RUB'},
        {'region_id': 126, 'dt': 1327521693, 'price': '10', 'currency': 'USD'},
        ])

    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, currency=currency.num_code)

    return client_id, person_id, contract_id, service_product_id


def export_and_check_payment(payment_id, expected_data, thirdparty_payment_id=None,
                             transaction_type=TransactionType.PAYMENT):
    if not thirdparty_payment_id:
        thirdparty_payment_id = payment_id

    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(thirdparty_payment_id, transaction_type)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


def create_specific_tpt_params(service_product_id, currency, product):
    balance_service_product_id = steps.SimpleApi.get_balance_service_product_id(service_product_id, SERVICE.id)
    params = {'amount': utils.dround(PRODUCT_PRICE, 2),
              'yandex_reward': utils.dround(PRODUCT_PRICE, 2),
              'internal': 1,
              'product_id': product.id,
              'service_product_id': balance_service_product_id}
    return params
