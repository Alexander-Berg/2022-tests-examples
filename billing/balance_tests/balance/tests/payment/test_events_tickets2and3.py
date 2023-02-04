# -*- coding: utf-8 -*-
from btestlib.environments import SimpleapiEnvironment, TrustDbNames, TrustApiUrls

__author__ = 'atkaya'

from check import db
from datetime import datetime, timedelta
from decimal import Decimal
from dateutil.relativedelta import relativedelta

import pytest
from hamcrest import contains_string, has_length, none, any_of

import btestlib.reporter as reporter
import simpleapi.steps.simple_steps as simpleapi_steps
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Users, PaymentType, PaysysType, TransactionType
from btestlib.data.simpleapi_defaults import DEFAULT_PRICE, DEFAULT_FEE, DEFAULT_COMMISSION_CATEGORY, \
    DEFAULT_PROMOCODE_AMOUNT
from btestlib.matchers import contains_dicts_with_entries
from simpleapi.common.payment_methods import TrustWebPage, Via
from simpleapi.data.defaults import Discounts, Promocode
from simpleapi.data.uids_pool import User
from btestlib.data.partner_contexts import EVENTS_TICKETS2_RU_CONTEXT, EVENTS_TICKETS2_KZ_CONTEXT,\
    EVENTS_TICKETS3_RU_CONTEXT

pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT, Features.EVENTS_TICKETS_NEW),
    pytest.mark.tickets('BALANCE-22273'),
    pytest.mark.usefixtures('switch_to_pg')
]

USER = User(Users.YB_ADM.uid, Users.YB_ADM.login, Users.YB_ADM.password)

MASTERCARD_DISCOUNT = Discounts.id100
DISCOUNT_COEFFICIENT = utils.fraction_from_percent(-MASTERCARD_DISCOUNT['pct'])
COMMISSION_FRACTION = DEFAULT_COMMISSION_CATEGORY / Decimal('10000')

PROMOCODE_EXPORT_ERROR = 'TrustPayment({}) delayed: waiting for appearing composite part in ' \
                         't_thirdparty_transactions completely'
PROMOCODE_REFUND_EXPORT_ERROR = 'Refund({}) delayed: waiting for appearing original payment in ' \
                                't_thirdparty_transactions for refund'

CONTRACT_START_DT = datetime.today() - relativedelta(months=1)

CONTEXTS = [
    EVENTS_TICKETS2_RU_CONTEXT, EVENTS_TICKETS2_KZ_CONTEXT,
    EVENTS_TICKETS3_RU_CONTEXT,
]

parametrize_context = pytest.mark.parametrize('context', CONTEXTS, ids=lambda x: x.name)


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


# --------------------------------------------------------
# Тесты на скидку при оплате мастеркардом

# тест на суммы платежей при оплате мастеркардом
@reporter.feature(Features.MASTERCARD_DISCOUNT)
@parametrize_context
def test_ticket_payment_discount(context):
    tech_client_id, tech_person_id, tickets_contract_id, \
        client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)

    payment_id, trust_payment_id, _, _, _ = create_tickets_payment(context, product, product_fee=product_fee,
                                                                   discounts=[MASTERCARD_DISCOUNT['id']])

    # формируем шаблон для сравнения
    expected_payment = create_expected_payment_data(context, contract_id, client_id, payment_id,
                                                    person_id, trust_payment_id, trust_payment_id,
                                                    discount_coefficient=DISCOUNT_COEFFICIENT)

    expected_fee = create_expected_fee_data(context, tickets_contract_id, tech_client_id, payment_id,
                                            tech_person_id, trust_payment_id, trust_payment_id)

    export_and_check_payment(payment_id, [expected_payment, expected_fee])


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new', write=False)
# тест на рефанд без сбора после платежа со скидкой от мастеркарда
@reporter.feature(Features.MASTERCARD_DISCOUNT)
@parametrize_context
def test_ticket_discount_refund_with_1_row(context):
    tech_client_id, tech_person_id, tickets_contract_id, \
        client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)

    payment_id, trust_payment_id, service_order_id_product, _, purchase_token = \
        create_tickets_payment(context, product, product_fee=product_fee, discounts=[MASTERCARD_DISCOUNT['id']])

    # создаем рефанд
    trust_refund_id, refund_id = create_refund(context, trust_payment_id, service_order_id_product,
                                               discount_coefficient=DISCOUNT_COEFFICIENT)

    # формируем шаблон для сравнения
    expected_payment = create_expected_refund_payment_data(context, contract_id, client_id, payment_id, person_id,
                                                           trust_refund_id, trust_payment_id,
                                                           discount_coefficient=DISCOUNT_COEFFICIENT)

    export_and_check_payment(refund_id, [expected_payment], payment_id, TransactionType.REFUND)


# --------------------------------------------------------
# Тесты на использование промокода

# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new', write=False)
# тест на суммы платежей при использовании промокода
@reporter.feature(Features.PROMOCODE)
@parametrize_context
def test_ticket_payment_promocode(context):
    tech_client_id, tech_person_id, tickets_contract_id, \
        client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)

    promocode_id = create_promo(context, promo_status=Promocode.Status.active, promo_amount=DEFAULT_PROMOCODE_AMOUNT)

    payment_id, trust_payment_id, _, _, _ = create_tickets_payment(context, product, product_fee=product_fee,
                                                                   promocode_id=promocode_id)

    # формируем шаблон для сравнения
    expected_payment = create_expected_payment_data(context, contract_id, client_id, payment_id,
                                                    person_id, trust_payment_id, trust_payment_id,
                                                    promocode_amount=DEFAULT_PROMOCODE_AMOUNT)

    expected_fee = create_expected_fee_data(context, tickets_contract_id, tech_client_id, payment_id,
                                            tech_person_id, trust_payment_id, trust_payment_id)

    export_and_check_payment(payment_id, [expected_payment, expected_fee])

    # получаем ids промокодного платежа
    promocode_payment_id, promocode_trust_payment_id = steps.SimpleApi.get_promocode_payment_ids(trust_payment_id)

    # формируем шаблон для сравнения
    expected_promo_payment = create_expected_payment_data(context, contract_id, client_id, promocode_payment_id,
                                                          person_id, promocode_trust_payment_id,
                                                          promocode_trust_payment_id,
                                                          payment_amount=DEFAULT_PROMOCODE_AMOUNT,
                                                          payment_type=PaymentType.NEW_PROMOCODE,
                                                          paysys_type_cc=PaysysType.YANDEX)

    export_and_check_payment(promocode_payment_id, [expected_promo_payment])


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new', write=False)
# тест на суммы платежей при использовании промокода, промокод больше комиссии и платежа
@reporter.feature(Features.PROMOCODE)
@parametrize_context
def test_ticket_payment_covered_payment_with_promocode(context):
    promo_amount = (DEFAULT_PRICE + DEFAULT_FEE).to_integral() - 1

    tech_client_id, tech_person_id, tickets_contract_id, \
        client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)

    promocode_id = create_promo(context, promo_status=Promocode.Status.active, promo_amount=promo_amount)

    payment_id, trust_payment_id, _, _, _ = create_tickets_payment(context, product, product_fee=product_fee,
                                                                   promocode_id=promocode_id)

    # формируем шаблон для сравнения
    expected_fee = create_expected_fee_data(context, tickets_contract_id, tech_client_id, payment_id,
                                            tech_person_id, trust_payment_id, trust_payment_id,
                                            fee_amount=DEFAULT_FEE + DEFAULT_PRICE - promo_amount)

    export_and_check_payment(payment_id, [expected_fee])

    # получаем ids промокодного платежа
    promocode_payment_id, promocode_trust_payment_id = steps.SimpleApi.get_promocode_payment_ids(trust_payment_id)

    expected_promo_payment = create_expected_payment_data(context, contract_id, client_id, promocode_payment_id,
                                                          person_id, promocode_trust_payment_id,
                                                          promocode_trust_payment_id,
                                                          payment_type=PaymentType.NEW_PROMOCODE,
                                                          paysys_type_cc=PaysysType.YANDEX)

    expected_promo_fee = create_expected_fee_data(context, tickets_contract_id, tech_client_id,
                                                  promocode_payment_id,
                                                  tech_person_id, promocode_trust_payment_id,
                                                  promocode_trust_payment_id,
                                                  fee_amount=promo_amount - DEFAULT_PRICE,
                                                  payment_type=PaymentType.NEW_PROMOCODE,
                                                  paysys_type_cc=PaysysType.YANDEX)

    export_and_check_payment(promocode_payment_id, [expected_promo_payment, expected_promo_fee])


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new', write=False)
# тест на рефанд со сбором после платежа с промокодом
@reporter.feature(Features.PROMOCODE)
@parametrize_context
def test_ticket_promocode_refund_with_2_rows(context):
    tech_client_id, tech_person_id, tickets_contract_id, \
        client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)

    promocode_id = create_promo(context, promo_status=Promocode.Status.active, promo_amount=DEFAULT_PROMOCODE_AMOUNT)

    payment_id, trust_payment_id, service_order_id_product, service_order_id_fee, purchase_token = \
        create_tickets_payment(context, product, product_fee=product_fee, promocode_id=promocode_id)

    trust_refund_id, refund_id = create_refund(context, trust_payment_id, service_order_id_product,
                                               service_order_id_fee, promocode_amount=DEFAULT_PROMOCODE_AMOUNT)

    # формируем шаблон для сравнения
    expected_payment = create_expected_refund_payment_data(context, contract_id, client_id,
                                                           payment_id, person_id,
                                                           trust_refund_id, trust_payment_id,
                                                           promocode_amount=DEFAULT_PROMOCODE_AMOUNT)

    expected_fee = create_expected_refund_fee_data(context, tickets_contract_id, tech_client_id,
                                                   payment_id, tech_person_id,
                                                   trust_refund_id, trust_payment_id)

    export_and_check_payment(refund_id, [expected_payment, expected_fee], payment_id, TransactionType.REFUND)

    # получаем ids промокодного платежа
    promocode_payment_id, promocode_trust_payment_id = steps.SimpleApi.get_promocode_payment_ids(trust_payment_id)

    promocode_trust_refund_id, promocode_refund_id = create_refund(context, promocode_trust_payment_id,
                                                                   service_order_id_product,
                                                                   payment_amount=DEFAULT_PROMOCODE_AMOUNT)

    # формируем шаблон для сравнения
    expected_promocode_payment = create_expected_refund_payment_data(context, contract_id, client_id,
                                                                     promocode_payment_id,
                                                                     person_id, promocode_trust_refund_id,
                                                                     promocode_trust_payment_id,
                                                                     payment_type=PaymentType.NEW_PROMOCODE,
                                                                     paysys_type_cc=PaysysType.YANDEX,
                                                                     payment_amount=DEFAULT_PROMOCODE_AMOUNT)

    export_and_check_payment(promocode_refund_id, [expected_promocode_payment], promocode_payment_id,
                             TransactionType.REFUND)


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new', write=False)
# тест на рефанд без сбора после платежа с промокодом
@reporter.feature(Features.PROMOCODE)
@parametrize_context
def test_ticket_promocode_refund_with_1_row(context):
    tech_client_id, tech_person_id, tickets_contract_id, \
        client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)

    promocode_id = create_promo(context, promo_status=Promocode.Status.active, promo_amount=DEFAULT_PROMOCODE_AMOUNT)

    payment_id, trust_payment_id, service_order_id_product, _, purchase_token = \
        create_tickets_payment(context, product, product_fee=product_fee, promocode_id=promocode_id)

    trust_refund_id, refund_id = create_refund(context, trust_payment_id, service_order_id_product,
                                               promocode_amount=DEFAULT_PROMOCODE_AMOUNT)

    # формируем шаблон для сравнения
    expected_payment = create_expected_refund_payment_data(context, contract_id, client_id, payment_id,
                                                           person_id, trust_refund_id, trust_payment_id,
                                                           promocode_amount=DEFAULT_PROMOCODE_AMOUNT)

    export_and_check_payment(refund_id, [expected_payment], payment_id, TransactionType.REFUND)

    # получаем ids промокодного платежа
    promocode_payment_id, promocode_trust_payment_id = steps.SimpleApi.get_promocode_payment_ids(trust_payment_id)

    promocode_trust_refund_id, promocode_refund_id = create_refund(context, promocode_trust_payment_id,
                                                                   service_order_id_product,
                                                                   payment_amount=DEFAULT_PROMOCODE_AMOUNT)

    # формируем шаблон для сравнения
    expected_promocode_payment = create_expected_refund_payment_data(context, contract_id, client_id,
                                                                     promocode_payment_id, person_id,
                                                                     promocode_trust_refund_id,
                                                                     promocode_trust_payment_id,
                                                                     payment_type=PaymentType.NEW_PROMOCODE,
                                                                     paysys_type_cc=PaysysType.YANDEX,
                                                                     payment_amount=DEFAULT_PROMOCODE_AMOUNT)

    export_and_check_payment(promocode_refund_id, [expected_promocode_payment], promocode_payment_id,
                             TransactionType.REFUND)


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new', write=False)
# тест на ошибку при экспорте промокодного платежа без реестра
@reporter.feature(Features.PROMOCODE)
@parametrize_context
def test_ticket_export_delay_error_promocode(context):
    tech_client_id, tech_person_id, tickets_contract_id, \
        client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)

    promocode_id = create_promo(context, promo_status=Promocode.Status.active, promo_amount=DEFAULT_PROMOCODE_AMOUNT)

    _, trust_payment_id, _, _, _ = create_tickets_payment(context, product, product_fee=product_fee,
                                                          promocode_id=promocode_id)

    promocode_payment_id, promocode_trust_payment_id = steps.SimpleApi.get_promocode_payment_ids(trust_payment_id)

    with pytest.raises(utils.XmlRpc.XmlRpcError) as error:
        steps.CommonPartnerSteps.export_payment(promocode_payment_id)

    utils.check_that(error.value.response,
                     contains_string(PROMOCODE_EXPORT_ERROR.format(promocode_payment_id)),
                     u'Проверяем текст ошибки экспорта')

    payment_id = steps.SimpleApi.get_payment_by_trust_payment_id(trust_payment_id)['id']

    # формируем шаблон для сравнения
    expected_payment = create_expected_payment_data(context, contract_id, client_id, payment_id,
                                                    person_id, trust_payment_id, trust_payment_id,
                                                    promocode_amount=DEFAULT_PROMOCODE_AMOUNT)

    expected_fee = create_expected_fee_data(context, tickets_contract_id, tech_client_id, payment_id,
                                            tech_person_id,
                                            trust_payment_id, trust_payment_id)

    export_and_check_payment(payment_id, [expected_payment, expected_fee])

    # формируем шаблон для сравнения
    expected_promo_payment = create_expected_payment_data(context, contract_id, client_id,
                                                          promocode_payment_id,
                                                          person_id, promocode_trust_payment_id,
                                                          promocode_trust_payment_id,
                                                          payment_amount=DEFAULT_PROMOCODE_AMOUNT,
                                                          payment_type=PaymentType.NEW_PROMOCODE,
                                                          paysys_type_cc=PaysysType.YANDEX)

    export_and_check_payment(promocode_payment_id, [expected_promo_payment])


