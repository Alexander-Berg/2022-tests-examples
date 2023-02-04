# -*- coding: utf-8 -*-
__author__ = 'atkaya'

from decimal import Decimal as D
import random

import pytest
from dateutil.relativedelta import relativedelta

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import shared
from btestlib import utils as utils
from btestlib.constants import NdsNew as Nds, Managers, PersonTypes
from btestlib.data.defaults import Partner
from btestlib.data.partner_contexts import RSYA_OFFER_BY, RSYA_SSP_RU, RSYA_OFFER_RU, RSYA_LICENSE_SW_SERVICES_AG
from btestlib.matchers import contains_dicts_with_entries, equal_to

pytestmark = [
    reporter.feature(Features.PARTNER_REWARD, Features.PARTNER_ACT)
]

START_DT, END_DT = utils.Date.previous_month_first_and_last_days()

CLICKS_SEARCH_FORMS = 10
CLICKS_STRIPES = 50
CLICKS_POPUPS = 120

# дополнительно проверяется кейс, что если договор начинается и заканчивается в середине одного месяца,
# то актом все равно закрывается
@reporter.feature(Features.PARTNER, Features.ACT, Features.SSP)
@pytest.mark.tickets('BALANCE-21811')
@pytest.mark.parametrize('context, with_nds',
                         [
                          pytest.param(RSYA_SSP_RU, 1, marks=[pytest.mark.audit(reporter.feature(AuditFeatures.RV_C11_1)), pytest.mark.smoke]),
                          pytest.param(RSYA_SSP_RU, 0, marks=[pytest.mark.smoke]),
                          pytest.param(RSYA_OFFER_BY, 1, marks=[pytest.mark.smoke]),
                          pytest.param(RSYA_OFFER_BY, 0),
                         ],
                         ids=[
                             'RSYA_SSP_RU_WITH_NDS',
                             'RSYA_SSP_RU_WITHOUT_NDS',
                             'RSYA_OFFER_BY_WITH_NDS',
                             'RSYA_OFFER_BY_WITHOUT_NDS',
                         ])
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_PARTNER_CONTRACT_PLACE_MVIEWS)
def test_create_ssp_with_acts(shared_data, context, with_nds):
    start_dt = START_DT + relativedelta(days=5)
    end_dt = END_DT - relativedelta(days=5)

    cache_vars = ['client_id', 'person_id', 'contract_id', 'external_id', 'place_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        params = dict(
            service_start_dt=start_dt,
            start_dt=start_dt,
            end_dt=end_dt,
            nds=context.nds.nds_id if with_nds else 0,
            manager_uid=Managers.NIGAI.uid,
            partners_contract_type=context.rsya_contract_type
        )
        client_id, person_id, contract_id, external_id = \
            steps.ContractSteps.create_partner_contract(context, additional_params=params)

        place_id = steps.PartnerSteps.create_partner_place(client_id)

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_partner_contract_place_mviews(shared_data=shared_data, before=before)

    steps.PartnerSteps.create_direct_partner_completion(place_id, start_dt)
    steps.PartnerSteps.create_dsp_partner_completions(start_dt, place_id=place_id)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, start_dt)

    # проверяем вознаграждение по Директу
    nds_koef = Nds.DEFAULT.koef_on_dt(start_dt)  # всегда отрывается Российский НДС
    expected_direct_reward = utils.roundup(1290 / nds_koef, 5)

    contract_nds_koef = context.nds.koef_on_dt(start_dt)
    check_reward(contract_id, description='Яндекс.Директ', nds=contract_nds_koef, expected_amount=expected_direct_reward)

    # проверяем вознаграждение по РТБ
    check_reward(contract_id, description='РТБ', nds=with_nds, expected_amount=Partner.PARTNER_REWARD_DSP)


