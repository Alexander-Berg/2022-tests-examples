# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import datetime
from decimal import Decimal as D
import attr

import pytest

from balance import balance_steps as steps
from balance.balance_objects import Context
from btestlib import utils
from btestlib.constants import NdsNew, Collateral, Currencies
from btestlib.data.partner_contexts import CLOUD_MARKETPLACE_CONTEXT, CLOUD_MARKETPLACE_SAG_CONTEXT
from dateutil.relativedelta import relativedelta
from btestlib.matchers import contains_dicts_with_entries

_, _, month_before_prev_start_dt, month_before_prev_end_dt, prev_month_start_dt, prev_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()
current_month_start_dt, current_month_end_dt = utils.Date.current_month_first_and_last_days()
prev_quarter_start_dt, prev_quarter_end_dt = utils.Date.get_previous_quarter(datetime.datetime.today())
COMPLETION_SUM = D('5004.1')
COEF_1 = D('0.5')
COEF_2 = D('1.3')
default_context = CLOUD_MARKETPLACE_CONTEXT


@attr.s(repr=False, init=False)
class Case(object):
    context = attr.ib(type=Context)
    nds = attr.ib(default=None)
    currency = attr.ib(default=None)

    def __init__(self, context, nds=None, currency=None):
        self.context = context
        self.nds = context.nds if nds is None else nds
        self.currency = context.currency if currency is None else currency

    def __repr__(self):
        return '{}(nds={}, currency={})'.format(self.context.name, self.nds.nds_id, self.currency.iso_code)


def create_entity_completions(context, client_id, completion_sum, dt):
    entity_id = steps.PartnerSteps.check_client_tarification(client_id)
    if not entity_id:
        entity_id = steps.PartnerSteps.create_tarification_entity_row(client_id)
    else:
        entity_id = entity_id[0]['id']

    steps.PartnerSteps.create_entity_completions_row(context.page_id, entity_id, context.source_id, completion_sum, dt)


@pytest.mark.parametrize('test_case',
                         [
                             Case(CLOUD_MARKETPLACE_CONTEXT),
                             Case(CLOUD_MARKETPLACE_CONTEXT, nds=NdsNew.ZERO),
                             Case(CLOUD_MARKETPLACE_SAG_CONTEXT),
                             Case(CLOUD_MARKETPLACE_SAG_CONTEXT, currency=Currencies.EUR),
                         ],
                         ids=lambda t: str(t)
                         )
def test_cloud_marketplace_two_months(test_case):
    # создаем данные
    nds = test_case.nds
    currency = test_case.currency
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        test_case.context,
        additional_params={'nds': nds.nds_id,
                           'start_dt': month_before_prev_start_dt,
                           'currency': currency.char_code})

    create_entity_completions(test_case.context, client_id, COMPLETION_SUM, month_before_prev_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, month_before_prev_end_dt)
    amount_prev_month = COMPLETION_SUM.quantize(D('1.00000'))

    act_data_prev_month = steps.CommonData.create_expected_pad(test_case.context, client_id, spendable_contract_id,
                                                               month_before_prev_end_dt,
                                                               partner_reward=amount_prev_month,
                                                               nds=nds,
                                                               currency=currency.char_code,
                                                               iso_currency=currency.iso_code)

    create_entity_completions(test_case.context,
                              client_id, COMPLETION_SUM * COEF_1, month_before_prev_start_dt + relativedelta(days=1))
    create_entity_completions(test_case.context,
                              client_id, COMPLETION_SUM * COEF_2, prev_month_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, prev_month_end_dt)

    amount_cur_month = (COMPLETION_SUM * COEF_1 + COMPLETION_SUM * COEF_2).quantize(D('1.00000'))

    act_data_cur_month = steps.CommonData.create_expected_pad(test_case.context, client_id, spendable_contract_id,
                                                              prev_month_end_dt, partner_reward=amount_cur_month,
                                                              nds=nds,
                                                              currency=currency.char_code,
                                                              iso_currency=currency.iso_code)

    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    expected_acts = [act_data_prev_month, act_data_cur_month]
    utils.check_that(acts, contains_dicts_with_entries(expected_acts),
                     'Сравниваем данные из акта с шаблоном')


def test_cloud_marketplace_change_nds():
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        default_context,
        additional_params={'start_dt': month_before_prev_start_dt})

    create_entity_completions(default_context, client_id, COMPLETION_SUM, month_before_prev_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, month_before_prev_end_dt)
    amount_prev_month = COMPLETION_SUM.quantize(D('1.00000'))

    act_data_prev_month = steps.CommonData.create_expected_pad(default_context, client_id, spendable_contract_id,
                                                               month_before_prev_end_dt,
                                                               partner_reward=amount_prev_month,
                                                               nds=default_context.nds)

    steps.ContractSteps.create_collateral(Collateral.NDS_CHANGE,
                                          {'CONTRACT2_ID': spendable_contract_id,
                                           'DT': utils.Date.nullify_time_of_date(current_month_start_dt).isoformat(),
                                           'IS_SIGNED':
                                               utils.Date.nullify_time_of_date(current_month_start_dt).isoformat(),
                                           'NDS': NdsNew.ZERO.nds_id})

    steps.ContractSteps.update_collateral_dt(spendable_contract_id, prev_month_start_dt, Collateral.NDS_CHANGE)

    create_entity_completions(default_context, client_id, COMPLETION_SUM * COEF_1, month_before_prev_start_dt)
    create_entity_completions(default_context, client_id, COMPLETION_SUM * COEF_2, prev_month_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, prev_month_end_dt)

    amount_cur_month = (COMPLETION_SUM * COEF_1 + COMPLETION_SUM * COEF_2).quantize(D('1.00000'))

    act_data_cur_month = steps.CommonData.create_expected_pad(default_context, client_id, spendable_contract_id,
                                                              prev_month_end_dt, partner_reward=amount_cur_month,
                                                              nds=NdsNew.ZERO)

    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    expected_acts = [act_data_prev_month, act_data_cur_month]
    utils.check_that(acts, contains_dicts_with_entries(expected_acts), 'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('nds',
                          [
                              default_context.nds,
                              NdsNew.ZERO
                          ],
                          ids=['with nds Russia',
                               'w/o nds Russia'
                               ]
                          )
def test_cloud_marketplace_quarter(nds):
    # создаем данные
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        default_context,
        additional_params={'nds': nds.nds_id,
                           'start_dt': prev_quarter_start_dt,
                           'payment_type': 2})
    create_entity_completions(default_context, client_id, COMPLETION_SUM, prev_quarter_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id,
                                                        utils.Date.last_day_of_month(prev_quarter_start_dt))
    create_entity_completions(default_context, client_id, COMPLETION_SUM / D('2'), utils.Date.first_day_of_month(prev_quarter_end_dt))
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, prev_quarter_end_dt)
    amount = (COMPLETION_SUM * D('1.5')).quantize(D('1.00000'))

    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    expected_acts = [
        steps.CommonData.create_expected_pad(default_context, client_id, spendable_contract_id,
                                             prev_quarter_start_dt, partner_reward=amount,
                                             nds=nds)
    ]
    expected_acts[0]['end_dt'] = prev_quarter_end_dt
    utils.check_that(acts, contains_dicts_with_entries(expected_acts),
                     'Сравниваем данные из акта с шаблоном')
