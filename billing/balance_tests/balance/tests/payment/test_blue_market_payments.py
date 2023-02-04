# -*- coding: utf-8 -*-
__author__ = 'alshkit'

import json

from datetime import datetime
from decimal import Decimal
import uuid

import btestlib.environments as env
import pytest
from dateutil.relativedelta import relativedelta
from functools import partial
from hamcrest import not_none, anything

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features

import btestlib
from btestlib import utils
from contextlib import contextmanager
from btestlib.constants import Services, TransactionType, PaymentType, \
    NdsNew, PaymentMethods, PaysysType, Export
from btestlib.data import simpleapi_defaults
from btestlib.matchers import contains_dicts_with_entries, equal_to
from btestlib.utils import XmlRpc
from btestlib.data.partner_contexts import BLUE_MARKET_PAYMENTS, BLUE_MARKET_SUBSIDY, BLUE_MARKET_SUBSIDY_SPASIBO, \
    BLUE_MARKET_612_ISRAEL, BLUE_MARKET_SUBSIDY_ISRAEL, DELIVERY_SERVICES_CONTEXT_SPENDABLE, SORT_CENTER_CONTEXT_SPENDABLE
from simpleapi.common.payment_methods import Cash, LinkedCard, MarketCredit, VirtualBnpl, YandexAccountTopup, CreditCession
from tenacity import retry, retry_if_exception, stop_after_attempt, wait_fixed

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
PREVIUS_MONTH_START_DT, PREVIUS_MONTH_END_DT = utils.Date.previous_month_first_and_last_days(datetime.today())
COMISSIONS = [Decimal('200'), Decimal('300'), Decimal('0')]
PRICES = [Decimal('1000'), Decimal('500'), Decimal('250')]

PRICE_CARD_NO_FEE = Decimal('23')
PRICE_CARD_FEE = Decimal('7')
PRICE_SPASIBO_FEE = Decimal('15')
PRICE_SPASIBO_NO_FEE = Decimal('5')
PRICE_CARD_RESIZE_NO_FEE = Decimal('2')
PRICE_CARD_RESIZE_FEE = Decimal('1')
PRICE_SPASIBO_RESIZE_NO_FEE = Decimal('4')
PRICE_SPASIBO_RESIZE_FEE = Decimal('3')

COMMISSION_CATEGORY_NO_FEE = Decimal('300')
COMMISSION_CATEGORY_FEE = Decimal('0')

PRICES_LIST = [PRICE_CARD_NO_FEE + PRICE_SPASIBO_NO_FEE, PRICE_CARD_FEE + PRICE_SPASIBO_FEE]
COMMISSION_CATEGORY_LIST = [COMMISSION_CATEGORY_NO_FEE, COMMISSION_CATEGORY_FEE]

SPASIBO_PAYMENT_METHOD_ID = PaymentMethods.SPASIBO.id
CARD_PAYMENT_METHOD_ID = PaymentMethods.CARD.id


context_payments = BLUE_MARKET_PAYMENTS
context_spasibo = BLUE_MARKET_SUBSIDY_SPASIBO

pytestmark = [pytest.mark.usefixtures('switch_to_pg'),
              reporter.feature(Features.TRUST, Features.PAYMENT)]


wait_refund_for = utils.wait_until2(partial(steps.SimpleApi.find_refund_by_orig_payment_id, strict=False),
                                    not_none())

PAYSYS_TYPE_MAP = {
    MarketCredit: PaysysType.BANK_CREDIT,
    VirtualBnpl: PaysysType.VIRTUAL_BNPL,
    CreditCession: PaysysType.CREDIT_CESSION,
}


# utils
def create_contract_for_partner(start_dt, context=BLUE_MARKET_PAYMENTS):
    with reporter.step(u'Создаем договор для клиента-партнера'):
        # создаем клиента-партнера
        client_id, product, product_fee = steps.SimpleApi.create_partner_product_and_fee(context.service)

        # создаем договор для клиента-партнера
        _, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(context,
                                                                                              client_id=client_id,
                                                                                              is_offer=1,
                                                                                              additional_params={
                                                                                                  'start_dt': start_dt
                                                                                              })

        return client_id, person_id, contract_id, contract_eid, product, product_fee


def create_ids_for_payments_blue_market(context=BLUE_MARKET_PAYMENTS, start_dt=None):
    # tech_client_id, tech_person_id, tech_contract_id = steps.CommonPartnerSteps.get_tech_ids(
    #     context.service)

    tech_client_id, tech_person_id, tech_contract_id = steps.CommonPartnerSteps.get_active_tech_ids(
        context_payments.service)

    client_id, person_id, contract_id, _, product, product_fee = create_contract_for_partner(start_dt, context=context)

    return tech_client_id, tech_person_id, tech_contract_id, client_id, person_id, contract_id, product, product_fee


class NotAllChildrenPaymentsExportedFromTrust(Exception):
    pass


@retry(retry=retry_if_exception(NotAllChildrenPaymentsExportedFromTrust), stop=stop_after_attempt(50),
       wait=wait_fixed(2), reraise=True)
def get_children_payments(group_trust_payment_id,
                          first_paymethod=CARD_PAYMENT_METHOD_ID,
                          second_paymethod=SPASIBO_PAYMENT_METHOD_ID):
    children_payments = steps.CommonPartnerSteps.get_children_trust_group_payments(group_trust_payment_id)
    try:
        utils.check_that(len(children_payments), equal_to(2), step=u'Проверим, что создалось 2 дочерних платежа')
    except AssertionError:
        raise NotAllChildrenPaymentsExportedFromTrust

    spasibo_payment, = filter(lambda r: r['payment_method_id'] == second_paymethod, children_payments)
    spasibo_payment_id, spasibo_trust_payment_id = spasibo_payment['payment_id'], spasibo_payment['trust_payment_id']
    card_payment, = filter(lambda r: r['payment_method_id'] == first_paymethod, children_payments)
    card_payment_id, card_trust_payment_id = card_payment['payment_id'], card_payment['trust_payment_id']
    return spasibo_payment_id, spasibo_trust_payment_id, card_payment_id, card_trust_payment_id


class SpasiboCashbackIsStillNotExportedFromTrust(Exception):
    pass


@retry(retry=retry_if_exception(SpasiboCashbackIsStillNotExportedFromTrust), stop=stop_after_attempt(50),
       wait=wait_fixed(2), reraise=True)
def get_spasibo_cashback_payments(group_payment_id):
    spasibo_cashback_payments = steps.CommonPartnerSteps.get_children_cashback_payments(group_payment_id)
    if len(spasibo_cashback_payments) == 0:
        raise SpasiboCashbackIsStillNotExportedFromTrust(u'Кешбеки спасибо не выгрузились из Траста')

    spasibo_cashback_payment, = spasibo_cashback_payments
    spasibo_cashback_payment_id, spasibo_cashback_trust_payment_id, spasibo_cashback_payment_method_id = \
        spasibo_cashback_payment['payment_id'], spasibo_cashback_payment['trust_payment_id'], spasibo_cashback_payment['payment_method_id']
    utils.check_that(int(spasibo_cashback_payment_method_id), equal_to(PaymentMethods.SPASIBO_CASHBACK.id),
                     step=u'Проверим, что cashback спасибо имеет правильный payment_method')
    return spasibo_cashback_payment_id, spasibo_cashback_trust_payment_id


@retry(retry=retry_if_exception(SpasiboCashbackIsStillNotExportedFromTrust), stop=stop_after_attempt(50),
       wait=wait_fixed(2), reraise=True)
