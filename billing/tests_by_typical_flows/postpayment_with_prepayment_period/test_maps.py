# -*- coding: utf-8 -*-

from collections import namedtuple
from copy import deepcopy
from datetime import datetime, timedelta as shift

import pytest

from btestlib.utils import aDict
from apikeys.apikeys_utils import get_parameters, trunc_date
import apikeys.tests_by_typical_flows.typical_flows as flow
from btestlib import utils

__author__ = 'kostya-karpus'

BASE_DT = trunc_date(datetime.utcnow().replace(hour=5),'hour')
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)

Free_limits = namedtuple('Free_limits', ['counters', 'limit', 'need_approve'])

maps_free = Free_limits(['total'], 25000, True)
city_free = Free_limits(['hits'], 500, False)
maps_trial = Free_limits(['total'], 1000, False)

Tariff = namedtuple('Tariff',
                    ['name', 'service_id', 'counters', 'year_price', 'day_limit',
                     'price_over_limit_per_1000', 'free_counters'])

expected_invoices_on_day = namedtuple('expected_invoices_on_day', ['shift_days', 'invoices_amount'])

APIMAPS_SRV = 'apimaps'
apimaps_tariffs = [
    pytest.mark.smoke(Tariff('apikeys_apimaps_1000_yearprepay_2017', APIMAPS_SRV, ['total'],
                             120000, 1000, 120, maps_free)),
    Tariff('apikeys_apimaps_10k_yearprepay_2017', APIMAPS_SRV, ['total'], 360000, 10000, 36, maps_free),
    Tariff('apikeys_apimaps_25k_yearprepay_2017', APIMAPS_SRV, ['total'], 600000, 25000, 24, maps_free),
    Tariff('apikeys_apimaps_50k_yearprepay_2017', APIMAPS_SRV, ['total'], 850000, 50000, 17, maps_free),
    Tariff('apikeys_apimaps_100k_yearprepay_2017', APIMAPS_SRV, ['total'], 1000000, 100000, 11, maps_free),
    Tariff('apikeys_apimaps_200k_yearprepay_2017', APIMAPS_SRV, ['total'], 1396000, 200000, 9.5, maps_free),
    Tariff('apikeys_apimaps_300k_yearprepay_2017', APIMAPS_SRV, ['total'], 1792000, 300000, 8, maps_free),
    Tariff('apikeys_apimaps_500k_yearprepay_2017', APIMAPS_SRV, ['total'], 2584000, 500000, 5.2, maps_free),
    Tariff('apikeys_apimaps_1b_yearprepay_2017', APIMAPS_SRV, ['total'], 4564000, 1000000, 4.5, maps_free),
    Tariff('apikeys_apimaps_1.5b_yearprepay_2017', APIMAPS_SRV, ['total'], 6544000, 1500000, 4.4, maps_free),
    Tariff('apikeys_apimaps_2b_yearprepay_2017', APIMAPS_SRV, ['total'], 8524000, 2000000, 4.3, maps_free),
    Tariff('apikeys_apimaps_3b_yearprepay_2017', APIMAPS_SRV, ['total'], 12484000, 3000000, 4.2, maps_free),
    Tariff('apikeys_apimaps_4b_yearprepay_2017', APIMAPS_SRV, ['total'], 16444000, 4000000, 4.1, maps_free),
    pytest.mark.smoke(Tariff('apikeys_apimaps_1000_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'],
                             620000, 1000, 120, maps_free)),
    Tariff('apikeys_apimaps_10k_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'], 860000, 10000, 36, maps_trial),
    Tariff('apikeys_apimaps_25k_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'], 1100000, 25000, 24, maps_trial),
    Tariff('apikeys_apimaps_50k_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'], 1350000, 50000, 17, maps_trial),
    Tariff('apikeys_apimaps_100k_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'], 1500000, 100000, 11, maps_trial),
    Tariff('apikeys_apimaps_500k_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'], 3084000, 500000, 5.2, maps_trial),
    Tariff('apikeys_apimaps_1b_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'], 5064000, 1000000, 4.5, maps_trial),
    Tariff('apikeys_apimaps_1.5b_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'], 7044000, 1500000, 4.4,
           maps_trial),
    Tariff('apikeys_apimaps_2b_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'], 9024000, 2000000, 4.3, maps_trial),
    Tariff('apikeys_apimaps_3b_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'], 12984000, 3000000, 4.2, maps_trial),
    Tariff('apikeys_apimaps_4b_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'], 16944000, 4000000, 4.1, maps_trial),
    Tariff('apikeys_apimaps_5b_yearprepay_2017', APIMAPS_SRV, ['total'], 20404000, 5000000, 4, maps_trial),
    Tariff('apikeys_apimaps_5b_yearprepay_noban_plus_2018', APIMAPS_SRV, ['total'], 20904000, 5000000, 4, maps_trial),
]

