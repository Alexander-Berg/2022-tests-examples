# -*- coding: utf-8 -*-

__author__ = 'atkaya'

from datetime import datetime

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string

from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils, reporter
from btestlib.constants import Services, TransactionType, PaysysType, Nds
from btestlib.data import simpleapi_defaults
from btestlib.matchers import contains_dicts_with_entries
from simpleapi.common.payment_methods import Coupon, Subsidy, BrandingSubsidy, GuaranteeFee, \
    TripBonus, PersonnelBonus, DiscountTaxi, SupportCoupon, BookingSubsidy, Dryclean, Cashrunner
from temp.igogor.balance_objects import Contexts

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)
ORDER_DT = utils.Date.moscow_offset_dt()
DT = datetime.now()

pytestmark = [
    pytest.mark.tickets('BALANCE-22816', 'BALANCE-28558'),
    reporter.feature(Features.TAXI, Features.TRUST, Features.PAYMENT, Features.COUPON, Features.SUBSIDY),
    pytest.mark.usefixtures("switch_to_pg")
]

CONTEXTS = [
    Contexts.TAXI_RU_CONTEXT,
    Contexts.TAXI_BV_ARM_USD_CONTEXT,
    Contexts.TAXI_UBER_BY_CONTEXT
]

PAYMETHODS = [
    Coupon(),
    Subsidy(),
    BrandingSubsidy(),
    GuaranteeFee(),
    TripBonus(),
    PersonnelBonus(),
    DiscountTaxi(),
    SupportCoupon(),
    BookingSubsidy(),
    Dryclean(),
    Cashrunner()
]


# @pytest.mark.parametrize("context", CONTEXTS, ids=lambda c: c.name)
# @pytest.mark.parametrize("paymethod", PAYMETHODS, ids=lambda p: p.title.upper())
def test_taxi_donate_refunds():
    context = Contexts.TAXI_RU_CONTEXT
    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context)
    service_product_id = steps.SimpleApi.create_service_product(Services.TAXI_DONATE, client_id)
    # добавить экспорт в оебс (клиент, договор, допник)
    steps.CommonSteps.export('OEBS', 'Client', client_id)
    steps.CommonSteps.export('OEBS', 'Contract', contract_id)
    collateral_id = steps.ContractSteps.get_collateral_id(contract_id)
    steps.CommonSteps.export('OEBS', 'ContractCollateral', collateral_id)

    #явно создать платежи обоих типов
    service_order_id_dryclean, trust_payment_id_dryclean, purchase_token, payment_id_dryclean = steps.SimpleApi.create_trust_payment(
        Services.TAXI_DONATE, service_product_id, paymethod=Dryclean(), user=simpleapi_defaults.USER_ANONYMOUS,
        currency=context.currency, order_dt=ORDER_DT)
    # делать для каждого
    steps.CommonPartnerSteps.export_payment(payment_id_dryclean)

    service_order_id_cashrunner, trust_payment_id_cashrunner, purchase_token, payment_id_cashrunner = steps.SimpleApi.create_trust_payment(
        Services.TAXI_DONATE, service_product_id, paymethod=Cashrunner(), user=simpleapi_defaults.USER_ANONYMOUS,
        currency=context.currency, order_dt=ORDER_DT)
    steps.CommonPartnerSteps.export_payment(payment_id_cashrunner)


    #тоже две строчки, в прайсе указать сумму меньше платежа
    trust_refund_id, refund_id_dryclean = steps.SimpleApi.create_refund(Services.TAXI_DONATE, service_order_id_dryclean,
                                                               trust_payment_id_dryclean,
                                                               delta_amount=simpleapi_defaults.DEFAULT_PRICE/3.)

    trust_refund_id, refund_id_cashrunner = steps.SimpleApi.create_refund(Services.TAXI_DONATE, service_order_id_cashrunner,
                                                                          trust_payment_id_cashrunner,
                                                               delta_amount=simpleapi_defaults.DEFAULT_PRICE/3.)

#делать для каждого
    steps.CommonPartnerSteps.export_payment(refund_id_dryclean)
    steps.CommonPartnerSteps.export_payment(refund_id_cashrunner)


    #забрать айди транзакции из t_thirdparty_transaction и для каждого сделать выгрузку
    #и позвать генерацию акта

    # expected_data = create_expected_refund(context, client_id, person_id, contract_id,
    #                                        payment_id, trust_payment_id, trust_refund_id, paymethod)
    # payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)
    # utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


# ------------------------------------------------------------
# Utils

def create_general_client_and_contract(context, finish_dt=None):
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(Services.TAXI_DONATE)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay', utils.remove_empty({
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT,
        'IS_SIGNED': START_DT.isoformat(),
        'PARTNER_COMMISSION_PCT2': context.commission_pct,
        'SERVICES': [service.id for service in context.services],
        'FIRM': context.firm.id,
        'CURRENCY': context.currency.num_code,
        'COUNTRY': context.region.id,
        'FINISH_DT': finish_dt
    }))
    return client_id, contract_id, service_product_id


def create_client_and_contract(context, finish_dt=None):
    client_id, contract_id, service_product_id = create_general_client_and_contract(context, finish_dt)

    partner_person_id = steps.PersonSteps.create(client_id, context.person_type.code, {'is-partner': '1'})
    partner_contract_id, _ = steps.ContractSteps.accept_taxi_offer(partner_person_id, contract_id,
                                                                   nds=Nds.get(context.nds))

    return client_id, partner_person_id, partner_contract_id, service_product_id