def get_spasibo_cashback_refunds(spasibo_cashback_payment_id):
    try:
        spasibo_cashback_refund_id, spasibo_cashback_trust_refund_id = \
            steps.SimpleApi.find_refund_by_orig_payment_id(spasibo_cashback_payment_id)
    except IndexError:
        raise SpasiboCashbackIsStillNotExportedFromTrust(u'Рефанды кешбеков спасибо не выгрузились из траста')
    return spasibo_cashback_refund_id, spasibo_cashback_trust_refund_id


def get_service_product_and_order_ids(service_product_no_fee, service_product_fee):
    # будет 2 платежа, объединенные групповым (карточный и спасибный). С каждого платежа - зачисление на 2 заказа.
    # Т.е. на каждый заказ будет зачисление с обоих платежей.
    service_product_ids = [service_product_no_fee, service_product_fee]

    balance_service_product_ids = [steps.SimpleApi.get_balance_service_product_id(service_product_no_fee),
                                   steps.SimpleApi.get_balance_service_product_id(service_product_fee)]

    # Генерим service_order_id для заказов, чтобы передать распределение сумм по ним в paymethod_markup
    service_order_ids = [uuid.uuid1().hex, uuid.uuid1().hex]
    return service_product_ids, service_order_ids, balance_service_product_ids


def create_paymethod_markup(service_order_id_list, link_card=True, first='card', second='spasibo'):
    paymethod_markup = {
        service_order_id_list[0]: {first: str(PRICE_CARD_NO_FEE if link_card else PRICE_CARD_RESIZE_NO_FEE),
                                   second: str(PRICE_SPASIBO_NO_FEE if link_card else PRICE_SPASIBO_RESIZE_NO_FEE)},
        service_order_id_list[1]: {first: str(PRICE_CARD_FEE if link_card else PRICE_CARD_RESIZE_FEE),
                                   second: str(PRICE_SPASIBO_FEE if link_card else PRICE_SPASIBO_RESIZE_FEE)}}

    paymethod = LinkedCard(card=simpleapi_defaults.SPASIBO_EMULATOR_CARD) if link_card else None

    return paymethod_markup, paymethod


def delete_virtual_prefix(string):
    prefix = 'virtual::'
    if string.startswith(prefix):
        return string[len(prefix):]
    return string


# подготовка данных о транзакциях со спасибо
# будет две карточных строки и одна спасибная (вторая появится после проставления payout_ready_dt)
# четвертую строку создаем в следующем методе
def create_expected_payment_data(client_id, contract_id, person_id, card_trust_payment_id, card_payment_id,
                                 personal_account_eid, balance_service_product_ids, tech_client_id, tech_contract_id,
                                 tech_person_id, spasibo_trust_payment_id, spasibo_payment_id,
                                 action_no_fee=None, action_fee=None,
                                 first_payment_type=PaymentType.CARD, second_payment_type=PaymentType.SPASIBO,
                                 first_paysys_type=PaysysType.MONEY, second_paysys_type=PaysysType.SPASIBO):
    expected_data_list_card_payment = []
    first_payment_type = delete_virtual_prefix(first_payment_type)

    if action_no_fee != 'cancel':
        amount_card_no_fee = PRICE_CARD_RESIZE_NO_FEE if action_no_fee == 'clear' else PRICE_CARD_NO_FEE
        expected_data_list_card_payment.append(
            steps.SimpleApi.create_expected_tpt_row(context_payments,
                                                    client_id, contract_id, person_id,
                                                    card_trust_payment_id,
                                                    card_payment_id, **{'amount': amount_card_no_fee,
                                                                    'invoice_eid': personal_account_eid,
                                                                    'yandex_reward': amount_card_no_fee * COMMISSION_CATEGORY_NO_FEE * Decimal('0.0001'),
                                                                    'service_product_id': balance_service_product_ids[0],
                                                                    'payment_type': first_payment_type,
                                                                    'paysys_type_cc': first_paysys_type}))

    # строка с fee всегда будет, но если ее отменяем/ресайзим, меняется сумма
    amount_card_fee = PRICE_CARD_RESIZE_FEE if action_fee == 'clear' else \
        (Decimal('0') if action_fee == 'cancel' else PRICE_CARD_FEE)
    expected_data_list_card_payment.append(
        steps.SimpleApi.create_expected_tpt_row(context_payments,
                                                tech_client_id, tech_contract_id, tech_person_id,
                                                card_trust_payment_id,
                                                card_payment_id, **{'amount': amount_card_fee,
                                                                    'yandex_reward': amount_card_fee,
                                                                    'service_product_id': balance_service_product_ids[1],
                                                                    'internal': 1,
                                                                    'payment_type': first_payment_type,
                                                                    'paysys_type_cc': first_paysys_type}))

    amount_spasibo_fee = PRICE_SPASIBO_RESIZE_FEE if action_fee == 'clear' else \
        (Decimal('0') if action_fee == 'cancel' else PRICE_SPASIBO_FEE)
    if second_payment_type == PaymentType.SPASIBO:
        yandex_reward_spasibo = amount_spasibo_fee
        second_paysys_type = context_payments.tpt_paysys_type_cc
    elif second_payment_type == PaymentType.YANDEX_ACCOUNT_WITHDRAW:
        yandex_reward_spasibo = None
    expected_data_list_spasibo_payment = [
        # fee product
        steps.SimpleApi.create_expected_tpt_row(context_payments,
                                                tech_client_id, tech_contract_id, tech_person_id,
                                                spasibo_trust_payment_id,
                                                spasibo_payment_id, **{'amount': amount_spasibo_fee,
                                                                       'payment_type': second_payment_type,
                                                                       'yandex_reward': yandex_reward_spasibo,
                                                                       'service_product_id':
                                                                           balance_service_product_ids[1],
                                                                       'internal': 1,
                                                                       'paysys_type_cc': second_paysys_type,
                                                                       }),
    ]

    return expected_data_list_card_payment, expected_data_list_spasibo_payment


def add_payout_ready_dt(expected_data_list_card_payment, expected_data_list_spasibo_payment, action_no_fee, payout_ready_dt):
    expected_data_list_card_payment[0].update({'payout_ready_dt': payout_ready_dt})
    if action_no_fee == 'clear':
        expected_data_list_card_payment[1].update({'payout_ready_dt': payout_ready_dt})
    for spasibo_payment in expected_data_list_spasibo_payment:
        if spasibo_payment['internal'] == 1:
            spasibo_payment['payout_ready_dt'] = payout_ready_dt
    return expected_data_list_card_payment, expected_data_list_spasibo_payment


def add_spasibo_no_fee(client_id, spendable_contract_id, spendable_person_id,
                       spasibo_trust_payment_id, spasibo_payment_id, balance_service_product_ids,
                       expected_data_list_card_payment, expected_data_list_spasibo_payment, action_no_fee,
                       payment_type='spasibo', paysys_type_cc='spasibo'):
    amount_spasibo_no_fee = PRICE_SPASIBO_RESIZE_NO_FEE if action_no_fee == 'clear' else \
        (Decimal('0') if action_no_fee == 'cancel' else PRICE_SPASIBO_NO_FEE)
    expected_data_list_spasibo_payment.append(
        steps.SimpleApi.create_expected_tpt_row(context_spasibo,
                                                client_id, spendable_contract_id, spendable_person_id,
                                                spasibo_trust_payment_id,
                                                spasibo_payment_id, **{'amount': amount_spasibo_no_fee,
                                                                       'service_product_id':
                                                                           balance_service_product_ids[0],
                                                                       'service_id': Services.BLUE_MARKET_SUBSIDY.id,
                                                                       'payment_type': payment_type,
                                                                       'paysys_type_cc': paysys_type_cc,
                                                                       }))
    return expected_data_list_card_payment, expected_data_list_spasibo_payment


