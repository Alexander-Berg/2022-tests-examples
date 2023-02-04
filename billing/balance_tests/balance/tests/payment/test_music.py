# coding: utf-8
__author__ = 'a-vasin'

from decimal import Decimal
import itertools

import pytest
from hamcrest import contains_string, empty

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import TransactionType, Products, TRUST_BILLING_SERVICE_MAP
from btestlib.data.partner_contexts import MUSIC_CONTEXT, MUSIC_MEDIASERVICE_CONTEXT,\
    MUSIC_TARIFFICATOR_CONTEXT, MUSIC_MEDIASERVICE_TARIFFICATOR_CONTEXT
from btestlib.matchers import contains_dicts_with_entries
from cashmachines.data.constants import CMNds
from simpleapi.common.payment_methods import FiscalMusic

pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT, Features.MUSIC),
    pytest.mark.tickets('BALANCE-25329'),
    pytest.mark.usefixtures("switch_to_pg")
]

contexts = (
    MUSIC_TARIFFICATOR_CONTEXT,
    MUSIC_MEDIASERVICE_TARIFFICATOR_CONTEXT,
    MUSIC_MEDIASERVICE_CONTEXT,
    MUSIC_CONTEXT,
)

TARIFFICATOR_SERVICES = {ctx.service.id for ctx in (MUSIC_TARIFFICATOR_CONTEXT, MUSIC_MEDIASERVICE_TARIFFICATOR_CONTEXT)}

# a-vasin: для музыки цена задается в продукте
PRODUCT_PRICE = Decimal('10')
SERVICE_PRICE = Decimal('200')

FISCAL_NDSES = [
    CMNds.NDS_10,
    CMNds.NDS_18,
    pytest.mark.smoke(CMNds.NDS_20)
]

parametrize_context = pytest.mark.parametrize('context', contexts, ids=lambda x: x.name)
parametrize_fiscal_nds = pytest.mark.parametrize('fiscal_nds', FISCAL_NDSES, ids=lambda nds: str(nds.name).upper())
parametrize_context_n_service_fee = pytest.mark.parametrize('context, service_fee',
                                                            list(itertools.chain.from_iterable(itertools.product([c], (c.service_fee_product_map or {None: None}).keys()) for c in contexts)),
                                                            ids=lambda c, service_fee: '{}-service_fee={}'.format(c.name, service_fee))


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@parametrize_context_n_service_fee
@parametrize_fiscal_nds
def test_payment(context, service_fee, fiscal_nds):
    client_id, person_id, contract_id, service_product_id = get_tech_ids(context, fiscal_nds, service_fee=service_fee)
    payment_price, expected_price = get_payment_and_expected_price_for(context)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id,
                                             price=payment_price,
                                             commission_category=None)

    additional_params = create_specific_tpt(context, service_product_id, expected_price, service_fee=service_fee)
    expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id, **additional_params)

    export_and_check_payment(payment_id, [expected_payment])


@parametrize_context
@parametrize_fiscal_nds
def test_refund(context, fiscal_nds):
    client_id, person_id, contract_id, service_product_id = get_tech_ids(context, fiscal_nds)
    payment_price, expected_price = get_payment_and_expected_price_for(context)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id,
                                             price=payment_price,
                                             commission_category=None)
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(context.service, service_order_id,
                                                               trust_payment_id, delta_amount=expected_price)

    additional_params = create_specific_tpt(context, service_product_id, expected_price)
    expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id, trust_refund_id,
                                                               **additional_params)

    export_and_check_payment(refund_id, [expected_payment], payment_id, TransactionType.REFUND)


@parametrize_context
def test_fiscal_payment_type(context):
    payment_method = FiscalMusic()
    payment_price, _ = get_payment_and_expected_price_for(context)
    service_product_id = steps.SimpleApi.create_service_product(context.service)

    _, _, _, payment_id = steps.SimpleApi.create_trust_payment(context.service, service_product_id,
                                                               price=payment_price, paymethod=payment_method,
                                                               commission_category=None)

    output = steps.CommonPartnerSteps.export_payment(payment_id)['output']
    expected_output = 'skipped: ignore fiscal payment_method: {}'.format(payment_method.id)
    utils.check_that(output, contains_string(expected_output), u'Проверяем текст ошибки экспорта')

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data, empty(), u'Проверяем, что платежей нет')


@parametrize_context
def test_refund_fiscal_payment_type(context):
    payment_method = FiscalMusic()
    payment_price, _ = get_payment_and_expected_price_for(context)
    service_product_id = steps.SimpleApi.create_service_product(context.service)

    service_order_id, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id,
                                             price=payment_price, paymethod=payment_method,
                                             commission_category=None)
    _, refund_id = steps.SimpleApi.create_refund(context.service, service_order_id, trust_payment_id,
                                                 delta_amount=PRODUCT_PRICE)

    output = steps.CommonPartnerSteps.export_payment(refund_id)['output']
    expected_output = 'skipped: ignore fiscal payment_method: {}'.format(payment_method.id)
    utils.check_that(output, contains_string(expected_output), u'Проверяем текст ошибки экспорта')

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)
    utils.check_that(payment_data, empty(), u'Проверяем, что платежей нет')


# ------------------------------------------------
# Utils

def get_tech_ids(context, fiscal_nds, service_fee=None):
    service_product_id = steps.SimpleApi.create_service_product(context.service,
                                                                fiscal_nds=fiscal_nds, service_fee=service_fee)
    service = TRUST_BILLING_SERVICE_MAP.get(context.service.id, context.service)
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(service,
                                                                                     currency=context.currency.num_code)
    return client_id, person_id, contract_id, service_product_id


def export_and_check_payment(transaction_id, expected_data, payment_id=None,
                             transaction_type=TransactionType.PAYMENT):
    if not payment_id:
        payment_id = transaction_id

    steps.CommonPartnerSteps.export_payment(transaction_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                     transaction_type)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


def create_specific_tpt(context, service_product_id, amount, service_fee=None):
    balance_service_product_id = steps.SimpleApi.get_balance_service_product_id(service_product_id, context.service.id)
    service_fee_product_map = context.service_fee_product_map
    if service_fee_product_map is not None:
        product_id = service_fee_product_map.get(service_fee).id
    elif context.service.id in (MUSIC_CONTEXT.service.id, MUSIC_TARIFFICATOR_CONTEXT.service.id):
        product_id = Products.MUSIC.id
    else:
        product_id = Products.MUSIC_MEDIASERVICE.id

    return {
        'service_id': TRUST_BILLING_SERVICE_MAP.get(context.service.id, context.service).id,
        'product_id': product_id,
        'service_product_id': balance_service_product_id,
        'internal': 1,
        'amount': utils.dround(amount, 2),
        'yandex_reward': utils.dround(amount, 2),
        'invoice_commission_sum': 0,
        'row_paysys_commission_sum': 0
    }


def get_payment_and_expected_price_for(context):
    if context.service.id in TARIFFICATOR_SERVICES:
        payment_price = SERVICE_PRICE
        expected_price = SERVICE_PRICE
    else:
        payment_price = None
        expected_price = PRODUCT_PRICE
    return payment_price, expected_price
