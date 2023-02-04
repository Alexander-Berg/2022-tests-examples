# -*- coding: utf-8 -*-
__author__ = 'a-vasin'

import pytest
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance import balance_steps as steps, balance_api as api
from balance.distribution.distribution_types import DistributionType, DistributionSubtype
from balance.features import Features, AuditFeatures
from btestlib import shared
from btestlib import utils
from btestlib.constants import DistributionContractType, Firms
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [
    pytest.mark.slow,
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
]

CONTRACT_TYPES = [DistributionContractType.AGILE, DistributionContractType.UNIVERSAL]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)


@reporter.feature(Features.INSTALLS, Features.DOWNLOADS)
def test_full_downloads_installs_contract():

    client_id, contract_id, tag_id = create_full_contract(DistributionContractType.DOWNLOADS_INSTALLS)

    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, [DistributionType.INSTALLS])

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


@reporter.feature(Features.REVSHARE)
@pytest.mark.parametrize("exclude_revshare_type", [
    # DistributionType.DIRECT,
    DistributionType.VIDEO_HOSTING
], ids=lambda dt: "EXCLUDE_{}".format(dt.name))
def test_full_revshare_contract(exclude_revshare_type):
    revshare_types = [distribution_type for distribution_type in DistributionType
                      if distribution_type.subtype == DistributionSubtype.REVSHARE
                      and distribution_type != exclude_revshare_type]

    # создаем клиента, плательщика и тэг
    client_id, contract_id, tag_id = create_full_contract(DistributionContractType.REVSHARE,
                                                          exclude_revshare_type=exclude_revshare_type)

    # создаем площадки
    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, revshare_types)

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


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C11_1))
@reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
@pytest.mark.parametrize('contract_type, firm, exclude_revshare_type', [
    (DistributionContractType.OFFER, Firms.YANDEX_1, DistributionType.DIRECT),
    (DistributionContractType.AGILE, Firms.MARKET_111, DistributionType.VIDEO_HOSTING),
    pytest.mark.smoke((DistributionContractType.UNIVERSAL, Firms.YANDEX_1, DistributionType.VIDEO_HOSTING)),
], ids=lambda ct, f, _: "{}_{}".format(DistributionContractType.name(ct), Firms.name(f)))
def test_full_contract_partner_act_data(contract_type, firm, exclude_revshare_type):

    client_id, contract_id, tag_id = create_full_contract(contract_type, firm, exclude_revshare_type)

    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

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
@pytest.mark.parametrize('contract_type, firm, exclude_revshare_type', [
    (DistributionContractType.OFFER, Firms.YANDEX_1, DistributionType.DIRECT),
    # (DistributionContractType.AGILE, Firms.MARKET_111, DistributionType.VIDEO_HOSTING),
    (DistributionContractType.UNIVERSAL, Firms.YANDEX_1, DistributionType.VIDEO_HOSTING),
    # (DistributionContractType.AGILE, Firms.MARKET_111, DistributionType.DIRECT)
], ids=lambda ct, f, _: "{}_{}".format(DistributionContractType.name(ct), Firms.name(f)))
def test_full_contract_act_info(contract_type, firm, exclude_revshare_type):

    client_id, contract_id, tag_id = create_full_contract(contract_type, firm, exclude_revshare_type)

    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    completions_info = steps.DistributionSteps.get_distribution_money(START_DT, places_ids)
    expected_completions_info = steps.DistributionData.create_expected_full_completion_rows(contract_id, client_id,
                                                                                            tag_id, places_ids,
                                                                                            START_DT)

    utils.check_that(completions_info, contains_dicts_with_entries(expected_completions_info),
                     u"Проверяем, что открутки имеют ожидаемые параметры")


def create_full_contract(contract_type, firm=Firms.YANDEX_1, exclude_revshare_type=DistributionType.VIDEO_HOSTING):
    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    # создаем договор дистрибуции
    contract_id, external_id = steps.DistributionSteps.create_full_contract(contract_type, client_id, person_id,
                                                                            tag_id, START_DT, START_DT, firm=firm,
                                                                            exclude_revshare_type=exclude_revshare_type)

    return client_id, contract_id, tag_id
