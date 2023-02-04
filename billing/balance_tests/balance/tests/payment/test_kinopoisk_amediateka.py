# coding: utf-8
from decimal import Decimal

import pytest

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import TransactionType, TRUST_BILLING_SERVICE_MAP
from btestlib.data.partner_contexts import KINOPOISK_AMEDIATEKA_CONTEXT, KINOPOISK_AMEDIATEKA_TARIFFICATOR_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT, Features.MUSIC),
    pytest.mark.usefixtures("switch_to_pg")
]

contexts = (
    KINOPOISK_AMEDIATEKA_TARIFFICATOR_CONTEXT,
    KINOPOISK_AMEDIATEKA_CONTEXT,
)

parametrize_context = pytest.mark.parametrize('context', contexts, ids=lambda x: x.name)

PRODUCT_PRICE = Decimal('10')
SERVICE_PRICE = Decimal('200')


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@parametrize_context
def test_payment(context):
    client_id, person_id, contract_id, service_product_id = get_tech_ids(context)
    payment_price, expected_price = get_payment_and_expected_price_for(context)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id,
                                             price=payment_price, commission_category=None)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id,
                                                               **create_specific_tpt(expected_price))

    export_and_check_payment(payment_id, [expected_payment])


@parametrize_context
def test_refund(context):
    client_id, person_id, contract_id, service_product_id = get_tech_ids(context)
    payment_price, expected_price = get_payment_and_expected_price_for(context)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id,
                                             price=payment_price, commission_category=None)
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(context.service, service_order_id,
                                                               trust_payment_id, delta_amount=expected_price)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id, trust_refund_id,
                                                               **create_specific_tpt(expected_price))

    export_and_check_payment(refund_id, [expected_payment], payment_id, TransactionType.REFUND)


# ------------------------------------------------
# Utils

def get_tech_ids(context):
    service_product_id = steps.SimpleApi.create_service_product(context.service)
    service = TRUST_BILLING_SERVICE_MAP.get(context.service.id, context.service)
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(service)
    return client_id, person_id, contract_id, service_product_id


def get_payment_and_expected_price_for(context):
    if context.service.id == KINOPOISK_AMEDIATEKA_TARIFFICATOR_CONTEXT.service.id:
        payment_price = SERVICE_PRICE
        expected_price = SERVICE_PRICE
    else:
        payment_price = None
        expected_price = PRODUCT_PRICE
    return payment_price, expected_price


def create_specific_tpt(expected_price):
    return {
        'service_id': KINOPOISK_AMEDIATEKA_CONTEXT.service.id,
        'internal': 1,
        'amount': utils.dround(expected_price, 2),
    }


def export_and_check_payment(payment_id, expected_data, thirdparty_payment_id=None,
                             transaction_type=TransactionType.PAYMENT):
    if not thirdparty_payment_id:
        thirdparty_payment_id = payment_id

    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(thirdparty_payment_id,
                                                                                     transaction_type)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')

