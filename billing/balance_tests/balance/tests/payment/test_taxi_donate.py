# -*- coding: utf-8 -*-

__author__ = 'atkaya'

from datetime import datetime

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string, empty
from decimal import Decimal as D
import datetime as dt

from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils, reporter
from btestlib.constants import Services, TransactionType, Export
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import *
from btestlib.matchers import contains_dicts_with_entries
from simpleapi.common.payment_methods import Coupon, Subsidy, BrandingSubsidy, GuaranteeFee, \
    TripBonus, PersonnelBonus, DiscountTaxi, SupportCoupon, BookingSubsidy, Dryclean, Cashrunner

BASE_DATE = utils.Date.nullify_time_of_date(dt.datetime.now())

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)
ORDER_DT = utils.Date.nullify_time_of_date(utils.Date.moscow_offset_dt())
EXTRA_STR = 'test1234!'

pytestmark = [
    pytest.mark.tickets('BALANCE-22816', 'BALANCE-28558'),
    reporter.feature(Features.TAXI, Features.TRUST, Features.PAYMENT, Features.COUPON, Features.SUBSIDY),
    pytest.mark.usefixtures("switch_to_pg")
]

CASES = [
    (TAXI_RU_CONTEXT, TAXI_RU_CONTEXT_SPENDABLE),
    (TAXI_UBER_BV_BY_BYN_CONTEXT, TAXI_UBER_BV_BY_BYN_CONTEXT_SPENDABLE),
    (TAXI_UBER_BV_BYN_BY_BYN_CONTEXT, TAXI_UBER_BV_BYN_BY_BYN_CONTEXT_SPENDABLE),
    (TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE),
    (TAXI_ISRAEL_CONTEXT, TAXI_ISRAEL_CONTEXT_SPENDABLE),
    (TAXI_YANGO_ISRAEL_CONTEXT, TAXI_YANGO_ISRAEL_CONTEXT_SPENDABLE),
    (TAXI_YANDEX_GO_SRL_CONTEXT, TAXI_YANDEX_GO_SRL_CONTEXT_SPENDABLE),
    (TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT, TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT_SPENDABLE), # дозавести терминалы https://st.yandex-team.ru/TRUSTDUTY-637
    (TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT, TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT_SPENDABLE),
    (TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT, TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT_SPENDABLE),
    (TAXI_GHANA_USD_CONTEXT, TAXI_GHANA_USD_CONTEXT_SPENDABLE),
    (TAXI_BOLIVIA_USD_CONTEXT, TAXI_BOLIVIA_USD_CONTEXT_SPENDABLE),
    (TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE),
    (TAXI_ZA_USD_CONTEXT, TAXI_ZA_USD_CONTEXT_SPENDABLE),
]

PAYMETHODS = [
    # Coupon(),
    pytest.mark.smoke(
        Subsidy()),
    # BrandingSubsidy(),
    # GuaranteeFee(),
    # TripBonus(),
    # PersonnelBonus(),
    # DiscountTaxi(),
    # SupportCoupon(),
    # BookingSubsidy(),
    # Dryclean(),
    # Cashrunner()
]

PAYMENT_TYPES = [
    PaymentType.SUBSIDY,
    # PaymentType.COUPON,
    # PaymentType.COMPENSATION,
    # PaymentType.BRANDING_SUBSIDY,
    # PaymentType.GUARANTEE_FEE,
    # PaymentType.TRIP_BONUS,
    # PaymentType.PERSONNEL_BONUS,
    # PaymentType.DISCOUNT_TAXI,
    # PaymentType.SUPPORT_COUPON,
    # PaymentType.BOOKING_SUBSIDY,
    # PaymentType.CASHRUNNER,
    # PaymentType.DRYCLEAN,
    # PaymentType.DRIVER_REFERRALS,
    # PaymentType.CARGO_SUBSIDY,
    # PaymentType.CARGO_COUPON,
    # PaymentType.DELIVERY_SUBSIDY,
    # PaymentType.DELIVERY_COUPON,
    # PaymentType.PARTNERS_LEARNING_CENTER,
    # PaymentType.PARTNERS_MOTIVATION_PROGRAM,
]


