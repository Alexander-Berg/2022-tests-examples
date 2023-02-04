# -*- coding: utf-8 -*-

from collections import namedtuple
from copy import deepcopy
from datetime import datetime, timedelta as shift

import pytest
from apikeys.tests_by_typical_flows import plain_function
from btestlib.utils import aDict
from apikeys.apikeys_utils import get_parameters, Person_type
import apikeys.tests_by_typical_flows.typical_flows as flow
from btestlib import utils

__author__ = 'kostya-karpus'

BASE_DT = datetime.utcnow()
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)

ur_pogoda = Person_type('ur', 11101001, 1003, True)
ph_pogoda = Person_type('ph', 11101001, 1001, True)

Tariff = namedtuple('Tariff', ['name', 'service_id', 'month_price', 'month_limit', 'price_over_limit_per_1'])
POGODA_SERVICE_CC = 'pogoda'
CALCULATORS = {'total_overlimit': plain_function.calculate_total_expected_overlimit}
main_2018 = Tariff('apikeys_pogoda_main_2018', POGODA_SERVICE_CC, 150000, 2000000, 0.02)

main_scenarios = [
    # Test-case 0
    {'description': u'limit - 1',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'hits': 1999999}, 'dt': START_PREVIOUS_MONTH + shift(hours=10)},
     ],
     'over_limit': False,
     'active_after_scenario': True,
     'expected_calculator': 'total_overlimit',
     'close_month': True},

    # Test-case 1
    {'description': u'equal to limit',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'hits': 2000000}, 'dt': START_PREVIOUS_MONTH},
     ],
     'over_limit': False,
     'active_after_scenario': True,
     'expected_calculator': 'total_overlimit',
     'close_month': True},

    # Test-case 2
    {'description': u'limit +',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'hits': 2000000}, 'dt': START_PREVIOUS_MONTH},
         {'completions': {'hits': 1000}, 'dt': START_PREVIOUS_MONTH + shift(days=1)},
     ],
     'over_limit': True,
     'active_after_scenario': True,
     'expected_calculator': 'total_overlimit',
     'close_month': True},

    # Test-case 3
    {'description': u'30 days limits validity',
     'base_dt': START_PREVIOUS_MONTH - shift(days=14),
     'signed_date': START_PREVIOUS_MONTH - shift(days=14),
     'stats': [
         {'completions': {'hits': 2000000}, 'dt': START_PREVIOUS_MONTH - shift(days=14)},
         {'completions': {'hits': 1000}, 'dt': START_PREVIOUS_MONTH + shift(days=15)},
     ],
     'over_limit': True,
     'active_after_scenario': True,
     'expected_calculator': 'total_overlimit'},
]


@pytest.mark.parametrize('person_type', [ur_pogoda], ids=lambda x: '[{}]'.format(x[0]))
@pytest.mark.parametrize('tariff', get_parameters([main_2018], ), ids=lambda x: x.name)
@pytest.mark.parametrize(
    'scenario', get_parameters(main_scenarios,)
    , ids=lambda x: x['description'])
def test_tariff_main(scenario, tariff, person_type, free_passport):
    scenario_copy = aDict(deepcopy(scenario))
    scenario_copy.tariff = tariff.name
    if hasattr(scenario_copy, 'expected_calculator'):
        calculator = CALCULATORS.get(scenario_copy.expected_calculator)
        scenario_copy.expected = calculator(scenario_copy.stats, tariff)
    flow.Prepayment.contract(scenario_copy, free_passport, tariff.service_id, tariff, person_type)