apimaps_tariff_ban = [
    pytest.mark.smoke(Tariff('apikeys_apimaps_1000_yearprepay_ban_minus_2018',
                             APIMAPS_SRV, ['total'], 120000, 1000, None, maps_free)),
    pytest.mark.smoke(Tariff('apikeys_apimaps_1000_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'],
                             620000, 1000, None, maps_free)),
    Tariff('apikeys_apimaps_10k_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 360000, 10000, None, maps_trial),
    Tariff('apikeys_apimaps_10k_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'], 860000, 10000, None, maps_trial),
    Tariff('apikeys_apimaps_25k_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 600000, 25000, None, maps_trial),
    Tariff('apikeys_apimaps_25k_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'], 1100000, 25000, None, maps_trial),
    Tariff('apikeys_apimaps_50k_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 850000, 50000, None, maps_trial),
    Tariff('apikeys_apimaps_50k_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'], 1350000, 50000, None, maps_trial),
    Tariff('apikeys_apimaps_100k_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 1000000, 100000, None, maps_trial),
    Tariff('apikeys_apimaps_100k_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'], 1500000, 100000, None, maps_trial),
    Tariff('apikeys_apimaps_200k_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 1396000, 200000, None, maps_trial),
    Tariff('apikeys_apimaps_300k_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 1792000, 300000, None, maps_trial),
    Tariff('apikeys_apimaps_500k_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 2584000, 500000, None, maps_trial),
    Tariff('apikeys_apimaps_500k_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'], 3084000, 500000, None, maps_trial),
    Tariff('apikeys_apimaps_1b_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 4564000, 1000000, None, maps_trial),
    Tariff('apikeys_apimaps_1b_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'], 5064000, 1000000, None, maps_trial),
    Tariff('apikeys_apimaps_1.5b_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 6544000, 1500000, None,
           maps_trial),
    Tariff('apikeys_apimaps_1.5b_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'], 7044000, 1500000, None, maps_trial),
    Tariff('apikeys_apimaps_2b_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 8524000, 2000000, None, maps_trial),
    Tariff('apikeys_apimaps_2b_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'], 9024000, 2000000, None, maps_trial),
    Tariff('apikeys_apimaps_3b_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 12484000, 3000000, None, maps_trial),
    Tariff('apikeys_apimaps_3b_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'], 12984000, 3000000, None, maps_trial),
    Tariff('apikeys_apimaps_4b_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 16444000, 4000000, None, maps_trial),
    Tariff('apikeys_apimaps_4b_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'], 16944000, 4000000, None, maps_trial),
    Tariff('apikeys_apimaps_5b_yearprepay_ban_minus_2018', APIMAPS_SRV, ['total'], 20404000, 5000000, None, maps_trial),
    Tariff('apikeys_apimaps_5b_yearprepay_ban_plus_2018', APIMAPS_SRV, ['total'], 20904000, 5000000, None, maps_trial),
]

