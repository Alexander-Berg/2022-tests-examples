# -*- coding: utf-8 -*-
__author__ = 'alshkit'

from datetime import datetime
from decimal import Decimal
import uuid

import pytest
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Services, Firms, Managers, Currencies, TransactionType, PaymentType, ContractSubtype, \
    NdsNew, PaysysType
from btestlib.data import simpleapi_defaults
from btestlib.data.defaults import SpendableContractDefaults as SpendableDefParams
from btestlib.matchers import contains_dicts_with_entries, equal_to
from btestlib.utils import XmlRpc
from simpleapi.common.payment_methods import Cash, LinkedCard
from btestlib.data.partner_contexts import BLUE_MARKET_PAYMENTS

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
PREVIUS_MONTH_START_DT, PREVIUS_MONTH_END_DT = utils.Date.previous_month_first_and_last_days(datetime.today())
COMISSIONS = [Decimal('200'), Decimal('300'), Decimal('0')]
PRICES = [Decimal('1000'), Decimal('500'), Decimal('250')]

SPASIBO_PAYMENT_METHOD_ID = 1527
CARD_PAYMENT_METHOD_ID = 1101

context = BLUE_MARKET_PAYMENTS

expected_payment_params = {'amount_fee': None,
                           'client_amount': None,
                           'internal': None,
                           'invoice_commission_sum': None,
                           'iso_currency': 'RUB',
                           'oebs_org_id': context.firm.oebs_org_id,
                           'partner_currency': context.currency.char_code,
                           'partner_iso_currency': context.currency.iso_code,
                           'paysys_partner_id': None,
                           'paysys_type_cc': 'yamoney',
                           'row_paysys_commission_sum': None,
                           'service_id': context.service.id}

# utils
def create_contract_for_partner(start_dt):
    with reporter.step(u'Создаем договор для клиента-партнера'):
        # создаем клиента-партнера
        client_id, product, product_fee = steps.SimpleApi.create_partner_product_and_fee(context.service)
        person_id = steps.PersonSteps.create(client_id, context.person_type.code, {'kpp': '234567891'})

        # contract_params = utils.copy_and_update_dict(common_params, {'client_id': client_id,
        #                                                              'person_id': person_id,
        #                                                              'start_dt': start_dt})

        # создаем договор для клиента-партнера
        _, _, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(context,
                                                                                      client_id=client_id,
                                                                                      person_id=person_id,
                                                                                      is_offer=1,
                                                                                      additional_params={
                                                                                          'start_dt': start_dt
                                                                                      })
        # contract_id, contract_eid = steps.ContractSteps.create_offer(contract_params)

        return client_id, person_id, contract_id, contract_eid, product, product_fee


def create_spendable_contract_for_partner(start_dt=None, client_id=None, nds=NdsNew.ZERO):
    if not client_id:
        client_id = steps.ClientSteps.create()
    spendable_contract_id, spendable_contract_eid, partner_person_id = \
        steps.ContractSteps.create_person_and_offer_with_additional_params(
            client_id,
            SpendableDefParams.BLUE_MARKET_SUBSIDY,
            is_spendable=1,
            additional_params={'start_dt': start_dt, 'nds': nds})
    return client_id, spendable_contract_id, spendable_contract_eid, partner_person_id


def create_ids_for_payments_blue_market(start_dt=None):
    tech_client_id, tech_person_id, tech_contract_id = steps.CommonPartnerSteps.get_tech_ids(
        context.service)

    client_id, person_id, contract_id, _, product, product_fee = create_contract_for_partner(start_dt)

    return tech_client_id, tech_person_id, tech_contract_id, client_id, person_id, contract_id, product, product_fee


def find_refund_by_orig_payment_id(orig_payment_id):
    with reporter.step(u"Получаем payment_id рефанда для платежа: {}".format(orig_payment_id)):
        query = "SELECT id, trust_refund_id FROM t_refund " \
                "WHERE orig_payment_id=:orig_payment_id"
        params = {'orig_payment_id': orig_payment_id}
        res = db.balance().execute(query, params)
        return res[0]['id'], res[0]['trust_refund_id']
