# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import empty

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import PaymentType, TransactionType, PaysysType, ServiceCode
from btestlib.data.partner_contexts import EVENTS_TICKETS2_RU_CONTEXT, EVENTS_TICKETS2_KZ_CONTEXT
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries

from hamcrest import has_length

CONTEXTS = [EVENTS_TICKETS2_RU_CONTEXT, EVENTS_TICKETS2_KZ_CONTEXT]
parametrize_context = pytest.mark.parametrize('context', CONTEXTS, ids=lambda x: x.name)

pytestmark = [
    reporter.feature(Features.ACT, Features.EVENTS_TICKETS_NEW)
]

PAYMENT_TECH_CLIENT = D('1000.4')
REFUND_TECH_CLIENT = D('12.3')

PAYMENT = D('1345.2')
REFUND = D('12.3')
DISCOUNT_PAYMENT = D('300.4')
DISCOUNT_REFUND = D('250.1')
PROMO_PAYMENT = D('34.2')
PROMO_REFUND = D('1.8')
CERTIFICATE_PAYMENT = D('220.3')
FAKE_REFUND_PAYMENT = D('200.2')


_, _, month2_start_dt, month2_end_dt, month3_start_dt, month3_end_dt = \
    utils.Date.previous_three_months_start_end_dates(dt=datetime.datetime.today())
contract_start_dt = month2_start_dt


def create_contract(context):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, additional_params={'start_dt': contract_start_dt})
    return client_id, person_id, contract_id


# правильность проставление тех клиента в tpt проверяется в тестах на платежи,
# здесь создаем фейковые строки с нужным нам договором
def create_completions_tech_contract(context, client_id, contract_id, person_id, dt, coef=D('1')):
    invoice_eid = steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code, 1)
    sum_ = steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, [
        {'transaction_type': TransactionType.PAYMENT, 'internal': 1, 'yandex_reward': PAYMENT_TECH_CLIENT * coef,
         'invoice_eid': invoice_eid},
        {'transaction_type': TransactionType.REFUND, 'internal': 1, 'yandex_reward': REFUND_TECH_CLIENT * coef,
         'invoice_eid': invoice_eid}
    ], sum_key='yandex_reward')
    return sum_


def create_completions_partner(context, client_id, contract_id, person_id, dt, coef=D('1')):
    invoice_eid = steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code, 1)
    sum_ = steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, [
        {'transaction_type': TransactionType.PAYMENT, 'yandex_reward': PAYMENT * coef, 'invoice_eid': invoice_eid},
        {'transaction_type': TransactionType.REFUND, 'yandex_reward': REFUND * coef, 'invoice_eid': invoice_eid},

        {'transaction_type': TransactionType.PAYMENT, 'payment_type': PaymentType.COMPENSATION,
         'paysys_type_cc': PaysysType.YANDEX, 'invoice_eid': invoice_eid},

        {'transaction_type': TransactionType.PAYMENT, 'yandex_reward': DISCOUNT_PAYMENT * coef,
         'payment_type': PaymentType.COMPENSATION_DISCOUNT, 'invoice_eid': invoice_eid},
        {'transaction_type': TransactionType.REFUND, 'yandex_reward': DISCOUNT_REFUND * coef,
         'payment_type': PaymentType.COMPENSATION_DISCOUNT, 'invoice_eid': invoice_eid},

        {'transaction_type': TransactionType.PAYMENT, 'yandex_reward': PROMO_PAYMENT * coef,
         'payment_type': PaymentType.NEW_PROMOCODE, 'paysys_type_cc': PaysysType.YANDEX, 'invoice_eid': invoice_eid},
        {'transaction_type': TransactionType.REFUND, 'yandex_reward': PROMO_REFUND * coef,
         'payment_type': PaymentType.NEW_PROMOCODE, 'paysys_type_cc': PaysysType.YANDEX, 'invoice_eid': invoice_eid},

        {'transaction_type': TransactionType.PAYMENT, 'yandex_reward': CERTIFICATE_PAYMENT * coef,
         'payment_type': PaymentType.AFISHA_CERTIFICATE, 'paysys_type_cc': PaysysType.YANDEX,
         'invoice_eid': invoice_eid},
        {'transaction_type': TransactionType.REFUND, 'yandex_reward': FAKE_REFUND_PAYMENT * coef,
         'payment_type': PaymentType.FAKE_REFUND, 'paysys_type_cc': PaysysType.FAKE_REFUND, 'invoice_eid': invoice_eid},
    ], sum_key='yandex_reward')
    return sum_