def add_payout_ready_dt_and_spasibo_no_fee(client_id, spendable_contract_id, spendable_person_id,
                                           spasibo_trust_payment_id, spasibo_payment_id, balance_service_product_ids,
                                           expected_data_list_card_payment, expected_data_list_spasibo_payment, action_no_fee=None,
                                           payout_ready_dt=CONTRACT_START_DT):

    expected_data_list_card_payment, expected_data_list_spasibo_payment = \
        add_payout_ready_dt(expected_data_list_card_payment, expected_data_list_spasibo_payment, action_no_fee, payout_ready_dt)

    expected_data_list_card_payment, expected_data_list_spasibo_payment = \
        add_spasibo_no_fee(client_id, spendable_contract_id, spendable_person_id,
                           spasibo_trust_payment_id, spasibo_payment_id, balance_service_product_ids,
                           expected_data_list_card_payment, expected_data_list_spasibo_payment, action_no_fee)

    return expected_data_list_card_payment, expected_data_list_spasibo_payment


@contextmanager
def switch_environment(*args, **kwargs):
    saved_kwags = {
        "dbname": env.SimpleapiEnvironment.DB_NAME,
        "xmlrpc_url": env.SimpleapiEnvironment.XMLRPC_URL
    }
    try:
        yield env.SimpleapiEnvironment.switch_param(*args, **kwargs)
    finally:
        env.SimpleapiEnvironment.switch_param(**saved_kwags)


def check_payment_fail_in_flag(payment_id):
    with reporter.step(u'Экспортируем платеж c флагов processThroughYt - ожидаем, что экспорт упадет в Skip'):
        export_result = steps.CommonPartnerSteps.export_payment(payment_id)
        assert export_result['state'] == '1'
        expected_error_part = 'skipped: transaction {} skipped due to [ ProcessThroughYt ] flag in payload'.format(
            payment_id)
        assert expected_error_part in export_result['output']


# tests================================================================================================================
@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


paymethod_and_delivery_parametrize = pytest.mark.parametrize(
    'context, payment_method, delivery',
    (
            pytest.param(BLUE_MARKET_PAYMENTS, Cash, False, marks=pytest.mark.smoke, id='Cash without delivery'),
            pytest.param(BLUE_MARKET_PAYMENTS, None, False, id='Not cash without delivery'),
            pytest.param(BLUE_MARKET_PAYMENTS, Cash, True, id='Cash with delivery'),
            pytest.param(BLUE_MARKET_PAYMENTS, None, True, id='Not cash with delivery'),
            pytest.param(BLUE_MARKET_PAYMENTS, MarketCredit, False, id='MarketCredit without delivery'),
            pytest.param(BLUE_MARKET_PAYMENTS, MarketCredit, True, id='MarketCredit with delivery'),
            pytest.param(BLUE_MARKET_PAYMENTS, VirtualBnpl, False, id='VirtualBnpl without delivery'),
            pytest.param(BLUE_MARKET_PAYMENTS, VirtualBnpl, True, id='VirtualBnpl with delivery'),
            pytest.param(BLUE_MARKET_PAYMENTS, CreditCession, False, id='CreditCession without delivery'),
            pytest.param(BLUE_MARKET_PAYMENTS, CreditCession, True, id='CreditCession with delivery'),
            # pytest.param(BLUE_MARKET_612_ISRAEL, Cash, False, marks=pytest.mark.smoke, id='Israel:Cash without delivery'),
            # pytest.param(BLUE_MARKET_612_ISRAEL, None, False, id='Israel:Not cash without delivery'),
            # pytest.param(BLUE_MARKET_612_ISRAEL, Cash, True, id='Israel:Cash with delivery'),
            # pytest.param(BLUE_MARKET_612_ISRAEL, None, True, id='Israel:Not cash with delivery'),
    )
)
paymethod_parametrize = pytest.mark.parametrize(
    'payment_method', (None, MarketCredit, VirtualBnpl, CreditCession)
)


# @pytest.mark.no_parallel('blue_market', write=False)
@reporter.feature(Features.TRUST, Features.PAYMENT, Features.MARKET)
@paymethod_and_delivery_parametrize
def test_payments_with_or_without_delivery(payment_method, delivery, context):
    tech_client_id, tech_person_id, tech_contract_id, \
        first_client_id, first_person_id, first_contract_id, \
        first_product, product_fee = create_ids_for_payments_blue_market(context)

    tech_client_id, tech_person_id, tech_contract_id, \
        second_client_id, second_person_id, second_contract_id, \
        second_product, product_fee = create_ids_for_payments_blue_market(context)

    PAYMENT_METHOD = payment_method(tech_client_id) if payment_method else None

    prices_list = PRICES if delivery else PRICES[:2]
    commission_category_list = COMISSIONS if delivery else COMISSIONS[:2]
    service_order_id_list, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service,
                                                       [first_product, second_product, product_fee],
                                                       commission_category_list=commission_category_list,
                                                       prices_list=prices_list,
                                                       paymethod=PAYMENT_METHOD,
                                                       currency=context.currency)
    steps.CommonPartnerSteps.export_payment(payment_id)

    first_client_inv_eid = steps.InvoiceSteps.get_personal_account_external_id(first_contract_id,
                                                                               context.service)

    second_client_inv_eid = steps.InvoiceSteps.get_personal_account_external_id(second_contract_id,
                                                                                context.service)

    balance_product_id = map(steps.SimpleApi.get_balance_service_product_id,
                             [first_product, second_product, product_fee])

    expected_data_list = [steps.SimpleApi.create_expected_tpt_row(context,
                                                                  first_client_id, first_contract_id, first_person_id,
                                                                  trust_payment_id,
                                                                  payment_id, **{'amount': PRICES[0],
                                                                                 'invoice_eid': first_client_inv_eid,
                                                                                 'payment_type': PAYMENT_METHOD.common_type if payment_method else PaymentType.CARD,
                                                                                 'paysys_partner_id': tech_client_id if payment_method == Cash else None,
                                                                                 'yandex_reward': PRICES[0] * COMISSIONS[0] * Decimal('0.0001'),
                                                                                 'service_product_id': balance_product_id[0],
                                                                                 'paysys_type_cc': PAYSYS_TYPE_MAP.get(payment_method, context.tpt_paysys_type_cc),
                                                                                 }),
                          steps.SimpleApi.create_expected_tpt_row(context,
                                                                  second_client_id, second_contract_id,
                                                                  second_person_id,
                                                                  trust_payment_id,
                                                                  payment_id, **{'amount': PRICES[1],
                                                                                 'invoice_eid': second_client_inv_eid,
                                                                                 'payment_type': PAYMENT_METHOD.common_type if payment_method else PaymentType.CARD,
                                                                                 'paysys_partner_id': tech_client_id if payment_method == Cash else None,
                                                                                 'yandex_reward': PRICES[1] * COMISSIONS[1] * Decimal('0.0001'),
                                                                                 'service_product_id': balance_product_id[1],
                                                                                 'paysys_type_cc': PAYSYS_TYPE_MAP.get(payment_method, context.tpt_paysys_type_cc),
                                                                                 }),
                          ]
    if delivery:
        expected_data_list.append(
            steps.SimpleApi.create_expected_tpt_row(context,
                                                    tech_client_id, tech_contract_id, tech_person_id, trust_payment_id,
                                                    payment_id, **{'amount': PRICES[2],
                                                                   'payment_type': PAYMENT_METHOD.common_type if payment_method else PaymentType.CARD,
                                                                   'paysys_partner_id': tech_client_id if payment_method == Cash else None,
                                                                   'yandex_reward': PRICES[2],
                                                                   'service_product_id': balance_product_id[2],
                                                                   'internal': 1,
                                                                   'paysys_type_cc': PAYSYS_TYPE_MAP.get(payment_method, context.tpt_paysys_type_cc)
                                                                   }),)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                     TransactionType.PAYMENT)
    # выгрузка в OEBS
    # for payment in payment_data:
    #     if payment['internal']:
    #        continue
    #
    #     steps.ExportSteps.export_oebs(client_id=payment['client_id'])
    #     steps.ExportSteps.export_oebs(person_id=payment['person_id'])
    #     steps.ExportSteps.export_oebs(contract_id=payment['contract_id'])
    #     collateral_id = steps.ContractSteps.get_contract_collateral_ids(payment['contract_id'])[0]
    #     steps.ExportSteps.export_oebs(collateral_id=collateral_id)
    #     # invoice_id = db.get_invoice_by_eid(payment['invoice_eid'])
    #
    #     data = db.get_invoices_by_contract_id(payment['contract_id'])
    #     for invoice in data:
    #         if invoice['external_id'] == payment['invoice_eid']:
    #             invoice_id = invoice['id']
    #
    #     steps.ExportSteps.export_oebs(invoice_id=invoice_id)
    #     steps.ExportSteps.export_oebs(transaction_id=payment['id'])

    utils.check_that(payment_data, contains_dicts_with_entries(expected_data_list),
                     step=u'Сравним платежи с ожидаемыми.')


