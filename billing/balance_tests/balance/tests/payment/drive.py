# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string, empty

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import Services, TransactionType, PaymentType, PaysysType, Currencies, ContractType, Firms, \
    Products
from btestlib.data.simpleapi_defaults import DEFAULT_PRICE
from btestlib.matchers import contains_dicts_with_entries
from cashmachines.data.constants import CMNds
from btestlib.data.partner_contexts import DRIVE_CONTEXT

#TODO: если тесты будем возвращать, то перевести на контексты

pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT, Features.DRIVE)
]

SERVICE = Services.DRIVE

START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))

# config = None
# Проверяем, что все fiscal_nds с заданным сервисным продуктом мапятся на один и тот же продукт, причем успешно
# Сервисный продукт зашит для Products.CARSHARING_WITH_NDS_1 в таблице t_partner_product

FISCAL_NDSES = [
    CMNds.NDS_10,
    CMNds.NDS_18,
    CMNds.NDS_20
]

# Пока тесты отключены - закомментирую no_parallel
# @pytest.mark.no_parallel('drive', write=False)
@pytest.mark.parametrize("fiscal_nds", FISCAL_NDSES, ids=lambda nds: str(nds.name).upper())
def test_payment(fiscal_nds, switch_to_pg):
    steps.CommonPartnerSteps.set_product_mapping_config(SERVICE, None)

    # client_id, person_id, contract_id, service_product_id = get_tech_ids_for_drive(ContractType.NOT_AGENCY)
    service_product_id = steps.CommonPartnerSteps.get_service_product(Products.CARSHARING_WITH_NDS_1)
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, ContractType.NOT_AGENCY)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None,
                                             fiscal_nds=fiscal_nds)

    expected_payment = create_expected_payment_data(contract_id, client_id, payment_id, person_id, trust_payment_id,
                                                    service_product_id, SERVICE.id)

    export_and_check_payment(payment_id, [expected_payment])

    # Отключили разбор платежей: TrustPayment(1033293286) skipped: row 999999999892734 skipped
    # steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, START_DT)

    # генерим счет на погашение и акт
    # steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)


# Пока тесты отключены - закомментирую no_parallel
# @pytest.mark.no_parallel('drive', write=False)
@pytest.mark.parametrize("fiscal_nds", FISCAL_NDSES, ids=lambda nds: str(nds.name).upper())
def test_refund(fiscal_nds, switch_to_pg):
    steps.CommonPartnerSteps.set_product_mapping_config(SERVICE, None)

    # client_id, person_id, contract_id, service_product_id = get_tech_ids_for_drive(ContractType.NOT_AGENCY)
    service_product_id = steps.CommonPartnerSteps.get_service_product(Products.CARSHARING_WITH_NDS_1)
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, ContractType.NOT_AGENCY)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None,
                                             fiscal_nds=fiscal_nds)
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(SERVICE, service_order_id, trust_payment_id)

    expected_payment = create_expected_refund(contract_id, client_id, payment_id, person_id, trust_refund_id,
                                              trust_payment_id, service_product_id, SERVICE.id)
    export_and_check_payment(refund_id, [expected_payment], payment_id, TransactionType.REFUND)


# Пока тесты отключены - закомментирую no_parallel
# @pytest.mark.no_parallel('drive', write=False)
def test_service_product_wo_mapping(switch_to_pg):
    steps.CommonPartnerSteps.set_product_mapping_config(SERVICE, None)
    service_product_id = steps.SimpleApi.create_service_product(SERVICE)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None)

    with pytest.raises(utils.XmlRpc.XmlRpcError) as error:
        steps.CommonPartnerSteps.export_payment(payment_id)

    expected_error = 'TrustPayment({})Export object is translated to state = 3'.format(payment_id)
    utils.check_that(error.value.response, contains_string(expected_error), u'Проверяем текст ошибки экспорта')


# Пока тесты отключены - закомментирую no_parallel
# @pytest.mark.no_parallel('drive')
def test_default_no_process_service_product(switch_to_pg):
    config = {
        'service_product_options': {
            'default': 'no_process'
        }
    }
    steps.CommonPartnerSteps.set_product_mapping_config(SERVICE, config)

    service_product_id = steps.SimpleApi.create_service_product(SERVICE)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None)
    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data, empty(), u'Проверяем отсутствие транзакций')


# ------------------------------------------------
# Utils

def export_and_check_payment(payment_id, expected_data, thirdparty_payment_id=None,
                             transaction_type=TransactionType.PAYMENT):
    if not thirdparty_payment_id:
        thirdparty_payment_id = payment_id

    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(thirdparty_payment_id, transaction_type)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


def create_common_expected_data(contract_id, partner_id, payment_id, person_id, trust_id, trust_payment_id,
                                payment_type, paysys_type_cc, service_product_id, service_id):
    balance_service_product_id = steps.SimpleApi.get_balance_service_product_id(service_product_id, service_id)

    return {
        'currency': Currencies.RUB.char_code,
        'partner_currency': Currencies.RUB.char_code,
        'commission_currency': Currencies.RUB.char_code,
        'service_id': SERVICE.id,
        'paysys_type_cc': paysys_type_cc,
        'payment_type': payment_type,
        'amount_fee': None,
        'client_id': None,
        'client_amount': None,
        'oebs_org_id': Firms.DRIVE_30.oebs_org_id,
        'commission_iso_currency': Currencies.RUB.iso_code,
        'iso_currency': Currencies.RUB.iso_code,
        'partner_iso_currency': Currencies.RUB.iso_code,
        'invoice_eid': None,
        'product_id': Products.CARSHARING_WITH_NDS_1.id,
        'service_product_id': balance_service_product_id,

        'contract_id': contract_id,
        'partner_id': partner_id,
        'payment_id': payment_id,
        'person_id': person_id,
        'trust_id': trust_id,
        'trust_payment_id': trust_payment_id
    }


def create_expected_payment_data(contract_id, partner_id, payment_id, person_id, trust_payment_id, service_product_id, service_id):
    expected_data = create_common_expected_data(contract_id, partner_id, payment_id, person_id, trust_payment_id,
                                                trust_payment_id, PaymentType.CARD, PaysysType.MONEY,
                                                service_product_id, service_id)

    yandex_reward = utils.dround(DEFAULT_PRICE, 2)
    amount = utils.dround(DEFAULT_PRICE, 2)

    expected_data.update({
        'amount': amount,
        'yandex_reward': yandex_reward,

        'transaction_type': TransactionType.PAYMENT.name,
        'internal': 1,
    })

    return expected_data


def create_expected_refund(contract_id, partner_id, payment_id, person_id, trust_id, trust_payment_id,
                           service_product_id, service_id):
    expected_data = create_common_expected_data(contract_id, partner_id, payment_id, person_id, trust_id,
                                                trust_payment_id, PaymentType.CARD, PaysysType.MONEY,
                                                service_product_id, service_id)

    yandex_reward = utils.dround(DEFAULT_PRICE, 2)
    amount = utils.dround(DEFAULT_PRICE, 2)

    expected_data.update({
        'amount': amount,
        'yandex_reward': yandex_reward,

        'transaction_type': TransactionType.REFUND.name,
        'internal': 1,
    })

    return expected_data
