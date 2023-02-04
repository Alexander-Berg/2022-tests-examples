# -*- coding: utf-8 -*-

from collections import namedtuple
from copy import deepcopy
from datetime import datetime, timedelta as shift

import pytest

from apikeys.apikeys_utils import get_parameters, trunc_date
import apikeys.tests_by_typical_flows.typical_flows as flow
from btestlib import utils
from btestlib.utils import aDict

__author__ = 'kostya-karpus'

BASE_DT = trunc_date(datetime.utcnow().replace(hour=5),'hour')
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)

Free_limits = namedtuple('Free_limits', ['counters', 'limit', 'need_approve'])

Tariff = namedtuple('Tariff',
                    ['name', 'service_id', 'counters', 'year_price', 'day_limit',
                     'price_over_limit_per_1000', 'free_counters', 'ban'])

expected_invoices_on_day = namedtuple('expected_invoices_on_day', ['shift_days', 'invoices_amount'])

ROUT_SRV = 'routingmatrix'
routing_tariffs = [
    pytest.mark.smoke(Tariff('apikeys_routingmatrix_1000_yearprepay_plus_2017',
                             ROUT_SRV, ['total'], 620000, 1000, 120, None, False)),
    Tariff('apikeys_routingmatrix_10000_yearprepay_plus_2017', ROUT_SRV, ['total'], 860000, 10000, 36, None, False),
    Tariff('apikeys_routingmatrix_25000_yearprepay_plus_2017', ROUT_SRV, ['total'], 1100000, 25000, 24, None, False),
    Tariff('apikeys_routingmatrix_50000_yearprepay_plus_2017', ROUT_SRV, ['total'], 1350000, 50000, 17, None, False),
    Tariff('apikeys_routingmatrix_100000_yearprepay_plus_2017', ROUT_SRV, ['total'], 1500000, 100000, 11, None, False),
    Tariff('apikeys_routingmatrix_200000_yearprepay_plus_2017', ROUT_SRV, ['total'], 1896000, 200000, 9.5, None, False),
    Tariff('apikeys_routingmatrix_300000_yearprepay_plus_2017', ROUT_SRV, ['total'], 2292000, 300000, 8, None, False),
    Tariff('apikeys_routingmatrix_500000_yearprepay_plus_2017', ROUT_SRV, ['total'], 3084000, 500000, 5.20, None, False),
    Tariff('apikeys_routingmatrix_1000000_yearprepay_plus_2017', ROUT_SRV, ['total'], 5064000, 1000000, 4.50, None, False),
    Tariff('apikeys_routingmatrix_1500000_yearprepay_plus_2017', ROUT_SRV, ['total'], 7044000, 1500000, 4.40, None, False),
    Tariff('apikeys_routingmatrix_2000000_yearprepay_plus_2017', ROUT_SRV, ['total'], 9024000, 2000000, 4.30, None, False),
    Tariff('apikeys_routingmatrix_3000000_yearprepay_plus_2017', ROUT_SRV, ['total'], 12984000, 3000000, 4.20, None, False),
    Tariff('apikeys_routingmatrix_4000000_yearprepay_plus_2017', ROUT_SRV, ['total'], 16944000, 4000000, 4.10, None, False),
    Tariff('apikeys_routingmatrix_5000000_yearprepay_plus_2017', ROUT_SRV, ['total'], 20904000, 5000000, 4, None, False),
    pytest.mark.smoke(Tariff('apikeys_routingmatrix_1000_yearprepay_minus_2017', ROUT_SRV,
                             ['total'], 120000, 1000, 120, None, False)),
    Tariff('apikeys_routingmatrix_10000_yearprepay_minus_2017', ROUT_SRV, ['total'], 360000, 10000, 36, None, False),
    Tariff('apikeys_routingmatrix_25000_yearprepay_minus_2017', ROUT_SRV, ['total'], 600000, 25000, 24, None, False),
    Tariff('apikeys_routingmatrix_50000_yearprepay_minus_2017', ROUT_SRV, ['total'], 850000, 50000, 17, None, False),
    Tariff('apikeys_routingmatrix_100000_yearprepay_minus_2017', ROUT_SRV, ['total'], 1000000, 100000, 11, None, False),
    Tariff('apikeys_routingmatrix_200000_yearprepay_minus_2017', ROUT_SRV, ['total'], 1396000, 200000, 9.5, None, False),
    Tariff('apikeys_routingmatrix_300000_yearprepay_minus_2017', ROUT_SRV, ['total'], 1792000, 300000, 8, None, False),
    Tariff('apikeys_routingmatrix_500000_yearprepay_minus_2017', ROUT_SRV, ['total'], 2584000, 500000, 5.20, None, False),
    Tariff('apikeys_routingmatrix_1000000_yearprepay_minus_2017', ROUT_SRV, ['total'], 4564000, 1000000, 4.50, None, False),
    Tariff('apikeys_routingmatrix_1500000_yearprepay_minus_2017', ROUT_SRV, ['total'], 6544000, 1500000, 4.40, None, False),
    Tariff('apikeys_routingmatrix_2000000_yearprepay_minus_2017', ROUT_SRV, ['total'], 8524000, 2000000, 4.30, None, False),
    Tariff('apikeys_routingmatrix_3000000_yearprepay_minus_2017', ROUT_SRV, ['total'], 12484000, 3000000, 4.20, None, False),
    Tariff('apikeys_routingmatrix_4000000_yearprepay_minus_2017', ROUT_SRV, ['total'], 16444000, 4000000, 4.10, None, False),
    Tariff('apikeys_routingmatrix_5000000_yearprepay_minus_2017', ROUT_SRV, ['total'], 20404000, 5000000, 4, None, False),
]

