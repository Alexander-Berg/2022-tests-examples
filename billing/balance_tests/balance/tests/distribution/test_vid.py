# coding: utf-8
__author__ = 'a-vasin'

import pytest
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance import balance_steps as steps, balance_api as api
from balance.distribution.distribution_types import DistributionType, DistributionSubtype
from balance.features import Features
from btestlib import shared
from btestlib import utils
from btestlib.constants import DistributionContractType
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [
    pytest.mark.slow,
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)

VIDS = [None, 0, 123]


# @pytest.mark.parametrize('contract_type', CONTRACT_TYPES, ids=lambda ct: DistributionContractType.name(ct))
@pytest.mark.parametrize('distribution_type',
                         [distribution_type for distribution_type in DistributionType
                          if distribution_type.subtype == DistributionSubtype.REVSHARE],
                         ids=lambda x: x.name)
def test_revshare_vid(distribution_type):
    contract_type = DistributionContractType.AGILE

    client_id, contract_id, tag_id, place_id = create_client_contract_and_place(contract_type, distribution_type)
    places_ids = {distribution_type: place_id}

    # добавляем открутки
    with reporter.step(u"Добавляем открутки для всех VID"):
        for vid in VIDS:
            steps.DistributionSteps.create_entity_completions(places_ids, START_DT, vid=vid)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_partner_act_data = [steps.DistributionData.create_revshare_data_row(distribution_type, contract_id,
                                                                                 client_id, tag_id,
                                                                                 place_id, START_DT,
                                                                                 acts_number=len(VIDS))]

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры")


@pytest.mark.tickets('BALANCE-21780')  # a-vasin: тикет на тест на клики
# @pytest.mark.parametrize('contract_type', CONTRACT_TYPES, ids=lambda ct: DistributionContractType.name(ct))
@pytest.mark.parametrize('distribution_type, expected_acts_number',
                         [
                             # a-vasin: для кликов и активаций всё суммируется,
                             # для загрузок и установок все vid кроме None игнорируются
                             # (DistributionType.CLICKS, len(VIDS)),
                             (DistributionType.ACTIVATIONS, len(VIDS)),
                             # vorobyov-as: Для загрузок вообще не должно быть vid
                             # Не понятно, откуда взялась информация, что должны быть
                             # (DistributionType.DOWNLOADS, 1),
                             (DistributionType.INSTALLS, 1)
                         ],
                         ids=lambda distribution_type, expected_acts_number: '{}-{}'.format(distribution_type.name,
                                                                                            expected_acts_number))
def test_fixed_vid(distribution_type, expected_acts_number):
    contract_type = DistributionContractType.UNIVERSAL

    client_id, contract_id, tag_id, place_id = create_client_contract_and_place(contract_type, distribution_type)

    places_ids = {distribution_type: place_id}

    # добавляем открутки
    with reporter.step(u"Добавляем открутки для всех VID"):
        for vid in VIDS:
            steps.DistributionSteps.create_entity_completions(places_ids, START_DT, vid=vid)

    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_partner_act_data = [steps.DistributionData.create_fixed_data_row(distribution_type, contract_id,
                                                                              client_id, tag_id,
                                                                              place_id, START_DT,
                                                                              acts_number=expected_acts_number)]

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры")


def create_client_contract_and_place(contract_type, distribution_type):
    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    exclude_revshare_type = DistributionType.VIDEO_HOSTING if distribution_type != DistributionType.VIDEO_HOSTING else DistributionType.DIRECT

    # создаем договор дистрибуции
    contract_id, external_id = steps.DistributionSteps.create_full_contract(contract_type, client_id, person_id, tag_id,
                                                                            START_DT, START_DT,
                                                                            exclude_revshare_type=exclude_revshare_type)

    # создаем площадку
    place_id = \
        steps.DistributionSteps.create_distr_place(client_id, tag_id, [distribution_type.result_page_id])[0]

    return client_id, contract_id, tag_id, place_id
