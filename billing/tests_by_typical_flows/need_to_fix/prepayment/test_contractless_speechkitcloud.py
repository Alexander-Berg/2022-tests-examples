# -*- coding: utf-8 -*-

from collections import namedtuple
from copy import deepcopy
from datetime import datetime, timedelta as shift

import pytest

from apikeys.tests_by_typical_flows import typical_flows as flow
from apikeys.apikeys_utils import get_parameters, Person_type
from btestlib import utils
from btestlib.utils import aDict

BASE_DT = datetime.utcnow()
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)

Tariff = namedtuple('Tariff', ['name', 'service_id', 'quantificator', 'price', 'shift_limit', 'shift_target'])
SPEECHKITCLOUD_SERVICE_CC = 'testspeechkitcloud'

GOLD = Tariff('apikeys_testspeechkitcloud_gold_contractless_2018', SPEECHKITCLOUD_SERVICE_CC, 1000, 100, None, None)
SILVER = Tariff('apikeys_testspeechkitcloud_silver_contractless_2018', SPEECHKITCLOUD_SERVICE_CC, 1000, 150, 6000000,
                GOLD)
BRONZE = Tariff('apikeys_testspeechkitcloud_bronze_contractless_2018', SPEECHKITCLOUD_SERVICE_CC, 1000, 200, 2500000,
                SILVER)

'''тест спичкитклауд:
1. Сильвер и голд не должны иметь кнопку перейти?
2. Нет шапки на UI'''

ur_speechkitcloud = Person_type('ur', 11101003, 1003, True)
ph_speechkitcloud = Person_type('ph', 11101001, 1001, True)

tariffs_for_test = [
    BRONZE,
]

general_scenarios = [
    # Test-case 0
    {'description': u'[quantificator - 1]'.encode('utf-8'),
     'base_dt': BASE_DT - shift(days=6),
     'stats': [
         {'completions': {'voice_unit': 999}, 'dt': BASE_DT - shift(days=6)},
     ],
     'active_after_scenario': True,
     'prepay_sum_multiplier': 1},

    # Test-case 1
    {'description': u'[equal to quantificator]'.encode('utf-8'),
     'base_dt': BASE_DT - shift(days=6),
     'stats': [
         {'completions': {'tts_unit': 1000}, 'dt': BASE_DT - shift(days=6)},
     ],
     'active_after_scenario': True,
     'prepay_sum_multiplier': 1},

    # Test-case 2
    {'description': u'[quantificator + 1]'.encode('utf-8'),
     'base_dt': BASE_DT - shift(days=6),
     'stats': [
         {'completions': {'ner_unit': 1001}, 'dt': BASE_DT - shift(days=6)},
     ],
     'active_after_scenario': True,
     'prepay_sum_multiplier': 2},

    # Test-case 3
    {'description': u'[quantificator + 1, no funds]'.encode('utf-8'),
     'base_dt': BASE_DT - shift(days=6),
     'stats': [
         {'completions': {'ner_unit': 1000}, 'dt': BASE_DT - shift(days=6)},
         {'completions': {'tts_unit': 1}, 'dt': BASE_DT},
     ],
     'active_after_scenario': False,
     'prepay_sum_multiplier': 1},

    # Test-case 4
    {'description': u'triple quantificator'.encode('utf-8'),
     'base_dt': BASE_DT - shift(days=6),
     'stats': [
         {'completions': {'tts_unit': 1000}, 'dt': BASE_DT - shift(days=6)},
         {'completions': {'ner_unit': 1000}, 'dt': BASE_DT - shift(days=6)},
         {'completions': {'voice_unit': 999}, 'dt': BASE_DT - shift(days=6)},
     ],
     'active_after_scenario': True,
     'prepay_sum_multiplier': 3},

    # Test-case 5
    {'description': u'[multikeys over quantificator]'.encode('utf-8'),
     'base_dt': BASE_DT - shift(days=6),
     'stats': [
         {'completions': {'tts_unit': 499}, 'dt': START_PREVIOUS_MONTH - shift(days=6), 'key': 1},
         {'completions': {'ner_unit': 1}, 'dt': START_PREVIOUS_MONTH - shift(days=6), 'key': 2},
         {'completions': {'voice_unit': 501}, 'dt': START_PREVIOUS_MONTH - shift(days=6), 'key': 3},
     ],
     'active_after_scenario': True,
     'prepay_sum_multiplier': 2},

    # Test-case 6
    {'description': u'[multikeys in quantificator]'.encode('utf-8'),
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'tts_unit': 499}, 'dt': START_PREVIOUS_MONTH - shift(days=6), 'key': 1},
         {'completions': {'ner_unit': 1}, 'dt': START_PREVIOUS_MONTH - shift(days=6), 'key': 2},
         {'completions': {'voice_unit': 500}, 'dt': START_PREVIOUS_MONTH - shift(days=6), 'key': 3},
     ],
     'active_after_scenario': True,
     'prepay_sum_multiplier': 1},

]


@pytest.mark.parametrize('person_type', [ur_speechkitcloud], ids=lambda x: '[{}]'.format(x[0]))
@pytest.mark.parametrize('tariff', get_parameters(tariffs_for_test, ), ids=lambda x: x.name)
@pytest.mark.parametrize(
    'scenario', get_parameters(general_scenarios, )
    , ids=lambda x: x.description)
def test_tariff_general(scenario, tariff, person_type, free_passport):
    scenario_copy = aDict(deepcopy(scenario))
    scenario_copy.tariff = tariff.name
    flow.Prepayment.basic(scenario_copy, free_passport, tariff.service_id, tariff, person_type)


tariff_autochange_scenarios = [
    # Test-case 0
    {'description': u'bronse-silver-gold change 1 day'.encode('utf-8'),
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'tts_unit': BRONZE.shift_limit}, 'dt': BASE_DT - shift(days=6)},
         {'completions': {'ner_unit': SILVER.shift_limit}, 'dt': BASE_DT - shift(days=6)},
     ],
     'expected_days': 1,
     'prepay_sum_multiplier': (BRONZE.shift_limit + SILVER.shift_limit)/1000},

    # Test-case 1
    {'description': u'bronse-silver-gold change few days'.encode('utf-8'),
     'base_dt': BASE_DT,
     'stats': [
         {'completions': {'ner_unit': BRONZE.shift_limit}, 'dt': BASE_DT - shift(days=6)},
         {'completions': {'voice_unit': SILVER.shift_limit}, 'dt': BASE_DT - shift(days=5)},
     ],
     'active_after_scenario': True,
     'prepay_sum_multiplier': (BRONZE.shift_limit + SILVER.shift_limit)/1000},
]


@pytest.mark.parametrize('person_type', [ur_speechkitcloud], ids=lambda x: '[{}]'.format(x[0]))
@pytest.mark.parametrize('tariff', get_parameters(tariffs_for_test[0], ), ids=lambda x: x.name)
@pytest.mark.parametrize(
    'scenario', get_parameters(tariff_autochange_scenarios, )
    , ids=lambda x: x.description)
def test_tariff_autochange(scenario, tariff, person_type, free_passport):
    scenario_copy = scenario()
    scenario_copy.tariff = tariff.name
    flow.Prepayment.basic(scenario_copy, free_passport, tariff.service_id, tariff, person_type)