scenarios = [
    # Test-case 0
    {'description': u'limit - 1',
     'base_dt': BASE_DT - shift(days=6),
     'stats': [
         {'completions': {'shift_limit': -1}, 'dt': BASE_DT - shift(days=6)},
     ],
     'over_limit': False,
     'close_month': True},

    # Test-case 1
    {'description': u'equal to limit',
     'base_dt': BASE_DT - shift(days=6),
     'stats': [
         {'completions': {'shift_limit': 0}, 'dt': BASE_DT - shift(days=6)},
         {'completions': {'shift_limit': 0}, 'dt': BASE_DT - shift(days=5)},
     ],
     'over_limit': False,
     'close_month': True},

    # Test-case 2
    {'description': u'limit + 1',
     'base_dt': BASE_DT - shift(days=5),
     'stats': [
         {'completions': {'shift_limit': 1}, 'dt': BASE_DT - shift(days=5)},
     ],
     'over_limit': True,
     'close_month': True},

    # Test-case 3
    {'description': u'0',
     'base_dt': BASE_DT - shift(days=2),
     'stats': [
         {'completions': {'shift_limit': 'to_zero'}, 'dt': BASE_DT - shift(days=2)},
     ],
     'over_limit': False},

    # Test-case 4
    {'description': u'change month',
     'base_dt': START_PREVIOUS_MONTH,
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
     'base_dt': START_PREVIOUS_MONTH,
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
     'base_dt': START_PREVIOUS_MONTH,
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
    get_parameters(routing_tariffs,)
    # Для выбора отдельных тарифов нужно написать индексы чрез запятую 0, 7
    , ids=lambda x: '{}-{}'.format(x.service_id, x.name.replace('.', '_')))
@pytest.mark.parametrize(
    'scenario', get_parameters(scenarios,)
    # Для выбора отдельных сценариев нужно написать индексы чрез запятую 0, 2
    , ids=lambda x: x.get('description'))
@pytest.mark.good
def test_paid(scenario, tariff, db_connection):
    scenario_copy = aDict(deepcopy(scenario))
    scenario_copy.tariff = tariff.name
    flow.PostpaymentWithPrepaymentPeriod.basic(scenario_copy, db_connection, tariff.service_id, tariff)
