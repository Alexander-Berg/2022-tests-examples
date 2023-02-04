# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime

import pytest
from functools import partial
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string, has_length, empty, not_, equal_to, greater_than_or_equal_to, not_none, any_of

import btestlib.reporter as reporter
import btestlib.utils as utils
from decimal import Decimal
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import TransactionType, ContractType, Products, PaymentMethods, PaymentType
from btestlib.data.simpleapi_defaults import DEFAULT_PRICE, STATIC_EMULATOR_CARD
from btestlib.matchers import contains_dicts_with_entries
from cashmachines.data.constants import CMNds
from btestlib.data.partner_contexts import KINOPOISK_PLUS_CONTEXT
from simpleapi.common.payment_methods import TrustWebPage, Via


pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT, Features.KINOPOISK_PLUS),
    pytest.mark.tickets('BALANCE-27903'),
    pytest.mark.usefixtures('switch_to_pg')
]

SERVICE = KINOPOISK_PLUS_CONTEXT.service

START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))

PARAMS = [
    (ContractType.LICENSE, CMNds.NDS_0),
    (ContractType.NOT_AGENCY, CMNds.NDS_18_118),
    pytest.mark.smoke(
        (ContractType.NOT_AGENCY, CMNds.NDS_20_120))
]

NDS_TO_PRODUCTS = {
    CMNds.NDS_0: Products.KINOPOISK_WO_NDS,
    CMNds.NDS_NONE: Products.KINOPOISK_WO_NDS,
    CMNds.NDS_18: Products.KINOPOISK_WITH_NDS,
    CMNds.NDS_18_118: Products.KINOPOISK_WITH_NDS,
    CMNds.NDS_20: Products.KINOPOISK_WITH_NDS,
    CMNds.NDS_20_120: Products.KINOPOISK_WITH_NDS,
}

MASTERCARD_DISCOUNT = Decimal('100.01')

parametrize_markup = pytest.mark.parametrize('markup', [
    {'virtual::kinopoisk_subs_discounts': str(MASTERCARD_DISCOUNT), 'card': str(DEFAULT_PRICE)},
    {'virtual::kinopoisk_card_discounts': str(MASTERCARD_DISCOUNT), 'card': str(DEFAULT_PRICE)},
    {'card': str(DEFAULT_PRICE)},
], ids=['card with subs_discounts', 'card with card_discounts', 'card'])


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
@pytest.mark.parametrize("contract_type, fiscal_nds", PARAMS, ids=lambda ct, nds: str(nds.name).upper())
def test_payment(contract_type, fiscal_nds):
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, contract_type)

    service_product_id = steps.SimpleApi.create_service_product(SERVICE, fiscal_nds=fiscal_nds)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None)

    additional_params = create_specific_tpt(service_product_id, fiscal_nds)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(KINOPOISK_PLUS_CONTEXT, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id, **additional_params)

    export_and_check_payment(payment_id, [expected_payment])


@pytest.mark.parametrize("contract_type, fiscal_nds", PARAMS, ids=lambda ct, nds: str(nds.name).upper())
@parametrize_markup
def test_markup_payment(contract_type, fiscal_nds, markup):
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, contract_type)
    service_product_id = steps.SimpleApi.create_service_product(SERVICE, fiscal_nds=fiscal_nds)

    payments, purchase_token, orders, paymethod_markup = create_markup_payment(service_product_id, markup, fiscal_nds)
    expected_payment_count = len(markup)  # количество платежей = количество переданных в разметке способов оплаты
    utils.check_that(payments, has_length(expected_payment_count),
                     u"Проверяем, что создано количество созданных платежей = {n}".format(n=expected_payment_count))
    expected_rows = []
    actual_rows = []
    for p in payments:
        payment_id, trust_payment_id, payment_method_id = p['payment_id'], p['trust_payment_id'], p['payment_method_id']
        export_result = steps.CommonPartnerSteps.export_payment(payment_id)
        # собираем фактически обработанные строки
        rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
        actual_rows.extend(rows)
        if payment_method_id == PaymentMethods.VIRTUAL.id:
            # строки kinopoisk_subs_discounts и kinopoisk_card_discounts должны пропускаться при обработке
            prefix = 'virtual::'
            virtual_payment_methods = [key for key in markup.keys() if key.startswith(prefix)]
            utils.check_that(virtual_payment_methods, not_(empty()),
                             'Проверяем что в разметке был виртуальный способ оплаты')
            expected_output = "TrustPayment({}) skipped: payment_method {}".format(
                payment_id, virtual_payment_methods[0][len(prefix):])
            utils.check_that(export_result['output'], equal_to(expected_output), u'Проверяем, что платеж пропущен')
            rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
            utils.check_that(rows, has_length(0), 'Проверяем что действительно строки платежа не были экспортированы')
            continue
        # для карточных платежей - создадим шаблон, по которому осуществим проверку
        row = create_expected_row(client_id, contract_id, person_id, service_product_id,
                                  trust_payment_id, payment_id, fiscal_nds)
        expected_rows.append(row)

    utils.check_that(actual_rows, contains_dicts_with_entries(expected_rows), u'Сравниваем платеж с шаблоном')


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('kinopoisk_plus', write=False)
@pytest.mark.parametrize("contract_type, fiscal_nds", PARAMS, ids=lambda ct, nds: str(nds.name).upper())
def test_refund(contract_type, fiscal_nds):
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, contract_type)

    service_product_id = steps.SimpleApi.create_service_product(SERVICE, fiscal_nds=fiscal_nds)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None)
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(SERVICE, service_order_id,
                                                               trust_payment_id)

    additional_params = create_specific_tpt(service_product_id, fiscal_nds)
    expected_payment = steps.SimpleApi.create_expected_tpt_row(KINOPOISK_PLUS_CONTEXT, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id, trust_refund_id, **additional_params)

    export_and_check_payment(refund_id, [expected_payment], payment_id, TransactionType.REFUND)