@parametrize_context
def test_promocode_refund_export_wo_payment(context):
    tech_client_id, tech_person_id, tickets_contract_id, \
        client_id, person_id, contract_id, \
        product, product_fee = create_ids_for_payments_event_tickets(context)

    promocode_id = create_promo(context, promo_status=Promocode.Status.active, promo_amount=DEFAULT_PROMOCODE_AMOUNT)

    payment_id, trust_payment_id, service_order_id_product, service_order_id_fee, purchase_token = \
        create_tickets_payment(context, product, product_fee=product_fee, promocode_id=promocode_id)

    # получаем ids промокодного платежа
    promocode_payment_id, promocode_trust_payment_id = steps.SimpleApi.get_promocode_payment_ids(trust_payment_id)
    promocode_trust_refund_id, promocode_refund_id = create_refund(context, promocode_trust_payment_id,
                                                                   service_order_id_product,
                                                                   payment_amount=DEFAULT_PROMOCODE_AMOUNT)

    with pytest.raises(utils.XmlRpc.XmlRpcError) as error_payment:
        steps.CommonPartnerSteps.export_payment(promocode_payment_id)

    with pytest.raises(utils.XmlRpc.XmlRpcError) as error_refund:
        steps.CommonPartnerSteps.export_payment(promocode_refund_id)

    utils.check_that(error_payment.value.response,
                     contains_string(PROMOCODE_EXPORT_ERROR.format(promocode_payment_id)),
                     u'Проверяем текст ошибки экспорта платежа')
    utils.check_that(error_refund.value.response,
                     contains_string(PROMOCODE_REFUND_EXPORT_ERROR.format(promocode_refund_id)),
                     u'Проверяем текст ошибки экспорта рефанда')