def get_wo_nds_product_id(context):
    query = 'select id from bo.t_product where engine_id = :service_id and service_code = :service_code'
    return db.balance().execute(
        query, {'service_id': context.service.id, 'service_code': ServiceCode.YANDEX_SERVICE_WO_VAT},
        descr='Выбираем главный продукт из t_partner_product по валюте и сервису')[0]['id']


def create_completions_wo_nds(context, client_id, contract_id, person_id, dt, coef=D('1')):
    invoice_eid = steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code, 0)
    product_id = get_wo_nds_product_id(context)
    sum_ = steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, [
        {'product_id': product_id, 'transaction_type': TransactionType.REFUND, 'amount': PAYMENT * coef,
         'invoice_eid': invoice_eid, 'paysys_type_cc': PaysysType.NETTING_WO_NDS},
        {'product_id': product_id, 'transaction_type': TransactionType.REFUND, 'amount': REFUND * coef,
         'invoice_eid': invoice_eid, 'paysys_type_cc': PaysysType.NETTING_WO_NDS},
    ], sum_key='amount')
    return -sum_


def create_expected_invoice(context, contract_id, person_id, amount, dt, **kw):
    params = {'paysys_id': context.paysys.id, 'dt': dt}
    params.update(kw)
    return steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id, person_id, amount, **params)


def create_expected_invoice_wo_nds(context, contract_id, person_id, amount, dt):
    return create_expected_invoice(
        context, contract_id, person_id, amount, dt,
        paysys_id=context.paysys_wo_nds.id, nds_pct=D('0'), nds=0)