@pytest.mark.parametrize("contract_type, fiscal_nds", PARAMS, ids=lambda ct, nds: str(nds.name).upper())
@parametrize_markup
def test_markup_refund(contract_type, fiscal_nds, markup):
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, contract_type)
    service_product_id = steps.SimpleApi.create_service_product(SERVICE, fiscal_nds=fiscal_nds)

    payments, purchase_token, orders, paymethod_markup = create_markup_payment(service_product_id, markup, fiscal_nds)
    # обработаем платежи
    for p in payments:
        steps.CommonPartnerSteps.export_payment(p['payment_id'])
    # создадим возврат
    refund_orders = [{'order_id': order['order_id'], 'delta_amount': order['price']} for order in orders]
    refund_paymethod_markup = paymethod_markup
    steps.SimpleNewApi.create_refund(
        SERVICE, purchase_token, orders=refund_orders, paymethod_markup=refund_paymethod_markup)

    wait_refund_for = utils.wait_until2(partial(steps.SimpleApi.find_refund_by_orig_payment_id, strict=False),
                                        not_none())
    expected_rows = []
    actual_rows = []
    for p in payments:
        payment_id, trust_payment_id, payment_method_id = p['payment_id'], p['trust_payment_id'], p['payment_method_id']
        if payment_method_id == PaymentMethods.VIRTUAL.id:  # игнорируем сервисные скидки т.к они скипаются
            continue
        refund_id, trust_refund_id = wait_refund_for(payment_id)
        steps.CommonPartnerSteps.export_payment(refund_id)
        # собираем фактически обработанные строки
        rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)
        actual_rows.extend(rows)
        # создаем ожидаемые значения
        row = create_expected_row(client_id, contract_id, person_id, service_product_id,
                                  trust_payment_id, payment_id, fiscal_nds, trust_refund_id=trust_refund_id)
        expected_rows.append(row)
    utils.check_that(actual_rows, contains_dicts_with_entries(expected_rows),
                     u'Сравниваем возврат с шаблоном')


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('kinopoisk_plus', write=False)
def test_fiscal_nds_override():
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, ContractType.LICENSE)

    service_product_id = steps.SimpleApi.create_service_product(SERVICE, fiscal_nds=CMNds.NDS_18)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None,
                                             fiscal_nds=CMNds.NDS_0)

    additional_params = create_specific_tpt(service_product_id, CMNds.NDS_0)
    expected_payment = steps.SimpleApi.create_expected_tpt_row(KINOPOISK_PLUS_CONTEXT, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id, **additional_params)

    export_and_check_payment(payment_id, [expected_payment])


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('kinopoisk_plus', write=False)
def test_payment_incorrect_fiscal_nds():
    steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, ContractType.LICENSE)
    service_product_id = steps.SimpleApi.create_service_product(SERVICE, fiscal_nds=CMNds.NDS_10)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None)

    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        steps.CommonPartnerSteps.export_payment(payment_id)

    expected_error = 'TrustPayment({}) delayed: Cannot match single contract ' \
                     'for fiscal_nds = {}'.format(payment_id, CMNds.NDS_10.name)
    utils.check_that(xmlrpc_error.value.response, contains_string(expected_error), u"Проверяем текст ошибки")