STATICMAPS_SERVICE = 'staticmaps'
staticmaps_tarifs = [
    pytest.mark.smoke(Tariff('apikeys_staticmaps_1000_yearprepay_2017', STATICMAPS_SERVICE, ['hits'],
                             120000, 1000, 120, None)),
    Tariff('apikeys_staticmaps_10k_yearprepay_2017', STATICMAPS_SERVICE, ['hits'], 360000, 10000, 36, None),
    Tariff('apikeys_staticmaps_25k_yearprepay_2017', STATICMAPS_SERVICE, ['hits'], 600000, 25000, 24, None),
    Tariff('apikeys_staticmaps_50k_yearprepay_2017', STATICMAPS_SERVICE, ['hits'], 850000, 50000, 17, None),
    Tariff('apikeys_staticmaps_100k_yearprepay_2017', STATICMAPS_SERVICE, ['hits'], 1000000, 100000, 11, None),
    Tariff('apikeys_staticmaps_500k_yearprepay_2017', STATICMAPS_SERVICE, ['hits'], 2584000, 500000, 5.2, None),
    Tariff('apikeys_staticmaps_1b_yearprepay_2017', STATICMAPS_SERVICE, ['hits'], 4564000, 1000000, 4.5, None),
    Tariff('apikeys_staticmaps_1.5b_yearprepay_2017', STATICMAPS_SERVICE, ['hits'], 6544000, 1500000, 4.4, None),
    Tariff('apikeys_staticmaps_2b_yearprepay_2017', STATICMAPS_SERVICE, ['hits'], 8524000, 2000000, 4.3, None),
    Tariff('apikeys_staticmaps_3b_yearprepay_2017', STATICMAPS_SERVICE, ['hits'], 12484000, 3000000, 4.2, None),
    Tariff('apikeys_staticmaps_4b_yearprepay_2017', STATICMAPS_SERVICE, ['hits'], 16444000, 4000000, 4.1, None),
    Tariff('apikeys_staticmaps_5b_yearprepay_2017', STATICMAPS_SERVICE, ['hits'], 20404000, 5000000, 4, None),
]

CITY_SERVICE = 'city'
city_tarifs = [
    pytest.mark.smoke(Tariff('apikeys_city_1000_yearprepay_2017', CITY_SERVICE, ['hits'],
                             120000, 1000, 120, city_free)),
    Tariff('apikeys_city_10k_yearprepay_2017', CITY_SERVICE, ['hits'], 360000, 10000, 36, city_free),
    Tariff('apikeys_city_25k_yearprepay_2017', CITY_SERVICE, ['hits'], 600000, 25000, 24, city_free),
    Tariff('apikeys_city_50k_yearprepay_2017', CITY_SERVICE, ['hits'], 850000, 50000, 17, city_free),
    Tariff('apikeys_city_100k_yearprepay_2017', CITY_SERVICE, ['hits'], 1000000, 100000, 11, city_free),
    Tariff('apikeys_city_500k_yearprepay_2017', CITY_SERVICE, ['hits'], 2584000, 500000, 5.2, city_free),
    Tariff('apikeys_city_1b_yearprepay_2017', CITY_SERVICE, ['hits'], 4564000, 1000000, 4.5, city_free),
    Tariff('apikeys_city_1.5b_yearprepay_2017', CITY_SERVICE, ['hits'], 6544000, 1500000, 4.4, city_free),
    Tariff('apikeys_city_2b_yearprepay_2017', CITY_SERVICE, ['hits'], 8524000, 2000000, 4.3, city_free),
    Tariff('apikeys_city_3b_yearprepay_2017', CITY_SERVICE, ['hits'], 12484000, 3000000, 4.2, city_free),
    Tariff('apikeys_city_4b_yearprepay_2017', CITY_SERVICE, ['hits'], 16444000, 4000000, 4.1, city_free),
    Tariff('apikeys_city_5b_yearprepay_2017', CITY_SERVICE, ['hits'], 20404000, 5000000, 4, city_free),
]

APIMAPSPLUS_SERVICE = 'apimapsplus'
apimapsplus_tarifs = [
    pytest.mark.smoke(Tariff('apikeys_apimapsplus_1000_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'],
                             620000, 1000, 120, None)),
    Tariff('apikeys_apimapsplus_10k_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'], 860000, 10000, 36, None),
    Tariff('apikeys_apimapsplus_25k_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'], 1100000, 25000, 24, None),
    Tariff('apikeys_apimapsplus_50k_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'], 1350000, 50000, 17, None),
    Tariff('apikeys_apimapsplus_100k_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'], 1500000, 100000, 11, None),
    Tariff('apikeys_apimapsplus_500k_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'], 3084000, 500000, 5.2, None),
    Tariff('apikeys_apimapsplus_1b_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'], 5064000, 1000000, 4.5, None),
    Tariff('apikeys_apimapsplus_1.5b_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'], 7044000, 1500000, 4.4, None),
    Tariff('apikeys_apimapsplus_2b_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'], 9024000, 2000000, 4.3, None),
    Tariff('apikeys_apimapsplus_3b_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'], 12984000, 3000000, 4.2, None),
    Tariff('apikeys_apimapsplus_4b_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'], 16944000, 4000000, 4.1, None),
    Tariff('apikeys_apimapsplus_5b_yearprepay_2017', APIMAPSPLUS_SERVICE, ['hits'], 20904000, 5000000, 4, None),
]

