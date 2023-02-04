# -*- coding: utf-8 -*-

from collections import namedtuple
from copy import deepcopy
from datetime import datetime, timedelta as shift
from apikeys.apikeys_utils import trunc_date

import pytest

from apikeys.tests_by_typical_flows import typical_flows as flow, plain_function as plain
from apikeys.apikeys_utils import get_parameters, Person_type
from btestlib import utils
from btestlib.utils import aDict

__author__ = 'kostya-karpus'

BASE_DT = trunc_date(datetime.utcnow(),'hour')
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)

Free_limits = namedtuple('Free_limits', ['counters', 'limit', 'need_approve'])

maps_free = Free_limits(['geocoder_hits', 'router_hits'], 25000, True)
city_free = Free_limits(['hits'], 500, False)

Tariff = namedtuple('Tariff',
                    ['name',  # Наименование Тарифа
                     'service_id',  # Наименование Сервиса
                     'counters',  # Наименование Счетчика (для некоторых продуктов их несколько
                     'year_price',  # Цена за год
                     'day_limit',  # Количество запросов в день
                     'price_over_limit_per_1000',  # Цена дополнительных 1000 запросов, сверх дневного лимита
                     'free_counters'  # ???свободные счетчики???
                     ])

expected_invoices_on_day = namedtuple('expected_invoices_on_day', ['shift_days', 'invoices_amount'])

ur_apimaps = Person_type('ur', 1117, 1003, False)
ph_apimaps = Person_type('ph', 1128, 1001, False)

TEST_MAPS_SRV = 'testapimaps'
testapimaps_contractless_tariffs = [

    # common
    pytest.mark.smoke(Tariff('testapimaps_1000_yearprepay_contractless', TEST_MAPS_SRV, ['total'], 120000, 1000, 120, maps_free)),
    Tariff('testapimaps_10000_yearprepay_contractless', TEST_MAPS_SRV, ['total'], 360000, 10000, 36, maps_free),
    Tariff('testapimaps_25000_yearprepay_contractless', TEST_MAPS_SRV, ['total'], 600000, 25000, 24, maps_free),
    Tariff('testapimaps_50000_yearprepay_contractless', TEST_MAPS_SRV, ['total'], 850000, 50000, 17, maps_free),
    Tariff('testapimaps_100000_yearprepay_contractless', TEST_MAPS_SRV, ['total'], 1000000, 100000, 11, maps_free),
]

MAPS_SRV = 'apimaps'
apimaps_contractless_tariffs = [
    # discount
    # Дисконтные тарифы закончили свое действие ссентябре 2019
    # pytest.mark.smoke(Tariff('apimaps_1000_yearprepay_contractless_15_discount', MAPS_SRV, ['total'],
    #                          102000, 1000, 102, maps_free)),
    # Tariff('apimaps_10000_yearprepay_contractless_15_discount', MAPS_SRV, ['total'], 306000, 10000, 30.6, maps_free),
    # Tariff('apimaps_25000_yearprepay_contractless_15_discount', MAPS_SRV, ['total'], 510000, 25000, 20.4, maps_free),
    # Tariff('apimaps_50000_yearprepay_contractless_15_discount', MAPS_SRV, ['total'], 722500, 50000, 14.45, maps_free),
    # Tariff('apimaps_100000_yearprepay_contractless_15_discount', MAPS_SRV, ['total'], 850000, 100000, 9.35, maps_free),

    # common
    pytest.mark.smoke(Tariff('apimaps_1000_yearprepay_contractless', MAPS_SRV, ['total'],
                             120000, 1000, 120, maps_free)),
    Tariff('apimaps_10000_yearprepay_contractless', MAPS_SRV, ['total'], 360000, 10000, 36, maps_free),
    Tariff('apimaps_25000_yearprepay_contractless', MAPS_SRV, ['total'], 600000, 25000, 24, maps_free),
    Tariff('apimaps_50000_yearprepay_contractless', MAPS_SRV, ['total'], 850000, 50000, 17, maps_free),
    Tariff('apimaps_100000_yearprepay_contractless', MAPS_SRV, ['total'], 1000000, 100000, 11, maps_free),
]

ROUTINGMATRIX_SRV = 'routingmatrix'
routingmatrix_contractless_tariffs = [
    # common
    pytest.mark.smoke(Tariff('routingmatrix_1000_yearprepay_contractless_072019', ROUTINGMATRIX_SRV, ['total'], 120000, 1000, 120, maps_free)),
]

