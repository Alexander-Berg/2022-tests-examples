# coding: utf-8

import pytest

from balance.distribution.distribution_types import DistributionType, DistributionSubtype

from balance import balance_steps as steps
from balance import balance_api as api, balance_db as db
from btestlib import utils
from btestlib.data import defaults
from btestlib.matchers import contains_dicts_with_entries, equal_to
from dateutil.relativedelta import relativedelta

from decimal import Decimal

PASSPORT_ID = defaults.PASSPORT_UID
ACT_DT = utils.Date.first_day_of_month()
START_DT = ACT_DT - relativedelta(months=1)
SERVICE_TOKEN = 'distribution_6e5ef72ae0cc3c399d8edd17026cbc5b'

RS_SCALE_CODE = 'distr_test_scale_(shows)'
RS_HIGH_SCALE_CODE = 'distr_test_scale_(shows)_2'  # scale with high second point

POINT1_PCT = Decimal('11.5')  # Процент первой точки в тестовой шкале. Должен сработать, если откруток в кэше ещё нет.
POINT2_PCT = Decimal('36.6')  # Процент второй точки в тестовой шкале. Сработает, если в кэше уже достаточно откруток.

CONFLICT_TYPES = [13003]  # Продукты, которые нельзя использовать в одном договоре с некоторыми другими продуктами

REVSHARE_TYPES = [distribution_type for distribution_type in DistributionType
                  if (distribution_type.subtype == DistributionSubtype.REVSHARE
                      and distribution_type.page_id not in CONFLICT_TYPES
                      and distribution_type.partner_units != 'amount')  # Для таких продуктов процент всегда 100
                  ]                                                     # И шкалы в принципе не применимы


@pytest.fixture(scope='module')
def pages():
    pages_with_shows = [row['page_id'] for row
                        in db.balance().execute(
            ''' select page_id from bo.t_page_data
                where product_metadata like '%"shows"%'
            ''')
                        ]
    return [distribution_type.result_page_id for distribution_type in REVSHARE_TYPES
            if distribution_type.result_page_id in pages_with_shows]


F_SCALE_CODE = 'distr_test_fix_scale'


# Выпилил второй кейс с пустым кэшем, т.к. если проценты шкал будут считаться внутри калькулятора
# такого случая просто не может возникнуть
def test_scale_pct(pages):
    upd = dict(products_revshare_scales={str(page): RS_SCALE_CODE for page in pages})
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
    contract_id = create_contract(client_id, person_id, tag_id, upd)
    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, REVSHARE_TYPES)

    # Создаём новые открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)

    api.test_balance().UpdateDistributionBudget(contract_id, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)
    expected_revshare_info = steps.DistributionData.create_expected_full_completion_rows(
            contract_id, client_id, tag_id, places_ids, START_DT, percent=POINT2_PCT)
    actual_revshare_info = steps.DistributionSteps.get_distribution_money(START_DT, places_ids)
    utils.check_that(actual_revshare_info, contains_dicts_with_entries(expected_revshare_info))


@pytest.mark.parametrize('accounted_regions, expected_num_completions', [
    ([225],      2),
    ([225, 169], 13)])
def test_scale_pct_with_region_filter(accounted_regions, expected_num_completions):

    upd = dict(products_revshare_scales={str(DistributionType.DIRECT.result_page_id): RS_HIGH_SCALE_CODE},
               use_geo_filter=1,
               accounted_regions=accounted_regions
               )
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
    contract_id = create_contract(client_id, person_id, tag_id, upd)
    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, [DistributionType.DIRECT])

    # Создаём новые открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=225, multiply=2)
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT, country_id=169, multiply=11)

    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # Если учитываются все открутки по обеим странам - процент перевалит за порог
    # Если учитываются только одна открутки для 225 региона - не перевалит
    expected_pct = POINT1_PCT if expected_num_completions < 10 else POINT2_PCT

    # Ожидаемые данные для откруток из 225 региона
    expected_225 = steps.DistributionData.create_expected_full_completion_rows(
        contract_id, client_id, tag_id, places_ids, START_DT, percent=expected_pct, acts_number=2)

    # Ожидаемые данные для откруток из 169 региона
    expected_169 = steps.DistributionData.create_expected_full_completion_rows(
        contract_id, client_id, tag_id, places_ids, START_DT, percent=expected_pct, acts_number=11)

    if 169 not in accounted_regions:
        for item in expected_169:
            item['PARTNER'] = Decimal(0)
            item['PARTNER_WO_NDS'] = Decimal(0)

    expected_all = expected_225 + expected_169

    actual_revshare_info = steps.DistributionSteps.get_distribution_money(START_DT, places_ids)

    utils.check_that(actual_revshare_info,
                     contains_dicts_with_entries(expected_all),
                     "Проверяем, что данные соответствуют ожидаемым")