def create_expected_transaction_common(context, client_id, person_id, contract_id, payment_id,
                                       trust_id, trust_payment_id, paymethod):
    paymethod = str(paymethod).rpartition('virtual:')[2][1:-1]
    return {
        'amount_fee': None,
        'yandex_reward': None,
        'internal': None,
        'client_id': None,
        'client_amount': None,
        'invoice_eid': None,
        'invoice_commission_sum': None,
        'row_paysys_commission_sum': None,
        'paysys_partner_id': None,
        'service_id': Services.TAXI_DONATE.id,
        'paysys_type_cc': PaysysType.TAXI,
        'product_id': None,
        'service_product_id': None,

        'oebs_org_id': context.firm.oebs_org_id,
        'currency': context.currency.char_code,
        'iso_currency': context.currency.iso_code,
        'partner_currency': context.currency.char_code,
        'partner_iso_currency': context.currency.iso_code,
        'commission_currency': context.currency.char_code,
        'commission_iso_currency': context.currency.iso_code,
        'payment_type': paymethod,

        'partner_id': client_id,
        'contract_id': contract_id,
        'person_id': person_id,
        'trust_id': trust_id,
        'trust_payment_id': trust_payment_id,
        'payment_id': payment_id,

        'amount': simpleapi_defaults.DEFAULT_PRICE
    }


def create_expected_payment(context, client_id, person_id, contract_id, payment_id, trust_payment_id,
                            paymethod):
    expected_payment = create_expected_transaction_common(context, client_id, person_id, contract_id,
                                                          payment_id, trust_payment_id, trust_payment_id, paymethod)

    expected_payment.update({
        'transaction_type': TransactionType.PAYMENT.name
    })

    return [expected_payment]


def create_expected_refund(context, client_id, person_id, contract_id, payment_id, trust_payment_id,
                           trust_refund_id, paymethod):
    expected_refund = create_expected_transaction_common(context, client_id, person_id, contract_id,
                                                         payment_id, trust_refund_id, trust_payment_id, paymethod)

    expected_refund.update({
        'transaction_type': TransactionType.REFUND.name
    })

    return [expected_refund]



def test_taxi_donate_refunds_tmp():
    context = Contexts.TAXI_RU_CONTEXT
    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context)
    service_product_id = steps.SimpleApi.create_service_product(Services.TAXI_DONATE, client_id)
    # добавить экспорт в оебс (клиент, договор, допник)
    # steps.CommonSteps.export('OEBS', 'Client', client_id)
    steps.CommonSteps.export('OEBS', 'Contract', contract_id)
    collateral_id = steps.ContractSteps.get_collateral_id(contract_id)
    steps.CommonSteps.export('OEBS', 'ContractCollateral', collateral_id)

    #явно создать платежи обоих типов
    service_order_id_dryclean, trust_payment_id_dryclean, purchase_token, payment_id_dryclean = steps.SimpleApi.create_trust_payment(
        Services.TAXI_DONATE, service_product_id, paymethod=Dryclean(), user=simpleapi_defaults.USER_ANONYMOUS,
        currency=context.currency, order_dt=ORDER_DT)
    # делать для каждого
    steps.CommonPartnerSteps.export_payment(payment_id_dryclean)

    service_order_id_cashrunner, trust_payment_id_cashrunner, purchase_token, payment_id_cashrunner = steps.SimpleApi.create_trust_payment(
        Services.TAXI_DONATE, service_product_id, paymethod=Cashrunner(), user=simpleapi_defaults.USER_ANONYMOUS,
        currency=context.currency, order_dt=ORDER_DT)
    steps.CommonPartnerSteps.export_payment(payment_id_cashrunner)


    #тоже две строчки, в прайсе указать сумму меньше платежа
    trust_refund_id, refund_id_dryclean = steps.SimpleApi.create_refund(Services.TAXI_DONATE, service_order_id_dryclean,
                                                               trust_payment_id_dryclean,
                                                               delta_amount=simpleapi_defaults.DEFAULT_PRICE/Decimal('3'))

    trust_refund_id, refund_id_cashrunner = steps.SimpleApi.create_refund(Services.TAXI_DONATE, service_order_id_cashrunner,
                                                                          trust_payment_id_cashrunner,
                                                               delta_amount=simpleapi_defaults.DEFAULT_PRICE/Decimal('3'))

#делать для каждого
    steps.CommonPartnerSteps.export_payment(refund_id_dryclean)
    steps.CommonPartnerSteps.export_payment(refund_id_cashrunner)

    thirdparty_ids = db.balance().execute("SELECT ID FROM bo.t_thirdparty_transactions WHERE contract_id =:contract_id",
                              {'contract_id': contract_id})

    for i in range(len(thirdparty_ids)):
        steps.CommonSteps.export('OEBS', 'ThirdPartyTransaction', str(thirdparty_ids[i]['id']))


    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, utils.Date.first_day_of_month(datetime.now()))
    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    #забрать айди транзакции из t_thirdparty_transaction и для каждого сделать выгрузку
    #и позвать генерацию акта

    # expected_data = create_expected_refund(context, client_id, person_id, contract_id,
    #                                        payment_id, trust_payment_id, trust_refund_id, paymethod)
    # payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)
    # utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')