@pytest.mark.parametrize("paymethod", PAYMETHODS, ids=lambda p: p.title.upper())
def test_taxi_donate_payments(paymethod, switch_to_trust, mock_simple_api):
    context_general, context_spendable = TAXI_RU_CONTEXT, TAXI_RU_CONTEXT_SPENDABLE
    switch_to_trust(service=context_spendable.service)
    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context_spendable, context_general)

    service_order_id, trust_payment_id, purchase_token, payment_id = steps.SimpleApi.create_trust_payment(
        context_spendable.service, service_product_id, paymethod=paymethod, user=simpleapi_defaults.USER_ANONYMOUS,
        currency=context_spendable.payment_currency, order_dt=ORDER_DT)

    steps.CommonPartnerSteps.export_payment(payment_id)

    expected_data = create_expected_data(context_spendable, client_id, person_id, contract_id,
                                         payment_id, trust_payment_id, paymethod.id,
                                         service_order_id_str=str(service_order_id))

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.Taxi_Payments))
@pytest.mark.parametrize("context_general, context_spendable", CASES, ids=lambda g, s: s.name)
@pytest.mark.parametrize("payment_type", PAYMENT_TYPES, ids=lambda p: p.upper())
def test_taxi_tlog_donate_payments(context_general, context_spendable, payment_type, shared_data_cache):
    client_id, person_id, contract_id = create_client_and_contract_tlog(context_spendable, context_general)
    side_payment_id, transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, ORDER_DT, simpleapi_defaults.DEFAULT_PRICE,
                                                          payment_type, context_spendable.service.id,
                                                          currency=context_spendable.payment_currency,
                                                          extra_dt_0=ORDER_DT, extra_str_0=EXTRA_STR)

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    expected_data = create_expected_data(context_spendable, client_id, person_id, contract_id,
                                         side_payment_id, transaction_id, payment_type, service_order_id_str=EXTRA_STR)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_payment_id, source='sidepayment')
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize("paymethod", PAYMETHODS, ids=lambda p: p.title.upper())
def test_taxi_donate_refunds(paymethod, switch_to_trust, mock_simple_api):
    context_general, context_spendable = TAXI_RU_CONTEXT, TAXI_RU_CONTEXT_SPENDABLE
    switch_to_trust(service=context_spendable.service)
    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context_spendable, context_general)

    service_order_id, trust_payment_id, purchase_token, payment_id = steps.SimpleApi.create_trust_payment(
        context_spendable.service, service_product_id, paymethod=paymethod, user=simpleapi_defaults.USER_ANONYMOUS,
        currency=context_spendable.payment_currency, order_dt=ORDER_DT)
    steps.CommonPartnerSteps.export_payment(payment_id)

    trust_refund_id, refund_id = steps.SimpleApi.create_refund(context_spendable.service, service_order_id, trust_payment_id)
    steps.CommonPartnerSteps.export_payment(refund_id)

    expected_data = create_expected_data(context_spendable, client_id, person_id, contract_id,
                                         payment_id, trust_payment_id, paymethod.id, trust_refund_id,
                                         service_order_id_str=str(service_order_id))
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.Taxi_Payments))
@pytest.mark.parametrize("context_general, context_spendable", CASES, ids=lambda g, s: s.name)
@pytest.mark.parametrize("payment_type", PAYMENT_TYPES, ids=lambda p: p.upper())
def test_taxi_tlog_donate_refunds(context_general, context_spendable, payment_type, shared_data_cache):
    client_id, person_id, contract_id = create_client_and_contract_tlog(context_spendable, context_general)
    side_payment_id, transaction_id_payment = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, ORDER_DT, simpleapi_defaults.DEFAULT_PRICE*D('2'),
                                                          payment_type, context_spendable.service.id,
                                                          currency=context_spendable.payment_currency,
                                                          extra_dt_0=ORDER_DT)

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    side_refund_id, transaction_id_refund = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, ORDER_DT, simpleapi_defaults.DEFAULT_PRICE,
                                                          payment_type, context_spendable.service.id,
                                                          currency=context_spendable.payment_currency,
                                                          extra_dt_0=ORDER_DT,
                                                          transaction_type=TransactionType.REFUND,
                                                          orig_transaction_id=transaction_id_payment,
                                                          extra_str_0=EXTRA_STR)

    steps.ExportSteps.create_export_record_and_export(side_refund_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    expected_data = create_expected_data(context_spendable, client_id, person_id, contract_id,
                                         side_refund_id, transaction_id_payment, payment_type, transaction_id_refund,
                                         service_order_id_str=EXTRA_STR)

    payment_data = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_refund_id,
                                                                          transaction_type=TransactionType.REFUND,
                                                                          source='sidepayment')
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