def get_acts_for(client_id):
    query = "SELECT * FROM t_act WHERE client_id = :client_id"
    params = {'client_id': client_id}
    act_data = db.balance().execute(query, params)

    if not act_data:
        raise utils.ServiceError(u"Тест остановлен: нет данных в акте")
    return act_data


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new')
# проверка конца месяца для договора с тех клиентом или партнером, когда данных нет
# пока различия закрытия тех клиента и партнера нет, тест один
@pytest.mark.tickets('BALANCE-22273')
@parametrize_context
def test_tickets2_act_wo_data(context):
    client_id, person_id, contract_id = create_contract(context)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id, contract_id, month2_end_dt, manual_export=False)

    # проверяем данные в счете
    invoice_data_fisrt_month = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data_first_month = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_invoice_data = [create_expected_invoice(context, contract_id, person_id, D('0'), contract_start_dt)]
    if context.name != EVENTS_TICKETS2_KZ_CONTEXT.name:
        expected_invoice_data.append(
            create_expected_invoice_wo_nds(context, contract_id, person_id, D('0'), contract_start_dt)
        )

    utils.check_that(invoice_data_fisrt_month, contains_dicts_with_entries(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_first_month, empty(),
                     'Сравниваем данные из акта с шаблоном')


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new')
# проверка v_rep_agent_rep (данных для тех клиента быть не должно)
@reporter.feature(Features.AGENT_REPORT)
@pytest.mark.tickets('BALANCE-23644')
@parametrize_context
def test_tech_tickets2_agent_report(context):
    # находим тех клиента
    client_id, _, _ = steps.CommonPartnerSteps.get_active_tech_ids(context.service)

    # ищем последний акт с тех клиентом
    query_for_act = "select id from (select * from t_act" \
                    " where client_id = :client_id order by dt desc) where rownum = 1"
    act_data = db.balance().execute(query_for_act, {'client_id': client_id})

    if not act_data:
        raise utils.ServiceError(u"Тест остановлен: нет данных в акте")

    act_id = act_data[0]['id']
    # проверяем акт в v_rep_agent_rep

    query_for_report = "select * from v_rep_agent_rep where act_id = :act_id"
    agent_report_data = db.balance().execute(query_for_report, {'act_id': act_id})

    utils.check_that(agent_report_data, empty(),
                     'Проверяем, что данных в репорте нет по техническому клиенту')


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new')
# проверка второго месяца для договора с тех клиентом, нарастающий итог
@pytest.mark.tickets('BALANCE-22273')
@pytest.mark.smoke
@parametrize_context
def test_tech_tickets2_act_second_month(context):
    client_id, person_id, contract_id = create_contract(context)

    sum_first_month = create_completions_tech_contract(context, client_id, contract_id, person_id, month2_end_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_end_dt)

    sum_second_month_1 = create_completions_tech_contract(context, client_id, contract_id,
                                                          person_id, month2_end_dt, coef=D('0.3'))
    sum_second_month_2 = create_completions_tech_contract(context, client_id, contract_id,
                                                          person_id, month3_end_dt, coef=D('0.4'))

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month3_end_dt)

    # проверяем данные в счете
    # возьмем лицевой счет без сервискода, после BALANCE-32181 их два
    invoice_data_second_month = filter(lambda x: x['service_code'] is None, steps.InvoiceSteps.get_personal_accounts_with_service_codes(client_id))[0]

    # проверяем данные в акте
    act_data_second_month = steps.ActsSteps.get_act_data_by_client(client_id)

    # создаем шаблон для сравнения

    expected_invoice_data = create_expected_invoice(
        context, contract_id, person_id,
        sum_first_month + sum_second_month_1 + sum_second_month_2,
        contract_start_dt,
        service_code=None
    )

    expected_act_data = [
        steps.CommonData.create_expected_act_data(sum_first_month, month2_end_dt),
        steps.CommonData.create_expected_act_data(sum_second_month_2 + sum_second_month_1, month3_end_dt),
    ]

    utils.check_that(invoice_data_second_month, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_second_month, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# проверка v_rep_agent_rep (проверяем, что есть данные по партнеру)
@reporter.feature(Features.AGENT_REPORT)
@pytest.mark.tickets('BALANCE-23644')
@parametrize_context
def test_partner_tickets2_agent_report(context):
    client_id, person_id, contract_id = create_contract(context)

    create_completions_partner(context, client_id, contract_id, person_id, month2_end_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_end_dt)

    act_data = get_acts_for(client_id)

    expected_agent_report_data = {
        'service_id': context.service.id,
        'act_id': act_data[0]['id'],
        'act_qty': act_data[0]['amount'],
        'invoice_id': act_data[0]['invoice_id'],
        'act_amount': act_data[0]['amount'],
        'dt': act_data[0]['dt'],
        'currency': context.currency.char_code,
        'contract_id': contract_id
    }

    # проверяем акт в v_rep_agent_rep
    agent_report_data = steps.CommonPartnerSteps.get_data_from_agent_rep(contract_id)

    utils.check_that(agent_report_data[0], equal_to_casted_dict(expected_agent_report_data),
                     'Проверяем, что данных в репорте нет по техническому клиенту')


# проверка второго месяца для договора с партнером, нарастающий итог
@pytest.mark.tickets('BALANCE-22273')
@parametrize_context
def test_partner_tickets2_act_second_month(context):
    client_id, person_id, contract_id = create_contract(context)

    sum_first_month = create_completions_partner(context, client_id, contract_id, person_id, month2_end_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_end_dt)

    sum_second_month_1 = create_completions_partner(context, client_id, contract_id,
                                                    person_id, month2_end_dt, coef=D('0.4'))
    sum_second_month_2 = create_completions_partner(context, client_id, contract_id,
                                                    person_id, month3_end_dt, coef=D('0.3'))

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month3_end_dt)

    # проверяем данные в счете
    invoice_data_second_month = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data_second_month = steps.ActsSteps.get_act_data_by_client(client_id)

    # создаем шаблон для сравнения
    expected_invoice_data = [create_expected_invoice(context, contract_id, person_id,
                                                     sum_first_month + sum_second_month_1 + sum_second_month_2,
                                                     contract_start_dt)]
    if context.name != EVENTS_TICKETS2_KZ_CONTEXT.name:
        expected_invoice_data.append(
            create_expected_invoice_wo_nds(context, contract_id, person_id, D('0'), contract_start_dt)
        )

    expected_act_data = [
        steps.CommonData.create_expected_act_data(sum_first_month, month2_end_dt),
        steps.CommonData.create_expected_act_data(sum_second_month_2 + sum_second_month_1, month3_end_dt),
    ]

    utils.check_that(invoice_data_second_month, contains_dicts_with_entries(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_second_month, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# проверка второго месяца для договора с партнером, нарастающий итог
@pytest.mark.tickets('BALANCE-22273')
def test_partner_tickets2_act_second_month_with_split_reward():
    context = EVENTS_TICKETS2_RU_CONTEXT
    client_id, person_id, contract_id = create_contract(context)

    sum_first_month = create_completions_partner(context, client_id, contract_id, person_id, month2_end_dt)
    sum_first_month_wo_nds = create_completions_wo_nds(context, client_id, contract_id, person_id, month2_end_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_end_dt)

    sum_second_month_1 = create_completions_partner(context, client_id, contract_id,
                                                    person_id, month2_end_dt, coef=D('0.4'))
    sum_second_month_2 = create_completions_partner(context, client_id, contract_id,
                                                    person_id, month3_end_dt, coef=D('0.3'))

    sum_second_month_wo_nds_1 = create_completions_wo_nds(context, client_id, contract_id,
                                                          person_id, month2_end_dt, coef=D('0.4'))
    sum_second_month_wo_nds_2 = create_completions_wo_nds(context, client_id, contract_id,
                                                          person_id, month3_end_dt, coef=D('0.3'))

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month3_end_dt)

    # проверяем данные в счете
    invoice_data_second_month = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data_second_month = steps.ActsSteps.get_act_data_by_client(client_id)

    # создаем шаблон для сравнения
    expected_invoice = create_expected_invoice(
        context, contract_id, person_id,
        sum_first_month + sum_second_month_1 + sum_second_month_2,
        dt=contract_start_dt
    )

    expected_invoice_wo_nds = create_expected_invoice_wo_nds(
        context, contract_id, person_id,
        sum_first_month_wo_nds + sum_second_month_wo_nds_1 + sum_second_month_wo_nds_2,
        contract_start_dt,
    )

    expected_act_data = [
        steps.CommonData.create_expected_act_data(sum_first_month, month2_end_dt),
        steps.CommonData.create_expected_act_data(sum_first_month_wo_nds, month2_end_dt),
        steps.CommonData.create_expected_act_data(sum_second_month_2 + sum_second_month_1, month3_end_dt),
        steps.CommonData.create_expected_act_data(sum_second_month_wo_nds_2 + sum_second_month_wo_nds_1, month3_end_dt),
    ]

    expected_invoice_data = [
        expected_invoice,
        expected_invoice_wo_nds,
    ]

    utils.check_that(invoice_data_second_month, contains_dicts_with_entries(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_second_month, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


@reporter.feature(Features.AGENT_REPORT)
@pytest.mark.tickets('BALANCE-23644')
def test_partner_tickets2_agent_report_with_split_reward():
    context = EVENTS_TICKETS2_RU_CONTEXT
    client_id, person_id, contract_id = create_contract(context)

    create_completions_partner(context, client_id, contract_id, person_id, month2_end_dt)
    create_completions_wo_nds(context, client_id, contract_id, person_id, month2_end_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_end_dt)

    act_data = get_acts_for(client_id)

    utils.check_that(act_data, has_length(2), u'Проверяем, что создано два акта')
    expected_agent_report = []
    for act in act_data:
        expected_agent_report.append({
            'service_id': context.service.id,
            'act_id': act['id'],
            'act_qty': act['amount'],
            'invoice_id': act['invoice_id'],
            'act_amount': act['amount'],
            'dt': act['dt'],
            'currency': context.currency.char_code,
            'contract_id': contract_id
        })

    # проверяем акт в v_rep_agent_rep
    agent_report_data = steps.CommonPartnerSteps.get_data_from_agent_rep(contract_id)

    utils.check_that(agent_report_data, contains_dicts_with_entries(expected_agent_report),
                     'Проверяем, шаблон с актами')
