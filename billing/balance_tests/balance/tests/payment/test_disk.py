# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import TransactionType, Currencies, Products, Regions
from btestlib.data.partner_contexts import DISK_CONTEXT
from btestlib.matchers import contains_dicts_with_entries
from cashmachines.data.constants import CMNds

pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT, Features.DISK),
    pytest.mark.tickets('BALANCE-27903'),
    pytest.mark.usefixtures("switch_to_pg")
]

SERVICE = DISK_CONTEXT.service

DISK_USD_CONTEXT = DISK_CONTEXT.new(
    payment_currency=Currencies.USD,
    currency=Currencies.USD,
)

START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))

# a-vasin: цена задается в продукте
PRODUCT_PRICE = Decimal('10')

PARAMS = [
    (Currencies.RUB, CMNds.NDS_18, Regions.RU),
    pytest.mark.smoke(
        (Currencies.RUB, CMNds.NDS_20, Regions.RU)),
    (Currencies.USD, CMNds.NDS_18_118, Regions.US),
    (Currencies.USD, CMNds.NDS_20_120, Regions.US)
]

CURRENCY_TO_PRODUCT = {
    Currencies.RUB: Products.DISK_RUB_WITH_NDS,
    Currencies.USD: Products.DISK_USD_WO_NDS
}

CURRENCY_TO_CONTEXT = {
    Currencies.RUB: DISK_CONTEXT,
    Currencies.USD: DISK_USD_CONTEXT
}


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('disk', write=False)
@pytest.mark.parametrize("currency, fiscal_nds, region", PARAMS, ids=lambda c, nds, r: c.char_code)
def test_payment(currency, fiscal_nds, region):
    client_id, person_id, contract_id, service_product_id = get_tech_ids_for_disk(currency)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None, price=None,
                                             fiscal_nds=fiscal_nds, currency=currency, region_id=region.id)

    additional_params = create_specific_tpt_params(service_product_id, currency)
    expected_payment = steps.SimpleApi.create_expected_tpt_row(CURRENCY_TO_CONTEXT[currency], client_id,
                                                               contract_id, person_id, trust_payment_id, payment_id,
                                                               **additional_params)

    export_and_check_payment(payment_id, [expected_payment])


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('disk', write=False)
@pytest.mark.parametrize("currency, fiscal_nds, region", PARAMS, ids=lambda c, nds, r: c.char_code)
def test_refund(currency, fiscal_nds, region):
    client_id, person_id, contract_id, service_product_id = get_tech_ids_for_disk(currency)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None, price=None,
                                             fiscal_nds=fiscal_nds, currency=currency, region_id=region.id)
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(SERVICE, service_order_id, trust_payment_id,
                                                               delta_amount=PRODUCT_PRICE)

    additional_params = create_specific_tpt_params(service_product_id, currency)
    expected_payment = steps.SimpleApi.create_expected_tpt_row(CURRENCY_TO_CONTEXT[currency], client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id, trust_refund_id,
                                                               **additional_params)
    export_and_check_payment(refund_id, [expected_payment], payment_id, TransactionType.REFUND)


# ------------------------------------------------
# Utils

def create_contract(client_id, person_id):
    # a-vasin: шаблон для музыки, потому что диск сделан на её основе
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(
        DISK_CONTEXT, client_id=client_id,
        person_id=person_id,
        additional_params={'start_dt': START_DT, 'finish_dt': START_DT + relativedelta(years=1)})
    return contract_id


def update_contract_currency(contract_id, currency):
    query = "UPDATE T_CONTRACT_ATTRIBUTES SET VALUE_NUM=:currency " \
            "WHERE CODE='CURRENCY' AND ATTRIBUTE_BATCH_ID=(SELECT ATTRIBUTE_BATCH_ID FROM T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID=:contract_id)"
    params = {
        'currency': currency.num_code,
        'contract_id': contract_id
    }
    db.balance().execute(query, params)
    steps.ContractSteps.refresh_contracts_cache(contract_id)


def get_tech_ids_for_disk(currency):
    service_product_id = steps.SimpleApi.create_service_product(SERVICE)

    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, currency=currency.num_code)

    return client_id, person_id, contract_id, service_product_id


def export_and_check_payment(payment_id, expected_data, thirdparty_payment_id=None,
                             transaction_type=TransactionType.PAYMENT):
    if not thirdparty_payment_id:
        thirdparty_payment_id = payment_id

    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(thirdparty_payment_id, transaction_type)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


def create_specific_tpt_params(service_product_id, currency):
    balance_service_product_id = steps.SimpleApi.get_balance_service_product_id(service_product_id, SERVICE.id)
    params = {'amount': utils.dround(PRODUCT_PRICE, 2),
              'yandex_reward': utils.dround(PRODUCT_PRICE, 2),
              'internal': 1,
              'product_id': CURRENCY_TO_PRODUCT[currency].id,
              'service_product_id': balance_service_product_id,
              'invoice_commission_sum': 0,
              'row_paysys_commission_sum': 0}
    return params
