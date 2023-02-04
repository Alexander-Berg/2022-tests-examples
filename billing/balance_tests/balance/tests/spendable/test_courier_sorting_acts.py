# coding=utf-8
__author__ = 'borograam'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import empty

from balance import balance_steps as steps
from balance.tests.payment.test_market_sidepayments import Context
from btestlib import utils
from btestlib.constants import SpendablePaymentType, Pages, TransactionType, NdsNew
from btestlib.matchers import contains_dicts_with_entries

payment_sum = D('6000.9')
refund_sum = D('2000.1')

previous_month_start_dt, previous_month_end_dt = utils.Date.previous_month_first_and_last_days()
prev_quarter_start_dt, prev_quarter_end_dt = utils.Date.get_previous_quarter(datetime.datetime.today())
prev_quarter_last_month_start_dt = prev_quarter_end_dt.replace(day=1)


def generate_data(context, start_dt, completions_dt, act_dt, payment_type, selfemployed=0):
    client_id, person_id, contract_id = create_contract(context, start_dt, payment_type, selfemployed=selfemployed)

    create_completions(context, client_id, person_id, contract_id, completions_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, act_dt)

    data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    return client_id, contract_id, data


def create_contract(context, start_dt, payment_type=SpendablePaymentType.MONTHLY, selfemployed=0):
    params = {
        'start_dt': start_dt,
        # 'nds': context.nds.nds_id,
        'payment_type': payment_type
    }
    if selfemployed:
        params['selfemployed'] = 1

    return steps.ContractSteps.create_partner_contract(context, additional_params=params)[:3]


def create_completions(context, client_id, person_id, contract_id, dt):
    create_transactions(context, client_id, person_id, contract_id, dt)


PAGES = {
    1060: {
        Pages.ACC_SORTING_REWARD: D('1.1'),
        Pages.ACC_SORTING_RETURN_REWARD: D('1.2'),
        Pages.ACC_STORING_RETURN_REWARD: D('1.3'),
        Pages.ACC_STORING_REWARD: D('1.4'),
    },
    1100: {
        Pages.ACC_CAR_DELIVERY: D('1.1'),
        Pages.ACC_TRUCK_DELIVERY: D('1.2'),
        Pages.ACC_LOAD_UNLOAD: D('1.3'),
    },
}

PAYMENT_TYPES_WO_PAGE = {
    1060: [
        'pay_sorting_reward',
        'pay_storing_reward',
        'pay_sorting_return_reward',
        'pay_storing_return_reward',
    ],
    1100: [
        'pay_car_delivery',
        'pay_truck_delivery',
        'pay_load_unload',
    ],
}


def create_transactions(context, client_id, person_id, contract_id, dt):
    rows = []
    for page, coef in PAGES[context.service.id].items():
        rows += (
            {
                'amount': coef * payment_sum,
                'transaction_type': TransactionType.PAYMENT,
                'internal': 1,
                'service_id': context.service.id,
                'payment_type': page.payment_type,
            },
            {
                'amount': coef * refund_sum,
                'transaction_type': TransactionType.REFUND,
                'internal': 1,
                'service_id': context.service.id,
                'payment_type': page.payment_type,
            },
        )
    for payment_type in PAYMENT_TYPES_WO_PAGE[context.service.id]:
        rows.append({
            'amount': payment_sum,
            'transaction_type': TransactionType.PAYMENT,
            'internal': None,
            'service_id': context.service.id,
            'payment_type': payment_type,
        })

    steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, rows)


def create_expected_act(context, client_id, contract_id, start_dt, act_dt):
    expected_amount_by_each_payment_type = D(payment_sum - refund_sum) / context.nds.koef_on_dt(act_dt)

    return [
        steps.CommonData.create_expected_pad(context, client_id, contract_id, start_dt,
                                             partner_reward=round(coef * expected_amount_by_each_payment_type, 5),
                                             nds=context.nds,
                                             description=page.desc,
                                             page_id=page.id,
                                             type_id=context.pad_type_id,
                                             end_dt=act_dt)
        for page, coef in PAGES[context.service.id].items()
    ]


@pytest.mark.parametrize('context, selfemployed',
                         [
                             (Context(1060), 0),
                             # в интеграции явно устанавливается 18. Есди надо, то только в новой интеграции
                             # Context(1060).new(nds=NdsNew.ZERO),
                             (Context(1100), 0),
                             (Context(1100).new(nds=NdsNew.ZERO), 1),
                         ],
                         ids=lambda c: '{} NDS {}'.format(c.service.id, c.nds.nds_id)
                         )
def test_spendable_act(context, selfemployed):
    start_dt = previous_month_start_dt
    act_dt = utils.Date.last_day_of_month(start_dt)

    client_id, contract_id, data = generate_data(
        context, start_dt, start_dt, act_dt, SpendablePaymentType.MONTHLY,
        selfemployed=selfemployed)
    expected_act_data = create_expected_act(context, client_id, contract_id, start_dt, act_dt)
    utils.check_that(data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('start_dt, completions_dt, act_dt', (
    (prev_quarter_start_dt, prev_quarter_start_dt, prev_quarter_end_dt),
    (prev_quarter_start_dt, prev_quarter_last_month_start_dt, prev_quarter_end_dt),
), ids=(
    'for the last month of period, RUS',
    'for the last month of period, contract start date = last month of period, RUS',
))
@pytest.mark.parametrize('context',
                         (
                             Context(1060),
                             Context(1100),
                         ),
                         ids=lambda c: u'{}: {}'.format(c.service.id, c.name)
                         )
def test_quarterly_acts(context, start_dt, completions_dt, act_dt):
    client_id, contract_id, data = generate_data(context, start_dt, completions_dt, act_dt,
                                                 SpendablePaymentType.QUARTERLY)
    expected_act_data = create_expected_act(context, client_id, contract_id, start_dt, act_dt)
    utils.check_that(data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('context',
                         [
                             Context(1060),
                             Context(1100),
                         ],
                         ids=lambda c: u'{}: {}'.format(c.service.id, c.name)
                         )
def test_first_month_of_quarter_generation(context):
    start_dt = prev_quarter_start_dt
    act_dt = utils.Date.last_day_of_month(start_dt)

    _, _, data = generate_data(context, start_dt, start_dt, act_dt, SpendablePaymentType.QUARTERLY)
    utils.check_that(data, empty(), u'Проверяем, что данных нет')