# Старые тесты, проверяющие логику Траста, уберем полностью после перехода на тлог
#
# # Больше не модифицируем технического партнёра в БД.
# # Потому можем теперь просто разбирать платежи на настоящий технический договор
# # @pytest.mark.no_parallel('events_tickets_new', write=False)
# # тест на рефанд со сбором после платежа со скидкой от мастеркарда
# @reporter.feature(Features.TRUST_LOGIC, Features.MASTERCARD_DISCOUNT)
# @parametrize_context
# def test_ticket_discount_refund_with_2_rows(context):
#     tech_client_id, tech_person_id, tickets_contract_id, \
#         client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)
#
#     payment_id, trust_payment_id, service_order_id_product, service_order_id_fee, purchase_token = \
#         create_tickets_payment(context, product, product_fee=product_fee, discounts=[MASTERCARD_DISCOUNT['id']])
#
#     # создаем рефанд
#     trust_refund_id, refund_id = create_refund(context, trust_payment_id, service_order_id_product,
#                                                service_order_id_fee, discount_coefficient=DISCOUNT_COEFFICIENT)
#
#     # формируем шаблон для сравнения
#     expected_payment = create_expected_refund_payment_data(context, contract_id, client_id, payment_id,
#                                                            person_id, trust_refund_id, trust_payment_id,
#                                                            discount_coefficient=DISCOUNT_COEFFICIENT)
#
#     expected_fee = create_expected_refund_fee_data(context, tickets_contract_id, tech_client_id, payment_id,
#                                                    tech_person_id, trust_refund_id, trust_payment_id)
#
#     export_and_check_payment(refund_id, [expected_payment, expected_fee], payment_id, TransactionType.REFUND)
#
#
# # Больше не модифицируем технического партнёра в БД.
# # Потому можем теперь просто разбирать платежи на настоящий технический договор
# # @pytest.mark.no_parallel('events_tickets_new', write=False)
# # тест на разделение промокода при наличии различных партнеров в заказах
# @reporter.feature(Features.TRUST_LOGIC)
# @parametrize_context
# def test_multiple_promocode_rows(context):
#     tech_client_id, tech_person_id, tickets_contract_id, \
#         client_id_1, person_id_1, contract_id_1, \
#         product_1, product_fee_1 = create_ids_for_payments_event_tickets(context)
#
#     client_id_2, person_id_2, contract_id_2, product_2, product_fee_2 = create_contract_for_partner(context)
#
#     promocode_id = create_promo(context, [context.service], promo_status=Promocode.Status.active,
#                                 promo_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     purchase_token, service_order_id_fees, service_order_id_products, trust_payment_id = \
#         create_multiple_tickets_payment(context, [product_1, product_2], product_fees=[product_fee_1, product_fee_2],
#                                         promocode_id=promocode_id)
#
#     payment_id = steps.SimpleApi.get_payment_by_trust_payment_id(trust_payment_id)['id']
#
#     expected_payments = [
#         create_expected_payment_data(context, contract_id_1, client_id_1, payment_id, person_id_1, trust_payment_id,
#                                      trust_payment_id, promocode_amount=DEFAULT_PROMOCODE_AMOUNT / 2),
#         create_expected_payment_data(context, contract_id_2, client_id_2, payment_id, person_id_2, trust_payment_id,
#                                      trust_payment_id, promocode_amount=DEFAULT_PROMOCODE_AMOUNT / 2),
#         create_expected_fee_data(context, tickets_contract_id, tech_client_id, payment_id,
#                                  tech_person_id, trust_payment_id, trust_payment_id),
#         create_expected_fee_data(context, tickets_contract_id, tech_client_id, payment_id,
#                                  tech_person_id, trust_payment_id, trust_payment_id)
#     ]
#
#     export_and_check_payment(payment_id, expected_payments)
#
#     # получаем ids промокодных платежей
#     with reporter.step(u'Получаем промокодные платежи по основному платежу из траста: {}'.format(trust_payment_id)):
#         composite_tag = steps.SimpleApi.wait_for_composite_tag(trust_payment_id)
#         promocode_payment_ids, promocode_trust_payment_ids = \
#             steps.SimpleApi.get_multiple_promocode_payment_ids_by_composite_tag(composite_tag)
#
#     utils.check_that(promocode_payment_ids, has_length(2), u"Проверяем, что промокод был разделен")
#
#     promo_payment_data = []
#     for promocode_payment_id in promocode_payment_ids:
#         steps.CommonPartnerSteps.export_payment(promocode_payment_id)
#         promo_payment_data += steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(promocode_payment_id)
#
#     expected_promo_payment_data_1 = [
#         create_expected_payment_data(context, contract_id_1, client_id_1, promocode_payment_ids[0],
#                                      person_id_1, promocode_trust_payment_ids[0],
#                                      promocode_trust_payment_ids[0], payment_amount=DEFAULT_PROMOCODE_AMOUNT / 2,
#                                      payment_type=PaymentType.NEW_PROMOCODE,
#                                      paysys_type_cc=PaysysType.YANDEX),
#         create_expected_payment_data(context, contract_id_2, client_id_2, promocode_payment_ids[1],
#                                      person_id_2, promocode_trust_payment_ids[1],
#                                      promocode_trust_payment_ids[1], payment_amount=DEFAULT_PROMOCODE_AMOUNT / 2,
#                                      payment_type=PaymentType.NEW_PROMOCODE,
#                                      paysys_type_cc=PaysysType.YANDEX)
#     ]
#     expected_promo_payment_data_2 = [
#         create_expected_payment_data(context, contract_id_1, client_id_1, promocode_payment_ids[1],
#                                      person_id_1, promocode_trust_payment_ids[1],
#                                      promocode_trust_payment_ids[1], payment_amount=DEFAULT_PROMOCODE_AMOUNT / 2,
#                                      payment_type=PaymentType.NEW_PROMOCODE,
#                                      paysys_type_cc=PaysysType.YANDEX),
#         create_expected_payment_data(context, contract_id_2, client_id_2, promocode_payment_ids[0],
#                                      person_id_2, promocode_trust_payment_ids[0],
#                                      promocode_trust_payment_ids[0], payment_amount=DEFAULT_PROMOCODE_AMOUNT / 2,
#                                      payment_type=PaymentType.NEW_PROMOCODE,
#                                      paysys_type_cc=PaysysType.YANDEX)
#     ]
#
#     utils.check_that(promo_payment_data, any_of(
#         contains_dicts_with_entries(expected_promo_payment_data_1),
#         contains_dicts_with_entries(expected_promo_payment_data_2)),
#         u"Проверяем, что присутствуют оба корректных промокодных платежа")
#
#
# # Больше не модифицируем технического партнёра в БД.
# # Потому можем теперь просто разбирать платежи на настоящий технический договор
# # @pytest.mark.no_parallel('events_tickets_new', write=False)
# # тест на рефанд при разделении промокода
# @reporter.feature(Features.TRUST_LOGIC)
# @parametrize_context
# def test_multiple_promocode_rows_refund(context):
#     tech_client_id, tech_person_id, tickets_contract_id, \
#         client_id_1, person_id_1, contract_id_1, \
#         product_1, product_fee_1 = create_ids_for_payments_event_tickets(context)
#     client_id_2, person_id_2, contract_id_2, product_2, product_fee_2 = create_contract_for_partner(context)
#
#     promocode_id = create_promo(context, [context.service], promo_status=Promocode.Status.active,
#                                 promo_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     purchase_token, service_order_id_fees, service_order_id_products, trust_payment_id = \
#         create_multiple_tickets_payment(context, [product_1, product_2], product_fees=[product_fee_1, product_fee_2],
#                                         promocode_id=promocode_id)
#
#     payment_id = steps.SimpleApi.get_payment_by_trust_payment_id(trust_payment_id)['id']
#
#     # создаем рефанд
#     delta_amount_list = [utils.dround(DEFAULT_PRICE - DEFAULT_PROMOCODE_AMOUNT / 2, 2)] * 2 + [DEFAULT_FEE] * 2
#
#     trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(
#         context.service, service_order_id_products + service_order_id_fees,
#         trust_payment_id, delta_amount_list)
#
#     # формируем шаблон для сравнения
#     expected_refunds = [
#         create_expected_refund_payment_data(context, contract_id_1, client_id_1, payment_id, person_id_1,
#                                             trust_refund_id, trust_payment_id,
#                                             promocode_amount=DEFAULT_PROMOCODE_AMOUNT / 2),
#         create_expected_refund_payment_data(context, contract_id_2, client_id_2, payment_id, person_id_2,
#                                             trust_refund_id, trust_payment_id,
#                                             promocode_amount=DEFAULT_PROMOCODE_AMOUNT / 2),
#         create_expected_refund_fee_data(context, tickets_contract_id, tech_client_id, payment_id, tech_person_id,
#                                         trust_refund_id, trust_payment_id),
#         create_expected_refund_fee_data(context, tickets_contract_id, tech_client_id, payment_id, tech_person_id,
#                                         trust_refund_id, trust_payment_id)
#     ]
#
#     export_and_check_payment(refund_id, expected_refunds, payment_id, TransactionType.REFUND)
#
#     # получаем ids промокодных платежей
#     with reporter.step(u'Получаем промокодные платежи по основному платежу из траста: {}'.format(trust_payment_id)):
#         composite_tag = steps.SimpleApi.wait_for_composite_tag(trust_payment_id)
#         promocode_payment_ids, promocode_trust_payment_ids = \
#             steps.SimpleApi.get_multiple_promocode_payment_ids_by_composite_tag(composite_tag)
#
#     utils.check_that(promocode_payment_ids, has_length(2), u"Проверяем, что промокод был разделен")
#
#     promo_refund_data = []
#     promo_trust_refund_ids = []
#     for promocode_payment_id, promocode_trust_payment_id in zip(promocode_payment_ids, promocode_trust_payment_ids):
#         # создаем рефанд
#         steps.CommonPartnerSteps.export_payment(promocode_payment_id)
#         service_order_id = steps.SimpleApi.get_service_order_id_by_trust_payment_id(promocode_trust_payment_id)
#
#         promo_trust_refund_id, promo_refund_id = steps.SimpleApi.create_refund(context.service,
#                                                                                service_order_id,
#                                                                                promocode_trust_payment_id,
#                                                                                delta_amount=DEFAULT_PROMOCODE_AMOUNT / 2)
#
#         # запускаем обработку платежа
#         steps.CommonPartnerSteps.export_payment(promo_refund_id)
#
#         promo_refund_data += steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(promocode_payment_id,
#                                                                                                TransactionType.REFUND)
#         promo_trust_refund_ids.append(promo_trust_refund_id)
#
#     expected_promo_payment_data_1 = [
#         create_expected_refund_payment_data(context, contract_id_1, client_id_1, promocode_payment_ids[0],
#                                             person_id_1, promo_trust_refund_ids[0], promocode_trust_payment_ids[0],
#                                             payment_type=PaymentType.NEW_PROMOCODE, paysys_type_cc=PaysysType.YANDEX,
#                                             payment_amount=DEFAULT_PROMOCODE_AMOUNT / 2),
#         create_expected_refund_payment_data(context, contract_id_2, client_id_2, promocode_payment_ids[1],
#                                             person_id_2, promo_trust_refund_ids[1], promocode_trust_payment_ids[1],
#                                             payment_type=PaymentType.NEW_PROMOCODE, paysys_type_cc=PaysysType.YANDEX,
#                                             payment_amount=DEFAULT_PROMOCODE_AMOUNT / 2)
#     ]
#     expected_promo_payment_data_2 = [
#         create_expected_refund_payment_data(context, contract_id_1, client_id_1, promocode_payment_ids[1],
#                                             person_id_1, promo_trust_refund_ids[1], promocode_trust_payment_ids[1],
#                                             payment_type=PaymentType.NEW_PROMOCODE, paysys_type_cc=PaysysType.YANDEX,
#                                             payment_amount=DEFAULT_PROMOCODE_AMOUNT / 2),
#         create_expected_refund_payment_data(context, contract_id_2, client_id_2, promocode_payment_ids[0],
#                                             person_id_2, promo_trust_refund_ids[0], promocode_trust_payment_ids[0],
#                                             payment_type=PaymentType.NEW_PROMOCODE, paysys_type_cc=PaysysType.YANDEX,
#                                             payment_amount=DEFAULT_PROMOCODE_AMOUNT / 2)
#     ]
#
#     utils.check_that(promo_refund_data, any_of(
#         contains_dicts_with_entries(expected_promo_payment_data_1),
#         contains_dicts_with_entries(expected_promo_payment_data_2)),
#         u"Проверяем, что присутствуют оба корректных промокодных рефанда")
#
# # Больше не модифицируем технического партнёра в БД.
# # Потому можем теперь просто разбирать платежи на настоящий технический договор
# # @pytest.mark.no_parallel('events_tickets_new', write=False)
# # тест на рефанд со сбором после платежа со скидкой от мастеркарда
# @reporter.feature(Features.TRUST_LOGIC, Features.MASTERCARD_DISCOUNT)
# @parametrize_context
# def test_ticket_discount_refund_with_2_rows(context):
#     tech_client_id, tech_person_id, tickets_contract_id, \
#         client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)
#
#     payment_id, trust_payment_id, service_order_id_product, service_order_id_fee, purchase_token = \
#         create_tickets_payment(context, product, product_fee=product_fee, discounts=[MASTERCARD_DISCOUNT['id']])
#
#     # создаем рефанд
#     trust_refund_id, refund_id = create_refund(context, trust_payment_id, service_order_id_product,
#                                                service_order_id_fee, discount_coefficient=DISCOUNT_COEFFICIENT)
#
#     # формируем шаблон для сравнения
#     expected_payment = create_expected_refund_payment_data(context, contract_id, client_id, payment_id,
#                                                            person_id, trust_refund_id, trust_payment_id,
#                                                            discount_coefficient=DISCOUNT_COEFFICIENT)
#
#     expected_fee = create_expected_refund_fee_data(context, tickets_contract_id, tech_client_id, payment_id,
#                                                    tech_person_id, trust_refund_id, trust_payment_id)
#
#     export_and_check_payment(refund_id, [expected_payment, expected_fee], payment_id, TransactionType.REFUND)
#
#
#
# # Больше не модифицируем технического партнёра в БД.
# # Потому можем теперь просто разбирать платежи на настоящий технический договор
# # @pytest.mark.no_parallel('events_tickets_new', write=False)
# # тест на суммы платежей при использовании промокода, промокод больше комиссии и платежа
# @reporter.feature(Features.TRUST_LOGIC, Features.PROMOCODE)
# @parametrize_context
# def test_ticket_payment_covered_payment_and_fee_with_promocode(context):
#     promo_amount = (2 * (DEFAULT_PRICE + DEFAULT_FEE)).to_integral()
#
#     tech_client_id, tech_person_id, tickets_contract_id, \
#         client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)
#
#     promocode_id = create_promo(context, promo_status=Promocode.Status.active, promo_amount=promo_amount)
#
#     payment_id, trust_payment_id, _, _, _ = create_tickets_payment(context, product, product_fee=product_fee,
#                                                                    promocode_id=promocode_id,
#                                                                    paymethod=TrustWebPage(Via.Promocode()))
#
#     # формируем шаблон для сравнения
#     expected_payment = create_expected_payment_data(context, contract_id, client_id, payment_id,
#                                                     person_id, trust_payment_id,
#                                                     trust_payment_id,
#                                                     payment_type=PaymentType.NEW_PROMOCODE,
#                                                     paysys_type_cc=PaysysType.YANDEX)
#
#     expected_fee = create_expected_fee_data(context, tickets_contract_id, tech_client_id, payment_id,
#                                             tech_person_id, trust_payment_id, trust_payment_id,
#                                             payment_type=PaymentType.NEW_PROMOCODE,
#                                             paysys_type_cc=PaysysType.YANDEX)
#
#     export_and_check_payment(payment_id, [expected_payment, expected_fee])
#
#     # проверяем, что этот платеж не композитный
#     composite_tag, _ = steps.SimpleApi.get_composite_tag_and_payment_method(trust_payment_id)
#     utils.check_that(composite_tag, none(), u'Проверяем, что composite tag отсутствует')
# --------------------------------------------------------
# Тесты на использование промокода со скидкой мастеркардка

# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new', write=False)
# тест на суммы платежей при использовании промокода и скидкой от мастеркарда
# @pytest.mark.smoke
# @reporter.feature(Features.TRUST_LOGIC, Features.MASTERCARD_DISCOUNT, Features.PROMOCODE)
# @parametrize_context
# def test_ticket_payment_promocode_with_discount(context):
#     tech_client_id, tech_person_id, tickets_contract_id, \
#         client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)
#
#     promocode_id = create_promo(context, promo_status=Promocode.Status.active, promo_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     payment_id, trust_payment_id, _, _, _ = create_tickets_payment(context, product, product_fee=product_fee,
#                                                                    promocode_id=promocode_id,
#                                                                    discounts=[MASTERCARD_DISCOUNT['id']])
#
#     # формируем шаблон для сравнения
#     expected_payment = create_expected_payment_data(context, contract_id, client_id, payment_id, person_id,
#                                                     trust_payment_id, trust_payment_id,
#                                                     discount_coefficient=DISCOUNT_COEFFICIENT,
#                                                     promocode_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     expected_fee = create_expected_fee_data(context, tickets_contract_id, tech_client_id, payment_id,
#                                             tech_person_id, trust_payment_id, trust_payment_id)
#
#     export_and_check_payment(payment_id, [expected_payment, expected_fee])
#
#     # получаем ids промокодного платежа
#     promocode_payment_id, promocode_trust_payment_id = steps.SimpleApi.get_promocode_payment_ids(trust_payment_id)
#
#     # формируем шаблон для сравнения
#     expected_promo_payment = create_expected_payment_data(context, contract_id, client_id,
#                                                           promocode_payment_id,
#                                                           person_id, promocode_trust_payment_id,
#                                                           promocode_trust_payment_id,
#                                                           payment_amount=DEFAULT_PROMOCODE_AMOUNT,
#                                                           payment_type=PaymentType.NEW_PROMOCODE,
#                                                           paysys_type_cc=PaysysType.YANDEX)
#
#     export_and_check_payment(promocode_payment_id, [expected_promo_payment])