@reporter.feature(Features.TRUST, Features.REFUND, Features.MARKET)
@paymethod_and_delivery_parametrize
# @context_parametrize
def test_payment_and_refund(payment_method, delivery, context):
    tech_client_id, tech_person_id, tech_contract_id, \
        first_client_id, first_person_id, first_contract_id, \
        first_product, product_fee = create_ids_for_payments_blue_market(context)

    tech_client_id, tech_person_id, tech_contract_id, \
        second_client_id, second_person_id, second_contract_id, \
        second_product, product_fee = create_ids_for_payments_blue_market(context)

    PAYMENT_METHOD = payment_method(tech_client_id) if payment_method else None

    prices_list = PRICES if delivery else PRICES[:2]
    commission_category_list = COMISSIONS if delivery else COMISSIONS[:2]
    service_order_id_list, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service,
                                                       [first_product, second_product, product_fee],
                                                       commission_category_list=commission_category_list,
                                                       prices_list=prices_list,
                                                       paymethod=PAYMENT_METHOD,
                                                       currency=context.currency)
    steps.CommonPartnerSteps.export_payment(payment_id)

    delta_amount_list = [price / Decimal('2') for price in prices_list]
    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(context.service,
                                                                         service_order_id_list, trust_payment_id,
                                                                         delta_amount_list=delta_amount_list)

    steps.CommonPartnerSteps.export_payment(refund_id)

    first_client_inv_eid = steps.InvoiceSteps.get_personal_account_external_id(first_contract_id,
                                                                               context.service)

    second_client_inv_eid = steps.InvoiceSteps.get_personal_account_external_id(second_contract_id,
                                                                                context.service)

    balance_product_id = map(steps.SimpleApi.get_balance_service_product_id,
                             [first_product, second_product, product_fee])

    payment_data = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                          transaction_type=TransactionType.REFUND)

    # выгрузка в OEBS
    # payments_to_oebs = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
    #                                                                       transaction_type=TransactionType.PAYMENT)
    # payments_to_oebs.extend(payment_data)
    #
    # for payment in payments_to_oebs:
    #     if payment['internal']:
    #        continue
    #
    #     steps.ExportSteps.export_oebs(client_id=payment['client_id'])
    #     steps.ExportSteps.export_oebs(person_id=payment['person_id'])
    #     steps.ExportSteps.export_oebs(contract_id=payment['contract_id'])
    #     collateral_id = steps.ContractSteps.get_contract_collateral_ids(payment['contract_id'])[0]
    #     steps.ExportSteps.export_oebs(collateral_id=collateral_id)
    #     # invoice_id = db.get_invoice_by_eid(payment['invoice_eid'])
    #
    #     data = db.get_invoices_by_contract_id(payment['contract_id'])
    #     for invoice in data:
    #         if invoice['external_id'] == payment['invoice_eid']:
    #             invoice_id = invoice['id']
    #
    #     steps.ExportSteps.export_oebs(invoice_id=invoice_id)
    #     steps.ExportSteps.export_oebs(transaction_id=payment['id'])

    expected_data_list = [steps.SimpleApi.create_expected_tpt_row(context,
                                                                  first_client_id, first_contract_id, first_person_id,
                                                                  trust_payment_id,
                                                                  payment_id,
                                                                  trust_refund_id=trust_refund_id,
                                                                  **{'amount': delta_amount_list[0],
                                                                     'invoice_eid': first_client_inv_eid,
                                                                     'payment_type': PAYMENT_METHOD.common_type if payment_method else PaymentType.CARD,
                                                                     'paysys_partner_id': tech_client_id if payment_method == Cash else None,
                                                                     'service_product_id': balance_product_id[0],
                                                                     'paysys_type_cc': PAYSYS_TYPE_MAP.get(payment_method, context.tpt_paysys_type_cc),
                                                                  }),
                          steps.SimpleApi.create_expected_tpt_row(context,
                                                                  second_client_id, second_contract_id,
                                                                  second_person_id,
                                                                  trust_payment_id,
                                                                  payment_id,
                                                                  trust_refund_id=trust_refund_id,
                                                                  **{'amount': delta_amount_list[1],
                                                                     'invoice_eid': second_client_inv_eid,
                                                                     'payment_type': PAYMENT_METHOD.common_type if payment_method else PaymentType.CARD,
                                                                     'paysys_partner_id': tech_client_id if payment_method == Cash else None,
                                                                     'service_product_id': balance_product_id[1],
                                                                     'paysys_type_cc': PAYSYS_TYPE_MAP.get(payment_method, context.tpt_paysys_type_cc),
                                                                  }),
                          ]
    if delivery:
        expected_data_list.append(
            steps.SimpleApi.create_expected_tpt_row(context,
                                                    tech_client_id, tech_contract_id, tech_person_id, trust_payment_id,
                                                    payment_id, trust_refund_id=trust_refund_id,
                                                    **{'amount': delta_amount_list[2],
                                                       'payment_type': PAYMENT_METHOD.common_type if payment_method else PaymentType.CARD,
                                                       'paysys_partner_id': tech_client_id if payment_method == Cash else None,
                                                       'service_product_id': balance_product_id[2],
                                                       'internal': 1,
                                                       'yandex_reward': delta_amount_list[2],
                                                       'paysys_type_cc': PAYSYS_TYPE_MAP.get(payment_method, context.tpt_paysys_type_cc),
                                                    }),)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data_list),
                     step=u'Сравним платежи с ожидаемыми.')


@reporter.feature(Features.TO_UNIT)
# payment_delay для 610 сервиса. Проставляется дата, по ней актится. Логики с откладыавнием платежа здесь нет. Только дата.
@paymethod_parametrize
def test_set_payout_ready_dt(payment_method):
    PAYMENT_METHOD = payment_method() if payment_method else None
    _, _, _, client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_blue_market()

    service_order_id_list, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context_payments.service,
                                                       [product, product_fee],
                                                       commission_category_list=COMISSIONS[1:3],
                                                       prices_list=PRICES[1:3],
                                                       paymethod=PAYMENT_METHOD)
    steps.CommonPartnerSteps.export_payment(payment_id)

    trust_payment_id = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)[0][
        'trust_payment_id']

    # дёрнем ручку для платежа по доставке
    api.medium().UpdatePayment({'TrustPaymentID': trust_payment_id}, {'PayoutReady': CONTRACT_START_DT})

    dt = steps.CommonPartnerSteps.get_delivered_date(payment_id)

    utils.check_that(dt, equal_to(CONTRACT_START_DT), step=u'Проверим, что дата проставилась.')