# tests================================================================================================================

# @pytest.mark.no_parallel('blue_market', write=False)
@reporter.feature(Features.TRUST, Features.PAYMENT, Features.MARKET)
@pytest.mark.parametrize('payment_method', [pytest.mark.smoke('Cash'), None])
def test_payments_with_delivery(payment_method):
    tech_client_id, tech_person_id, tech_contract_id, \
    first_client_id, first_person_id, first_contract_id, first_product, product_fee = create_ids_for_payments_blue_market()

    tech_client_id, tech_person_id, tech_contract_id, \
    second_client_id, second_person_id, second_contract_id, second_product, product_fee = create_ids_for_payments_blue_market()

    PAYMENT_METHOD = Cash(tech_client_id) if payment_method else None

    service_order_id_list, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service,
                                                       [first_product, second_product, product_fee],
                                                       commission_category_list=COMISSIONS,
                                                       prices_list=PRICES,
                                                       paymethod=PAYMENT_METHOD)
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
                                                                                 'payment_type': PaymentType.CASH if payment_method else PaymentType.CARD,
                                                                                 'paysys_partner_id': tech_client_id if payment_method else None,
                                                                                 'yandex_reward': PRICES[0] * COMISSIONS[0] * Decimal('0.0001'),
                                                                                 'service_product_id': balance_product_id[0],
                                                                                 'paysys_type_cc': PaysysType.MONEY}),
                          steps.SimpleApi.create_expected_tpt_row(context,
                                                                  second_client_id, second_contract_id,
                                                                  second_person_id,
                                                                  trust_payment_id,
                                                                  payment_id, **{'amount': PRICES[1],
                                                                                 'invoice_eid': second_client_inv_eid,
                                                                                 'payment_type': PaymentType.CASH if payment_method else PaymentType.CARD,
                                                                                 'paysys_partner_id': tech_client_id if payment_method else None,
                                                                                 'yandex_reward': PRICES[1] * COMISSIONS[1] * Decimal('0.0001'),
                                                                                 'service_product_id': balance_product_id[1],
                                                                                 'paysys_type_cc': PaysysType.MONEY}),
                          steps.SimpleApi.create_expected_tpt_row(context,
                                                                  tech_client_id, tech_contract_id,
                                                                  tech_person_id,
                                                                  trust_payment_id,
                                                                  payment_id, **{'amount': PRICES[2],
                                                                                 'payment_type': PaymentType.CASH if payment_method else PaymentType.CARD,
                                                                                 'paysys_partner_id': tech_client_id if payment_method else None,
                                                                                 'yandex_reward': PRICES[2],
                                                                                 'service_product_id': balance_product_id[2],
                                                                                 'paysys_type_cc': PaysysType.MONEY,
                                                                                 'internal': 1}),
                          ]
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                     TransactionType.PAYMENT)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data_list),
                     step=u'Сравним платежи с ожидаемыми.')


