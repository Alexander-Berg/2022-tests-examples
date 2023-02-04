# coding: utf-8
__author__ = 'a-vasin'

# TODO Насколько вообще нужен этот тест? Кажется, он по большей части дублирует то, что уже есть в других. (vorobyov-as)

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

CONTRACT_TYPES = [
    (DistributionContractType.AGILE, DistributionType.DIRECT),
    (DistributionContractType.UNIVERSAL, DistributionType.VIDEO_HOSTING),
]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)


# ручка отдает данные без договора
@reporter.feature(Features.TO_UNIT)
def test_get_distribution_revenue_share_full():

    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    # создаем площадки
    revshare_types = [distribution_type for distribution_type in DistributionType
                      if distribution_type.subtype == DistributionSubtype.REVSHARE]

    # Нет смысла проверять для Дзена и подобных сервисов, которые могут появится в будущем
    # ручка используется только для старых продуктов и берёт данные из старой таблицы
    revshare_types = [rt for rt in revshare_types if rt.partner_units != 'amount']

    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, revshare_types)

    # добавляем открутки
    steps.DistributionSteps.create_revshare_completions(places_ids, START_DT)

    full_revshare_info = steps.DistributionSteps.get_distribution_revenue_share_full_for_places(places_ids, START_DT)
    expected_full_revshare_info = steps.DistributionData.create_expected_revshare_full(client_id, places_ids, START_DT)
    utils.check_that(full_revshare_info, contains_dicts_with_entries(expected_full_revshare_info),
                     u"Проверяем, что метод вернул ожидаемые параметры")


@pytest.mark.shared(block=steps.SharedBlocks.UPDATE_DISTR_VIEWS)
@pytest.mark.parametrize('contract_type, exclude_revshare_type', CONTRACT_TYPES,
                         ids=lambda ct, _: DistributionContractType.name(ct))
@reporter.feature(Features.TO_UNIT)
def test_get_distribution_acted(shared_data, contract_type, exclude_revshare_type):

    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    # создаем договор дистрибуции
    contract_id, external_id = steps.DistributionSteps.create_full_contract(contract_type, client_id, person_id,
                                                                            tag_id, START_DT, START_DT,
                                                                            exclude_revshare_type=exclude_revshare_type)

    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    acts_info = steps.DistributionSteps.get_distribution_acted(client_id, START_DT)
    expected_acts_info = steps.DistributionData.create_expected_distribution_acted(contract_id, external_id, client_id,
                                                                                   tag_id, places_ids, START_DT)

    utils.check_that(acts_info, contains_dicts_with_entries(expected_acts_info),
                     u"Проверяем, что метод вернул ожидаемые параметры")
