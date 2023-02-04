# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType, Pages
from btestlib.data.partner_contexts import SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT,\
    SCOUTS_KZ_CONTEXT, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT
from btestlib.matchers import contains_dicts_equal_to
from balance.features import AuditFeatures
import btestlib.reporter as reporter

_, _, first_month_start_dt, _, second_month_start_dt, _ = \
    utils.Date.previous_three_months_start_end_dates()

delta = datetime.timedelta

FIRST_MONTH = utils.Date.first_day_of_month() - relativedelta(months=2)
SECOND_MONTH = FIRST_MONTH + relativedelta(months=1)

AMOUNTS = [{'type': Pages.SCOUTS, 'payment_sum': Decimal('100.1'), 'refund_sum': Decimal('95.9')},
           {'type': Pages.SCOUTS_SZ, 'payment_sum': Decimal('42.77'), 'refund_sum': Decimal('24.47')},
           {'type': Pages.SCOUT_CARGO_SUBSIDY, 'payment_sum': Decimal('14.32'), 'refund_sum': Decimal('5.33')},
           {'type': Pages.SCOUT_CARGO_SZ_SUBSIDY, 'payment_sum': Decimal('421.43'), 'refund_sum': Decimal('99.99')}]


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('context_scout, context_taxi', [
    pytest.mark.smoke((SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT)),
    # (SCOUTS_KZ_CONTEXT, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT),
], ids=lambda p: p.name)
def test_act_two_months(context_scout, context_taxi):
    client_id, person_id, contract_id = create_client_and_contract(context_scout, context_taxi)
    create_completions(client_id, person_id, contract_id, first_month_start_dt, context_scout)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)

    create_completions(client_id, person_id, contract_id, first_month_start_dt, context_scout, coef=3)
    create_completions(client_id, person_id, contract_id, second_month_start_dt, context_scout, coef=4)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, second_month_start_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    expected_act_data = create_expected_acts(client_id, contract_id, FIRST_MONTH, context_scout) + \
                        create_expected_acts(client_id, contract_id, SECOND_MONTH, context_scout, coef=7)

    utils.check_that(act_data, contains_dicts_equal_to(expected_act_data), u'Сравниваем данные из акта с шаблоном')


#-----------------Utils-----------------------

def create_client_and_contract(context_scout, context_taxi):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context_taxi,
                                                                                       additional_params={
                                                                                           'start_dt': first_month_start_dt})

    _, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(context_scout,
                                                                                                   client_id=client_id,
                                                                                                   is_offer=1,
                                                                                                   additional_params={
                                                                                                       'start_dt': first_month_start_dt,
                                                                                                       'link_contract_id': contract_id})

    return client_id, spendable_person_id, spendable_contract_id


def create_completions(client_id, person_id, contract_id, dt, context_scout, coef=1):
    for item in AMOUNTS:
        steps.SimpleApi.create_fake_tpt_data(context_scout, client_id, person_id, contract_id,
                                             dt,
                                             [{'amount': coef * item['payment_sum'],
                                               'transaction_type': TransactionType.PAYMENT,
                                               'payment_type': item['type'].payment_type},
                                              {'amount': coef * item['refund_sum'],
                                               'transaction_type': TransactionType.REFUND,
                                               'payment_type': item['type'].payment_type}])


def create_expected_acts(client_id, contract_id, dt, context_scout, coef=Decimal('1')):
    expected_data = []

    for item in AMOUNTS:
        total_amount = (item['payment_sum'] - item['refund_sum']) * coef
        reward = utils.dround(total_amount / context_scout.nds.koef_on_dt(dt), 5)
        expected_data.append(steps.CommonData.create_expected_pad(context_scout, client_id, contract_id, dt,
                                                                  partner_reward=reward, nds=context_scout.nds,
                                                                  page_id=item['type'].id,
                                                                  description=item['type'].desc))
    return expected_data