@pytest.mark.parametrize('distribution_type', [
    DistributionType.ADDAPPTER2_RETAIL,
    DistributionType.TAXI_NEW_PASSENGER])
def test_accumulated_scale(distribution_type):
    AMOUNT = 8000
    page_id = distribution_type.result_page_id
    upd = dict(products_download_scales={str(page_id): F_SCALE_CODE})

    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    contract_id = create_contract(client_id, person_id, tag_id, upd)

    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, [distribution_type])

    # Создаём открутки за несколько дней
    # Даты берём с пропусками, чтобы убедиться, что в таком случае корректно работает
    compl_days = (2, 3, 5, 6, 9)
    for day in compl_days:
        dt = START_DT.replace(day=day)
        steps.DistributionSteps.create_entity_completions(places_ids, dt, amount=AMOUNT)

    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    expected_reward = {
        1: 0*1,              # budget = 0, val = 0
        2: 5000*1 + 3000*2,  # budget = 0, val = 8000
        3: 2000*2 + 6000*3,  # budget = 8000, val = 8000
        4: 0*3,              # budget = 8000, val = 0
        5: 8000*3,           # budget = 16000, val = 8000
        6: 1000*3 + 7000*5,  # budget = 24000, val = 8000
        7: 0*5,              # budget = 32000, val = 0
        8: 0*5,              # budget = 32000, val = 0
        9: 8000*5,           # budget = 32000, val = 8000
        28: 0*5,             # budget = 32000, val = 0
    }

    for day in expected_reward.keys():
        dt = START_DT.replace(day=day)
        actual_revshare_info = steps.DistributionSteps.get_distribution_money(dt, places_ids)
        if day in compl_days:
            utils.check_that(actual_revshare_info, contains_dicts_with_entries([
                                                        {'PARTNER_WO_NDS': Decimal(expected_reward[day]),
                                                         'SHOWS': AMOUNT
                                                         }]))
        else:
            utils.check_that(len(actual_revshare_info), equal_to(0))


# Проверяем, что бюджет действительно считается за весь месяц, а не за день
@pytest.mark.long
def test_many_days_completions(pages):

    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    upd = dict(products_revshare_scales={str(page): RS_HIGH_SCALE_CODE for page in pages})

    contract_id = create_contract(client_id, person_id, tag_id, upd)

    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, REVSHARE_TYPES)

    # Создаём открутки за 12 дней
    for shift in range(1, 12 + 1):
        dt = START_DT + relativedelta(days=shift)
        steps.DistributionSteps.create_entity_completions(places_ids, dt)

    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    for shift in range(1, 12 + 1):
        dt = START_DT + relativedelta(days=shift)
        revshare_info = steps.DistributionSteps.get_distribution_money(dt, places_ids)
        for item in revshare_info:
            real_pct = Decimal(item['PARTNER']) / Decimal(item['FULLPARTNER'])
            utils.check_that(real_pct, equal_to(Decimal('0.366')))


def create_contract(client_id, person_id, tag_id, upd):

    supplements = []
    if 'products_revshare_scales' in upd:
        supplements.append(1)
    if 'products_download_scales' in upd:
        supplements.append(2)

    params = {'client_id': client_id,
              'ctype': 'DISTRIBUTION',
              'currency': 'RUR',
              'currency_calculation': 1,
              'distribution_contract_type': 3,
              'distribution_tag': tag_id,
              'download_domains': 'test',
              'firm_id': 1,
              'manager_bo_code': 20431,
              'manager_uid': '3692781',
              'nds': '18',
              'person_id': person_id,
              'product_searchf': 'test',
              'products_currency': 'RUR',
              'reward_type': 1,
              'service_start_dt': START_DT,
              'signed': 1,
              'start_dt': START_DT,
              'supplements': supplements}

    params.update(**upd)

    contract = (api.medium()
                .CreateCommonContract(
                    PASSPORT_ID,
                    params
                ))

    contract_id = contract['ID']
    return contract_id
