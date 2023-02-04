# -*- coding: utf-8 -*-
__author__ = 'atkaya'

from datetime import datetime
from decimal import Decimal

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import shared
from btestlib import utils
from btestlib.data.defaults import Partner, Client
from btestlib.matchers import equal_to_casted_dict
from btestlib.constants import NdsNew as Nds

pytestmark = [
    reporter.feature(Features.PARTNER, Features.GET_DSP_STAT, Features.XMLRPC)
]

COMPLETIONS_NUMBER = 2


def dsp_completions():
    completion_dt, place_id, client_id = create_client_contract_and_place()

    # добавляем открутки по DSP
    steps.PartnerSteps.create_dsp_partner_completions(completion_dt, place_id=place_id)
    return completion_dt, place_id, client_id


def multiple_dsp_completions():
    completion_dt, place_id, client_id = create_client_contract_and_place()

    # добавляем открутки по DSP
    for i in xrange(COMPLETIONS_NUMBER):
        steps.PartnerSteps.create_dsp_partner_completions(completion_dt, place_id = place_id,
                                                          update_params={'yandex_price': Partner.DSP_YANDEX_PRICE + i,
                                                                         'fake_price': Partner.DSP_FAKE_PRICE + i})

    return completion_dt, place_id, client_id


@pytest.mark.tickets('BALANCE-21796')
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_PARTNER_CONTRACT_PLACE_MVIEWS)
@pytest.mark.parametrize('include_partner_stat_id, include_deals', [
    pytest.mark.smoke((1, 1)),
    (1, 0),
    (1, None),
    (0, 0),
    (0, 1),
    (0, None),
    (None, 1),
    (None, 0),
    (None, None)
])
def test_get_dsp_stat(shared_data, include_deals, include_partner_stat_id):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['completion_dt', 'place_id', 'client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        completion_dt, place_id, client_id = dsp_completions()

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_partner_contract_place_mviews(shared_data=shared_data, before=before)

    # вызываем GetDspStat и записываем ответ
    dsp_stat = steps.PartnerSteps.get_dsp_stat_by_page_id(place_id, completion_dt, include_deals,
                                                          include_partner_stat_id)

    expected_dsp_stat = create_expected_dsp_stat(client_id, place_id, completion_dt, include_deals,
                                                 include_partner_stat_id)

    # utils.check_that(dsp_stat, has_entries_casted(expected_dsp_stat), u"Проверяем, что метод вернул ожидаемые значения")
    utils.check_that(dsp_stat, equal_to_casted_dict(expected_dsp_stat),
                     u"Проверяем, что метод вернул ожидаемые значения")


@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_PARTNER_CONTRACT_PLACE_MVIEWS)
@pytest.mark.parametrize('include_partner_stat_id', [1, 0])
@pytest.mark.parametrize('include_deals', [1, 0])
def test_grouping_get_dsp_stat(shared_data, include_deals, include_partner_stat_id):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['completion_dt', 'place_id', 'client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        completion_dt, place_id, client_id = multiple_dsp_completions()

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_partner_contract_place_mviews(shared_data=shared_data, before=before)

    # вызываем GetDspStat и записываем ответ
    dsp_stat = steps.PartnerSteps.get_dsp_stat_by_page_id(place_id, completion_dt, include_deals,
                                                          include_partner_stat_id)

    expected_dsp_stat = create_expected_dsp_stat(client_id, place_id, completion_dt, include_deals,
                                                 include_partner_stat_id, COMPLETIONS_NUMBER,
                                                 update_expected={
                                                     'YANDEX_PRICE': Partner.DSP_YANDEX_PRICE + COMPLETIONS_NUMBER - 1,
                                                     'FAKE_PRICE': Partner.DSP_FAKE_PRICE + COMPLETIONS_NUMBER - 1
                                                 })

    # utils.check_that(dsp_stat, has_entries_casted(expected_dsp_stat), u"Проверяем, что метод вернул ожидаемые значения")
    utils.check_that(dsp_stat, equal_to_casted_dict(expected_dsp_stat),
                     u"Проверяем, что метод вернул ожидаемые значения")


def create_client_contract_and_place():
    completion_dt = utils.Date.nullify_time_of_date(datetime.today())

    # создаем партнера и плательщика
    client_id, person_id = steps.PartnerSteps.create_partner_client_person()

    # делаем клиента агрегатором
    steps.ClientSteps.set_partner_type(client_id, Client.AGGREGATOR_PARTNER_TYPE)

    # создаем универсальный РСЯ договор
    steps.ContractSteps.create_contract('rsya_universal', {'CLIENT_ID': client_id, 'PERSON_ID': person_id})

    # создаем площадку
    place_id = steps.PartnerSteps.create_partner_place(client_id)

    return completion_dt, place_id, client_id


def create_expected_dsp_stat(client_id, place_id, completion_dt, include_deals=0, include_partner_stat_id=0,
                             completions_number=1, update_expected={}):

    nds_koef = Nds.DEFAULT.koef_on_dt(completion_dt)
    expected_data = {
        'PAGE_ID': place_id,
        'CLIENT_ID': client_id,
        'DT': completion_dt.isoformat(' '),
        'DSP_ID': Partner.DSP_ID,
        'HITS': Partner.DSP_HITS * completions_number,
        'BLOCK_ID': Partner.DSP_BLOCK_ID,
        'TOTAL_BID_SUM': utils.dround(Partner.DSP_TOTAL_BID_SUM * completions_number),
        'TOTAL_RESPONSE_COUNT': Partner.DSP_TOTAL_RESPONSE_COUNT * completions_number,
        'SHOWS': Partner.DSP_SHOWS * completions_number,
        'DSP': utils.dround(Partner.DSP_CHARGE * completions_number * nds_koef),
        'DSPWITHOUTNDS': utils.dround(Partner.DSP_CHARGE * completions_number),
        'PARTNER': utils.dround(Partner.PARTNER_REWARD_DSP * completions_number * nds_koef),
        'PARTNERWITHOUTNDS': utils.dround(Partner.PARTNER_REWARD_DSP * completions_number),
        'AGGREGATORWITHOUTNDS': utils.dround(Partner.PARTNER_REWARD_DSP
                                             * (Decimal(1) + Partner.DSP_AGGREGATION_PCT / Decimal(100))
                                             * completions_number),
        'AGGREGATOR': utils.dround(Partner.PARTNER_REWARD_DSP
                                   * (Decimal(1) + Partner.DSP_AGGREGATION_PCT / Decimal(100))
                                   * completions_number * nds_koef)
    }

    if include_deals:
        expected_data.update({
            'DEAL_ID': Partner.DSP_DEAL_ID,
            'YANDEX_PRICE': Partner.DSP_YANDEX_PRICE,
            'FAKE_PRICE': Partner.DSP_FAKE_PRICE
        })

    if include_partner_stat_id:
        expected_data['PARTNER_STAT_ID'] = Partner.PARTNER_STAT_ID

    expected_data.update({k: v for k, v in update_expected.iteritems() if k in expected_data})

    return expected_data