def test_taxi_tlog_donate_skip_processed_through_trust():
    context_general = TAXI_RU_CONTEXT
    context_spendable = TAXI_RU_CONTEXT_SPENDABLE
    paymethod = Subsidy()
    client_id, person_id, contract_id = create_client_and_contract_tlog(context_spendable, context_general)

    side_payment_id, transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, ORDER_DT, simpleapi_defaults.DEFAULT_PRICE,
                                                          paymethod.id, context_spendable.service.id,
                                                          currency=context_spendable.payment_currency,
                                                          extra_dt_0=ORDER_DT,
                                                          payload='{\"ProcessThroughTrust\": 1}')

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    payment_data = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_payment_id, source='sidepayment')
    utils.check_that(payment_data, empty(), u"Проверяем, что строки в thirdparty не появилось")


# АХТУНГ! Тест не удалять и не переносить в юнит-тесты, на него смотрит аудит
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_8_Taxi))
def test_invalid_spendable_contract_absence(switch_to_trust, mock_simple_api):
    context_general = TAXI_RU_CONTEXT
    context_spendable = TAXI_RU_CONTEXT_SPENDABLE
    paymethod = Coupon()
    switch_to_trust(service=context_spendable.service)

    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(context_spendable.service)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context_general, client_id=client_id,
                                                                               additional_params={'start_dt': START_DT})

    service_order_id, trust_payment_id, purchase_token, payment_id = steps.SimpleApi.create_trust_payment(
        Services.TAXI_DONATE, service_product_id, paymethod=paymethod, user=simpleapi_defaults.USER_ANONYMOUS,
        currency=context_spendable.payment_currency, order_dt=ORDER_DT)

    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        steps.CommonPartnerSteps.export_payment(payment_id)

    expected_error = 'TrustPayment({}) delayed: no active contracts found for client {}'.format(payment_id, client_id)

    utils.check_that(xmlrpc_error.value.response, contains_string(expected_error), u"Проверяем текст ошибки")


def test_tlog_invalid_spendable_contract_absence():
    context_spendable = TAXI_RU_CONTEXT_SPENDABLE
    paymethod = Coupon()

    client_id = steps.ClientSteps.create(params={'service_id': context_spendable.service.id})
    resource_dt = BASE_DATE - relativedelta(days=21)

    side_payment_id, transaction_id_payment = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, ORDER_DT, simpleapi_defaults.DEFAULT_PRICE*D('2'),
                                                          paymethod.id, context_spendable.service.id,
                                                          currency=context_spendable.payment_currency,
                                                          extra_dt_0=resource_dt)

    steps.ExportSteps.create_export_record(side_payment_id, classname=Export.Classname.SIDE_PAYMENT)
    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        steps.CommonPartnerSteps.export_sidepayment(sidepayment_id=side_payment_id)
    expected_error = 'SidePayment({}) delayed: no active contracts found for client {}'.format(side_payment_id, client_id)

    utils.check_that(xmlrpc_error.value.response, contains_string(expected_error), u"Проверяем текст ошибки")


