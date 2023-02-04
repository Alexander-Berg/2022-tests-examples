# coding: utf-8
__author__ = 'a-vasin'

import pytest
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance import balance_steps as steps, balance_api as api
from balance.distribution.distribution_types import DistributionType
from balance.features import Features
from btestlib import shared
from btestlib import utils
from btestlib.constants import DistributionContractType, NdsNew as Nds, Firms
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [
    pytest.mark.slow,
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
]

# CONTRACT_TYPES = [
#     (DistributionContractType.AGILE, DistributionType.VIDEO_HOSTING),
#     (DistributionContractType.UNIVERSAL, DistributionType.DIRECT),
#     (DistributionContractType.OFFER, DistributionType.VIDEO_HOSTING),
# ]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)


# @pytest.mark.parametrize('contract_type, exclude_revshare_type', CONTRACT_TYPES,
#                          ids=lambda ct, _: DistributionContractType.name(ct))
@pytest.mark.parametrize('contract_type, firm, person_type, nds, exclude_revshare_type',
                         [
                             (DistributionContractType.AGILE, Firms.YANDEX_1, 'yt', Nds.NOT_RESIDENT, DistributionType.VIDEO_HOSTING),
                             (DistributionContractType.UNIVERSAL, Firms.YANDEX_1, 'ur', Nds.YANDEX_RESIDENT, DistributionType.DIRECT),
                             (DistributionContractType.AGILE, Firms.YANDEX_1, 'ur', Nds.YANDEX_RESIDENT, DistributionType.VIDEO_HOSTING),
                             (DistributionContractType.OFFER, Firms.MARKET_111, 'ph', Nds.ZERO, DistributionType.VIDEO_HOSTING),
                             (DistributionContractType.AGILE, Firms.SERVICES_AG_16, 'sw_ur', Nds.SAG_RESIDENT, DistributionType.VIDEO_HOSTING),
                             (DistributionContractType.UNIVERSAL, Firms.EUROPE_AG_7, 'sw_yt', Nds.NOT_RESIDENT, DistributionType.VIDEO_HOSTING)
                         ],
                         ids=[
                              'YANDEX-0',
                              'YANDEX-18 VIDEO HOSTING',
                              'YANDEX-18',
                              'YANDEX-PH-0',
                              'SERVICES_AG-8',
                              'YANDEX_EUROPE_AG-0'])
def test_nds_partner_act(contract_type, firm, person_type, nds, exclude_revshare_type):

    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag(person_type=person_type)

    # создаем договор дистрибуции
    contract_id, external_id = steps.DistributionSteps.create_full_contract(contract_type, client_id, person_id,
                                                                            tag_id,
                                                                            START_DT, START_DT, nds=nds, firm=firm,
                                                                            exclude_revshare_type=exclude_revshare_type)
    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    expected_completions_info = steps.DistributionData.create_expected_full_completion_rows(contract_id, client_id,
                                                                                            tag_id, places_ids,
                                                                                            START_DT, nds=nds)

    actual_completions_info = steps.DistributionSteps.get_distribution_money(START_DT, places_ids)

    utils.check_that(actual_completions_info, contains_dicts_with_entries(expected_completions_info),
                     u"Проверяем, что открутки имеют ожидаемые параметры")