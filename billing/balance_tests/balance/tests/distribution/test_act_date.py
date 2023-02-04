# -*- coding: utf-8 -*-
__author__ = 'a-vasin'

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty, has_length

from balance import balance_steps as steps, balance_api as api
import btestlib.reporter as reporter
from balance.distribution.distribution_types import DistributionType, DistributionSubtype
from balance.features import Features, AuditFeatures
from btestlib import shared
from btestlib import utils
from btestlib.constants import DistributionContractType, ContractSubtype, Collateral
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [
    pytest.mark.slow,
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
]

SERVICE_START_DT = utils.Date.first_day_of_month() - relativedelta(months=4)
START_DT = SERVICE_START_DT + relativedelta(months=1)
END_DT = START_DT + relativedelta(months=2, days=-1)

CONTRACT_TYPES = [
    (DistributionContractType.AGILE, DistributionType.DIRECT),
    (DistributionContractType.UNIVERSAL, DistributionType.VIDEO_HOSTING),
    (DistributionContractType.OFFER, DistributionType.DIRECT),
]

EXCLUDE_PRODUCTS = [DistributionType.DIRECT,
                  DistributionType.VIDEO_HOSTING]


@pytest.mark.parametrize('exclude_revshare_type', EXCLUDE_PRODUCTS)
@pytest.mark.parametrize('contract_type, act_date, tail_time',
                         [
                             (DistributionContractType.AGILE, SERVICE_START_DT - relativedelta(months=1), 1),
                             (DistributionContractType.AGILE, SERVICE_START_DT, 1),
                             (DistributionContractType.AGILE, END_DT + relativedelta(months=1), 0),
                             (DistributionContractType.AGILE, END_DT + relativedelta(months=2), 1),
                             (DistributionContractType.UNIVERSAL, SERVICE_START_DT - relativedelta(months=1), 1),
                             (DistributionContractType.UNIVERSAL, SERVICE_START_DT, 1),
                             (DistributionContractType.UNIVERSAL, END_DT + relativedelta(months=1), 0),
                             (DistributionContractType.UNIVERSAL, END_DT + relativedelta(months=2), 1)
                         ],
                         ids=['BEFORE_SERVICE_START_DT AGILE', 'BEFORE_START_DT AGILE',
                              'AFTER_TAIL_0 AGILE', 'AFTER_TAIL_1 AGILE',
                              'BEFORE_SERVICE_START_DT UNIVERSAL', 'BEFORE_START_DT UNIVERSAL',
                              'AFTER_TAIL_0 UNIVERSAL', 'AFTER_TAIL_1 UNIVERSAL'])
def test_not_generated_acts(contract_type, act_date, tail_time, exclude_revshare_type):

    client_id, contract_id, tag_id = create_client_and_contract(contract_type, exclude_revshare_type, tail_time)

    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, act_date)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, act_date)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    utils.check_that(partner_act_data, empty(), u"Проверяем, что партнерские акты не созданы")


@pytest.mark.parametrize('contract_type, exclude_revshare_type', CONTRACT_TYPES,
                         ids=lambda ct, _: DistributionContractType.name(ct))
def test_head_act_date(shared_data, contract_type, exclude_revshare_type):

    client_id, contract_id, tag_id = create_client_and_contract(contract_type, exclude_revshare_type)

    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    completion_date = START_DT - relativedelta(months=1)
    act_date = START_DT

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, completion_date)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(contract_id, client_id,
                                                                                             tag_id, places_ids,
                                                                                             act_date, nds_dt=completion_date)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры")


def test_same_tag():
    contract_type = DistributionContractType.AGILE
    exclude_revshare_type = DistributionType.VIDEO_HOSTING

    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    fixed_contract_id = create_contract(client_id, contract_type, person_id, tag_id, exclude_revshare_type,
                                        supplements=[2, 3])

    revshare_contract_id = create_contract(client_id, contract_type, person_id, tag_id, exclude_revshare_type,
                                           supplements=[1])
    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, SERVICE_START_DT)
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)

    steps.DistributionSteps.run_calculator_for_contract(fixed_contract_id)
    steps.DistributionSteps.run_calculator_for_contract(revshare_contract_id)

    fixed_places = {distr_type: place_id for distr_type, place_id in places_ids.items()
                    if distr_type.subtype != DistributionSubtype.REVSHARE}
    revshare_places = {distr_type: place_id for distr_type, place_id in places_ids.items()
                       if distr_type.subtype == DistributionSubtype.REVSHARE}

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(fixed_contract_id, START_DT)
    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(fixed_contract_id)
    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(fixed_contract_id,
                                                                                             client_id, tag_id,
                                                                                             fixed_places, START_DT,
                                                                                             acts_number=2)
    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры для fixed договора")

    api.test_balance().GeneratePartnerAct(revshare_contract_id, START_DT)
    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(revshare_contract_id)
    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(revshare_contract_id,
                                                                                             client_id, tag_id,
                                                                                             revshare_places,
                                                                                             START_DT, acts_number=2)
    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры для revshare договора")


