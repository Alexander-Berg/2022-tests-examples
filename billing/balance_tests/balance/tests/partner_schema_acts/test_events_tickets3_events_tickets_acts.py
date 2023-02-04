# -*- coding: utf-8 -*-

__author__ = 'atkaya'

from decimal import Decimal as D

import pytest
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
import balance.balance_db as db
from btestlib import utils
from btestlib.constants import PaymentType, TransactionType, PaysysType, ServiceCode
from btestlib.data.partner_contexts import EVENTS_TICKETS3_RU_CONTEXT, EVENTS_TICKETS_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

CONTEXTS = [EVENTS_TICKETS3_RU_CONTEXT, EVENTS_TICKETS_CONTEXT]
parametrize_context = pytest.mark.parametrize('context', CONTEXTS, ids=lambda x: x.name)

pytestmark = [
    reporter.feature(Features.ACT, Features.EVENTS_TICKETS_NEW)
]

payment_sum = D('3000.4')
refund_sum = D('1000.2')
promo_payment_sum = D('10.34')
promo_refund_sum = D('3.71')
discount_payment_sum = D('66.08')
discount_refund_sum = D('6.3')
certificate_payment = D('1200.25')
fake_refund_payment = D('980.03')

_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


def create_partner_client_and_contract_for_acts(context):
    # создаем клиента-партнера
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, additional_params={'start_dt': first_month_start_dt})

    return client_id, person_id, contract_id


def create_completions(context, client_id, contract_id, person_id, dt, coef=D('1')):
    invoice_eid = steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code, 1)
    sum_ = steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, [
        {'transaction_type': TransactionType.PAYMENT, 'yandex_reward': payment_sum * coef, 'invoice_eid': invoice_eid},
        {'transaction_type': TransactionType.REFUND, 'yandex_reward': refund_sum * coef, 'invoice_eid': invoice_eid},

        {'transaction_type': TransactionType.PAYMENT, 'amount_fee': D('100500.3'), 'invoice_eid': invoice_eid},
        {'transaction_type': TransactionType.REFUND, 'amount_fee': D('100.1'), 'invoice_eid': invoice_eid},

        {'transaction_type': TransactionType.PAYMENT, 'payment_type': PaymentType.COMPENSATION,
         'paysys_type_cc': PaysysType.YANDEX, 'invoice_eid': invoice_eid},

        {'transaction_type': TransactionType.PAYMENT, 'yandex_reward': discount_payment_sum * coef,
         'payment_type': PaymentType.COMPENSATION_DISCOUNT, 'paysys_type_cc': PaysysType.YANDEX,
         'invoice_eid': invoice_eid},
        {'transaction_type': TransactionType.REFUND, 'yandex_reward': discount_refund_sum * coef,
         'payment_type': PaymentType.COMPENSATION_DISCOUNT, 'paysys_type_cc': PaysysType.YANDEX,
         'invoice_eid': invoice_eid},

        {'transaction_type': TransactionType.PAYMENT, 'yandex_reward': promo_payment_sum * coef,
         'payment_type': PaymentType.NEW_PROMOCODE, 'paysys_type_cc': PaysysType.YANDEX,
         'invoice_eid': invoice_eid},
        {'transaction_type': TransactionType.REFUND, 'yandex_reward': promo_refund_sum * coef,
         'payment_type': PaymentType.NEW_PROMOCODE, 'paysys_type_cc': PaysysType.YANDEX,
         'invoice_eid': invoice_eid},

        {'transaction_type': TransactionType.PAYMENT, 'yandex_reward': certificate_payment * coef,
         'payment_type': PaymentType.AFISHA_CERTIFICATE, 'paysys_type_cc': PaysysType.YANDEX,
         'invoice_eid': invoice_eid},
        {'transaction_type': TransactionType.REFUND, 'yandex_reward': fake_refund_payment * coef,
         'payment_type': PaymentType.FAKE_REFUND, 'paysys_type_cc': PaysysType.FAKE_REFUND,
         'invoice_eid': invoice_eid},

    ], sum_key='yandex_reward')
    return sum_


def get_wo_nds_product_id(context):
    query = 'select id from bo.t_product where engine_id = :service_id and service_code = :service_code'
    return db.balance().execute(
        query, {'service_id': context.service.id, 'service_code': ServiceCode.YANDEX_SERVICE_WO_VAT},
        descr='Выбираем главный продукт из t_partner_product по валюте и сервису')[0]['id']


