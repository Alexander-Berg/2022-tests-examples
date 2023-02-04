# coding: utf-8
__author__ = 'vorobyov-as'

from decimal import Decimal

from dateutil.relativedelta import relativedelta

from balance import balance_db as db, balance_api as api
from balance import balance_steps as steps
from balance.distribution.distribution_types import DistributionType
from btestlib import utils
from btestlib.constants import DistributionContractType
from btestlib.matchers import contains_dicts_with_entries

import pytest


START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)
DTYPE = DistributionType.DIRECT  # Пока ограничение по регионам работает только для директа


@pytest.mark.parametrize('regions_on', [True, False])  # Отдаётся ли статистика уже с учётом регионов или ещё нет
def test_region_main(regions_on):
    revshare_types = [DTYPE]

    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    # создаем договор дистрибуции
    # с ограничением по странам: Россия(225), Грузия(169), Монголия(10099)
    contract_id, _ = steps.DistributionSteps.create_full_contract(DistributionContractType.UNIVERSAL, client_id,
                                                                  person_id, tag_id, START_DT, START_DT,
                                                                  revshare_types=revshare_types,
                                                                  exclude_revshare_type=None,
                                                                  accounted_regions=[225, 169, 10099])
    # создаем площадки
    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, revshare_types)

    if regions_on:
        # добавляем открутки для России(225), Грузии(169), Азербайджана(167) и Модовы(208)
        steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=225)
        steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=169)
        steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=167)
        steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=208)
    else:
        # если сервис ещё не начал дробить открутки по регионам,
        # то заборщик их саггрегирует в одну строчку, так как ключи у них не отличаются
        steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=-1, multiply=4)
    # и запускаем калькулятор
    api.test_balance().RunPartnerCalculator(contract_id, START_DT)

    sql = ''' select te.key_num_4 as country_id, em.money_1 as reward, em.money_2 as turnover
              from bo.t_entity_money em
              join bo.t_tarification_entity te on (em.entity_id = te.id)
              where dt = :dt and contract_id = :contract_id
          '''

    money = db.balance().execute(sql, dict(dt=START_DT, contract_id=contract_id))

    expected_reward = steps.DistributionData.get_revshare_reward(DTYPE.default_price,
                                                                 DTYPE.default_amount / DTYPE.units_type_rate)
    expected_turnover = steps.DistributionData.get_revshare_turnover(DTYPE.default_amount / DTYPE.units_type_rate)

    if regions_on:
        # Ожидаем ненулевой reward только для тех стран, которые для договора указаны в accounted_regions
        expected_money = [
            dict(country_id=225, turnover=expected_turnover, reward=expected_reward),
            dict(country_id=169, turnover=expected_turnover, reward=expected_reward),
            dict(country_id=167, turnover=expected_turnover, reward=Decimal(0)),
            dict(country_id=208, turnover=expected_turnover, reward=Decimal(0)),
        ]
    else:
        expected_money = [
            dict(country_id=-1, turnover=4*expected_turnover, reward=4*expected_reward),
        ]

    utils.check_that(money, contains_dicts_with_entries(expected_money),
                     u"Проверяем, что результат калькулятора соответствует ожидаемому")


def test_get_distribution_money_with_regions():
    revshare_types = [DTYPE]

    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    # создаем договор дистрибуции
    # с ограничением по странам: Россия(225), Грузия(169), Монголия(10099)
    contract_id, _ = steps.DistributionSteps.create_full_contract(DistributionContractType.UNIVERSAL, client_id,
                                                                  person_id, tag_id, START_DT, START_DT,
                                                                  revshare_types=revshare_types,
                                                                  exclude_revshare_type=None,
                                                                  accounted_regions=[225, 169, 10099])
    # создаем площадки
    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, revshare_types)

    steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=225)
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=169)
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=167)
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=208)
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=-1)
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=0)

    api.test_balance().RunPartnerCalculator(contract_id, START_DT)

    data = steps.DistributionSteps.get_distribution_money(START_DT, places_ids)

    expected_rows = steps.DistributionData.create_expected_full_completion_rows(contract_id, client_id,
                                                                                tag_id, places_ids,
                                                                                START_DT)
    assert len(expected_rows) == 1

    expected_row = expected_rows[0]

    all_countries = [225, 169, 167, 208, -1, 0]
    rewarded_countries = [225, 169, -1]
    expected_data = []
    for c in all_countries:
        row = expected_row.copy()
        row['COUNTRY_ID'] = c
        if c not in rewarded_countries:
            row['PARTNER'] = Decimal(0)
            row['PARTNER_WO_NDS'] = Decimal(0)
        expected_data.append(row)


    utils.check_that(data, contains_dicts_with_entries(expected_data))