#
# # Больше не модифицируем технического партнёра в БД.
# # Потому можем теперь просто разбирать платежи на настоящий технический договор
# # @pytest.mark.no_parallel('events_tickets_new', write=False)
# # тест на рефанд со сбором после платежа с промокодом и скидкой от мастеркарда
# @reporter.feature(Features.TRUST_LOGIC, Features.MASTERCARD_DISCOUNT, Features.PROMOCODE)
# @parametrize_context
# def test_ticket_promocode_with_discount_refund_with_2_rows(context):
#     tech_client_id, tech_person_id, tickets_contract_id, \
#         client_id, person_id, contract_id, product, product_fee = create_ids_for_payments_event_tickets(context)
#
#     promocode_id = create_promo(context, promo_status=Promocode.Status.active, promo_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     payment_id, trust_payment_id, service_order_id_product, service_order_id_fee, purchase_token = \
#         create_tickets_payment(context, product, product_fee=product_fee, promocode_id=promocode_id,
#                                discounts=[MASTERCARD_DISCOUNT['id']])
#
#     # создаем рефанд
#     trust_refund_id, refund_id = create_refund(context, trust_payment_id, service_order_id_product,
#                                                service_order_id_fee,
#                                                promocode_amount=DEFAULT_PROMOCODE_AMOUNT,
#                                                discount_coefficient=DISCOUNT_COEFFICIENT)
#
#     # формируем шаблон для сравнения
#     expected_payment = create_expected_refund_payment_data(context, contract_id, client_id, payment_id, person_id,
#                                                            trust_refund_id, trust_payment_id,
#                                                            discount_coefficient=DISCOUNT_COEFFICIENT,
#                                                            promocode_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     expected_fee = create_expected_refund_fee_data(context, tickets_contract_id, tech_client_id, payment_id, tech_person_id,
#                                                    trust_refund_id, trust_payment_id)
#
#     export_and_check_payment(refund_id, [expected_payment, expected_fee], payment_id, TransactionType.REFUND)
#
#     # получаем ids промокодного платежа
#     promocode_payment_id, promocode_trust_payment_id = steps.SimpleApi.get_promocode_payment_ids(trust_payment_id)
#
#     promocode_trust_refund_id, promocode_refund_id = create_refund(context, promocode_trust_payment_id,
#                                                                    service_order_id_product,
#                                                                    payment_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     # формируем шаблон для сравнения
#     expected_promocode_payment = create_expected_refund_payment_data(context, contract_id, client_id, promocode_payment_id,
#                                                                      person_id, promocode_trust_refund_id,
#                                                                      promocode_trust_payment_id,
#                                                                      payment_type=PaymentType.NEW_PROMOCODE,
#                                                                      paysys_type_cc=PaysysType.YANDEX,
#                                                                      payment_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     export_and_check_payment(promocode_refund_id, [expected_promocode_payment], promocode_payment_id,
#                              TransactionType.REFUND)
#
#
# # Больше не модифицируем технического партнёра в БД.
# # Потому можем теперь просто разбирать платежи на настоящий технический договор
# # @pytest.mark.no_parallel('events_tickets_new', write=False)
# # тест на рефанд без сбора после платежа с промокодом и скидкой от мастеркарда
# @reporter.feature(Features.TRUST_LOGIC, Features.MASTERCARD_DISCOUNT, Features.PROMOCODE)
# @parametrize_context
# def test_ticket_promocode_with_discount_refund_with_1_row(context):
#     tech_client_id, tech_person_id, tickets_contract_id, \
#         client_id, person_id, contract_id, \
#         product, product_fee = create_ids_for_payments_event_tickets(context)
#
#     promocode_id = create_promo(context, promo_status=Promocode.Status.active, promo_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     payment_id, trust_payment_id, service_order_id_product, _, purchase_token = \
#         create_tickets_payment(context, product, product_fee=product_fee, promocode_id=promocode_id,
#                                discounts=[MASTERCARD_DISCOUNT['id']])
#
#     # создаем рефанд
#     trust_refund_id, refund_id = create_refund(context, trust_payment_id, service_order_id_product,
#                                                promocode_amount=DEFAULT_PROMOCODE_AMOUNT,
#                                                discount_coefficient=DISCOUNT_COEFFICIENT)
#
#     # формируем шаблон для сравнения
#     expected_payment = create_expected_refund_payment_data(context, contract_id, client_id, payment_id,
#                                                            person_id, trust_refund_id, trust_payment_id,
#                                                            discount_coefficient=DISCOUNT_COEFFICIENT,
#                                                            promocode_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     export_and_check_payment(refund_id, [expected_payment], payment_id, TransactionType.REFUND)
#
#     # получаем ids промокодного платежа
#     promocode_payment_id, promocode_trust_payment_id = steps.SimpleApi.get_promocode_payment_ids(trust_payment_id)
#
#     promocode_trust_refund_id, promocode_refund_id = create_refund(context, promocode_trust_payment_id,
#                                                                    service_order_id_product,
#                                                                    payment_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     # формируем шаблон для сравнения
#     expected_promocode_payment = create_expected_refund_payment_data(context, contract_id, client_id,
#                                                                      promocode_payment_id, person_id,
#                                                                      promocode_trust_refund_id,
#                                                                      promocode_trust_payment_id,
#                                                                      payment_type=PaymentType.NEW_PROMOCODE,
#                                                                      paysys_type_cc=PaysysType.YANDEX,
#                                                                      payment_amount=DEFAULT_PROMOCODE_AMOUNT)
#
#     export_and_check_payment(promocode_refund_id, [expected_promocode_payment], promocode_payment_id,
#                              TransactionType.REFUND)
#
# ------------------------------------------------
# No active contract found tests