# посмотрим, что выгружается правильно.
@reporter.feature(Features.TO_UNIT)
@paymethod_parametrize
def test_set_payout_ready_dt_before_export_payment(payment_method):
    PAYMENT_METHOD = payment_method() if payment_method else None
    _, _, _, client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_blue_market()

    service_order_id_list, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context_payments.service,
                                                       [product, product_fee],
                                                       commission_category_list=COMISSIONS[1:3],
                                                       prices_list=PRICES[1:3],
                                                       paymethod=PAYMENT_METHOD)
    # дёрнем ручку для платежа по доставке
    api.medium().UpdatePayment({'TrustPaymentID': trust_payment_id}, {'PayoutReady': CONTRACT_START_DT})

    steps.CommonPartnerSteps.export_payment(payment_id)

    dt = steps.CommonPartnerSteps.get_delivered_date(payment_id, 'T_THIRDPARTY_TRANSACTIONS', 'payment_id')

    utils.check_that(dt, equal_to(CONTRACT_START_DT), step=u'Проверим, что дата проставилась.')


# Тест на сберовское Спасибо.

# Оплата баллами спасибо в тесте сейчас возможна только конкретными картами, на которые сбер руками зачисляет эти баллы.
# Карты указаны в https://st.yandex-team.ru/PCIDSS-1497
# Если неожиданно оплаты перестали проходить - возможно, просто закончились баллы - спроси в трасте
# (например, у Коли @sage). По той же причине в тестах следует использовать небольшие суммы.
# В будущем траст обещает эмулятор.
# Привязал к дефолтному тестовому пользователю карту с баллами из тикета, для оплаты будет использоваться она.
# Привязка делается так:

# from simpleapi.steps import trust_steps as trust
# from btestlib.data.simpleapi_defaults import DEFAULT_USER
# def test_bind_card_for_new_user():
#     user = DEFAULT_USER
#     card = {
#         'cardholder': 'TEST TEST',
#         'cvn': '126',
#         'expiration_month': '05',
#         'expiration_year': '2020',
#         'descr': 'emulator_card',
#         'type': 'MasterCard',
#         'card_number': '5469380041179762'
#     }
#     linked_cards, trust_payment_id = trust.process_binding(user=user, cards=card)

# Если карты будут меняться - следует отвязывать старые, т.к. у одного юзера может быть привязно не более 5 карт.
# Подсмотреть отвязку карт можно в simpleapi.tests.test_bind_card.TestBindUnbind#test_unbind_card_short_id

# Оплата баллами спасибо в трасте реализована через композитный платеж, дока:
# https://wiki.yandex-team.ru/TRUST/composite-payments/
# В дополнение к доке:
#  В payment_markup спасибная часть имеет код 'spasibo'
#  Необходимо передавать spasibo_order_map (нет в доке) - сейчас создается автоматически в
#   balance.balance_steps.simple_api_steps.SimpleApi#create_multiple_trust_payments
#   (см. комменарий там, если понадобится)
@pytest.mark.parametrize(
    "test_params",
    (
        pytest.param({"paymethod": LinkedCard(card=simpleapi_defaults.SPASIBO_EMULATOR_CARD),
                      "pm_1st": PaymentMethods.CARD,
                      "pm_2nd": PaymentMethods.SPASIBO,
                      "pt_1st": PaysysType.MONEY,
                      "pt_2nd": PaysysType.SPASIBO},
                     id='card+spasibo'),
        pytest.param({"paymethod": None,  # card
                      "pm_1st": PaymentMethods.CARD,
                      "pm_2nd": PaymentMethods.YANDEX_ACCOUNT_WITHDRAW,
                      "pt_1st": PaysysType.MONEY,
                      "pt_2nd": 'yamarketplus'},
                     id='card+plus'),
        pytest.param({"paymethod": None,
                      "pm_1st": PaymentMethods.VIRTUAL_BNPL,  # 'virtual::bnpl',
                      "pm_2nd": PaymentMethods.YANDEX_ACCOUNT_WITHDRAW,
                      "pt_1st": PaysysType.VIRTUAL_BNPL,
                      "pt_2nd": 'yamarketplus'},
                     id='bnpl+plus'),
        pytest.param({"paymethod": None,
                      "pm_1st": PaymentMethods.CREDIT_CESSION,
                      "pm_2nd": PaymentMethods.YANDEX_ACCOUNT_WITHDRAW,
                      "pt_1st": PaysysType.CREDIT_CESSION,
                      "pt_2nd": 'yamarketplus'},
                     id='credit::cession+plus'),
    )
)
def test_blue_market_composite(test_params):
    paymethod, pm_1st, pm_2nd, pt_1st, pt_2nd = [test_params[key]
                                                 for key in ('paymethod', 'pm_1st', 'pm_2nd', 'pt_1st', 'pt_2nd')]

    tech_client_id, tech_person_id, tech_contract_id, client_id, person_id, contract_id, service_product_no_fee, \
        service_product_fee = create_ids_for_payments_blue_market()
    personal_account_eid = steps.InvoiceSteps.get_personal_account_external_id(contract_id, context_payments.service)

    user = None
    markup_2nd_paymethod = pm_2nd.cc
    if pm_2nd == PaymentMethods.YANDEX_ACCOUNT_WITHDRAW:
        user = simpleapi_defaults.USER_NEW_API
        with switch_environment():
            # в маркап также можно передать конкретный кошелёк:
            # account = steps.payments_api_steps.Account.create(context_payments.service, user)
            # markup_2nd_paymethod = account['payment_method_id']
            markup_2nd_paymethod = 'yandex_account'

            steps.SimpleNewApi.create_topup_payment(
                context_payments.service, service_product_no_fee, user=user,
                paymethod=YandexAccountTopup(markup_2nd_paymethod),
                currency=context_payments.payment_currency.iso_code,
                amount=Decimal('30'),
                wait_for_export_from_bs=False,
            )

    _, spendable_person_id, spendable_contract_id, spendable_contract_eid = \
        steps.ContractSteps.create_partner_contract(BLUE_MARKET_SUBSIDY, client_id=client_id, is_offer=1,
                                                    additional_params={'nds': NdsNew.ZERO.nds_id})

    service_product_id_list, service_order_id_list, balance_service_product_ids = \
        get_service_product_and_order_ids(service_product_no_fee, service_product_fee)

    paymethod_markup, _ = create_paymethod_markup(
        service_order_id_list,
        first=pm_1st.cc,
        second=markup_2nd_paymethod)

    service_order_id_list, group_trust_payment_id, _, group_payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context_payments.service,
                                                       service_product_id_list=service_product_id_list,
                                                       service_order_id_list=service_order_id_list,
                                                       commission_category_list=COMMISSION_CATEGORY_LIST,
                                                       prices_list=PRICES_LIST,
                                                       paymethod_markup=paymethod_markup,
                                                       paymethod=paymethod,
                                                       user=user)

    second_payment_id, second_trust_payment_id, first_payment_id, first_trust_payment_id = \
        get_children_payments(group_trust_payment_id,
                              first_paymethod=pm_1st.id,
                              second_paymethod=pm_2nd.id)

    with reporter.step(u'Экспортируем групповой платеж - ожидаем, что экспорт упадет в Skip'):
        result = steps.CommonPartnerSteps.export_payment(group_payment_id)
        utils.check_that(result['state'] == '1', equal_to(True), step=u'Проверим, что групповой платеж в state 1')
        utils.check_that('skipped: payment_method composite' in result['output'], equal_to(True),
                         step=u'Проверим, групповой платеж skipped')

    # TODO расскоментировать, когда траст починит кешбеки
    # with reporter.step(u'Экспортируем платеж spasibo_cashback - ожидаем Skip'):
    #     spasibo_cashback_payment_id, spasibo_cashback_trust_payment_id = get_spasibo_cashback_payments(group_payment_id)
    #     result = steps.CommonPartnerSteps.export_payment(spasibo_cashback_payment_id)
    #     utils.check_that('skipped: payment_method spasibo_cashback' in result['output'], equal_to(True),
    #                      step=u'Проверим, что кешбек Спасибо skipped')

    with reporter.step(u'Экспортируем платеж Спасибо без карточного платежа - ожидаем, что экспорт упадет в Delay'):
        with pytest.raises(XmlRpc.XmlRpcError) as exc_info:
            steps.CommonPartnerSteps.export_payment(second_payment_id)
        utils.check_that(
            'delayed: waiting for appearing composite part in t_thirdparty_transactions completely for transaction'
            in exc_info.value.response, equal_to(True),
            step=u'Проверим, что платеж Спасибо без карточного платежа Delayed')

    with reporter.step(u'Экспортируем платежи без payout_ready_dt'):
        steps.CommonPartnerSteps.export_payment(first_payment_id)
        steps.CommonPartnerSteps.export_payment(second_payment_id)

    expected_data_list_card_payment, expected_data_list_spasibo_payment = \
        create_expected_payment_data(client_id, contract_id, person_id, first_trust_payment_id, first_payment_id,
                                     personal_account_eid, balance_service_product_ids, tech_client_id,
                                     tech_contract_id, tech_person_id, second_trust_payment_id, second_payment_id,
                                     first_payment_type=pm_1st.cc, second_payment_type=pm_2nd.cc,
                                     first_paysys_type=pt_1st, second_paysys_type=pt_2nd)

    expected_data_list_card_payment, expected_data_list_spasibo_payment = \
        add_spasibo_no_fee(client_id, spendable_contract_id, spendable_person_id,
                           second_trust_payment_id, second_payment_id, balance_service_product_ids,
                           expected_data_list_card_payment, expected_data_list_spasibo_payment,
                           action_no_fee=None,
                           payment_type=pm_2nd.cc, paysys_type_cc=pt_2nd)

    card_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(first_payment_id,
                                                                                          TransactionType.PAYMENT)
    spasibo_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(second_payment_id,
                                                                                             TransactionType.PAYMENT)
    utils.check_that(card_payment_data, contains_dicts_with_entries(expected_data_list_card_payment),
                     step=u'Сравним карточные платежи без payout_ready_dt с ожидаемыми.')
    utils.check_that(spasibo_payment_data, contains_dicts_with_entries(expected_data_list_spasibo_payment),
                     step=u'Сравним платежи Спасибо без payout_ready_dt с ожидаемыми.')

    with reporter.step(u'Проставляем payout_ready_dt: передаем групповой платеж - ручка расставляет в дочерние'):
        api.medium().UpdatePayment({'TrustPaymentID': group_trust_payment_id}, {'PayoutReady': CONTRACT_START_DT})

    with reporter.step(u'Экспортируем платежи с payout_ready_dt'):
        steps.CommonPartnerSteps.export_payment(first_payment_id)
        steps.CommonPartnerSteps.export_payment(second_payment_id)

    expected_data_list_card_payment, expected_data_list_spasibo_payment = \
        add_payout_ready_dt(expected_data_list_card_payment, expected_data_list_spasibo_payment, action_no_fee=None,
                            payout_ready_dt=CONTRACT_START_DT)
    card_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(first_payment_id,
                                                                                          TransactionType.PAYMENT)
    spasibo_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(second_payment_id,
                                                                                             TransactionType.PAYMENT)
    utils.check_that(card_payment_data, contains_dicts_with_entries(expected_data_list_card_payment),
                     step=u'Сравним карточные платежи c payout_ready_dt с ожидаемыми.')
    utils.check_that(spasibo_payment_data, contains_dicts_with_entries(expected_data_list_spasibo_payment),
                     step=u'Сравним платежи Спасибо c payout_ready_dt с ожидаемыми.')


