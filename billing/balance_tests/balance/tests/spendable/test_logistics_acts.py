# -*- coding: utf-8 -*-

__author__ = 'yuelyasheva'

import datetime
from decimal import Decimal as D
from collections import defaultdict

import pytest
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import TransactionType, NdsNew, Services, Pages, Firms, SpendablePaymentType
from btestlib.data.partner_contexts import (
    LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE,
    LOGISTICS_PARTNERS_ISRAEL_CONTEXT_SPENDABLE, LOGISTICS_PARTNERS_BY_CONTEXT_SPENDABLE,
    LOGISTICS_PARTNERS_KZ_CONTEXT_SPENDABLE, LOGISTICS_PARTNERS_AM_CONTEXT_SPENDABLE,
    LOGISTICS_PARTNERS_YANGO_DELIVERY_ISRAEL_CONTEXT_SPENDABLE,
    LOGISTICS_PARTNERS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_SPENDABLE,
    LOGISTICS_PARTNERS_YANGO_CHILE_SPA_CONTEXT_SPENDABLE,
    LOGISTICS_PARTNERS_YANDEX_LOG_OZB_RUB_CONTEXT_SPENDABLE,
    LOGISTICS_PARTNERS_YANDEX_LOG_OZB_UZS_CONTEXT_SPENDABLE,
    LOGISTICS_PARTNERS_YANDEX_LOG_OZB_USD_CONTEXT_SPENDABLE
)

from btestlib.matchers import contains_dicts_with_entries

previous_month_start_dt, previous_month_end_dt = utils.Date.previous_month_first_and_last_days()
prev_quarter_start_dt, prev_quarter_end_dt = utils.Date.get_previous_quarter(datetime.datetime.today())
prev_quarter_last_month_start_dt = prev_quarter_end_dt.replace(day=1)

payment_sum = D('6000.9')
refund_sum = D('2000.1')

CLIENT_AMOUNT = D('1000')

PAGES_MAP = {
    Services.LOGISTICS_PARTNERS.id: [
        Pages.LOGISTICS_CARGO,
        Pages.LOGISTICS_DELIVERY,
    ],
}

PAGES_COEF = {
    Pages.LOGISTICS_CARGO: D('1.2'),
    Pages.LOGISTICS_DELIVERY: D('1.3'),
}

@pytest.mark.parametrize('context',
                         [
                             pytest.mark.smoke(LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE),
                             LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                             LOGISTICS_PARTNERS_ISRAEL_CONTEXT_SPENDABLE,
                             LOGISTICS_PARTNERS_ISRAEL_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                             LOGISTICS_PARTNERS_BY_CONTEXT_SPENDABLE,
                             LOGISTICS_PARTNERS_BY_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                             LOGISTICS_PARTNERS_KZ_CONTEXT_SPENDABLE,
                             LOGISTICS_PARTNERS_KZ_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                             LOGISTICS_PARTNERS_YANGO_DELIVERY_ISRAEL_CONTEXT_SPENDABLE,
                             LOGISTICS_PARTNERS_YANGO_DELIVERY_ISRAEL_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                             LOGISTICS_PARTNERS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_SPENDABLE,
                             LOGISTICS_PARTNERS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                             LOGISTICS_PARTNERS_YANGO_CHILE_SPA_CONTEXT_SPENDABLE,
                             LOGISTICS_PARTNERS_YANGO_CHILE_SPA_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_RUB_CONTEXT_SPENDABLE,
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_RUB_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_USD_CONTEXT_SPENDABLE,
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_USD_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_UZS_CONTEXT_SPENDABLE,
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_UZS_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                         ],
                         ids=[
                             'LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE STANDART NDS',
                             'LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE ZERO NDS',
                             'LOGISTICS_PARTNERS_ISRAEL_CONTEXT_SPENDABLE STANDART NDS',
                             'LOGISTICS_PARTNERS_ISRAEL_CONTEXT_SPENDABLE ZERO NDS',
                             'LOGISTICS_PARTNERS_BY_CONTEXT_SPENDABLE STANDART NDS',
                             'LOGISTICS_PARTNERS_BY_CONTEXT_SPENDABLE ZERO NDS',
                             'LOGISTICS_PARTNERS_KZ_CONTEXT_SPENDABLE STANDART NDS',
                             'LOGISTICS_PARTNERS_KZ_CONTEXT_SPENDABLE ZERO NDS',
                             'LOGISTICS_PARTNERS_YANGO_DELIVERY_ISRAEL_CONTEXT_SPENDABLE STANDART NDS',
                             'LOGISTICS_PARTNERS_YANGO_DELIVERY_ISRAEL_CONTEXT_SPENDABLE ZERO NDS',
                             LOGISTICS_PARTNERS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_SPENDABLE.name + ' STANDART NDS',
                             LOGISTICS_PARTNERS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_SPENDABLE.name + ' ZERO NDS',
                             LOGISTICS_PARTNERS_YANGO_CHILE_SPA_CONTEXT_SPENDABLE.name + ' STANDART NDS',
                             LOGISTICS_PARTNERS_YANGO_CHILE_SPA_CONTEXT_SPENDABLE.name + ' ZERO NDS',
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_RUB_CONTEXT_SPENDABLE.name + ' STANDART NDS',
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_RUB_CONTEXT_SPENDABLE.name + ' ZERO NDS',
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_USD_CONTEXT_SPENDABLE.name + ' STANDART NDS',
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_USD_CONTEXT_SPENDABLE.name + ' ZERO NDS',
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_UZS_CONTEXT_SPENDABLE.name + ' STANDART NDS',
                             LOGISTICS_PARTNERS_YANDEX_LOG_OZB_UZS_CONTEXT_SPENDABLE.name + ' ZERO NDS',
                         ])