MAPKIT_SRV = 'mapkit'
mapkit_contractless_tariffs = [
    # common
    pytest.mark.smoke(Tariff('mapkit_1000_yearprepay_contractless', MAPKIT_SRV, ['total'], 120000, 1000, 120, maps_free)),
]

CITY_SRV = 'city'
city_contractless_tariffs = [
    # discount
    # Дисконтные тарифы закончили свое действие ссентябре 2019
    # pytest.mark.smoke(Tariff('city_1000_yearprepay_contractless_15_discount', CITY_SRV, ['hits'],
    #                          102000, 1000, 102, maps_free)),
    # Tariff('city_10000_yearprepay_contractless_15_discount', CITY_SRV, ['hits'], 306000, 10000, 30.6, maps_free),
    # Tariff('city_25000_yearprepay_contractless_15_discount', CITY_SRV, ['hits'], 510000, 25000, 20.4, maps_free),
    # Tariff('city_50000_yearprepay_contractless_15_discount', CITY_SRV, ['hits'], 722500, 50000, 14.45, maps_free),
    # Tariff('city_100000_yearprepay_contractless_15_discount', CITY_SRV, ['hits'], 850000, 100000, 9.35, maps_free),

    # common
    pytest.mark.smoke(Tariff('city_1000_yearprepay_contractless', CITY_SRV, ['hits'],
                             120000, 1000, 120, city_free)),
    Tariff('city_10000_yearprepay_contractless', CITY_SRV, ['hits'], 360000, 10000, 36, city_free),
    Tariff('city_25000_yearprepay_contractless', CITY_SRV, ['hits'], 600000, 25000, 24, city_free),
    Tariff('city_50000_yearprepay_contractless', CITY_SRV, ['hits'], 850000, 50000, 17, city_free),
    Tariff('city_100000_yearprepay_contractless', CITY_SRV, ['hits'], 1000000, 100000, 11, city_free),
]

STATICMAPS_SRV = 'staticmaps'
staticmaps_contractless_tariffs = [
    # discount
# Дисконтные тарифы закончили свое действие ссентябре 2019
    # pytest.mark.smoke(Tariff('staticmaps_1000_yearprepay_contractless_15_discount', STATICMAPS_SRV, ['hits'],
    #                          102000, 1000, 102, maps_free)),
    # Tariff('staticmaps_10000_yearprepay_contractless_15_discount', STATICMAPS_SRV, ['hits'], 306000, 10000, 30.6,
    #        maps_free),
    # Tariff('staticmaps_25000_yearprepay_contractless_15_discount', STATICMAPS_SRV, ['hits'], 510000, 25000, 20.4,
    #        maps_free),
    # Tariff('staticmaps_50000_yearprepay_contractless_15_discount', STATICMAPS_SRV, ['hits'], 722500, 50000, 14.45,
    #        maps_free),
    # Tariff('staticmaps_100000_yearprepay_contractless_15_discount', STATICMAPS_SRV, ['hits'], 850000, 100000, 9.35,
    #        maps_free),

    # common
    pytest.mark.smoke(Tariff('staticmaps_1000_yearprepay_contractless', STATICMAPS_SRV, ['hits'],
                             120000, 1000, 120, None)),
    Tariff('staticmaps_10000_yearprepay_contractless', STATICMAPS_SRV, ['hits'], 360000, 10000, 36, None),
    Tariff('staticmaps_25000_yearprepay_contractless', STATICMAPS_SRV, ['hits'], 600000, 25000, 24, None),
    Tariff('staticmaps_50000_yearprepay_contractless', STATICMAPS_SRV, ['hits'], 850000, 50000, 17, None),
    Tariff('staticmaps_100000_yearprepay_contractless', STATICMAPS_SRV, ['hits'], 1000000, 100000, 11, None),
]