def test_markup_payment_incorrect_fiscal_nds():
    fiscal_nds = CMNds.NDS_10
    steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, ContractType.LICENSE)
    service_product_id = steps.SimpleApi.create_service_product(SERVICE, fiscal_nds=fiscal_nds)
    markup = {'virtual::kinopoisk_subs_discounts': str(MASTERCARD_DISCOUNT), 'card': str(DEFAULT_PRICE)}

    payments, purchase_token, orders, paymethod_markup = create_markup_payment(service_product_id, markup, fiscal_nds)
    for p in payments:
        payment_id, payment_method_id = p['payment_id'], p['payment_method_id']
        if payment_method_id == PaymentMethods.VIRTUAL.id:
            continue
        with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
            steps.CommonPartnerSteps.export_payment(payment_id)

        expected_error = 'TrustPayment({}) delayed: Cannot match single contract ' \
                         'for fiscal_nds = {}'.format(payment_id, CMNds.NDS_10.name)
        utils.check_that(xmlrpc_error.value.response, contains_string(expected_error), u"Проверяем текст ошибки")


# ------------------------------------------------
# Utils

def create_contract(client_id, person_id, contract_type):
    # a-vasin: шаблон для музыки, потому что кинопоиск плюс сделан на её основе
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(KINOPOISK_PLUS_CONTEXT,
                                                                       client_id=client_id,
                                                                       person_id=person_id,
                                                                       additional_params={'start_dt': START_DT,
                                                                                          'commission': contract_type})
    return contract_id


def create_markup_payment(service_product_id, markup, fiscal_nds):
    # создаем разметку
    total_amount = sum(Decimal(price) for price in markup.values())
    orders = steps.SimpleNewApi.create_orders_for_payment(
        SERVICE, service_product_id, amount=total_amount, commission_category=None,
        fiscal_nds=fiscal_nds.name
    )
    utils.check_that(orders, has_length(1), u'Проверяем, что создан один заказ')
    order_id = orders[0]['order_id']
    paymethod_markup = {order_id: markup}
    paymethod = TrustWebPage(Via.card(STATIC_EMULATOR_CARD, unbind_before=False))

    # создаем платеж
    group_trust_payment_id, group_payment_id, purchase_token = steps.SimpleNewApi.create_payment(
        SERVICE, orders=orders, amount=str(total_amount),
        currency=KINOPOISK_PLUS_CONTEXT.currency.iso_code, paymethod_markup=paymethod_markup,
        paymethod=paymethod, fiscal_nds=fiscal_nds)

    # находим связанные платежи
    expected_payment_count = len(markup.keys())
    wait_payments_for = utils.wait_until2(steps.CommonPartnerSteps.get_children_trust_group_payments,
                                          has_length(greater_than_or_equal_to(expected_payment_count)))
    payments = wait_payments_for(group_trust_payment_id)
    return payments, purchase_token, orders, paymethod_markup


def export_and_check_payment(payment_id, expected_data, thirdparty_payment_id=None,
                             transaction_type=TransactionType.PAYMENT):
    if not thirdparty_payment_id:
        thirdparty_payment_id = payment_id

    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
        thirdparty_payment_id, transaction_type)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


def create_specific_tpt(service_product_id, fiscal_nds, amount=DEFAULT_PRICE):
    balance_service_product_id = steps.SimpleApi.get_balance_service_product_id(
        service_product_id, KINOPOISK_PLUS_CONTEXT.service.id)
    return {
        'product_id': NDS_TO_PRODUCTS[fiscal_nds].id,
        'service_product_id': balance_service_product_id,
        'internal': 1,
        'amount': amount,
        'yandex_reward': amount,
        'invoice_commission_sum': 0,
        'row_paysys_commission_sum': 0
    }


def create_expected_row(client_id, contract_id, person_id, service_product_id,
                        trust_payment_id, payment_id, fiscal_nds, trust_refund_id=None):
    additional_params = create_specific_tpt(service_product_id, fiscal_nds, DEFAULT_PRICE)
    additional_params['payment_type'] = any_of(PaymentType.DIRECT_CARD, PaymentType.CARD)
    return steps.SimpleApi.create_expected_tpt_row(
        KINOPOISK_PLUS_CONTEXT, client_id, contract_id, person_id,
        trust_payment_id, payment_id, trust_refund_id, ** additional_params)
