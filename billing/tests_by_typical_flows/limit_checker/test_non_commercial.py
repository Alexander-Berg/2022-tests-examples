# -*- coding: utf-8 -*-

from collections import namedtuple
from copy import deepcopy
from datetime import datetime, timedelta as shift

import pytest

from apikeys.tests_by_typical_flows import typical_flows as flow, plain_function as plain
from apikeys.apikeys_utils import get_parameters, Person_type
from btestlib import utils
from btestlib.utils import aDict

__author__ = 'ilya_knysh'

BASE_DT = datetime.utcnow()
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)

Tariff = namedtuple('Tariff',
                    ['tariff_cc',  # Наименование Тарифа
                     'service_id', # Наименование Сервиса
                     'counters',   # Наименование Счетчика (для некоторых продуктов их несколько
                     'unit'        # Наименование Юнита
                     ])

non_commercial_tariffs = [

    Tariff('routingmatrix_custom', 'routingmatrix', ['cells'], 'routingmatrix_cells_daily'),
    Tariff('apimaps_custom', 'apimaps', ['total'], 'apimaps_total_daily'),
    Tariff('mapkit_custom', 'mapkit', ['total'], 'mapkit_total_daily'),
    Tariff('staticmaps_custom', 'staticmaps', ['hits'], 'staticmaps_hits_daily'),
    Tariff('city_custom', 'city', ['hits'], 'city_hits_daily'),
]

general_scenarios = [
    # Test-case 0
    {'description': u'[unlim]',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': 1000000}, 'dt': BASE_DT},
     ],
     'active_after_scenario': True,
     'limit': -1},

    # Test-case 1
    {'description': u'[10000 limit +]',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': 1}, 'dt': BASE_DT},
     ],
     'active_after_scenario': False,
     'limit': 10000},

    # Test-case 2
    {'description': u'[10000 limit equal]',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': 0}, 'dt': BASE_DT},
     ],
     'active_after_scenario': False,
     'limit': 10000},

    # Test-case 3
    {'description': u'[10000 limit -]',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': -1}, 'dt': BASE_DT},
     ],
     'active_after_scenario': True,
     'limit': 10000},

    # Test-case 4
    {'description': u'[multikeys 10000 limit +]',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': -4999}, 'dt': BASE_DT, 'key': 1},
         {'completions': {'shift_limit': -5000}, 'dt': BASE_DT, 'key': 2},
     ],
     'active_after_scenario': False,
     'limit': 10000},

    # Test-case 5
    {'description': u'[multikeys 10000 limit -]',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': -5001}, 'dt': BASE_DT, 'key': 1},
         {'completions': {'shift_limit': -5000}, 'dt': BASE_DT, 'key': 2},
     ],
     'active_after_scenario': True,
     'limit': 10000},

    # Test-case 6
    {'description': u'[limitless_duration]',
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'shift_limit': 1000000}, 'dt': BASE_DT},
     ],
     'active_after_scenario': True,
     'limit': -1,
     'validity_period': 367,
     }
]


@pytest.mark.parametrize('tariff', get_parameters(non_commercial_tariffs), ids=lambda x: x.tariff_cc)
@pytest.mark.parametrize(
    'scenario', get_parameters(general_scenarios, )
    , ids=lambda x: x['description'])
@pytest.mark.good
def test_non_commercial(scenario, tariff, free_passport):
    scenario_copy = aDict(deepcopy(scenario))
    scenario_copy.tariff = tariff.tariff_cc
    flow.LimitChecker.non_commercial(scenario_copy, free_passport, tariff.service_id, tariff)