@pytest.mark.parametrize('context, with_nds',
                         [
                             pytest.mark.smoke((RSYA_SSP_RU, 1)),
                             (RSYA_SSP_RU, 0),
                             pytest.mark.smoke((RSYA_OFFER_BY, 1)),
                             (RSYA_OFFER_BY, 0),
                             (RSYA_LICENSE_SW_SERVICES_AG, 0)
                         ],
                         ids=[
                             'RSYA_SSP_RU_WITH_NDS',
                             'RSYA_SSP_RU_WITHOUT_NDS',
                             'RSYA_OFFER_BY_WITH_NDS',
                             'RSYA_OFFER_BY_WITHOUT_NDS',
                             'RSYA_LICENSE_SW_SERVICES_AG_WITHOUT_NDS'
                         ])
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_PARTNER_CONTRACT_PLACE_MVIEWS)
def test_rsya_acts_type_2(shared_data, context, with_nds):
    nds_pct = context.nds.pct_on_dt(START_DT) if with_nds else 0

    cache_vars = ['client_id', 'person_id', 'contract_id', 'external_id', 'place_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        params = dict(
            service_start_dt=START_DT,
            start_dt=START_DT,
            nds=context.nds.nds_id if with_nds else 0,
            manager_uid=Managers.NIGAI.uid,
            partners_contract_type=context.rsya_contract_type
        )
        client_id, person_id, contract_id, external_id = \
            steps.ContractSteps.create_partner_contract(context, additional_params=params)

        place_id = steps.PartnerSteps.create_partner_place(client_id)

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_partner_contract_place_mviews(shared_data=shared_data, before=before)

    add_completions_for_type_2(place_id)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, START_DT)

    actual_data = get_act_data(contract_id)
    expected_data = create_expected_act_data_for_type_2(place_id, nds_pct, is_aggregator=0)
    utils.check_that(actual_data, contains_dicts_with_entries(expected_data),
                     u'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('nds',
                         [
                             pytest.mark.smoke((Nds.DEFAULT)),
                             (Nds.NOT_RESIDENT)
                         ],
                         ids=[
                             'Nds 18',
                             'Nds 0',
                         ]
                         )
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_PARTNER_CONTRACT_PLACE_MVIEWS)
def test_rsya_acts_aggregator_type_2(shared_data, nds):
    nds_pct = nds.pct_on_dt(START_DT)

    cache_vars = ['client_id', 'person_id', 'contract_id', 'external_id', 'place_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id = steps.PartnerSteps.create_partner_client_person()
        steps.ClientSteps.set_client_partner_type(client_id)
        contract_id, external_id = steps.ContractSteps.create_contract('rsya_aggregator',
                                                                       {
                                                                           'CLIENT_ID': client_id,
                                                                           'PERSON_ID': person_id,
                                                                           'NDS': str(nds.nds_id),
                                                                       })

        place_id = steps.PartnerSteps.create_partner_place(client_id)

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_partner_contract_place_mviews(shared_data=shared_data, before=before)

    add_completions_for_type_2(place_id)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, START_DT)

    actual_data = get_act_data(contract_id)
    expected_data = create_expected_act_data_for_type_2(place_id, nds_pct, is_aggregator=1)
    utils.check_that(actual_data, contains_dicts_with_entries(expected_data),
                     u'Сравниваем данные из акта с шаблоном')


def test_rsya_internal_type_2():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.set_client_partner_type(client_id)
    place_id = steps.PartnerSteps.create_partner_place(client_id, internal_type=2)
    person_id = steps.PersonSteps.create(client_id, 'ur', {
        'is-partner': '1'
    })

    add_completions_for_type_2(place_id)

    query = "SELECT page_id, round(partner_reward,5) AS partner_reward FROM v_partner_internal_stat WHERE place_id = :place_id"
    params = {
        'place_id': place_id
    }
    actual_data = db.balance().execute(query, params)
    expected_data = [
        {
            'page_id': Partner.PAGE_ID_POPUPS,
            'partner_reward': round(get_partner_reward(Partner.PAGE_ID_POPUPS, CLICKS_POPUPS, wo_nds=False), 5)
        },
        {
            'page_id': Partner.PAGE_ID_STRIPES,
            'partner_reward': round(get_partner_reward(Partner.PAGE_ID_STRIPES, CLICKS_STRIPES, wo_nds=False), 5)
        },
        {
            'page_id': Partner.PAGE_ID_SEARCH_FORMS,
            'partner_reward': round(get_partner_reward(Partner.PAGE_ID_SEARCH_FORMS, CLICKS_SEARCH_FORMS, wo_nds=False),
                                    5)
        }
    ]

    utils.check_that(actual_data, contains_dicts_with_entries(expected_data))