general_scenarios = [
    # Test-case 0
    {'description': u'limit - 1',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': -1}, 'dt': BASE_DT},
     ],
     'over_limit': False,
     'close_month': True},

    # Test-case 1
    {'description': u'equal to limit',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': 0}, 'dt': BASE_DT - shift(days=6)},
         {'completions': {'shift_limit': 0}, 'dt': BASE_DT},
     ],
     'over_limit': False,
     'close_month': True},

    # Test-case 2
    {'description': u'limit + 1',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': 1}, 'dt': BASE_DT - shift(days=1)},
     ],
     'over_limit': True,
     'close_month': True},

    # Test-case 3
    {'description': u'0',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': 'to_zero'}, 'dt': BASE_DT - shift(days=2)},
     ],
     'over_limit': False},

    # Test-case 4
    {'description': u'change month',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': 0}, 'dt': START_PREVIOUS_MONTH + shift(days=4)},
         {'completions': {'shift_limit': 1999}, 'dt': START_PREVIOUS_MONTH + shift(days=5)},
         {'completions': {'shift_limit': 1}, 'dt': START_PREVIOUS_MONTH + shift(days=6)},
         {'completions': {'shift_limit': -1}, 'dt': START_PREVIOUS_MONTH + shift(days=8)},
         {'completions': {'shift_limit': -999}, 'dt': START_PREVIOUS_MONTH + shift(days=10)},
         {'completions': {'shift_limit': 'to_zero'}, 'dt': END_PREVIOUS_MONTH - shift(days=2)},
         {'completions': {'shift_limit': 1001}, 'dt': END_PREVIOUS_MONTH - shift(hours=5)},
         {'completions': {'shift_limit': 5500}, 'dt': START_CURRENT_MONTH + shift(hours=5)},
     ],
     'over_limit': True},

    # Test-case 5
    {'description': u'multikeys over limit',
     'base_dt': BASE_DT,
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
     'over_limit': True},

    # Test-case 6
    {'description': u'multikeys in limit',
     'base_dt': BASE_DT,
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
     'over_limit': False},
]


@pytest.mark.parametrize(
    'tariff',
    get_parameters(
        # apimaps_tariff_ban +
        apimaps_tariffs
        # staticmaps_tarifs +
        # city_tarifs +
        # apimapsplus_tarifs,
        )
    # Для выбора отдельных тарифов нужно написать индексы чрез запятую 0, 7
    , ids=lambda x: '{}-{}'.format(x.service_id, x.name.replace('.', '_')))
@pytest.mark.parametrize(
    'scenario', get_parameters(general_scenarios,)
    # Для выбора отдельных сценариев нужно написать индексы чрез запятую 0, 2
    , ids=lambda x: x.get('description'))
@pytest.mark.good
def test_paid(scenario, tariff, db_connection):
    scenario_copy = aDict(deepcopy(scenario))
    scenario_copy.tariff = tariff.name
    flow.PostpaymentWithPrepaymentPeriod.basic(scenario_copy, db_connection, tariff.service_id, tariff)