EXPORT_ERROR_TEMPLATE = "TrustPayment({}) delayed: no active contracts found for client {}"

NO_ACTIVE_CONTRACT_UPDATES = [
    # не подписанный договор
    lambda contract_id: steps.ContractSteps.clear_contract_is_signed(contract_id),
    # завершившийся договор
    lambda contract_id: steps.ContractSteps.insert_attribute(
        contract_id, 'FINISH_DT', value_dt=utils.Date.nullify_time_of_date(datetime.today())),
    # не начавшийся договор
    lambda contract_id: steps.ContractSteps.update_contract_start_dt(
        contract_id, utils.Date.nullify_time_of_date(datetime.today() + timedelta(days=1)))
]

NO_ACTIVE_CONTRACT_IDS = ['UNSIGNED', 'ENDED', 'NOT_STARTED']


def get_payment_id_and_export_error(context, product, product_fee):
    with reporter.step(u"Создаем платеж и возвращаем ошибку"):
        # создаем платеж в трасте
        payment_id, trust_payment_id, _, _, _ = steps.SimpleApi.create_tickets_payment_with_id(
            context.service, product,
            product_fee=product_fee)

        # запускаем обработку платежа
        with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
            steps.CommonPartnerSteps.export_payment(payment_id)

        export_error = xmlrpc_error.value.response

        reporter.attach(u"Ошибка экспорта платежа", export_error)

        return payment_id, export_error


# ------------------------------------------------
# Utils
def create_contract_for_partner(context):
    with reporter.step(u'Создаем договор для клиента-партнера'):
        # создаем клиента-партнера
        client_id, product, product_fee = steps.SimpleApi.create_partner_product_and_fee(context.service)
        _ = steps.SimpleApi.create_thenumberofthebeast_service_product(context.service, client_id, service_fee=666)

        _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            context, client_id=client_id, additional_params={'start_dt': CONTRACT_START_DT})

        return client_id, person_id, contract_id, product, product_fee


def create_ids_for_payments_event_tickets(context):
    tech_client_id, tech_person_id, tech_contract_id = steps.CommonPartnerSteps.get_active_tech_ids(
        context.service, currency=context.currency.num_code)

    client_id, person_id, contract_id, product, product_fee = create_contract_for_partner(context)
    _ = steps.SimpleApi.create_thenumberofthebeast_service_product(context.service, client_id, service_fee=666)

    return tech_client_id, tech_person_id, tech_contract_id, client_id, person_id, contract_id, product, product_fee


def export_and_check_payment(payment_id, expected_data, thirdparty_payment_id=None,
                             transaction_type=TransactionType.PAYMENT):
    if not thirdparty_payment_id:
        thirdparty_payment_id = payment_id
    else:
        steps.CommonPartnerSteps.export_payment(thirdparty_payment_id)
    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(thirdparty_payment_id, transaction_type)

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')

    utils.check_that(payment_data, has_length(len(expected_data)), u"Проверяем, что отсутствуют дополнительные записи")


def create_tickets_payment(context, product, **kwargs):
    return steps.SimpleApi.create_tickets_payment_with_id(
        context.service, product,
        region_id=context.special_contract_params['country'],
        currency=context.currency,
        **kwargs
    )


def create_multiple_tickets_payment(context, products, product_fees, **kwargs):
    return steps.SimpleApi.create_multiple_tickets_payment(
        context.service, products,
        region_id=context.special_contract_params['country'],
        currency=context.currency,
        product_fees=product_fees,
        **kwargs
    )