# АХТУНГ! Тест не удалять и не переносить в юнит-тесты, на него смотрит аудит
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_8_Taxi))
def test_invalid_general_contract(switch_to_trust, mock_simple_api):
    context_general = TAXI_RU_CONTEXT
    context_spendable = TAXI_RU_CONTEXT_SPENDABLE
    paymethod = Coupon()
    finish_dt = datetime.now()
    switch_to_trust(service=context_spendable.service)

    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context_spendable, context_general,
                                                                                       finish_dt=finish_dt)

    service_order_id, trust_payment_id, purchase_token, payment_id = steps.SimpleApi.create_trust_payment(
        Services.TAXI_DONATE, service_product_id, paymethod=paymethod, user=simpleapi_defaults.USER_ANONYMOUS,
        currency=context_spendable.payment_currency, order_dt=ORDER_DT)

    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        steps.CommonPartnerSteps.export_payment(payment_id)

    expected_error = 'TrustPayment({}) delayed: no active contracts found for client {}'.format(payment_id, client_id)

    utils.check_that(xmlrpc_error.value.response, contains_string(expected_error), u"Проверяем текст ошибки")


def test_tlog_invalid_general_contract():
    context_general = TAXI_RU_CONTEXT
    context_spendable = TAXI_RU_CONTEXT_SPENDABLE
    paymethod = Coupon()
    finish_dt = datetime.now()

    client_id, person_id, contract_id = create_client_and_contract_tlog(context_spendable, context_general,
                                                                        finish_dt=finish_dt)
    resource_dt = BASE_DATE - relativedelta(days=23)

    side_payment_id, transaction_id_payment = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, ORDER_DT,
                                                          simpleapi_defaults.DEFAULT_PRICE * D('2'),
                                                          paymethod.id, context_spendable.service.id,
                                                          currency=context_spendable.payment_currency,
                                                          extra_dt_0=resource_dt)

    steps.ExportSteps.create_export_record(side_payment_id, classname=Export.Classname.SIDE_PAYMENT)
    sidepayment_id = steps.CommonPartnerSteps.get_sidepayment_id(context_spendable.service.id, transaction_id_payment)
    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        steps.CommonPartnerSteps.export_sidepayment(sidepayment_id=sidepayment_id)
    expected_error = 'SidePayment({}) delayed: no active contracts found for client {}'.format(sidepayment_id, client_id)

    utils.check_that(xmlrpc_error.value.response, contains_string(expected_error), u"Проверяем текст ошибки")


# ------------------------------------------------------------
# Utils
def create_client_and_contract(context_spendable, context_general, finish_dt=None):
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(context_spendable.service)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context_general, client_id=client_id,
                                                                               additional_params={'start_dt': START_DT,
                                                                                                  'finish_dt': finish_dt})

    partner_person_id = steps.PersonSteps.create(client_id, context_spendable.person_type.code, {'is-partner': '1'})
    partner_contract_id, _ = steps.ContractSteps.accept_taxi_offer(partner_person_id, contract_id,
                                                                   nds=context_spendable.nds.nds_id)

    return client_id, partner_person_id, partner_contract_id, service_product_id


def create_client_and_contract_tlog(context_spendable, context_general, finish_dt=None):
    client_id = steps.ClientSteps.create(params={'service_id': context_spendable.service.id})
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context_general, client_id=client_id,
                                                                               additional_params={'start_dt': START_DT,
                                                                                                  'finish_dt': finish_dt})

    partner_person_id = steps.PersonSteps.create(client_id, context_spendable.person_type.code, {'is-partner': '1'})
    partner_contract_id, _ = steps.ContractSteps.accept_taxi_offer(partner_person_id, contract_id,
                                                                   nds=context_spendable.nds.nds_id)

    return client_id, partner_person_id, partner_contract_id


def create_expected_data(context, client_id, person_id, contract_id, payment_id, trust_payment_id,
                         payment_type, trust_refund_id=None, service_order_id_str=None):

    expected_tpt_data = steps.SimpleApi.create_expected_tpt_row(
        context, client_id, contract_id, person_id,
        trust_payment_id, payment_id,
        trust_refund_id=trust_refund_id,
        payment_type=str(payment_type).rpartition('virtual::')[2],
        service_order_id_str=service_order_id_str)
    return [expected_tpt_data]
