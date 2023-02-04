# coding: utf-8

import pytest

from balance.distribution.distribution_types import DistributionType, DistributionSubtype

from balance import balance_steps as steps
from balance import balance_api as api, balance_db as db
from btestlib import utils
from btestlib import shared
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
                      and distribution_type.page_id not in CONFLICT_TYPES)
                  ]

PAGES_WITH_SHOWS = [row['page_id'] for row
                    in db.balance().execute(
                        ''' select page_id from bo.t_page_data
                            where product_metadata like '%"shows"%'
                        ''')
                    ]

PAGES = [distribution_type.result_page_id for distribution_type in REVSHARE_TYPES
         if distribution_type.result_page_id in PAGES_WITH_SHOWS]

F_SCALE_CODE = 'distr_test_fix_scale'


# Выпилил второй кейс с пустым кэшем, т.к. если проценты шкал будут считаться внутри калькулятора
# такого случая просто не может возникнуть
@pytest.mark.shared(block=steps.SharedBlocks.UPDATE_DISTR_VIEWS)
def test_scale_pct(shared_data):

    cache_vars = ['client_id', 'tag_id', 'contract_id', 'places_ids']

    upd = dict(products_revshare_scales={str(page): RS_SCALE_CODE for page in PAGES})

    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

        contract_id = create_contract(client_id, person_id, tag_id, upd)

        places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, REVSHARE_TYPES)

    steps.SharedBlocks.update_distr_views(shared_data=shared_data, before=before)

    # Создаём старые и новые открутки
    steps.DistributionSteps.create_revshare_completions(places_ids, START_DT)
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)

    api.test_balance().UpdateDistributionBudget(contract_id, START_DT)
    api.test_balance().RunPartnerCalculator(contract_id, START_DT)

    old_revshare_info = steps.DistributionSteps.old_views_result(places_ids, START_DT)

    actual_revshare_info = steps.DistributionSteps.get_distribution_money(START_DT, places_ids)

    utils.check_that(actual_revshare_info, contains_dicts_with_entries(old_revshare_info))


def test_accumulated_scale():

    page_id = DistributionType.ADDAPPTER2_RETAIL.result_page_id
    upd = dict(products_download_scales={str(page_id): F_SCALE_CODE})

    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    contract_id = create_contract(client_id, person_id, tag_id, upd)

    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, [DistributionType.ADDAPPTER2_RETAIL])

    # Создаём открутки за несколько дней
    # Даты берём с пропусками, чтобы убедиться, что в таком случае корректно работает
    compl_days = (2, 3, 5, 6, 9)
    for day in compl_days:
        dt = START_DT.replace(day=day)
        steps.DistributionSteps.create_entity_completions(places_ids, dt)

    api.test_balance().RunPartnerCalculator(contract_id, START_DT)

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
                                                         'SHOWS': DistributionType.ADDAPPTER2_RETAIL.default_amount
                                                         }]))
        else:
            utils.check_that(len(actual_revshare_info), equal_to(0))


# Проверяем, что бюджет действительно считается за весь месяц, а не за день
def test_many_days_completions():

    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    upd = dict(products_revshare_scales={str(page): RS_HIGH_SCALE_CODE for page in PAGES})

    contract_id = create_contract(client_id, person_id, tag_id, upd)

    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, REVSHARE_TYPES)

    # Создаём открутки за 12 дней
    for shift in range(1, 12 + 1):
        dt = START_DT + relativedelta(days=shift)
        steps.DistributionSteps.create_entity_completions(places_ids, dt)

    api.test_balance().RunPartnerCalculator(contract_id, START_DT)

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


def test_addappter2_from_scale_to_price():
    upd = dict(products_download_scales={'4013': 'Y.Browser Standart'})

    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
    contract_id = create_contract(client_id, person_id, tag_id, upd)
    steps.ContractSteps.report_url(contract_id)

    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, [DistributionType.ADDAPPTER2_RETAIL])

    # Создаём открутки за несколько дней: 1 месяц, по шкале
    compl_days = (2, 3, 5, 6, 9)
    for day in compl_days:
        dt = START_DT.replace(day=day)
        steps.DistributionSteps.create_entity_completions(places_ids, dt)

    api.test_balance().RunPartnerCalculator(contract_id, START_DT)
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(contract_id,
                                                                                             client_id, tag_id,
                                                                                             places_ids, START_DT,
                                                                                             acts_number=len(compl_days))

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры для второго договора")