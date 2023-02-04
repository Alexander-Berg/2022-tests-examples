# -*- coding: utf-8 -*-

from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import TransactionType, Pages
from btestlib.data.partner_contexts import FOOD_COURIER_SPENDABLE_CONTEXT, \
    FOOD_COURIER_SPENDABLE_BY_CONTEXT, FOOD_COURIER_SPENDABLE_KZ_CONTEXT, \
    LAVKA_COURIER_SPENDABLE_CONTEXT, LAVKA_COURIER_SPENDABLE_ISR_CONTEXT, LAVKA_COURIER_SPENDABLE_FR_EUR_CONTEXT
from btestlib.matchers import contains_dicts_equal_to

pytestmark = [reporter.feature(Features.FOOD, Features.PARTNER, Features.ACT)]

_, _, first_month_start_dt, first_act_dt, second_month_start_dt, second_act_dt = \
    utils.Date.previous_three_months_start_end_dates()


def reward_on_dt(context, amount, dt_):
    return (amount / context.nds.koef_on_dt(dt_)).quantize(D('0.00001'))


PAYMENT_SUM = D('6000.9')
REFUND_SUM = D('2000.1')

PAGES = {
    Pages.FOOD_COUPON: D('1'),
    Pages.FOOD_SUBSIDY: D('1.1'),
    Pages.CORP_FOOD_COUPON: D('1.2'),
    Pages.CORP_FOOD_SUBSIDY: D('1.3'),
}

LAVKA_PAGES = {
    Pages.LAVKA_COUPON: D('1'),
    Pages.LAVKA_SUBSIDY: D('1.1'),
}

CONTEXTS_PAGES = [
    pytest.param(FOOD_COURIER_SPENDABLE_CONTEXT, PAGES, id=FOOD_COURIER_SPENDABLE_CONTEXT.name),
    pytest.param(FOOD_COURIER_SPENDABLE_BY_CONTEXT, PAGES, id=FOOD_COURIER_SPENDABLE_BY_CONTEXT.name),
    pytest.param(FOOD_COURIER_SPENDABLE_KZ_CONTEXT, PAGES, id=FOOD_COURIER_SPENDABLE_KZ_CONTEXT.name),
    pytest.param(LAVKA_COURIER_SPENDABLE_CONTEXT, LAVKA_PAGES, id=LAVKA_COURIER_SPENDABLE_CONTEXT.name),
    pytest.param(LAVKA_COURIER_SPENDABLE_ISR_CONTEXT, LAVKA_PAGES, id=LAVKA_COURIER_SPENDABLE_ISR_CONTEXT.name),
]

@pytest.mark.smoke
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('context, pages', CONTEXTS_PAGES)
def test_food_spendable_acts(context, pages):
    params = {
        'start_dt': first_month_start_dt
    }
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params=params)

    create_transactions(context, pages, client_id, person_id, contract_id, first_month_start_dt, monthly_coef=1)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_act_dt)

    create_transactions(context, pages, client_id, person_id, contract_id, second_month_start_dt, monthly_coef=2)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, second_act_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    expected_act_data = create_expected_act(context, pages, client_id, contract_id, first_act_dt, monthly_coef=1) \
                        + create_expected_act(context, pages, client_id, contract_id, second_act_dt, monthly_coef=2)
    utils.check_that(act_data, contains_dicts_equal_to(expected_act_data), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('context, pages', CONTEXTS_PAGES)
def test_food_spendable_incomplete_month(context, pages):
    AMOUNT = [D('1000.1'), D('200.2'), D('30.3'), D('4.4')]

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                                       additional_params={
                                                                                           'start_dt': first_month_start_dt.replace(day=10),
                                                                                           'end_dt': first_month_start_dt.replace(day=20)})

    steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id, dt=first_month_start_dt.replace(day=9),
                                        amount=AMOUNT[0], transaction_type=TransactionType.PAYMENT)
    steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id, dt=first_month_start_dt.replace(day=10),
                                        amount=AMOUNT[1], transaction_type=TransactionType.PAYMENT)
    steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id, dt=first_month_start_dt.replace(day=20),
                                        amount=AMOUNT[2], transaction_type=TransactionType.PAYMENT)
    steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id, dt=first_month_start_dt.replace(day=21),
                                        amount=AMOUNT[3], transaction_type=TransactionType.PAYMENT)

    # договор действует с 10 по 20 число, учитываем только открутки за 10 и 20 число
    month_sum = utils.dround2(AMOUNT[1] + AMOUNT[2])

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    partner_reward = reward_on_dt(context, month_sum, first_month_start_dt)
    expected_act_data = [steps.CommonData.create_expected_pad(context, client_id, contract_id, first_month_start_dt,
                                                              nds=context.nds, partner_reward=partner_reward)]

    utils.check_that(act_data, contains_dicts_equal_to(expected_act_data), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('context', [LAVKA_COURIER_SPENDABLE_FR_EUR_CONTEXT], ids=lambda context: context.name)
def test_food_spendable_contract(context):
    params = {
        'start_dt': first_month_start_dt
    }
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params=params)


# -----------------------------------------------------------------------------------------------------------
# Utils

def create_transactions(context, pages, client_id, person_id, contract_id, dt, monthly_coef):
    rows = []

    for page, coef in pages.iteritems():
        rows += [
            {
                'amount': monthly_coef * coef * PAYMENT_SUM,
                'transaction_type': TransactionType.PAYMENT,
                'payment_type': page.payment_type
            },
            {
                'amount': monthly_coef * coef * REFUND_SUM,
                'transaction_type': TransactionType.REFUND,
                'payment_type': page.payment_type
            }
        ]

    steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, rows)


def create_expected_act(context, pages, client_id, contract_id, act_dt, monthly_coef):
    expected_amount = (PAYMENT_SUM - REFUND_SUM) / context.nds.koef_on_dt(act_dt)

    return [
        steps.CommonData.create_expected_pad(context, client_id, contract_id, utils.Date.first_day_of_month(act_dt),
                                             partner_reward=round(monthly_coef * coef * expected_amount, 5),
                                             nds=context.nds,
                                             description=page.desc,
                                             page_id=page.id,
                                             type_id=context.pad_type_id)
        for page, coef in pages.iteritems()
    ]
