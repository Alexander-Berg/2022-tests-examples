# -*- coding: utf-8 -*-

from collections import namedtuple
from copy import deepcopy
from datetime import datetime, timedelta as shift

import pytest


from btestlib.utils import aDict
from apikeys.apikeys_utils import get_parameters, trunc_date
import apikeys.tests_by_typical_flows.typical_flows as flow
from btestlib import utils

__author__ = 'ilya_knysh'

BASE_DT = trunc_date(datetime.utcnow().replace(hour=5),'hour')
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)

Free_limits = namedtuple('Free_limits', ['counters', 'limit', 'need_approve'])
mapkit_free = Free_limits('total', 1000, True)

Tariff_call = namedtuple('Tariff',
                         ['name', 'service_id', 'counters', 'year_price', 'day_limit', 'price_over_limit_per_1000',
                          'free_counters', 'ban'])

Tariff_device = namedtuple('Tariff',
                           ['name', 'service_id', 'counters', 'year_price', 'month_limit', 'price_over_limit_per_1',
                            'ban'])

MAPKIT_SRV = 'mapkit'

mapkit_tariffs_call = [
    Tariff_call('apikeys_mapkit_1000_yearprepay_2018', MAPKIT_SRV, ['total'], 120000, 1000, 120, mapkit_free, False),
    Tariff_call('apikeys_mapkit_1000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 120000, 1000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_10000_yearprepay_2018', MAPKIT_SRV, ['total'], 360000, 10000, 36, mapkit_free, False),
    Tariff_call('apikeys_mapkit_10000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 360000, 10000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_25000_yearprepay_2018', MAPKIT_SRV, ['total'], 600000, 25000, 24, mapkit_free, False),
    Tariff_call('apikeys_mapkit_25000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 600000, 25000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_50000_yearprepay_2018', MAPKIT_SRV, ['total'], 850000, 50000, 17, mapkit_free, False),
    Tariff_call('apikeys_mapkit_50000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 850000, 50000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_100000_yearprepay_2018', MAPKIT_SRV, ['total'], 1000000, 100000, 11, mapkit_free, False),
    Tariff_call('apikeys_mapkit_100000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 1000000, 100000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_200000_yearprepay_2018', MAPKIT_SRV, ['total'], 1396000, 200000, 9.5, mapkit_free, False),
    Tariff_call('apikeys_mapkit_200000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 1396000, 200000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_300000_yearprepay_2018', MAPKIT_SRV, ['total'], 1792000, 300000, 8, mapkit_free, False),
    Tariff_call('apikeys_mapkit_300000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 1792000, 300000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_500000_yearprepay_2018', MAPKIT_SRV, ['total'], 2584000, 500000, 5.2, mapkit_free, False),
    Tariff_call('apikeys_mapkit_500000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 2584000, 500000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_1000000_yearprepay_2018', MAPKIT_SRV, ['total'], 4564000, 1000000, 4.5, mapkit_free, False),
    Tariff_call('apikeys_mapkit_1000000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 4564000, 1000000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_1500000_yearprepay_2018', MAPKIT_SRV, ['total'], 6544000, 1500000, 4.4, mapkit_free, False),
    Tariff_call('apikeys_mapkit_1500000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 6544000, 1500000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_2000000_yearprepay_2018', MAPKIT_SRV, ['total'], 8524000, 2000000, 4.3, mapkit_free, False),
    Tariff_call('apikeys_mapkit_2000000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 8524000, 2000000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_3000000_yearprepay_2018', MAPKIT_SRV, ['total'], 12484000, 3000000, 4.2, mapkit_free, False),
    Tariff_call('apikeys_mapkit_3000000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 12484000, 3000000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_4000000_yearprepay_2018', MAPKIT_SRV, ['total'], 16444000, 4000000, 4.1, mapkit_free, False),
    Tariff_call('apikeys_mapkit_4000000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 16444000, 4000000, None, mapkit_free, True),
    Tariff_call('apikeys_mapkit_5000000_yearprepay_2018', MAPKIT_SRV, ['total'], 20404000, 5000000, 4, mapkit_free, False),
    Tariff_call('apikeys_mapkit_5000000_yearprepay_ban_2018', MAPKIT_SRV, ['total'], 20404000, 5000000, None, mapkit_free, True),
]

mapkit_tariffs_device = [
    Tariff_device('apikeys_mapkit_100_per_device_2018', MAPKIT_SRV, ['devices'], 100000, 100, 125, False),
    Tariff_device('apikeys_mapkit_200_per_device_2018', MAPKIT_SRV, ['devices'], 200000, 200, 125, False),
    Tariff_device('apikeys_mapkit_500_per_device_2018', MAPKIT_SRV, ['devices'], 450000, 500, 113, False),
    Tariff_device('apikeys_mapkit_1000_per_device_2018', MAPKIT_SRV, ['devices'], 900000, 1000, 113, False),
    Tariff_device('apikeys_mapkit_2000_per_device_2018', MAPKIT_SRV, ['devices'], 1700000, 2000, 106, False),
    Tariff_device('apikeys_mapkit_3000_per_device_2018', MAPKIT_SRV, ['devices'], 2550000, 3000, 106, False),
    Tariff_device('apikeys_mapkit_4000_per_device_2018', MAPKIT_SRV, ['devices'], 3400000, 4000, 106, False),
    Tariff_device('apikeys_mapkit_5000_per_device_2018', MAPKIT_SRV, ['devices'], 4250000, 5000, 106, False),
    Tariff_device('apikeys_mapkit_7000_per_device_2018', MAPKIT_SRV, ['devices'], 5600000, 7000, 100, False),
    Tariff_device('apikeys_mapkit_10000_per_device_2018', MAPKIT_SRV, ['devices'], 8000000, 10000, 100, False),
]

general_scenarios = [
    # Test-case 0
    {'description': u'limit - 1',
     'base_dt': BASE_DT - shift(days=1),
     'stats': [
         {'completions': {'shift_limit': -1}, 'dt': BASE_DT - shift(days=1)},
     ],
     'over_limit': False,
     'active_after_stats': True,
     'close_month': True},

    # Test-case 1
    {'description': u'equal to limit',
     'base_dt': BASE_DT - shift(days=1),
     'stats': [
         {'completions': {'shift_limit': 0}, 'dt': BASE_DT - shift(days=1)},
     ],
     'over_limit': False,
     'active_after_stats': True,
     'close_month': True},

    # Test-case 2
    {'description': u'limit + 1',
     'base_dt': BASE_DT - shift(days=1),
     'stats': [
         {'completions': {'shift_limit': 1}, 'dt': BASE_DT - shift(days=1)},

     ],
     'over_limit': True,
     'active_after_stats': True,
     'close_month': True},

    # Test-case 3
    {'description': u'0',
     'base_dt': BASE_DT - shift(days=1),
     'stats': [
         {'completions': {'shift_limit': 'to_zero'}, 'dt': BASE_DT - shift(days=1)},
     ],
     'over_limit': False,
     'active_after_stats': True},
]


@pytest.mark.parametrize(
    'tariff',
    get_parameters(mapkit_tariffs_call + mapkit_tariffs_device,)
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