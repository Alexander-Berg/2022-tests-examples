# -*- coding: utf-8 -*-
__author__ = 'mindlin'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import empty
import btestlib.reporter as reporter

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import NdsNew, Managers, Firms
from balance.features import Features
from btestlib.data.partner_contexts import (
    INGAME_PURCHASES_CONTEXT,
    INGAME_PURCHASES_CONTEXT_SW_YT_USD,
    INGAME_PURCHASES_CONTEXT_SW_YTPH_EUR,
    RSYA_LICENSE_SW_SERVICES_AG,
)
from dateutil.relativedelta import relativedelta
from btestlib.matchers import contains_dicts_with_entries

_, _, month_before_prev_start_dt, month_before_prev_end_dt, prev_month_start_dt, prev_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()
current_month_start_dt, current_month_end_dt = utils.Date.current_month_first_and_last_days()
prev_quarter_start_dt, prev_quarter_end_dt = utils.Date.get_previous_quarter(datetime.datetime.today())
COMPLETION_SUM = D('5004.1')
COEF_1 = D('0.5')
COEF_2 = D('1.3')

INGAME_CONTEXTS_PARAMS = [
    pytest.param(INGAME_PURCHASES_CONTEXT, id=INGAME_PURCHASES_CONTEXT.name),
    pytest.param(INGAME_PURCHASES_CONTEXT_SW_YT_USD, id=INGAME_PURCHASES_CONTEXT_SW_YT_USD.name),
    pytest.param(INGAME_PURCHASES_CONTEXT_SW_YTPH_EUR, id=INGAME_PURCHASES_CONTEXT_SW_YTPH_EUR.name),
]

game_id = 99999


def create_entity_completions(context, client_id, completion_sum, dt):
    entity_id = steps.PartnerSteps.check_client_tarification(client_id, page_id=context.page_id,
                                                             key_num_2=context.currency.iso_num_code,
                                                             key_num_3=game_id)
    if not entity_id:
        entity_id = steps.PartnerSteps.create_tarification_entity_row(client_id, page_id=context.page_id,
                                                                      key_num_2=context.currency.iso_num_code,
                                                                      key_num_3=game_id)
    else:
        entity_id = entity_id[0]['id']

    steps.PartnerSteps.create_entity_completions_row(context.page_id, entity_id, context.source_id, completion_sum, dt)


@reporter.feature(Features.INGAME_PURCHASES)
@pytest.mark.smoke
@pytest.mark.parametrize('context', INGAME_CONTEXTS_PARAMS)
def test_ingame_purchases_two_months(context):
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': month_before_prev_start_dt})

    create_entity_completions(context, client_id, COMPLETION_SUM, month_before_prev_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, month_before_prev_end_dt)
    amount_prev_month = COMPLETION_SUM.quantize(D('1.00000'))

    act_data_prev_month = steps.CommonData.create_expected_partner_act_data(client_id,
                                                               spendable_contract_id,
                                                               month_before_prev_end_dt,
                                                               context.pad_description,
                                                               page_id=context.page_id,
                                                               type_id=context.pad_type_id,
                                                               partner_reward=amount_prev_month,
                                                               nds=NdsNew.ZERO,
                                                               place_id=game_id)

    create_entity_completions(context, client_id, COMPLETION_SUM * COEF_1,
                              month_before_prev_start_dt + relativedelta(days=1))
    create_entity_completions(context, client_id, COMPLETION_SUM * COEF_2, prev_month_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, prev_month_end_dt)

    amount_cur_month = (COMPLETION_SUM * COEF_1 + COMPLETION_SUM * COEF_2).quantize(D('1.00000'))

    act_data_cur_month = steps.CommonData.create_expected_partner_act_data(client_id,
                                                               spendable_contract_id,
                                                               prev_month_end_dt,
                                                               context.pad_description,
                                                               page_id=context.page_id,
                                                               type_id=context.pad_type_id,
                                                               partner_reward=amount_cur_month,
                                                               nds=NdsNew.ZERO,
                                                               place_id=game_id)

    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    expected_acts = [act_data_prev_month, act_data_cur_month]
    utils.check_that(acts, contains_dicts_with_entries(expected_acts),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('context', INGAME_CONTEXTS_PARAMS)
def test_ingame_purchases_compl_before_contract(context):
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': current_month_start_dt})

    create_entity_completions(context, client_id, COMPLETION_SUM, prev_month_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, prev_month_end_dt)
    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    utils.check_that(acts, empty(), u'Проверяем, что акты не сгенерированы')


@pytest.mark.parametrize('context', INGAME_CONTEXTS_PARAMS)
def test_ingame_purchases_compl_after_contract(context):
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': month_before_prev_start_dt,
                           'end_dt': month_before_prev_end_dt})

    create_entity_completions(context, client_id, COMPLETION_SUM, prev_month_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, prev_month_end_dt)
    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    utils.check_that(acts, empty(), u'Проверяем, что акты не сгенерированы')


@pytest.mark.parametrize('additional_params',
                         [
                             {'start_dt': prev_month_start_dt + relativedelta(days=1)},
                             {'start_dt': month_before_prev_start_dt,
                              'end_dt': prev_month_start_dt + relativedelta(days=1)}
                         ],
                         ids=['start in the middle of month',
                              'end in the middle of month'
                              ]
                         )
@pytest.mark.parametrize('context', INGAME_CONTEXTS_PARAMS)
def test_ingame_purchases_contract_middle_month(additional_params, context):
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params=additional_params)

    create_entity_completions(context, client_id, COMPLETION_SUM, prev_month_start_dt)
    create_entity_completions(context, client_id, COMPLETION_SUM, prev_month_start_dt + relativedelta(days=2))
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, prev_month_end_dt)

    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    amount = COMPLETION_SUM.quantize(D('1.00000'))

    act_data = steps.CommonData.create_expected_partner_act_data(client_id,
                                                               spendable_contract_id,
                                                               prev_month_end_dt,
                                                               context.pad_description,
                                                               page_id=context.page_id,
                                                               type_id=context.pad_type_id,
                                                               partner_reward=amount,
                                                               nds=NdsNew.ZERO,
                                                               place_id=game_id)

    expected_acts = [act_data]
    utils.check_that(acts, contains_dicts_with_entries(expected_acts),
                     'Сравниваем данные из акта с шаблоном')


def test_ingame_contract_together_rsya():
    """Проверям, что возможно заключить договор по 677 сервису с тем же клиентом, плательщиком, что в договоре РСЯ"""

    rsya_client_id, rsya_person_id, rsya_contract_id, _ = steps.ContractSteps.create_partner_contract(
        context=RSYA_LICENSE_SW_SERVICES_AG.new(firm_id=Firms.EUROPE_AG_7),
        additional_params=dict(start_dt=current_month_start_dt,
                               manager_uid=Managers.NIGAI.uid))

    ingame_client_id, ingame_person_id, ingame_contract_id, _ = steps.ContractSteps.create_partner_contract(
        context=INGAME_PURCHASES_CONTEXT_SW_YT_USD,
        client_id=rsya_client_id,
        person_id=rsya_person_id,
        additional_params=dict(start_dt=current_month_start_dt))

    assert rsya_client_id == ingame_client_id
    assert rsya_person_id == ingame_person_id