def test_logistics_spendable_act(context):
    start_dt = previous_month_start_dt
    act_dt = utils.Date.last_day_of_month(start_dt)

    # Костыль (т.к. фирма на момент на тестовые даты еще не заведена). Удалить в октябре 2022 или позднее
    if context.firm == Firms.YANGO_DELIVERY_BEOGRAD_1898 and datetime.datetime.now() < datetime.datetime(2022, 10, 1):
        start_dt = datetime.datetime(2022, 6, 15)
        act_dt = datetime.datetime(2022, 6, 30)
    # Костыль (т.к. tax_policy_pct на тестовые даты еще не заведен). Удалить в ноябре 2022 или позднее
    if context.firm == Firms.YANDEX_LOG_OZB and datetime.datetime.now() < datetime.datetime(2022, 11, 1):
        start_dt = datetime.datetime(2022, 7, 15)
        act_dt = datetime.datetime(2022, 7, 31)

    client_id, contract_id, data = generate_data(context, start_dt, start_dt, act_dt, SpendablePaymentType.MONTHLY)
    expected_act_data = create_expected_act(context, client_id, contract_id, start_dt, act_dt)
    utils.check_that(data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('context',
                         [
                             LOGISTICS_PARTNERS_AM_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                         ],
                         ids=[
                             'LOGISTICS_PARTNERS_AM_CONTEXT_SPENDABLE ZERO NDS',
                         ])
def test_logistics_spendable_contract(context):
    start_dt = previous_month_start_dt
    act_dt = utils.Date.last_day_of_month(start_dt)
    client_id, person_id, contract_id = create_contact(context, start_dt, SpendablePaymentType.MONTHLY)


@pytest.mark.parametrize('context, start_dt, completions_dt, act_dt',
                         [
                             (LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE, prev_quarter_start_dt, prev_quarter_start_dt, prev_quarter_end_dt),
                             (LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE, prev_quarter_start_dt, prev_quarter_last_month_start_dt, prev_quarter_end_dt),
                         ],
                         ids=[
                             'Acts for the last month of period, RUS',
                             'Acts for the last month of period, contract start date = last month of period, RUS',
                         ])
def test_quarterly_acts(context, start_dt, completions_dt, act_dt):
    client_id, contract_id, data = generate_data(context, start_dt, completions_dt, act_dt,
                                                 SpendablePaymentType.QUARTERLY)
    expected_act_data = create_expected_act(context, client_id, contract_id, start_dt, act_dt)
    utils.check_that(data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('context', [LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE], ids=['RUS'])
def test_first_month_of_quarter_generation(context):
    start_dt = prev_quarter_start_dt
    act_dt = utils.Date.last_day_of_month(start_dt)

    _, _, data = generate_data(context, start_dt, start_dt, act_dt, SpendablePaymentType.QUARTERLY)
    utils.check_that(data, empty(), u'Проверяем, что данных нет')


def generate_data(context, start_dt, completions_dt, act_dt, payment_type):
    client_id, person_id, contract_id = create_contact(context, start_dt, payment_type)

    create_completions(context, client_id, person_id, contract_id, completions_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, act_dt)

    data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    return client_id, contract_id, data


def create_contact(context, start_dt, payment_type=SpendablePaymentType.MONTHLY):
    params = {
        'start_dt': start_dt,
        'nds': context.nds.nds_id,
        'payment_type': payment_type
    }

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params=params)

    return client_id, person_id, contract_id


def create_completions(context, client_id, person_id, contract_id, dt):
    create_transactions(context, client_id, person_id, contract_id, dt, context.service.id)


def create_expected_act(context, client_id, contract_id, start_dt, act_dt):
    expected_amount_by_each_payment_type = D(payment_sum - refund_sum) / context.nds.koef_on_dt(act_dt)

    pages_coefs = defaultdict(int)

    for page in PAGES_MAP[context.service.id]:
        pages_coefs[page] += PAGES_COEF[page]

    return [
        steps.CommonData.create_expected_pad(context, client_id, contract_id, start_dt,
                                             partner_reward=round(count * expected_amount_by_each_payment_type, 5),
                                             nds=context.nds,
                                             description=page.desc,
                                             page_id=page.id,
                                             type_id=context.pad_type_id,
                                             end_dt=act_dt)
        for page, count in pages_coefs.iteritems()
    ]


def create_transactions(context, client_id, person_id, contract_id, dt, service, internal=None):
    rows = []

    for page in PAGES_MAP[service]:
        rows += [
            {
                'client_amount': CLIENT_AMOUNT,
                'amount': PAGES_COEF[page] * payment_sum,
                'transaction_type': TransactionType.PAYMENT,
                'internal': internal,
                'service_id': service,
                'payment_type': page.payment_type
            },
            {
                'client_amount': CLIENT_AMOUNT,
                'amount': PAGES_COEF[page] * refund_sum,
                'transaction_type': TransactionType.REFUND,
                'internal': internal,
                'service_id': service,
                'payment_type': page.payment_type
            }
        ]

    steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, rows)