def create_promo(context, services=None, promo_status=Promocode.Status.active, promo_amount=DEFAULT_PROMOCODE_AMOUNT):
    SimpleapiEnvironment.switch_param(dbname=TrustDbNames.BS_ORACLE, xmlrpc_url=TrustApiUrls.XMLRPC_ORA)
    promocode_id = simpleapi_steps.process_promocode_creating(context.service, services, promo_status,
                                                              promo_amount=promo_amount)
    SimpleapiEnvironment.switch_param(dbname=TrustDbNames.BS_PG, xmlrpc_url=TrustApiUrls.XMLRPC_PG)
    return promocode_id


def create_refund(context, trust_payment_id, service_order_id_product, service_order_id_fee=None,
                  payment_amount=DEFAULT_PRICE, promocode_amount=Decimal('0'), discount_coefficient=Decimal('1')):
    # создаем рефанд
    delta_amount = utils.dround((payment_amount - promocode_amount) * discount_coefficient, 2)

    trust_refund_id, refund_id = steps.SimpleApi.create_refund(context.service, service_order_id_product,
                                                               trust_payment_id, service_order_id_fee, delta_amount)

    return trust_refund_id, refund_id


# ------------------------------------------------
# Expected data creation
def create_common_expected_data(context, contract_id, partner_id, payment_id, person_id, trust_id, trust_payment_id,
                                payment_type=None, paysys_type_cc=None):
    payment_type = payment_type or any_of(PaymentType.DIRECT_CARD, PaymentType.CARD)
    return steps.SimpleApi.create_expected_tpt_row(
        context, partner_id, contract_id, person_id, trust_payment_id,
        payment_id, trust_id=trust_id,
        paysys_type_cc=paysys_type_cc or context.tpt_paysys_type_cc,
        payment_type=payment_type,
        invoice_eid=steps.InvoiceSteps.get_invoice_eid(contract_id, partner_id, context.currency.char_code, 1)
    )


def create_expected_payment_data(context, contract_id, partner_id, payment_id, person_id, trust_id, trust_payment_id,
                                 promocode_amount=Decimal('0'), discount_coefficient=Decimal('1'),
                                 payment_type=None, paysys_type_cc=None,
                                 payment_amount=DEFAULT_PRICE):
    expected_data = create_common_expected_data(context, contract_id, partner_id, payment_id, person_id, trust_id,
                                                trust_payment_id, payment_type, paysys_type_cc)

    amount = utils.dround((payment_amount - promocode_amount) * discount_coefficient, 2)
    yandex_reward = max(utils.dround(payment_amount * COMMISSION_FRACTION * discount_coefficient, 5), Decimal('0.01'))

    expected_data.update({
        'amount': amount,
        'yandex_reward': yandex_reward,

        'transaction_type': TransactionType.PAYMENT.name,
        'internal': None,
    })

    return expected_data


def create_expected_fee_data(context, contract_id, partner_id, payment_id, person_id, trust_id, trust_payment_id,
                             payment_type=None, paysys_type_cc=None,
                             fee_amount=DEFAULT_FEE):
    expected_data = create_common_expected_data(context, contract_id, partner_id, payment_id, person_id, trust_id,
                                                trust_payment_id, payment_type, paysys_type_cc)

    expected_data.update({
        'amount': fee_amount,
        'yandex_reward': fee_amount,

        'transaction_type': TransactionType.PAYMENT.name,
        'internal': 1,
    })

    return expected_data


def create_expected_refund_payment_data(context, contract_id, partner_id, payment_id, person_id, trust_id, trust_payment_id,
                                        promocode_amount=Decimal('0'), discount_coefficient=Decimal('1'),
                                        payment_type=None, paysys_type_cc=None,
                                        payment_amount=DEFAULT_PRICE):
    expected_data = create_expected_payment_data(context, contract_id, partner_id, payment_id, person_id, trust_id,
                                                 trust_payment_id, promocode_amount, discount_coefficient, payment_type,
                                                 paysys_type_cc, payment_amount)

    reward_refund = steps.SimpleApi.get_reward_refund_for_service(context.service)
    if reward_refund:
        yandex_reward = max(utils.dround(payment_amount * COMMISSION_FRACTION * discount_coefficient, 5),
                            Decimal('0.01'))
    else:
        yandex_reward = None

    expected_data.update({
        'transaction_type': TransactionType.REFUND.name,
        'yandex_reward': yandex_reward
    })

    return expected_data


def create_expected_refund_fee_data(context, contract_id, partner_id, payment_id, person_id, trust_id, trust_payment_id,
                                    payment_type=None, paysys_type_cc=None,
                                    fee_amount=DEFAULT_FEE):
    expected_data = create_expected_fee_data(context, contract_id, partner_id, payment_id, person_id, trust_id,
                                             trust_payment_id, payment_type, paysys_type_cc, fee_amount)

    expected_data.update({
        'transaction_type': TransactionType.REFUND.name,
    })

    return expected_data


def get_and_check_payout_ready_dt(payment_id, expected_payout_ready_dt):
    payment_data = db.get_payment_data(payment_id)[0] or None
    payout_ready_dt = payment_data['payout_ready_dt'] or None
    if expected_payout_ready_dt:
        utils.check_that(payout_ready_dt, expected_payout_ready_dt)
    else:
        utils.check_that(payout_ready_dt, none())


def check_payout_ready_dt_updating(trust_payment_id, payment_id, related_payments_ids, payout_ready_dt=None):
    with reporter.step(u'Проверяем, что у всех платежей поле payout_ready_dt пустое'):
        get_and_check_payout_ready_dt(payment_id, None)
        for related_payments_id in related_payments_ids:
            get_and_check_payout_ready_dt(related_payments_id, None)

    if payout_ready_dt is None:
        payout_ready_dt = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d %H:%M:%S')

    with reporter.step(u'Проставляем значение payout_ready_dt '):
        api.medium().UpdatePayment({'TrustPaymentID': trust_payment_id}, {'PayoutReady': payout_ready_dt})
        steps.CommonPartnerSteps.export_payment(payment_id)

    with reporter.step(u'Проверяем, что всем связанным платежам проставилось значение payout_ready_dt'):
        get_and_check_payout_ready_dt(payment_id, payout_ready_dt)
        for related_payments_id in related_payments_ids:
            get_and_check_payout_ready_dt(related_payments_id, payout_ready_dt)