def test_blue_market_spasibo_and_refunds():
    tech_client_id, tech_person_id, tech_contract_id, \
    client_id, person_id, contract_id, service_product_no_fee, service_product_fee = \
        create_ids_for_payments_blue_market()
    personal_account_eid = steps.InvoiceSteps.get_personal_account_external_id(contract_id, context_payments.service)

    _, spendable_person_id, spendable_contract_id, spendable_contract_eid = \
        steps.ContractSteps.create_partner_contract(BLUE_MARKET_SUBSIDY, client_id=client_id, is_offer=1,
                                                    additional_params={'nds': NdsNew.ZERO.nds_id})

    service_product_id_list, service_order_id_list, balance_service_product_ids = \
        get_service_product_and_order_ids(service_product_no_fee, service_product_fee)

    paymethod_markup, paymethod = create_paymethod_markup(service_order_id_list)

    service_order_id_list, group_trust_payment_id, _, group_payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context_payments.service,
                                                       service_product_id_list=service_product_id_list,
                                                       service_order_id_list=service_order_id_list,
                                                       commission_category_list=COMMISSION_CATEGORY_LIST,
                                                       prices_list=PRICES_LIST,
                                                       paymethod_markup=paymethod_markup,
                                                       paymethod=paymethod)

    spasibo_payment_id, spasibo_trust_payment_id, card_payment_id, card_trust_payment_id = \
        get_children_payments(group_trust_payment_id)

    # TODO расскоментировать, когда траст починит кешбеки
    # with reporter.step(u'Экспортируем платеж spasibo_cashback - ожидаем Skip'):
    #     spasibo_cashback_payment_id, spasibo_cashback_trust_payment_id = get_spasibo_cashback_payments(group_payment_id)
    #     result = steps.CommonPartnerSteps.export_payment(spasibo_cashback_payment_id)
    #     utils.check_that('skipped: payment_method spasibo_cashback' in result['output'], equal_to(True),
    #                      step=u'Проверим, что кешбек Спасибо skipped')

    with reporter.step(u'Экспортируем платежи без payout_ready_dt'):
        steps.CommonPartnerSteps.export_payment(card_payment_id)
        steps.CommonPartnerSteps.export_payment(spasibo_payment_id)

    with reporter.step(u'Проставляем payout_ready_dt: передаем групповой платеж - ручка расставляет в дочерние'):
        api.medium().UpdatePayment({'TrustPaymentID': group_trust_payment_id}, {'PayoutReady': CONTRACT_START_DT})

    with reporter.step(u'Экспортируем платежи с payout_ready_dt'):
        steps.CommonPartnerSteps.export_payment(card_payment_id)
        steps.CommonPartnerSteps.export_payment(spasibo_payment_id)

    paymethod_markup_refund = {
        service_order_id_list[0]: {'card': str(PRICE_CARD_NO_FEE), 'spasibo': str(PRICE_SPASIBO_NO_FEE)}}
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(context_payments.service, service_order_id_list[0],
                                                               group_trust_payment_id,
                                                               delta_amount=PRICE_CARD_NO_FEE + PRICE_SPASIBO_NO_FEE,
                                                               paymethod_markup=paymethod_markup_refund)

    card_refund_id, card_trust_refund_id = wait_refund_for(card_payment_id)
    spasibo_refund_id, spasibo_trust_refund_id = wait_refund_for(spasibo_payment_id)

    # TODO расскоментировать, когда траст починит кешбеки
    # with reporter.step(u'Экспортируем рефанд spasibo_cashback - ожидаем Skip'):
    #     spasibo_cashback_refund_id, spasibo_cashback_trust_refund_id = \
    #         get_spasibo_cashback_refunds(spasibo_cashback_payment_id)
    #     result = steps.CommonPartnerSteps.export_payment(spasibo_cashback_refund_id)
    #     utils.check_that('skipped: payment_method spasibo_cashback' in result['output'], equal_to(True),
    #                      step=u'Проверим, что кешбек Спасибо skipped')

    steps.CommonPartnerSteps.export_payment(refund_id)
    steps.CommonPartnerSteps.export_payment(card_refund_id)
    steps.CommonPartnerSteps.export_payment(spasibo_refund_id)

    expected_data_list_card_payment, expected_data_list_spasibo_payment = \
        create_expected_payment_data(client_id, contract_id, person_id, card_trust_payment_id, card_payment_id,
                                     personal_account_eid, balance_service_product_ids, tech_client_id,
                                     tech_contract_id, tech_person_id, spasibo_trust_payment_id, spasibo_payment_id)

    expected_data_list_card_payment, expected_data_list_spasibo_payment = \
        add_payout_ready_dt_and_spasibo_no_fee(client_id, spendable_contract_id, spendable_person_id,
                                               spasibo_trust_payment_id, spasibo_payment_id,
                                               balance_service_product_ids, expected_data_list_card_payment,
                                               expected_data_list_spasibo_payment)

    expected_data_list_card_refund = [
        steps.SimpleApi.create_expected_tpt_row(context_payments,
                                                client_id, contract_id, person_id,
                                                card_trust_payment_id,
                                                card_payment_id,
                                                card_trust_refund_id, **{'amount': PRICE_CARD_NO_FEE,
                                                                         'invoice_eid': personal_account_eid,
                                                                         'service_product_id': balance_service_product_ids[0],
                                                                         'payout_ready_dt': CONTRACT_START_DT})]

    expected_data_list_spasibo_refund = [
        steps.SimpleApi.create_expected_tpt_row(context_spasibo,
                                                client_id, spendable_contract_id, spendable_person_id,
                                                spasibo_trust_payment_id,
                                                spasibo_payment_id,
                                                spasibo_trust_refund_id, **{'amount': PRICE_SPASIBO_NO_FEE,
                                                                            'service_product_id': balance_service_product_ids[0]
                                                                            })]

    card_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(card_payment_id,
                                                                                          TransactionType.PAYMENT)
    card_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(card_payment_id,
                                                                                         TransactionType.REFUND)
    spasibo_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(spasibo_payment_id,
                                                                                             TransactionType.PAYMENT)
    spasibo_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(spasibo_payment_id,
                                                                                            TransactionType.REFUND)
    utils.check_that(card_payment_data, contains_dicts_with_entries(expected_data_list_card_payment),
                     step=u'Сравним карточные платежи с ожидаемыми.')
    utils.check_that(card_refund_data, contains_dicts_with_entries(expected_data_list_card_refund),
                     step=u'Сравним карточные рефанды с ожидаемыми.')
    utils.check_that(spasibo_payment_data, contains_dicts_with_entries(expected_data_list_spasibo_payment),
                     step=u'Сравним платежи Спасибо с ожидаемыми.')
    utils.check_that(spasibo_refund_data, contains_dicts_with_entries(expected_data_list_spasibo_refund),
                     step=u'Сравним рефанды Спасибо с ожидаемыми.')