def create_wo_nds_completions(context, client_id, contract_id, person_id, dt, coef=D('1')):
    invoice_eid = steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code, 0)
    product_id = get_wo_nds_product_id(context)
    sum_ = steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, [
        {'product_id': product_id, 'transaction_type': TransactionType.REFUND, 'amount': payment_sum * coef,
         'invoice_eid': invoice_eid, 'paysys_type_cc': PaysysType.NETTING_WO_NDS},
        {'product_id': product_id, 'transaction_type': TransactionType.REFUND, 'amount': refund_sum * coef,
         'invoice_eid': invoice_eid, 'paysys_type_cc': PaysysType.NETTING_WO_NDS},
    ], sum_key='amount')
    return -sum_


def generate_act(client_id, contract_id, dt):
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, dt)


def create_expected_invoice(context, contract_id, person_id, amount, dt, **kw):
    params = {'paysys_id': context.paysys.id, 'dt': dt}
    params.update(kw)
    return steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id, person_id, amount, **params)


def create_expected_invoice_wo_nds(context, contract_id, person_id, amount, dt):
    return create_expected_invoice(
        context, contract_id, person_id, amount, dt,
        paysys_id=context.paysys_wo_nds.id, nds_pct=D('0'), nds=0)


# проверка конца месяца, когда данных нет
@parametrize_context
def test_tickets_act_wo_data(context):
    client_id, person_id, contract_id = create_partner_client_and_contract_for_acts(context)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_end_dt,
                                                                   manual_export=False)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data_first_month = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_invoice = create_expected_invoice(context, contract_id, person_id, D('0'), first_month_start_dt)

    expected_invoice_wo_nds = create_expected_invoice_wo_nds(
        context, contract_id, person_id, D('0'), first_month_start_dt)

    expected_invoice_data = [expected_invoice, expected_invoice_wo_nds]

    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_first_month, empty(),
                     'Сравниваем данные из акта с шаблоном')


# проверка закрытия двух месяцев с нарастающим итогом
@pytest.mark.smoke
@parametrize_context
def test_tickets_act(context):
    # создаем договор
    client_id, person_id, contract_id = create_partner_client_and_contract_for_acts(context)

    # добавляем открутки в первом месяце и закрываем первый месяц
    first_month_sum = create_completions(context, client_id, contract_id, person_id, first_month_start_dt)
    first_month_sum_wo_nds = create_wo_nds_completions(context, client_id, contract_id, person_id, first_month_start_dt)
    generate_act(client_id, contract_id, first_month_end_dt)

    # добавляем новые открутки в первом месяце (для проверки нарастающего итога) и во второй месяц, закрываем второй месяц
    first_month_sum_new = create_completions(context, client_id, contract_id, person_id, first_month_end_dt, coef=D('0.3'))
    second_month_sum = create_completions(context, client_id, contract_id, person_id, second_month_end_dt, coef=D('0.2'))

    first_month_sum_new_wo_nds = create_wo_nds_completions(context, client_id, contract_id, person_id,
                                                           first_month_end_dt, coef=D('0.3'))
    second_month_sum_wo_nds = create_wo_nds_completions(context, client_id, contract_id, person_id,
                                                        second_month_end_dt, coef=D('0.2'))
    generate_act(client_id, contract_id, second_month_end_dt)

    # забираем данные по счету из базы
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # забираем данные по актам из базы
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    # создаем шаблон для сравнения счета
    expected_invoice = create_expected_invoice(
        context, contract_id, person_id,
        utils.dround2(first_month_sum + first_month_sum_new + second_month_sum), first_month_end_dt)

    expected_invoice_wo_nds = create_expected_invoice_wo_nds(
        context, contract_id, person_id,
        utils.dround2(first_month_sum_wo_nds + first_month_sum_new_wo_nds + second_month_sum_wo_nds),
        first_month_end_dt)

    expected_invoice_data = [
        expected_invoice,
        expected_invoice_wo_nds
    ]

    # создаем шаблон для сравнения актов
    expected_act_data = [
        steps.CommonData.create_expected_act_data(first_month_sum, first_month_end_dt),
        steps.CommonData.create_expected_act_data(
            utils.dround2(first_month_sum_new + second_month_sum), second_month_end_dt),

        steps.CommonData.create_expected_act_data(first_month_sum_wo_nds, first_month_end_dt),
        steps.CommonData.create_expected_act_data(
            utils.dround2(first_month_sum_new_wo_nds + second_month_sum_wo_nds), second_month_end_dt),
    ]

    # сравниваем счет и акты с ожидаемыми
    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из актов с шаблоном')