general_scenarios = [
    # Test-case 0
    {'description': u'[limit - 1]',
     'base_dt': BASE_DT - shift(days=6),
     'stats': [
         {'completions': {'shift_limit': -1}, 'dt': BASE_DT - shift(days=6)},
     ],
     'need_turn_on_tariff': False,
     'active_after_scenario': True,
     'over_limit': False,
     'close_month': True},

    # Test-case 1
    {'description': u'[equal to limit]',
     'base_dt': BASE_DT - shift(days=6),
     'stats': [
         {'completions': {'shift_limit': 0}, 'dt': BASE_DT - shift(days=6)},
         {'completions': {'shift_limit': 0}, 'dt': BASE_DT - shift(days=5)},
     ],
     'need_turn_on_tariff': False,
     'active_after_scenario': True,
     'over_limit': False,
     'close_month': True},

    # Test-case 2
    {'description': u'[limit + 1, payed]',
     'base_dt': BASE_DT - shift(days=5),
     'stats': [
         {'completions': {'shift_limit': 1}, 'dt': BASE_DT - shift(days=5)},
         {'completions': {'shift_limit': 0}, 'dt': BASE_DT - shift(days=4)},
     ],
     'need_turn_on_tariff': False,
     'active_after_scenario': True,
     'over_limit': True,
     'close_month': True},

    # Test-case 3
    {'description': u'[0]',
     'base_dt': BASE_DT - shift(days=2),
     'stats': [
         {'completions': {'shift_limit': 'to_zero'}, 'dt': BASE_DT - shift(days=2)},
     ],
     'need_turn_on_tariff': False,
     'active_after_scenario': True,
     'over_limit': False},

    # Test-case 4
    {'description': u'[change month]',
     'base_dt': START_PREVIOUS_MONTH + shift(days=4),
     'stats': [
         {'completions': {'shift_limit': 0}, 'dt': START_PREVIOUS_MONTH + shift(days=4)},
         {'completions': {'shift_limit': 1999}, 'dt': START_PREVIOUS_MONTH + shift(days=5)},
         {'completions': {'shift_limit': 1}, 'dt': START_PREVIOUS_MONTH + shift(days=6)},
         {'completions': {'shift_limit': -1}, 'dt': START_PREVIOUS_MONTH + shift(days=8)},
         {'completions': {'shift_limit': -999}, 'dt': START_PREVIOUS_MONTH + shift(days=10)},
         {'completions': {'shift_limit': 'to_zero'}, 'dt': END_PREVIOUS_MONTH - shift(days=2)},
         {'completions': {'shift_limit': 1001}, 'dt': END_PREVIOUS_MONTH - shift(hours=5)},
         {'completions': {'shift_limit': 5500}, 'dt': START_CURRENT_MONTH + shift(hours=5)},
         # {'completions': {'shift_limit': 1000}, 'dt': START_CURRENT_MONTH + shift(days=1)},
     ],
     'need_turn_on_tariff': False,
     'active_after_scenario': True,
     'over_limit': True},

    # Test-case 5
    {'description': u'[multikeys over limit]',
     'base_dt': START_PREVIOUS_MONTH + shift(days=1),
     'stats': [
         {'completions': {'shift_limit': 1000}, 'dt': START_PREVIOUS_MONTH + shift(days=1), 'key': 1},
         {'completions': {'shift_limit': 1999}, 'dt': START_PREVIOUS_MONTH + shift(days=1), 'key': 2},
         {'completions': {'shift_limit': 500}, 'dt': START_PREVIOUS_MONTH + shift(days=1), 'key': 3},
         {'completions': {'shift_limit': 1}, 'dt': START_PREVIOUS_MONTH + shift(days=2), 'key': 1},
         {'completions': {'shift_limit': -1}, 'dt': START_PREVIOUS_MONTH + shift(days=2), 'key': 2},
         {'completions': {'shift_limit': 'to_zero'}, 'dt': START_PREVIOUS_MONTH + shift(days=3), 'key': 1},
         {'completions': {'shift_limit': -1}, 'dt': START_PREVIOUS_MONTH + shift(days=3), 'key': 2},
         {'completions': {'shift_limit': 1}, 'dt': START_PREVIOUS_MONTH + shift(days=4), 'key': 1},
         {'completions': {'shift_limit': 'to_zero'}, 'dt': START_PREVIOUS_MONTH + shift(days=4), 'key': 2},
     ],
     'need_turn_on_tariff': False,
     'active_after_scenario': True,
     'over_limit': True},

    # Test-case 6
    {'description': u'[multikeys in limit]',
     'base_dt': START_PREVIOUS_MONTH + shift(days=1),
     'stats': [
         {'completions': {'shift_limit': 0}, 'dt': START_PREVIOUS_MONTH + shift(days=1), 'key': 1},
         {'completions': {'shift_limit': 'to_zero'}, 'dt': START_PREVIOUS_MONTH + shift(days=1), 'key': 2},
         {'completions': {'shift_limit': 'to_zero'}, 'dt': START_PREVIOUS_MONTH + shift(days=1), 'key': 3},
         {'completions': {'shift_limit': 'to_zero'}, 'dt': START_PREVIOUS_MONTH + shift(days=2), 'key': 1},
         {'completions': {'shift_limit': -1}, 'dt': START_PREVIOUS_MONTH + shift(days=2), 'key': 2},
         {'completions': {'shift_limit': 'to_zero'}, 'dt': START_PREVIOUS_MONTH + shift(days=3), 'key': 1},
         {'completions': {'shift_limit': -1}, 'dt': START_PREVIOUS_MONTH + shift(days=3), 'key': 2},
         {'completions': {'shift_limit': 0}, 'dt': START_PREVIOUS_MONTH + shift(days=4), 'key': 1},
         {'completions': {'shift_limit': 'to_zero'}, 'dt': START_PREVIOUS_MONTH + shift(days=4), 'key': 2},
     ],
     'need_turn_on_tariff': False,
     'active_after_scenario': True,
     'over_limit': False},

    # Test-case 7
    {'description': u'[limit + 1, not payed]',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': 0}, 'dt': BASE_DT},
         {'completions': {'shift_limit': 1}, 'dt': BASE_DT+shift(hours=1)},
     ],
     'need_turn_on_tariff': False,
     'active_after_scenario': True,
     'over_limit': False,
     'close_month': True},
]