@pytest.mark.parametrize("action_no_fee, action_fee", [
    pytest.param('clear', 'clear', id='clear:clear'),
    pytest.param('cancel', 'clear', id='cancel:clear'),
    pytest.param('clear', 'cancel', id='clear:cancel')
])
def test_blue_market_spasibo_and_reversal(action_no_fee, action_fee):
    tech_client_id, tech_person_id, tech_contract_id, \
    client_id, person_id, contract_id, service_product_no_fee, service_product_fee = create_ids_for_payments_blue_market()
    personal_account_eid = steps.InvoiceSteps.get_personal_account_external_id(contract_id, context_payments.service)

    _, spendable_person_id, spendable_contract_id, \
    spendable_contract_eid = steps.ContractSteps.create_partner_contract(BLUE_MARKET_SUBSIDY, client_id=client_id,
                                                                         is_offer=1,
                                                                         additional_params={'nds': NdsNew.ZERO.nds_id})

    service_product_id_list, service_order_id_list, balance_service_product_ids = \
        get_service_product_and_order_ids(service_product_no_fee, service_product_fee)

    paymethod_markup, paymethod = create_paymethod_markup(service_order_id_list)

    service_order_id_list, group_trust_payment_id, _, _ = \
        steps.SimpleApi.create_multiple_trust_payments(context_payments.service,
                                                       service_product_id_list=service_product_id_list,
                                                       service_order_id_list=service_order_id_list,
                                                       commission_category_list=COMMISSION_CATEGORY_LIST,
                                                       prices_list=PRICES_LIST,
                                                       paymethod_markup=paymethod_markup,
                                                       paymethod=paymethod,
                                                       need_postauthorize=False,
                                                       wait_for_export_from_bs=False)

    # частичная отмена
    action_list = [action_no_fee, action_fee]
    paymethod_markup, _ = create_paymethod_markup(service_order_id_list, False)

    amount_list = [PRICE_CARD_RESIZE_NO_FEE + PRICE_SPASIBO_RESIZE_NO_FEE,
                   PRICE_CARD_RESIZE_FEE + PRICE_SPASIBO_RESIZE_FEE]

    steps.SimpleApi.postauthorize(context_payments.service, group_trust_payment_id, service_order_id_list,
                                  actions=action_list, amounts=amount_list, paymethod_markup=paymethod_markup)

    steps.SimpleApi.wait_for_payment(group_trust_payment_id)
    spasibo_payment_id, spasibo_trust_payment_id, card_payment_id, card_trust_payment_id = \
        get_children_payments(group_trust_payment_id)

    card_refund_id, card_trust_refund_id = wait_refund_for(card_payment_id)
    spasibo_refund_id, spasibo_trust_refund_id = wait_refund_for(spasibo_payment_id)

    # TODO расскоментировать, когда траст починит кешбеки
    # with reporter.step(u'Экспортируем платеж spasibo_cashback - ожидаем Skip'):
    #     spasibo_cashback_payment_id, spasibo_cashback_trust_payment_id = get_spasibo_cashback_payments(group_payment_id)
    #     result = steps.CommonPartnerSteps.export_payment(spasibo_cashback_payment_id)
    #     utils.check_that('skipped: payment_method spasibo_cashback' in result['output'], equal_to(True),
    #                      step=u'Проверим, что кешбек Спасибо skipped')

    # TODO Включить когда Коля Ульянов профиксит баг с отсутствием рефандов/реверсалов по кешбекам спасибо.
    # with reporter.step(u'Экспортируем рефанд spasibo_cashback - ожидаем Skip'):
    #     spasibo_cashback_refund_id, spasibo_cashback_trust_refund_id = \
    #         get_spasibo_cashback_refunds(spasibo_cashback_payment_id)
    #     result = steps.CommonPartnerSteps.export_payment(spasibo_cashback_refund_id)
    #     utils.check_that('skipped: payment_method spasibo_cashback' in result['output'], equal_to(True),
    #                      step=u'Проверим, что кешбек Спасибо skipped')

    with reporter.step(u'Экспортируем платежи без payout_ready_dt'):
        steps.CommonPartnerSteps.export_payment(card_payment_id)
        steps.CommonPartnerSteps.export_payment(card_refund_id)
        steps.CommonPartnerSteps.export_payment(spasibo_payment_id)
        steps.CommonPartnerSteps.export_payment(spasibo_refund_id)

    expected_data_list_card_payment, expected_data_list_spasibo_payment = \
        create_expected_payment_data(client_id, contract_id, person_id, card_trust_payment_id, card_payment_id,
                                     personal_account_eid, balance_service_product_ids, tech_client_id,
                                     tech_contract_id, tech_person_id, spasibo_trust_payment_id, spasibo_payment_id,
                                     action_no_fee=action_no_fee, action_fee=action_fee)

    expected_data_list_card_payment, expected_data_list_spasibo_payment = \
        add_spasibo_no_fee(client_id, spendable_contract_id, spendable_person_id,
                           spasibo_trust_payment_id, spasibo_payment_id, balance_service_product_ids,
                           expected_data_list_card_payment, expected_data_list_spasibo_payment,
                           action_no_fee=action_no_fee)
    card_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(card_payment_id)
    utils.check_that(card_payment_data, contains_dicts_with_entries(expected_data_list_card_payment),
                     step=u'Сравним карточные платежи без payout_ready_dt с ожидаемыми.')

    spasibo_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(spasibo_payment_id)
    utils.check_that(spasibo_payment_data, contains_dicts_with_entries(expected_data_list_spasibo_payment),
                     step=u'Сравним платежи Спасибо без payout_ready_dt с ожидаемыми.')

    with reporter.step(u'Протсавляем payout_ready_dt: передаем групповой платеж - ручка расставляет в дочерние'):
        api.medium().UpdatePayment({'TrustPaymentID': group_trust_payment_id}, {'PayoutReady': CONTRACT_START_DT})

    with reporter.step(u'Экспортируем платежи с payout_ready_dt'):
        steps.CommonPartnerSteps.export_payment(card_payment_id)
        steps.CommonPartnerSteps.export_payment(card_refund_id)
        steps.CommonPartnerSteps.export_payment(spasibo_payment_id)
        steps.CommonPartnerSteps.export_payment(spasibo_refund_id)

    expected_data_list_card_payment, expected_data_list_spasibo_payment = \
        add_payout_ready_dt(expected_data_list_card_payment, expected_data_list_spasibo_payment,
                            action_no_fee=action_no_fee, payout_ready_dt=CONTRACT_START_DT)

    card_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(card_payment_id,
                                                                                          transaction_type=None)
    utils.check_that(card_payment_data, contains_dicts_with_entries(expected_data_list_card_payment),
                     step=u'Сравним карточные платежи c payout_ready_dt с ожидаемыми.')

    spasibo_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(spasibo_payment_id,
                                                                                             transaction_type=None)
    utils.check_that(spasibo_payment_data, contains_dicts_with_entries(expected_data_list_spasibo_payment),
                     step=u'Сравним платежи Спасибо c payout_ready_dt с ожидаемыми.')