@pytest.mark.parametrize('context, head_months',
                         [
                          (RSYA_OFFER_RU, 1),
                          (RSYA_OFFER_RU, 0),
                          (RSYA_OFFER_BY, 1),
                          (RSYA_OFFER_BY, 0),

                         ],
                         ids=[
                             'RSYA_OFFER_RU_with_head',
                             'RSYA_OFFER_RU_without_head',
                             'RSYA_OFFER_BY_with_head',
                             'RSYA_OFFER_BY_without_head',
                         ])
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_PARTNER_CONTRACT_PLACE_MVIEWS)
def test_rsya_head(shared_data, context, head_months):
    cur_month_dt = utils.Date.first_day_of_month()
    contract_start_dt = cur_month_dt
    act_month_dt, act_month_end_dt = utils.Date.previous_month_first_and_last_days()
    start_dt = act_month_dt
    service_start_dt = contract_start_dt - relativedelta(months=head_months)

    cache_vars = ['client_id', 'person_id', 'contract_id', 'external_id', 'place_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        params = dict(
            service_start_dt=service_start_dt,
            start_dt=contract_start_dt,
            signed=1,
            manager_uid=Managers.NIGAI.uid,
            partners_contract_type=context.rsya_contract_type
        )
        client_id, person_id, contract_id, external_id = \
            steps.ContractSteps.create_partner_contract(context, is_offer=1, additional_params=params)

        place_id = steps.PartnerSteps.create_partner_place(client_id)

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_partner_contract_place_mviews(shared_data=shared_data, before=before)

    steps.PartnerSteps.create_direct_partner_completion(place_id, start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, act_month_dt)

    act_dates = get_act_dates(contract_id)

    expected_act_dates = []
    if head_months:
        expected_act_dates.append({
            'dt': None,
            'end_dt': None,
            'report_dt': act_month_dt,
            'report_end_dt': act_month_end_dt
        })

    utils.check_that(act_dates, contains_dicts_with_entries(expected_act_dates),
                     u"Проверяем, что в созданном акте даты проставлены "
                     u"именно так, как должны быть для фиктивного акта тестового периода.")


@pytest.mark.parametrize('context, person_type', [
    (RSYA_OFFER_RU, PersonTypes.PH),
    (RSYA_OFFER_BY, PersonTypes.BYU),
    (RSYA_OFFER_BY, PersonTypes.BYP),
], ids=lambda c, pt: c.name + '-' + pt.code.upper())
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_PARTNER_CONTRACT_PLACE_MVIEWS)
def test_rsya_testmode(shared_data, context, person_type):
    context = context.new(name='%s-%s_r%s' % (context.name, person_type.code.upper(), random.randint(66666, 6666666)),
                          person_type=person_type)

    cache_vars = ['client_id', 'person_id', 'contract_id', 'external_id', 'place_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        params = dict(
            service_start_dt=START_DT,
            start_dt=START_DT,
            test_mode=1,
            manager_uid=Managers.NIGAI.uid,
            partners_contract_type=context.rsya_contract_type
        )
        client_id, person_id, contract_id, external_id = \
            steps.ContractSteps.create_partner_contract(context, is_offer=1, unsigned=True, additional_params=params)

        place_id = steps.PartnerSteps.create_partner_place(client_id)

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_partner_contract_place_mviews(shared_data=shared_data, before=before)

    steps.PartnerSteps.create_direct_partner_completion(place_id, START_DT)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, START_DT)

    act_dates = get_act_dates(contract_id)

    expected_act_dates = [{
        'dt': None,
        'end_dt': None,
        'report_dt': START_DT,
        'report_end_dt': END_DT
    }]

    utils.check_that(act_dates, contains_dicts_with_entries(expected_act_dates),
                     u"Проверяем, что в созданном акте даты проставлены "
                     u"именно так, как должны быть для фиктивного акта тестового периода.")


# --------------------------------------------------------------------------------------
# Utils

def check_reward(contract_id, description, nds, expected_amount):
    query = "SELECT nds, round(partner_reward_wo_nds,5) reward " \
            "FROM t_partner_act_data " \
            "WHERE partner_contract_id = :contract_id " \
            "   AND description = :description"
    params = {
        'nds': nds,
        'contract_id': contract_id,
        'description': description
    }
    reward = db.balance().execute(query, params)[0]['reward']

    utils.check_that(D(reward), equal_to(D(expected_amount)), u"Проверяем вознаграждение")


def get_act_dates(contract_id):
    query = 'SELECT dt, end_dt, report_dt, report_end_dt ' \
            'FROM bo.t_partner_act_data ' \
            'WHERE partner_contract_id=:cid'

    params = {
        'cid': contract_id
    }

    return db.balance().execute(query, params)