# @pytest.mark.no_parallel('blue_market', write=False)
@pytest.mark.parametrize('payment_method', ['Cash', None])
def test_payments_without_delivery(payment_method):
    tech_client_id, tech_person_id, tech_contract_id, \
    first_client_id, first_person_id, first_contract_id, first_product, product_fee = create_ids_for_payments_blue_market()

    tech_client_id, tech_person_id, tech_contract_id, \
    second_client_id, second_person_id, second_contract_id, second_product, product_fee = create_ids_for_payments_blue_market()

    PAYMENT_METHOD = Cash(tech_client_id) if payment_method else None

    service_order_id_list, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service,
                                                       [first_product, second_product],
                                                       commission_category_list=COMISSIONS[:2],
                                                       prices_list=PRICES[:2],
                                                       paymethod=PAYMENT_METHOD)
    steps.CommonPartnerSteps.export_payment(payment_id)

    first_client_inv_eid = steps.InvoiceSteps.get_personal_account_external_id(first_contract_id,
                                                                               context.service)

    second_client_inv_eid = steps.InvoiceSteps.get_personal_account_external_id(second_contract_id,
                                                                                context.service)

    balance_product_id = map(steps.SimpleApi.get_balance_service_product_id, [first_product, second_product])

    expected_data_list = [steps.SimpleApi.create_expected_tpt_row(context,
                                                                  first_client_id, first_contract_id, first_person_id,
                                                                  trust_payment_id,
                                                                  payment_id, **{'amount': PRICES[0],
                                                                                 'invoice_eid': first_client_inv_eid,
                                                                                 'payment_type': PaymentType.CASH if payment_method else PaymentType.CARD,
                                                                                 'paysys_partner_id': tech_client_id if payment_method else None,
                                                                                 'yandex_reward': PRICES[0] * COMISSIONS[0] * Decimal('0.0001'),
                                                                                 'service_product_id': balance_product_id[0],
                                                                                 'paysys_type_cc': PaysysType.MONEY}),
                          steps.SimpleApi.create_expected_tpt_row(context,
                                                                  second_client_id, second_contract_id,
                                                                  second_person_id, trust_payment_id,
                                                                  payment_id, **{'amount': PRICES[1],
                                                                                 'invoice_eid': second_client_inv_eid,
                                                                                 'payment_type': PaymentType.CASH if payment_method else PaymentType.CARD,
                                                                                 'paysys_partner_id': tech_client_id if payment_method else None,
                                                                                 'yandex_reward': PRICES[1] * COMISSIONS[1] * Decimal('0.0001'),
                                                                                 'service_product_id': balance_product_id[1],
                                                                                 'paysys_type_cc': PaysysType.MONEY})
                          ]

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                     TransactionType.PAYMENT)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data_list),
                     step=u'Сравним платежи с ожидаемыми.')


# payment_delay для 610 сервиса. Проставляется дата, по ней актится. Логики с откладыавнием платежа здесь нет. Только дата.
def test_set_payout_ready_dt():
    _, _, _, client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_blue_market()

    service_order_id_list, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service,
                                                       [product, product_fee],
                                                       commission_category_list=COMISSIONS[1:3],
                                                       prices_list=PRICES[1:3],
                                                       paymethod=None)
    steps.CommonPartnerSteps.export_payment(payment_id)

    trust_payment_id = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)[0][
        'trust_payment_id']

    # дёрнем ручку для платежа по доставке
    api.medium().UpdatePayment({'TrustPaymentID': trust_payment_id}, {'PayoutReady': CONTRACT_START_DT})

    dt = steps.CommonPartnerSteps.get_delivered_date(payment_id)

    utils.check_that(dt, equal_to(CONTRACT_START_DT), step=u'Проверим, что дата проставилась.')


# посмотрим, что выгружается правильно.
def test_set_payout_ready_dt_before_export_payment():
    _, _, _, client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_blue_market()

    service_order_id_list, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service,
                                                       [product, product_fee],
                                                       commission_category_list=COMISSIONS[1:3],
                                                       prices_list=PRICES[1:3])
    # дёрнем ручку для платежа по доставке
    api.medium().UpdatePayment({'TrustPaymentID': trust_payment_id}, {'PayoutReady': CONTRACT_START_DT})

    steps.CommonPartnerSteps.export_payment(payment_id)

    dt = steps.CommonPartnerSteps.get_delivered_date(payment_id, 'T_THIRDPARTY_TRANSACTIONS', 'payment_id')

    utils.check_that(dt, equal_to(CONTRACT_START_DT), step=u'Проверим, что дата проставилась.')


# Тест на сберовское Спасибо.