def test_two_heads():
    contract_type = DistributionContractType.AGILE
    exclude_revshare_type = DistributionType.VIDEO_HOSTING
    first_service_start_dt = SERVICE_START_DT - relativedelta(months=9)
    first_start_dt = START_DT - relativedelta(months=9)
    first_end_dt = END_DT - relativedelta(months=9)

    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    first_contract_id = create_contract(client_id, contract_type, person_id, tag_id, exclude_revshare_type,
                                        tail_time=0, start_dt=first_start_dt,
                                        service_start_dt=first_service_start_dt, end_dt=first_end_dt)
    second_contract_id = create_contract(client_id, contract_type, person_id, tag_id, exclude_revshare_type,
                                         tail_time=0, multiplier=2)

    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, first_service_start_dt)
    steps.DistributionSteps.create_entity_completions(places_ids, first_start_dt)
    steps.DistributionSteps.run_calculator_for_contract(first_contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(first_contract_id, first_start_dt)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(first_contract_id)

    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(first_contract_id,
                                                                                             client_id, tag_id,
                                                                                             places_ids, first_start_dt,
                                                                                             acts_number=2)
    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры для первого договора")

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, SERVICE_START_DT)
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(second_contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(second_contract_id, START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(second_contract_id)

    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(second_contract_id,
                                                                                             client_id, tag_id,
                                                                                             places_ids, START_DT,
                                                                                             acts_number=2,
                                                                                             price_multiplier=2)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры для второго договора")


def test_two_months_after_start_dt():
    contract_type = DistributionContractType.AGILE
    exclude_revshare_type = DistributionType.VIDEO_HOSTING

    client_id, contract_id, tag_id = create_client_and_contract(contract_type, exclude_revshare_type)
    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    completion_during_head = START_DT - relativedelta(months=1)
    first_month_act_date = START_DT
    second_month_act_date = START_DT + relativedelta(months=1)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, completion_during_head)
    steps.DistributionSteps.create_entity_completions(places_ids, first_month_act_date)
    steps.DistributionSteps.create_entity_completions(places_ids, second_month_act_date)

    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, first_month_act_date)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(contract_id, client_id,
                                                                                             tag_id, places_ids,
                                                                                             first_month_act_date,
                                                                                             acts_number=2)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты за первый месяц имеют ожидаемые параметры")
    utils.check_that(partner_act_data, has_length(len(expected_partner_act_data)),
                     u"Проверяем, что за первый месяц присутствуют только ожидаемые акты")

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, second_month_act_date)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_partner_act_data += steps.DistributionData.create_expected_full_partner_act_data(contract_id, client_id,
                                                                                              tag_id, places_ids,
                                                                                              second_month_act_date)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты за второй месяц имеют ожидаемые параметры")
    utils.check_that(partner_act_data, has_length(len(expected_partner_act_data)),
                     u"Проверяем, что во втором месяце присутствуют только что созданные акты, а также предыдущие")


@pytest.mark.parametrize('contract_type, exclude_revshare_type', CONTRACT_TYPES,
                         ids=lambda ct, _: DistributionContractType.name(ct))
def test_tail_act_date(contract_type, exclude_revshare_type):

    tail_time = 1
    client_id, contract_id, tag_id = create_client_and_contract(contract_type, exclude_revshare_type, tail_time)

    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    act_date = utils.Date.first_day_of_month(END_DT + relativedelta(months=1))

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, act_date)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, act_date)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_partner_act_data = steps.DistributionData.filter_not_tail_data_rows(
        steps.DistributionData.create_expected_full_partner_act_data(contract_id, client_id, tag_id, places_ids,
                                                                     act_date)
    )

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры")
    utils.check_that(partner_act_data, has_length(len(expected_partner_act_data)),
                     u"Проверяем, что акты по активациям, установкам и загрузкам не созданы")

# Utils
# ----------------------------------------------
def create_client_and_contract(contract_type, exclude_revshare_type, tail_time=1):
    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    contract_id = create_contract(client_id, contract_type, person_id, tag_id, exclude_revshare_type, tail_time)

    return client_id, contract_id, tag_id


def create_contract(client_id, contract_type, person_id, tag_id, exclude_revshare_type, tail_time=1, multiplier=1,
                    start_dt=None, service_start_dt=None, end_dt=None, supplements=None):
    if not start_dt:
        start_dt = START_DT

    if not service_start_dt:
        service_start_dt = SERVICE_START_DT

    if not end_dt:
        end_dt = END_DT

    # создаем договор дистрибуции
    contract_id, _ = steps.DistributionSteps.create_full_contract(contract_type, client_id, person_id, tag_id, start_dt,
                                                                  service_start_dt, multiplier=multiplier,
                                                                  supplements=supplements,
                                                                  exclude_revshare_type=exclude_revshare_type)

    steps.ContractSteps.create_collateral(Collateral.DISTR_TERMINATION, {'CONTRACT2_ID': contract_id,
                                          'DT': (start_dt + relativedelta(days=1)).isoformat(),
                                          'TAIL_TIME': str(tail_time),
                                          'END_DT': end_dt.isoformat(),
                                          'IS_SIGNED': service_start_dt.isoformat()})

    return contract_id