# почему-то в случае полного реверсала спасибная часть приходит с postauth_amount = 0, рефанда нет
def test_blue_market_spasibo_full_reversal():
    tech_client_id, tech_person_id, tech_contract_id, client_id, person_id, contract_id, \
        service_product_no_fee, service_product_fee = create_ids_for_payments_blue_market()

    _, spendable_person_id, spendable_contract_id, spendable_contract_eid = \
        steps.ContractSteps.create_partner_contract(BLUE_MARKET_SUBSIDY, client_id=client_id,
                                                    is_offer=1, additional_params={'nds': NdsNew.ZERO.nds_id})

    service_product_id_list, service_order_id_list, balance_service_product_ids = \
        get_service_product_and_order_ids(service_product_no_fee, service_product_fee)

    paymethod_markup, paymethod = create_paymethod_markup(service_order_id_list)

    service_order_id_list, group_trust_payment_id, _, _ = \
        steps.SimpleApi.create_multiple_trust_payments(context_payments.service,
                                                       service_product_id_list=service_product_id_list,
                                                       service_order_id_list=service_order_id_list,
                                                       commission_category_list=COMMISSION_CATEGORY_LIST,
                                                       prices_list=PRICES_LIST,
                                                       paymethod_markup=paymethod_markup,
                                                       paymethod=paymethod,
                                                       need_postauthorize=False,
                                                       wait_for_export_from_bs=False)

    # полная отмена
    action_list = ['cancel', 'cancel']
    steps.SimpleApi.postauthorize(context_payments.service, group_trust_payment_id, service_order_id_list,
                                  actions=action_list)

    group_payment_id = steps.SimpleApi.wait_for_payment(group_trust_payment_id)

    spasibo_payment_id, spasibo_trust_payment_id, card_payment_id, card_trust_payment_id = \
        get_children_payments(group_trust_payment_id)

    card_refund_id, card_trust_refund_id = wait_refund_for(card_payment_id)
    spasibo_refund_id, spasibo_trust_refund_id = wait_refund_for(spasibo_payment_id)

    with reporter.step(u'Экспортируем платежи'):
        with reporter.step(u'Экспортируем карточный платеж - ожидаем skipped'):
            result = steps.CommonPartnerSteps.export_payment(card_payment_id)
            utils.check_that('skipped: payment has been completely cancelled' in result['output'], equal_to(True),
                             step=u'Проверим, карточный платеж skipped')
            result = steps.CommonPartnerSteps.export_payment(card_refund_id)
            utils.check_that('skipped: reversal is not exportable' in result['output'], equal_to(True),
                             step=u'Проверим, карточный рефанд skipped')
            result = steps.CommonPartnerSteps.export_payment(spasibo_payment_id)
            utils.check_that('skipped: Money part of composite payment is cancelled or reversed' in result['output'], equal_to(True),
                             step=u'Проверим, спасибный платеж skipped')
            result = steps.CommonPartnerSteps.export_payment(spasibo_refund_id)
            utils.check_that('skipped: reversal is not exportable' in result['output'], equal_to(True),
                             step=u'Проверим, спасибный рефанд skipped')

    with reporter.step(u'Экспортируем платеж spasibo_cashback - ожидаем Skip'):
        spasibo_cashback_payment_id, spasibo_cashback_trust_payment_id = get_spasibo_cashback_payments(group_payment_id)
        result = steps.CommonPartnerSteps.export_payment(spasibo_cashback_payment_id)
        utils.check_that('skipped: payment_method spasibo_cashback' in result['output'], equal_to(True),
                         step=u'Проверим, что кешбек Спасибо skipped')

    with reporter.step(u'Экспортируем рефанд spasibo_cashback - ожидаем Skip'):
        spasibo_cashback_refund_id, spasibo_cashback_trust_refund_id = \
            get_spasibo_cashback_refunds(spasibo_cashback_payment_id)
        result = steps.CommonPartnerSteps.export_payment(spasibo_cashback_refund_id)
        utils.check_that('skipped: payment_method spasibo_cashback' in result['output'], equal_to(True),
                         step=u'Проверим, что кешбек Спасибо skipped')


def test_through_yt_flag():
    client_id, person_id, contract_id, _, product, product_fee = create_contract_for_partner(None)
    _, product, product_fee = steps.SimpleApi.create_partner_product_and_fee(context_payments.service)

    service_order_id_list, trust_payment_id, _, payment_id = steps.SimpleApi.create_multiple_trust_payments(
        context_payments.service,
        [product, product_fee],
        commission_category_list=COMISSIONS[1:3],
        prices_list=PRICES[1:3],
        developer_payload_basket=json.dumps({'ProcessThroughYt': 1})
    )
    check_payment_fail_in_flag(payment_id)

    return service_order_id_list, trust_payment_id, PRICES[1:3]  # to refund test


def test_through_yt_flag_refund():
    service_order_id_list, trust_payment_id, prices = test_through_yt_flag()
    delta_amount_list = [price / Decimal('2') for price in prices]

    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(context_payments.service,
                                                                         service_order_id_list, trust_payment_id,
                                                                         delta_amount_list=delta_amount_list)
    check_payment_fail_in_flag(refund_id)

# SidePayment тесты вынесены в файл test_market_sidepayments.py