@pytest.mark.parametrize('person_type', [ur_apimaps], ids=lambda x: '[{}]'.format(x[0]))
@pytest.mark.parametrize('tariff', get_parameters(
    routingmatrix_contractless_tariffs +
    mapkit_contractless_tariffs+
    # testapimaps_contractless_tariffs +
    apimaps_contractless_tariffs +
    city_contractless_tariffs +
    staticmaps_contractless_tariffs),
                         ids=lambda x: x.name)
@pytest.mark.parametrize(
    'scenario', get_parameters(general_scenarios,)
    , ids=lambda x: x['description'])
@pytest.mark.good
def test_tariff_general(scenario, tariff, db_connection, person_type):
    scenario_copy = aDict(deepcopy(scenario))
    scenario_copy.tariff = tariff.name
    scenario_copy.stats = plain.completions_shift_limit(scenario_copy.stats, tariff.day_limit, tariff.counters)
    flow.Prepayment.basic(scenario_copy, db_connection, tariff.service_id, tariff, person_type)


general_overlimit = [
    # Test-case 0
    {'description': u'[over limit]',
     'base_dt': BASE_DT - shift(days=6),
     'stats': [
         {'completions': {'shift_limit': 0}, 'dt': BASE_DT - shift(days=6)},
         {'completions': {'shift_limit': 1}, 'dt': BASE_DT - shift(days=5)},
         {'completions': {'shift_limit': 10}, 'dt': BASE_DT - shift(days=1)},
         {'completions': {'shift_limit': 10}, 'dt': BASE_DT - shift(days=1, hours=1)},
         {'completions': {'shift_limit': 10}, 'dt': BASE_DT - shift(days=1, hours=2)},

     ],
     'need_turn_on_tariff': False,
     'active_after_scenario': True,
     'extra_money_multiplier': 2,
     'over_limit': True},
]


@pytest.mark.parametrize('person_type', [ur_apimaps], ids=lambda x: '[{}]'.format(x[0]))
@pytest.mark.parametrize('tariff', get_parameters(apimaps_contractless_tariffs, ), ids=lambda x: x.name)
@pytest.mark.parametrize(
    'scenario', get_parameters(general_overlimit, )
    , ids=lambda x: x['description'])
@pytest.mark.good
def test_overlimit(scenario, tariff, person_type, db_connection):
    scenario_copy = aDict(deepcopy(scenario))
    scenario_copy.tariff = tariff.name
    scenario_copy.stats = plain.completions_shift_limit(scenario_copy.stats, tariff.day_limit, tariff.counters)
    scenario_copy.prepaid_money = tariff.year_price
    flow.Prepayment.overlimit(scenario_copy, db_connection, tariff.service_id, tariff, person_type)
