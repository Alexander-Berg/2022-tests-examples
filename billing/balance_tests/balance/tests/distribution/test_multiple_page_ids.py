# coding: utf-8
__author__ = 'a-vasin'

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import has_length

import btestlib.reporter as reporter
from balance import balance_steps as steps, balance_api as api
from balance.distribution.distribution_types import DistributionType, DistributionSubtype
from balance.features import Features
from btestlib import shared
from btestlib import utils
from btestlib.constants import DistributionContractType
from btestlib.matchers import contains_dicts_with_entries, equal_to

pytestmark = [
    pytest.mark.slow
]

CONTRACT_TYPES = [
    (DistributionContractType.AGILE, DistributionType.DIRECT),
    (DistributionContractType.UNIVERSAL, DistributionType.VIDEO_HOSTING),
    # (DistributionContractType.OFFER, DistributionType.VIDEO_HOSTING)
]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)

DISTRIBUTION_TYPES = [DistributionType.SEARCHES, DistributionType.ACTIVATIONS, DistributionType.DIRECT]


@reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
@pytest.mark.parametrize('contract_type, exclude_revshare_type', CONTRACT_TYPES,
                         ids=lambda ct, _: DistributionContractType.name(ct))
def test_single_place_all_types_act_data(contract_type, exclude_revshare_type):

    # получаем все возможные page_ids
    page_ids = steps.DistributionSteps.get_all_page_ids()

    client_id, contract_id, places_ids, tag_id = create_client_tag_contract_and_places(contract_type, page_ids,
                                                                                       exclude_revshare_type)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(contract_id, client_id,
                                                                                             tag_id, places_ids,
                                                                                             START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры")


@reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
@pytest.mark.parametrize('contract_type, exclude_revshare_type', CONTRACT_TYPES,
                         ids=lambda ct, _: DistributionContractType.name(ct))
def test_single_place_all_types_act_info(contract_type, exclude_revshare_type):

    # получаем все возможные page_ids
    page_ids = steps.DistributionSteps.get_all_page_ids()

    client_id, contract_id, places_ids, tag_id = create_client_tag_contract_and_places(contract_type, page_ids,
                                                                                       exclude_revshare_type)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    # vorobyov-as:
    # Изначальная логика этого теста была завязана на тот факт, что все place_id получаются одинаковые
    # Новая версия работает немного не так, и я не уверен, нужно ли отдельно это проверять
    # Но на всякий случай проверю:
    utils.check_that(len(set(places_ids.values())), equal_to(1))

    completions_info = steps.DistributionSteps.get_distribution_money(START_DT, places_ids)
    expected_completions_info = steps.DistributionData.create_expected_full_completion_rows(contract_id, client_id,
                                                                                            tag_id, places_ids,
                                                                                            START_DT)

    utils.check_that(completions_info, contains_dicts_with_entries(expected_completions_info),
                     u"Проверяем, что открутки имеют ожидаемые параметры")


@reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
@pytest.mark.parametrize('contract_type, exclude_revshare_type', CONTRACT_TYPES,
                         ids=lambda ct, _: DistributionContractType.name(ct))
def test_single_place_searches_activations_direct_act_data(contract_type, exclude_revshare_type):

    page_ids = [distribution_type.result_page_id for distribution_type in DISTRIBUTION_TYPES]

    client_id, contract_id, places_ids, tag_id = create_client_tag_contract_and_places(contract_type, page_ids,
                                                                                       exclude_revshare_type)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    places_ids = {distribution_type: place_id for distribution_type, place_id in places_ids.iteritems()
                  if distribution_type in DISTRIBUTION_TYPES}

    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(contract_id, client_id,
                                                                                             tag_id, places_ids,
                                                                                             START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры")
    utils.check_that(partner_act_data, has_length(len(expected_partner_act_data)),
                     u"Проверяем, что сгенерировано ожидаемое количество актов")


@reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
@pytest.mark.parametrize('contract_type, exclude_revshare_type', CONTRACT_TYPES,
                         ids=lambda ct, _: DistributionContractType.name(ct))
def test_single_place_searches_activations_direct_act_info(contract_type, exclude_revshare_type):

    page_ids = [distribution_type.result_page_id for distribution_type in DISTRIBUTION_TYPES]

    client_id, contract_id, places_ids, tag_id = create_client_tag_contract_and_places(contract_type, page_ids,
                                                                                       exclude_revshare_type)
    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    places_ids = {distribution_type: place_id for distribution_type, place_id in places_ids.iteritems()
                  if distribution_type in DISTRIBUTION_TYPES}

    completions_info = steps.DistributionSteps.get_distribution_money(START_DT, places_ids)
    expected_completions_info = steps.DistributionData.create_expected_full_completion_rows(contract_id, client_id,
                                                                                            tag_id, places_ids,
                                                                                            START_DT)

    utils.check_that(completions_info, contains_dicts_with_entries(expected_completions_info),
                     u"Проверяем, что открутки имеют ожидаемые параметры")
    utils.check_that(completions_info, has_length(len(expected_completions_info)),
                     u"Проверяем, что откруток ожидаемое количество")


def create_client_tag_contract_and_places(contract_type, page_ids, exclude_revshare_type):
    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    # создаем договор дистрибуции
    contract_id, external_id = steps.DistributionSteps.create_full_contract(contract_type, client_id, person_id, tag_id,
                                                                            START_DT, START_DT,
                                                                            exclude_revshare_type=exclude_revshare_type)

    # создаем площадки
    place_id, _ = steps.DistributionSteps.create_distr_place(client_id, tag_id, page_ids)

    places_ids = {distribution_type: place_id for distribution_type in DistributionType
                  if distribution_type.subtype in [DistributionSubtype.REVSHARE, DistributionSubtype.FIXED] and
                  distribution_type != exclude_revshare_type}

    return client_id, contract_id, places_ids, tag_id