free_to_paid_scenarios = [
    # Test-case 0
    {'description': u'free and paid equal to limit',
     'free': {
         'base_dt': BASE_DT-shift(days=180),
         'stats': [
             {'completions': {'shift_limit': 0}, 'dt': BASE_DT - shift(days=180)},
         ]},
     'paid': {
         'base_dt': BASE_DT,
         'stats': [
             {'completions': {'shift_limit': 0}, 'dt': BASE_DT - shift(days=4)},
         ],
         'over_limit': False},
     },

    # Test-case 1
    {'description': u'free and paid over limit',
     'free': {
         'base_dt': BASE_DT-shift(days=180),
         'stats': [
             {'completions': {'shift_limit': 1}, 'dt': BASE_DT - shift(days=180)},
         ]},
     'paid': {
         'base_dt': BASE_DT,
         'stats': [
             {'completions': {'shift_limit': -1}, 'dt': BASE_DT - shift(days=4)},
             {'completions': {'shift_limit': 1}, 'dt': BASE_DT - shift(days=3)},
         ],
         'over_limit': True},
     'close_month': True},

    # Test-case 2
    {'description': u'free and paid under limit',
     'free': {
         'base_dt': BASE_DT-shift(days=180),
         'stats': [
             {'completions': {'shift_limit': -1}, 'dt': BASE_DT - shift(days=180)},
         ]},
     'paid': {
         'base_dt': BASE_DT,
         'stats': [
             {'completions': {'shift_limit': -1}, 'dt': BASE_DT - shift(days=4)},
         ],
         'over_limit': False},
     'close_month': True,
     },

    # Test-case 3
    {'description': u'free < paid >',
     'free': {
         'base_dt': BASE_DT-shift(days=180),
         'stats': [
             {'completions': {'shift_limit': -10}, 'dt': BASE_DT - shift(days=180)},
         ]},
     'paid': {
         'base_dt': BASE_DT,
         'stats': [
             {'completions': {'shift_limit': 0}, 'dt': BASE_DT - shift(days=4)},
             {'completions': {'shift_limit': 1999}, 'dt': BASE_DT - shift(days=3)},
         ],
         'over_limit': True},
     },

    # Test-case 4
    {'description': u'free > paid < one day',
     'free': {
         'base_dt': BASE_DT-shift(days=180),
         'stats': [
             {'completions': {'shift_limit': 10}, 'dt': BASE_DT - shift(days=180)},
         ]},
     'paid': {
         'base_dt': BASE_DT,
         'stats': [
             {'completions': {'shift_limit': -500}, 'dt': BASE_DT - shift(days=5)},
         ],
         'over_limit': False},
     },
]


@pytest.mark.parametrize(
    'tariff',
    get_parameters(apimaps_tariffs + city_tarifs,)
    # Для выбора отдельных тарифов нужно написать индексы чрез запятую 0, 7
    , ids=lambda x: x.name.replace('.', '_'))
@pytest.mark.parametrize(
    'scenario',
    get_parameters(free_to_paid_scenarios,)
    # Для выбора отдельных сценариев нужно написать индексы чрез запятую 0, 2,
    , ids=lambda x: x.get('description'))
@pytest.mark.good
def test_free_to_paid(scenario, tariff, db_connection):
    scenario_copy = aDict(deepcopy(scenario))
    scenario_copy.paid['tariff'] = tariff.name
    flow.PostpaymentWithPrepaymentPeriod.free_to_paid(scenario_copy, db_connection, tariff.service_id, tariff)


@pytest.mark.invoices
@pytest.mark.parametrize(
    'tariff',
    get_parameters(apimaps_tariffs + staticmaps_tarifs + city_tarifs + apimapsplus_tarifs, 0)
    # Для выбора отдельных тарифов нужно написать индексы чрез запятую 0, 7
    , ids=lambda x: '{}-{}'.format(x.service_id, x.name.replace('.', '_')))
@pytest.mark.parametrize(
    'scenario',
    [{'description': u'one year',
      'base_dt': BASE_DT,
      'shift_days_invoices': [
          expected_invoices_on_day(shift_days=0, invoices_amount=1),
          expected_invoices_on_day(shift_days=335, invoices_amount=1),
          expected_invoices_on_day(shift_days=336, invoices_amount=2),
          expected_invoices_on_day(shift_days=365, invoices_amount=2),
      ]}
     ],
    ids=lambda x: x.get('description'))
@pytest.mark.good
def test_invoices(scenario, tariff, db_connection):
    scenario_copy = aDict(deepcopy(scenario))
    scenario_copy.tariff = tariff.name
    flow.PostpaymentWithPrepaymentPeriod.invoices(scenario_copy, db_connection, tariff.service_id, tariff)


if __name__ == '__main__':
    pytest.main('-v')
