# -*- coding: utf-8 -*-

__author__ = 'sfreest'

import datetime
from decimal import Decimal as D
from collections import defaultdict

import pytest
from hamcrest import empty

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType, NdsNew, Services, Pages, Firms, SpendablePaymentType
from btestlib.data.partner_contexts import DIGITAL_COROBA_RU_CONTEXT_SPENDABLE
from btestlib.matchers import contains_dicts_with_entries

previous_month_start_dt, previous_month_end_dt = utils.Date.previous_month_first_and_last_days()
prev_quarter_start_dt, prev_quarter_end_dt = utils.Date.get_previous_quarter(datetime.datetime.today())
prev_quarter_last_month_start_dt = prev_quarter_end_dt.replace(day=1)

payment_sum = D('6000.9')
refund_sum = D('2000.1')

CLIENT_AMOUNT = D('1000')

PAGES_MAP = {
    Services.DIGITAL_COROBA.id: [
        Pages.DOOH_FIX,
        Pages.DOOH_RENT,
        Pages.DOOH_BONUS,
    ],
}

PAGES_COEF = {
    Pages.DOOH_FIX: D('1.2'),
    Pages.DOOH_RENT: D('1.3'),
    Pages.DOOH_BONUS: D('1.4'),
}

@pytest.mark.parametrize('context',
                         [
                             pytest.mark.smoke(DIGITAL_COROBA_RU_CONTEXT_SPENDABLE),
                             DIGITAL_COROBA_RU_CONTEXT_SPENDABLE.new(nds=NdsNew.ZERO),
                         ],
                         ids=[
                             'DIGITAL_COROBA_RU_CONTEXT_SPENDABLE STANDART NDS',
                             'DIGITAL_COROBA_RU_CONTEXT_SPENDABLE ZERO NDS',
                         ])
def test_digital_coroba_spendable_act(context):
    start_dt = previous_month_start_dt
    act_dt = utils.Date.last_day_of_month(start_dt)

    client_id, contract_id, data = generate_data(context, start_dt, start_dt, act_dt, SpendablePaymentType.MONTHLY)
    expected_act_data = create_expected_act(context, client_id, contract_id, start_dt, act_dt)
    utils.check_that(data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('context, start_dt, completions_dt, act_dt',
                         [
                             (DIGITAL_COROBA_RU_CONTEXT_SPENDABLE, prev_quarter_start_dt, prev_quarter_start_dt, prev_quarter_end_dt),
                             (DIGITAL_COROBA_RU_CONTEXT_SPENDABLE, prev_quarter_start_dt, prev_quarter_last_month_start_dt, prev_quarter_end_dt),
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


@pytest.mark.parametrize('context', [DIGITAL_COROBA_RU_CONTEXT_SPENDABLE],
                         ids=['DIGITAL_COROBA_RU_CONTEXT_SPENDABLE'])
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