# метод для добавления откруток с type_id = 2 (type_id из t_page_data)
def add_completions_for_type_2(place_id):
    steps.PartnerSteps.create_direct_partner_completion(place_id, START_DT, page_id=Partner.PAGE_ID_STRIPES,
                                                        clicks=CLICKS_STRIPES)
    steps.PartnerSteps.create_direct_partner_completion(place_id, START_DT, page_id=Partner.PAGE_ID_POPUPS,
                                                        clicks=CLICKS_POPUPS)
    steps.PartnerSteps.create_direct_partner_completion(place_id, START_DT, page_id=Partner.PAGE_ID_SEARCH_FORMS,
                                                        clicks=CLICKS_SEARCH_FORMS)


# расчет цены по page_id
def get_price_by_page_id(page_id):
    query_price = "SELECT rur_click_price, nds FROM t_page_data WHERE page_id = :page_id"
    params_invoice = {
        'page_id': page_id
    }
    price = db.balance().execute(query_price, params_invoice)[0]['rur_click_price']
    nds = db.balance().execute(query_price, params_invoice)[0]['nds']
    if nds:
        final_price = D(price)
    else:
        nds_default_on_dt = Nds.DEFAULT.pct_on_dt(START_DT)
        final_price = D(price) * (D('1') + nds_default_on_dt / D('100'))
    return final_price


def get_partner_reward(page_id, clicks, wo_nds=True):
    price = get_price_by_page_id(page_id)
    if wo_nds:
        nds_pct = Nds.DEFAULT.pct_on_dt(START_DT)
    else:
        nds_pct = Nds.NOT_RESIDENT.pct_on_dt(START_DT)
    reward = price * clicks / (D('1') + nds_pct / D('100'))
    return utils.dround(reward, 5)


def get_aggregator_reward(page_id, clicks, is_aggregator):
    nds_pct = Nds.DEFAULT.pct_on_dt(START_DT)
    if is_aggregator:
        price = get_price_by_page_id(page_id)
        reward = clicks * price / (Partner.PARTNER_PCT / D('100')) * (Partner.AGGREGATOR_PCT / D('100')) / (
                    D('1') + nds_pct / D('100'))
    else:
        reward = 0
    return utils.dround(reward, 5)


def create_expected_act_data_for_type_2(place_id, nds, is_aggregator):
    return [
        {
            'clicks': CLICKS_POPUPS,
            'page_id': Partner.PAGE_ID_POPUPS,
            'place_id': place_id,
            'aggregator_reward_wo_nds': get_aggregator_reward(Partner.PAGE_ID_POPUPS, CLICKS_POPUPS, is_aggregator),
            'partner_reward_wo_nds': get_partner_reward(Partner.PAGE_ID_POPUPS, CLICKS_POPUPS),
            'nds': nds
        },
        {
            'clicks': CLICKS_STRIPES,
            'page_id': Partner.PAGE_ID_STRIPES,
            'place_id': place_id,
            'aggregator_reward_wo_nds': get_aggregator_reward(Partner.PAGE_ID_STRIPES, CLICKS_STRIPES, is_aggregator),
            'partner_reward_wo_nds': get_partner_reward(Partner.PAGE_ID_STRIPES, CLICKS_STRIPES),
            'nds': nds
        },
        {
            'clicks': CLICKS_SEARCH_FORMS,
            'page_id': Partner.PAGE_ID_SEARCH_FORMS,
            'place_id': place_id,
            'aggregator_reward_wo_nds': get_aggregator_reward(Partner.PAGE_ID_SEARCH_FORMS, CLICKS_SEARCH_FORMS,
                                                              is_aggregator),
            'partner_reward_wo_nds': get_partner_reward(Partner.PAGE_ID_SEARCH_FORMS, CLICKS_SEARCH_FORMS),
            'nds': nds
        }
    ]


def get_act_data(contract_id):
    query = "SELECT round(partner_reward_wo_nds,5) partner_reward_wo_nds, " \
            "   page_id, " \
            "   place_id, " \
            "   clicks, " \
            "   round(aggregator_reward_wo_nds, 5) AS aggregator_reward_wo_nds, " \
            "   nds " \
            "FROM t_partner_act_data " \
            "WHERE partner_contract_id = :contract_id"
    params = {
        'contract_id': contract_id
    }

    return db.balance().execute(query, params)