# Оплата баллами спасибо в тесте сейчас возможна только конкретными картами, на которые сбер руками зачисляет эти баллы.
# Карты указаны в https://st.yandex-team.ru/PCIDSS-1497
# Если неожиданно оплаты перестали проходить - возможно, просто закончились баллы - сроси в трасте
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
def test_blue_market_spasibo_and_refunds():
    tech_client_id, tech_person_id, tech_contract_id, \
    client_id, person_id, contract_id, service_product_no_fee, service_product_fee = create_ids_for_payments_blue_market()
    personal_account_eid = steps.InvoiceSteps.get_personal_account_external_id(contract_id, context.service)

    _, spendable_contract_id, spendable_contract_eid, spendable_person_id = \
        create_spendable_contract_for_partner(client_id=client_id)

    # будет 2 платежа, объединенные групповым (карточный и спасибный). С каждого платежа - зачисление на 2 заказа.
    # Т.е. на каждый заказ будет зачисление с обоих платежей.

    PRICE_CARD_NO_FEE = Decimal('23')
    PRICE_CARD_FEE = Decimal('7')
    PRICE_SPASIBO_FEE = Decimal('15')
    PRICE_SPASIBO_NO_FEE = Decimal('5')

    COMMISSION_CATEGORY_NO_FEE = Decimal('300')
    COMMISSION_CATEGORY_FEE = Decimal('0')

    service_product_id_list = [service_product_no_fee, service_product_fee]

    balance_service_product_id_no_fee = steps.SimpleApi.get_balance_service_product_id(service_product_no_fee)
    balance_service_product_id_fee = steps.SimpleApi.get_balance_service_product_id(service_product_fee)

    prices_list = [PRICE_CARD_NO_FEE + PRICE_SPASIBO_NO_FEE, PRICE_CARD_FEE + PRICE_SPASIBO_FEE]
    commission_category_list = [COMMISSION_CATEGORY_NO_FEE, COMMISSION_CATEGORY_FEE]

    # Генерим service_order_id для заказов, чтобы передать распределение сумм по ним в paymethod_markup
    service_order_id_product, service_order_id_product_fee = uuid.uuid1().hex, uuid.uuid1().hex
    service_order_id_list = [service_order_id_product, service_order_id_product_fee]

    # распределение сумм по заказам
    paymethod_markup = {service_order_id_product: {'card': str(PRICE_CARD_NO_FEE), 'spasibo': str(PRICE_SPASIBO_NO_FEE)},
                        service_order_id_product_fee: {'card': str(PRICE_CARD_FEE), 'spasibo': str(PRICE_SPASIBO_FEE)}}

    paymethod = LinkedCard(card=simpleapi_defaults.SPASIBO_EMULATOR_CARD)

    service_order_id_list, group_trust_payment_id, _, group_payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service,
                                                       service_product_id_list=service_product_id_list,
                                                       service_order_id_list=service_order_id_list,
                                                       commission_category_list=commission_category_list,
                                                       prices_list=prices_list,
                                                       paymethod_markup=paymethod_markup,
                                                       paymethod=paymethod)

    children_payments = steps.CommonPartnerSteps.get_children_trust_group_payments(group_trust_payment_id)
    utils.check_that(len(children_payments), equal_to(2), step=u'Проверим, что создалось 2 дочерних платежа')
    spasibo_payment, = filter(lambda r: r['payment_method_id'] == SPASIBO_PAYMENT_METHOD_ID, children_payments)
    spasibo_payment_id, spasibo_trust_payment_id = spasibo_payment['payment_id'], spasibo_payment['trust_payment_id']
    card_payment, = filter(lambda r: r['payment_method_id'] == CARD_PAYMENT_METHOD_ID, children_payments)
    card_payment_id, card_trust_payment_id = card_payment['payment_id'], card_payment['trust_payment_id']

    with reporter.step(u'Экспортируем групповой платеж - ожидаем, что экспорт упадет в Skip'):
        result = steps.CommonPartnerSteps.export_payment(group_payment_id)
        utils.check_that(result['state'] == '1', equal_to(True), step=u'Проверим, что групповой платеж в state 1')
        utils.check_that('skipped: transaction is trust group payment' in result['output'], equal_to(True),
                         step=u'Проверим, групповой платеж skipped')

    with reporter.step(u'Экспортируем платеж Спасибо без карточного платежа - ожидаем, что экспорт упадет в Delay'):
        with pytest.raises(XmlRpc.XmlRpcError) as exc_info:
            result = steps.CommonPartnerSteps.export_payment(spasibo_payment_id)
        utils.check_that('delayed: waiting for appearing composite part in t_thirdparty_transactions completely for transaction'
                         in exc_info.value.response, equal_to(True),
                         step=u'Проверим, что платеж Спасибо без карточного платежа Delayed')

    with reporter.step(u'Экспортируем платежи без payout_ready_dt'):
        steps.CommonPartnerSteps.export_payment(card_payment_id)
        steps.CommonPartnerSteps.export_payment(spasibo_payment_id)

    expected_data_list_card_payment = [
        # no fee product
        steps.SimpleApi.create_expected_tpt_row(context,
                                                client_id, contract_id, person_id,
                                                card_trust_payment_id,
                                                card_payment_id, **{'amount': PRICE_CARD_NO_FEE,
                                                               'invoice_eid': personal_account_eid,
                                                               'payment_type': PaymentType.CARD,
                                                               'yandex_reward': PRICE_CARD_NO_FEE * COMMISSION_CATEGORY_NO_FEE * Decimal('0.0001'),
                                                               'service_product_id': balance_service_product_id_no_fee,
                                                               'paysys_type_cc': 'yamoney'}),

        # fee product
        steps.SimpleApi.create_expected_tpt_row(context,
                                                tech_client_id, tech_contract_id, tech_person_id,
                                                card_trust_payment_id,
                                                card_payment_id, **{'amount': PRICE_CARD_FEE,
                                                                    'payment_type': PaymentType.CARD,
                                                                    'yandex_reward': PRICE_CARD_FEE,
                                                                    'service_product_id': balance_service_product_id_fee,
                                                                    'paysys_type_cc': 'yamoney',
                                                                    'internal': 1}),
        ]

    expected_data_list_spasibo_payment = [
        # fee product
        steps.SimpleApi.create_expected_tpt_row(context,
                                                tech_client_id, tech_contract_id, tech_person_id,
                                                spasibo_trust_payment_id,
                                                spasibo_payment_id, **{'amount': PRICE_SPASIBO_FEE,
                                                                    'payment_type': PaymentType.SPASIBO,
                                                                    'yandex_reward': PRICE_SPASIBO_FEE,
                                                                    'service_product_id': balance_service_product_id_fee,
                                                                    'paysys_type_cc': 'yamoney',
                                                                    'internal': 1}),
        ]

    card_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(card_payment_id,
                                                                                          TransactionType.PAYMENT)
    utils.check_that(card_payment_data, contains_dicts_with_entries(expected_data_list_card_payment),
                     step=u'Сравним карточные платежи без payout_ready_dt с ожидаемыми.')

    spasibo_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(spasibo_payment_id,
                                                                                             TransactionType.PAYMENT)
    utils.check_that(spasibo_payment_data, contains_dicts_with_entries(expected_data_list_spasibo_payment),
                     step=u'Сравним платежи Спасибо без payout_ready_dt с ожидаемыми.')

    with reporter.step(u'Протсавляем payout_ready_dt: передаем групповой платеж - ручка расставляет в дочерние'):
        api.medium().UpdatePayment({'TrustPaymentID': group_trust_payment_id}, {'PayoutReady': CONTRACT_START_DT})

    with reporter.step(u'Экспортируем платежи с payout_ready_dt'):
        steps.CommonPartnerSteps.export_payment(card_payment_id)
        steps.CommonPartnerSteps.export_payment(spasibo_payment_id)

    paymethod_markup_refund = {
        service_order_id_product: {'card': str(PRICE_CARD_NO_FEE), 'spasibo': str(PRICE_SPASIBO_NO_FEE)}}
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(context.service, service_order_id_product,
                                  group_trust_payment_id, delta_amount=PRICE_CARD_NO_FEE+PRICE_SPASIBO_NO_FEE, paymethod_markup=paymethod_markup_refund)

    card_refund_id, card_trust_refund_id = find_refund_by_orig_payment_id(card_payment_id)
    spasibo_refund_id, spasibo_trust_refund_id = find_refund_by_orig_payment_id(spasibo_payment_id)

    steps.CommonPartnerSteps.export_payment(refund_id)
    steps.CommonPartnerSteps.export_payment(card_refund_id)
    steps.CommonPartnerSteps.export_payment(spasibo_refund_id)

    expected_data_list_card_payment = [
        # no fee product
        steps.SimpleApi.create_expected_tpt_row(context,
                                                client_id, contract_id, person_id,
                                                card_trust_payment_id,
                                                card_payment_id, **{'amount': PRICE_CARD_NO_FEE,
                                                                    'invoice_eid': personal_account_eid,
                                                                    'payment_type': PaymentType.CARD,
                                                                    'yandex_reward': PRICE_CARD_NO_FEE * COMMISSION_CATEGORY_NO_FEE * Decimal(
                                                                        '0.0001'),
                                                                    'service_product_id': balance_service_product_id_no_fee,
                                                                    'paysys_type_cc': 'yamoney',
                                                                    'payout_ready_dt': CONTRACT_START_DT}),
        # fee product
        steps.SimpleApi.create_expected_tpt_row(context,
                                                tech_client_id, tech_contract_id, tech_person_id,
                                                card_trust_payment_id,
                                                card_payment_id, **{'amount': PRICE_CARD_FEE,
                                                                    'payment_type': PaymentType.CARD,
                                                                    'yandex_reward': PRICE_CARD_FEE,
                                                                    'service_product_id': balance_service_product_id_fee,
                                                                    'paysys_type_cc': 'yamoney',
                                                                    'internal': 1,
                                                                    'payout_ready_dt': CONTRACT_START_DT}),
    ]

    expected_data_list_card_refund = [
        # refund
        steps.SimpleApi.create_expected_tpt_row(context,
                                                client_id, contract_id, person_id,
                                                card_trust_payment_id,
                                                card_payment_id,
                                                card_trust_refund_id,**{'amount': PRICE_CARD_NO_FEE,
                                                                    'invoice_eid': personal_account_eid,
                                                                    'payment_type': PaymentType.CARD,
                                                                    'service_product_id': balance_service_product_id_no_fee,
                                                                    'paysys_type_cc': 'yamoney',
                                                                    'payout_ready_dt': CONTRACT_START_DT,
                                                                    'transaction_type': TransactionType.REFUND.name})
        ]

    expected_data_list_spasibo_payment = [
        # fee product
        steps.SimpleApi.create_expected_tpt_row(context,
                                                tech_client_id, tech_contract_id, tech_person_id,
                                                spasibo_trust_payment_id,
                                                spasibo_payment_id, **{'amount': PRICE_SPASIBO_FEE,
                                                                       'payment_type': PaymentType.SPASIBO,
                                                                       'yandex_reward': PRICE_SPASIBO_FEE,
                                                                       'service_product_id': balance_service_product_id_fee,
                                                                       'paysys_type_cc': 'yamoney',
                                                                       'internal': 1,
                                                                       'payout_ready_dt': CONTRACT_START_DT}),
        # no fee product
        steps.SimpleApi.create_expected_tpt_row(context,
                                                client_id, spendable_contract_id, spendable_person_id,
                                                spasibo_trust_payment_id,
                                                spasibo_payment_id, **{'amount': PRICE_SPASIBO_NO_FEE,
                                                                       'payment_type': PaymentType.SPASIBO,
                                                                       'service_product_id': balance_service_product_id_no_fee,
                                                                       'paysys_type_cc': 'spasibo',
                                                                       'service_id': Services.BLUE_MARKET_SUBSIDY.id
                                                                       })
    ]

    expected_data_list_spasibo_refund = [
        # refund
        steps.SimpleApi.create_expected_tpt_row(context,
                                                client_id, spendable_contract_id, spendable_person_id,
                                                spasibo_trust_payment_id,
                                                spasibo_payment_id,
                                                spasibo_trust_refund_id, **{'amount': PRICE_SPASIBO_NO_FEE,
                                                                    'payment_type': PaymentType.SPASIBO,
                                                                    'service_product_id': balance_service_product_id_no_fee,
                                                                    'paysys_type_cc': 'spasibo',
                                                                    'service_id': Services.BLUE_MARKET_SUBSIDY.id,
                                                                    'transaction_type': TransactionType.REFUND.name
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


def test_blue_market_spasibo_and_reversal():
    tech_client_id, tech_person_id, tech_contract_id, \
    client_id, person_id, contract_id, service_product_no_fee, service_product_fee = create_ids_for_payments_blue_market()
    personal_account_eid = steps.InvoiceSteps.get_personal_account_external_id(contract_id, context.service)

    _, spendable_contract_id, spendable_contract_eid, spendable_person_id = \
        create_spendable_contract_for_partner(client_id=client_id)

    # будет 2 платежа, объединенные групповым (карточный и спасибный). С каждого платежа - зачисление на 2 заказа.
    # Т.е. на каждый заказ будет зачисление с обоих платежей.

    PRICE_CARD_NO_FEE = Decimal('23')
    PRICE_CARD_FEE = Decimal('7')
    PRICE_SPASIBO_FEE = Decimal('15')
    PRICE_SPASIBO_NO_FEE = Decimal('5')

    COMMISSION_CATEGORY_NO_FEE = Decimal('300')
    COMMISSION_CATEGORY_FEE = Decimal('0')

    service_product_id_list = [service_product_no_fee, service_product_fee]

    balance_service_product_id_no_fee = steps.SimpleApi.get_balance_service_product_id(service_product_no_fee)
    balance_service_product_id_fee = steps.SimpleApi.get_balance_service_product_id(service_product_fee)

    prices_list = [PRICE_CARD_NO_FEE + PRICE_SPASIBO_NO_FEE, PRICE_CARD_FEE + PRICE_SPASIBO_FEE]
    commission_category_list = [COMMISSION_CATEGORY_NO_FEE, COMMISSION_CATEGORY_FEE]

    # Генерим service_order_id для заказов, чтобы передать распределение сумм по ним в paymethod_markup
    service_order_id_product, service_order_id_product_fee = uuid.uuid1().hex, uuid.uuid1().hex
    service_order_id_list = [service_order_id_product, service_order_id_product_fee]

    # распределение сумм по заказам
    paymethod_markup = {service_order_id_product: {'card': str(PRICE_CARD_NO_FEE), 'spasibo': str(PRICE_SPASIBO_NO_FEE)},
                        service_order_id_product_fee: {'card': str(PRICE_CARD_FEE), 'spasibo': str(PRICE_SPASIBO_FEE)}}

    paymethod = LinkedCard(card=simpleapi_defaults.SPASIBO_EMULATOR_CARD)

    service_order_id_list, group_trust_payment_id, _, group_payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service,
                                                       service_product_id_list=service_product_id_list,
                                                       service_order_id_list=service_order_id_list,
                                                       commission_category_list=commission_category_list,
                                                       prices_list=prices_list,
                                                       paymethod_markup=paymethod_markup,
                                                       paymethod=paymethod,
                                                       need_postauthorize=False,
                                                       wait_for_export_from_bs=False)

    # частичная отмена, fee
    action_list = ['cancel', 'cancel']
    # paymethod_markup = {service_order_id_product: {'card': str(2), 'spasibo': str(1)},
    #                     service_order_id_product_fee: {'card': str(6), 'spasibo': str(4)}}
    # amount_list = [Decimal('3'), Decimal('10')]

    steps.SimpleApi.postauthorize(context.service, group_trust_payment_id, service_order_id_list,
                                  actions=action_list)
    steps.SimpleApi.wait_for_payment_export_from_bs(group_trust_payment_id)
    # children_payments = steps.CommonPartnerSteps.get_children_trust_group_payments(group_trust_payment_id)

    #
    # orders = [{'service_order_id': service_order_id_product}]
    # orders_for_update = form_orders_for_update(orders, default_action='cancel')
    # update_basket(context.service, orders=orders_for_update, trust_payment_id=group_trust_payment_id)
    #
    # orders = [{'service_order_id': service_order_id_product_fee}]
    # orders_for_update = form_orders_for_update(orders, default_action='clear')
    # update_basket(context.service, orders=orders_for_update, trust_payment_id=group_trust_payment_id)
    # steps.SimpleApi.wait_for_payment_export_from_bs(group_trust_payment_id)