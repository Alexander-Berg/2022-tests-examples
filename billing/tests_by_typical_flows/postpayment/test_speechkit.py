# coding: utf-8

from datetime import datetime, timedelta as shift
from collections import namedtuple
from decimal import Decimal as D
import pytest

from apikeys.apikeys_utils import trunc_date
from apikeys.tests_by_typical_flows import plain_function
from apikeys.tests_by_typical_flows import typical_flows as flow
from apikeys.apikeys_utils import get_parameters
from btestlib import utils
from btestlib.utils import aDict

__author__ = 'torvald'

Tariff = namedtuple('Tariff',
                    ['name', 'service_id', 'price', 'min_quantum', 'free_call'])

mobile_sdk_tariffs = [Tariff('apikeys_speechkitmobile_simple', 'speechkitmobile', D('0.4'), 0, 10000)]

CLOUD_SERVICE = 'speechkitcloud'

clouds_general_tariffs = [
    Tariff('apikeys_speechkitcloud_client_201702', CLOUD_SERVICE, D('0.2'), 1000, 0),
    Tariff('apikeys_speechkitcloud_partner_silver_201702', CLOUD_SERVICE, D('0.15'), 1000, 0),
    pytest.mark.smoke(Tariff('apikeys_speechkitcloud_partner_gold_201702', CLOUD_SERVICE, D('0.1'), 1000, 0)),
]

custom_tariffs = [
    pytest.mark.smoke(Tariff('apikeys_speechkitcloud_custom', CLOUD_SERVICE, D('0'), 1000, 0),)
]

clouds_ner_unit_tariffs = [
    Tariff('apikeys_speechkitcloud_client_201705', CLOUD_SERVICE, D('0.2'), 1000, 0),
    Tariff('apikeys_speechkitcloud_partner_silver_201705', CLOUD_SERVICE, D('0.15'), 1000, 0),
    pytest.mark.smoke(Tariff('apikeys_speechkitcloud_partner_gold_201705', CLOUD_SERVICE, D('0.1'), 1000, 0)),
]

CALCULATORS = {'postpay': plain_function.calculate_expected_postpay,
               'postpay_two_months': plain_function.calculate_expected_postpay_two_months}

BASE_DT = trunc_date(datetime.utcnow().replace(hour=5),'hour')
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)

general_tariffs_scenarios = [
    # Test-case 0
    {'description': u'[Null]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [],
     'expected_calculator': 'postpay'
     },

    # Test-case 1
    {'description': u'[0]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH}],
     'expected_calculator': 'postpay',
     },

    # Test-case 2
    {'description': u'[1, _]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 1, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 1},
          'dt': START_PREVIOUS_MONTH + shift(days=2)}],
     'expected_calculator': 'postpay'
     },

    # Test-case 3
    {'description': u'Control test: [999, _]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 999, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 1},
          'dt': START_PREVIOUS_MONTH + shift(days=2)}],
     'expected_calculator': 'postpay',
     'close_month': True,
     },

    # Test-case 4
    {'description': u'Control test: [1000, _]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 1000, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 1},
          'dt': START_PREVIOUS_MONTH + shift(days=3)}],
     'expected_calculator': 'postpay'
     },

    # Test-case 5
    {'description': u'Control test: [50k, 20k, 30k]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 50000, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 20000},
          'dt': START_PREVIOUS_MONTH + shift(days=2)},
         {'completions': {'voice_unit': 30000, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=3)}],
     'expected_calculator': 'postpay'
     },

    # Test-case 6
    {'description': u'2_months [999, 999]',
     'base_dt': START_PREVIOUS_MONTH - shift(days=1),
     'signed_date': START_PREVIOUS_MONTH - shift(days=1),
     'stats': [
         {'completions': {'voice_unit': 990, 'tts_unit': 9},
          'dt': START_PREVIOUS_MONTH - shift(days=1)},
         {'completions': {'voice_unit': 9, 'tts_unit': 990},
          'dt': START_PREVIOUS_MONTH}],
     'expected_calculator': 'postpay_two_months'
     },

    # Test-case 7
    {'description': u'2_months',
     'base_dt': START_PREVIOUS_MONTH - shift(days=2),
     'signed_date': START_PREVIOUS_MONTH - shift(days=2),
     'stats': [
         {'completions': {'voice_unit': 1000, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH - shift(days=2)},
         {'completions': {'voice_unit': 300, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH - shift(days=1)},
         {'completions': {'voice_unit': 600, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 10, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=1)},
         {'completions': {'voice_unit': 20, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=2)},
         {'completions': {'voice_unit': 30, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=3)},
     ],
     'expected_calculator': 'postpay_two_months',
     'close_month': True
     },

    # Test-case 8
    {'description': u'2_months [0, 1002]',
     'base_dt': START_PREVIOUS_MONTH - shift(days=3),
     'signed_date': START_PREVIOUS_MONTH - shift(days=3),
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH - shift(days=3)},
         {'completions': {'voice_unit': 1000, 'tts_unit': 1},
          'dt': START_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 0, 'tts_unit': 1},
          'dt': START_PREVIOUS_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months',
     'close_month': True
     },

    # Test-case 9
    {'description': u'2_months [1001, 0]',
     'base_dt': START_PREVIOUS_MONTH - shift(days=2),
     'signed_date': START_PREVIOUS_MONTH - shift(days=2),
     'stats': [
         {'completions': {'voice_unit': 1000, 'tts_unit': 1},
          'dt': START_PREVIOUS_MONTH - shift(days=2)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months'
     },

    # Test-case 10
    {'description': u'2_months [0, 0]',
     'base_dt': START_PREVIOUS_MONTH - shift(days=3),
     'signed_date': START_PREVIOUS_MONTH - shift(days=3),
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH - shift(days=3)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months'
     },

    # Test-case 11
    {'description': u'2_months control test: [50k, 30k]',
     'base_dt': START_PREVIOUS_MONTH - shift(days=20),
     'signed_date': START_PREVIOUS_MONTH - shift(days=20),
     'stats': [
         {'completions': {'voice_unit': 5000, 'tts_unit': 5000},
          'dt': START_PREVIOUS_MONTH - shift(days=20)},
         {'completions': {'voice_unit': 8000, 'tts_unit': 2000},
          'dt': START_PREVIOUS_MONTH - shift(days=15)},
         {'completions': {'voice_unit': 20000, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH - shift(days=10)},
         {'completions': {'voice_unit': 0, 'tts_unit': 10000},
          'dt': START_PREVIOUS_MONTH - shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 1000, 'tts_unit': 1000},
          'dt': START_PREVIOUS_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 1000, 'tts_unit': 2000},
          'dt': START_PREVIOUS_MONTH + shift(hours=4)},
         {'completions': {'voice_unit': 3000, 'tts_unit': 2000},
          'dt': START_PREVIOUS_MONTH + shift(hours=6)},
         {'completions': {'voice_unit': 10000, 'tts_unit': 10000},
          'dt': START_PREVIOUS_MONTH + shift(days=1)},
     ],
     'expected_calculator': 'postpay_two_months',
     'close_month': True},
]


@pytest.mark.parametrize('tariff', get_parameters(clouds_general_tariffs,)
    , ids=lambda x: '{}-{}'.format(x.service_id, x.name))
@pytest.mark.parametrize(
    'scenario', get_parameters(general_tariffs_scenarios,11)
    # Для выбора отдельных сценариев нужно написать индексы чрез запятую
    , ids=lambda x: x['description'])
@pytest.mark.good
def test_clouds_general(tariff, scenario, db_connection):
    current_scenario = aDict(scenario)
    service_id = tariff.service_id
    current_scenario.tariff = tariff.name
    if hasattr(current_scenario, 'expected_calculator'):
        calculator = CALCULATORS.get(current_scenario.expected_calculator)
        current_scenario.expected = calculator(current_scenario.stats, tariff)

    flow.Postpayment.basic(current_scenario, db_connection, service_id)


with_ner_unit_scenarios = [
    # Test-case 0
    {'description': u'[Null]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [],
     'expected_calculator': 'postpay'
     },

    # Test-case 1
    {'description': u'[0]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH}],
     'expected_calculator': 'postpay'
     },

    # Test-case 2
    {'description': u'[1, _]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 1},
          'dt': START_PREVIOUS_MONTH + shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 1, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=2)}],
     'expected_calculator': 'postpay'
     },
    # Test-case 3
    {'description': u'Control test: [999, 1]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 999},
          'dt': START_PREVIOUS_MONTH + shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 1, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=2)}],
     'expected_calculator': 'postpay'
     },

    # Test-case 4
    {'description': u'Control test: [1000, 1]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 1000},
          'dt': START_PREVIOUS_MONTH + shift(days=1)},
         {'completions': {'voice_unit': 1, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=2)}],
     'expected_calculator': 'postpay'
     },

    # Test-case 5
    {'description': u'Control test: [55k, 25k, 35k]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 50000, 'tts_unit': 0, 'ner_unit': 5000},
          'dt': START_PREVIOUS_MONTH + shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 5000, 'ner_unit': 20000},
          'dt': START_PREVIOUS_MONTH + shift(days=2)},
         {'completions': {'voice_unit': 2500, 'tts_unit': 2500,
                          'ner_unit': 30000},
          'dt': START_PREVIOUS_MONTH + shift(days=3)}],
     'expected_calculator': 'postpay'
     },

    # Test-case 6
    {'description': u'2_months [999, 999]',
     'base_dt': START_PREVIOUS_MONTH - shift(days=1),
     'signed_date': START_PREVIOUS_MONTH - shift(days=1),
     'stats': [
         {'completions': {'voice_unit': 900, 'tts_unit': 9, 'ner_unit': 90},
          'dt': START_PREVIOUS_MONTH - shift(days=1)},
         {'completions': {'voice_unit': 9, 'tts_unit': 90, 'ner_unit': 900},
          'dt': START_PREVIOUS_MONTH}],
     'expected_calculator': 'postpay_two_months'
     },

    # Test-case 7
    {'description': u'2_months [1001, 1002]',
     'base_dt': START_PREVIOUS_MONTH - shift(days=1),
     'signed_date': START_PREVIOUS_MONTH - shift(days=1),
     'stats': [
         {'completions': {'voice_unit': 1000, 'tts_unit': 0, 'ner_unit': 1},
          'dt': START_PREVIOUS_MONTH - shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 1, 'ner_unit': 1000},
          'dt': START_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 1},
          'dt': START_PREVIOUS_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months'
     },

    # Test-case 8
    {'description': u'2_months [0, 1002]',
     'base_dt': START_PREVIOUS_MONTH - shift(days=1),
     'signed_date': START_PREVIOUS_MONTH - shift(days=1),
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH - shift(days=1)},
         {'completions': {'voice_unit': 1000, 'tts_unit': 1, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 1},
          'dt': START_PREVIOUS_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months',
     'close_month': True
     },

    # Test-case 9
    {'description': u'2_months [1001, 0]',
     'base_dt': START_PREVIOUS_MONTH - shift(days=1),
     'signed_date': START_PREVIOUS_MONTH - shift(days=1),
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 1, 'ner_unit': 1000},
          'dt': START_PREVIOUS_MONTH - shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months'
     },

    # Test-case 10
    {'description': u'2_months [0, 0]',
     'base_dt': START_PREVIOUS_MONTH - shift(days=1),
     'signed_date': START_PREVIOUS_MONTH - shift(days=1),
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH - shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months'
     },

    # Test-case 11
    {'description': u'2_months control test: [50k, 30k]',
     'base_dt': START_PREVIOUS_MONTH - shift(days=20),
     'signed_date': START_PREVIOUS_MONTH - shift(days=20),
     'stats': [
         {'completions': {'voice_unit': 5000, 'tts_unit': 5000, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH - shift(days=20)},
         {'completions': {'voice_unit': 4000, 'tts_unit': 2000, 'ner_unit': 4000},
          'dt': START_PREVIOUS_MONTH - shift(days=10)},
         {'completions': {'voice_unit': 15000, 'tts_unit': 0, 'ner_unit': 5000},
          'dt': START_PREVIOUS_MONTH - shift(days=2)},
         {'completions': {'voice_unit': 0, 'tts_unit': 10000, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH - shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 0},
          'dt': START_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 1000, 'tts_unit': 0, 'ner_unit': 1000},
          'dt': START_PREVIOUS_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 500, 'tts_unit': 2000, 'ner_unit': 500},
          'dt': START_PREVIOUS_MONTH + shift(hours=4)},
         {'completions': {'voice_unit': 3000, 'tts_unit': 1000, 'ner_unit': 1000},
          'dt': START_PREVIOUS_MONTH + shift(hours=6)},
         {'completions': {'voice_unit': 5000, 'tts_unit': 10000, 'ner_unit': 5000},
          'dt': START_PREVIOUS_MONTH + shift(days=1)},
     ],
     'expected_calculator': 'postpay_two_months'
     },
]


@pytest.mark.parametrize('tariff', get_parameters(clouds_ner_unit_tariffs, )
    , ids=lambda x: '{}-{}'.format(x.service_id, x.name))
@pytest.mark.parametrize(
    'scenario', get_parameters(with_ner_unit_scenarios,)
    , ids=lambda x: x['description'])
def test_with_ner_unit(tariff, scenario, db_connection):
    current_scenario = aDict(scenario)
    service_id = tariff.service_id
    current_scenario.tariff = tariff.name
    if hasattr(current_scenario, 'expected_calculator'):
        calculator = CALCULATORS.get(current_scenario.expected_calculator)
        current_scenario.expected = calculator(current_scenario.stats, tariff)

    flow.Postpayment.basic(current_scenario, db_connection, service_id)


mobile_sdk_scenarios = [
    # Test-case 0
    {'description': u'Control test: [>, >]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [{'completions': {'voice_unit': 15204, 'tts_unit': 2000},
                'dt': BASE_DT - shift(days=2)},
               {'completions': {'voice_unit': 10018, 'tts_unit': 2500},
                'dt': BASE_DT - shift(days=1)}],
     'expected_calculator': 'postpay'},

    # Test-case 1
    {'description': u'Control test: [>, <, >]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [{'completions': {'voice_unit': 15204, 'tts_unit': 2000},
                'dt': BASE_DT - shift(days=3)},
               {'completions': {'voice_unit': 1100, 'tts_unit': 800},
                'dt': BASE_DT - shift(days=2)},
               {'completions': {'voice_unit': 2000, 'tts_unit': 11006},
                'dt': BASE_DT - shift(days=1)}],
     'expected_calculator': 'postpay'},

    # Test-case 2
    {'description': u'Control test: [10000]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [{'completions': {'voice_unit': 3333, 'tts_unit': 6667},
                'dt': BASE_DT - shift(days=1)}],
     'expected_calculator': 'postpay'},

    # Test-case 3
    {'description': u'Control test: [>;voice_unit=0]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [{'completions': {'voice_unit': 0, 'tts_unit': 10001},
                'dt': BASE_DT - shift(days=1)}],
     'expected_calculator': 'postpay'},

    # Test-case 4
    {'description': u'Control test: [>, _, >]',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [{'completions': {'voice_unit': 15204, 'tts_unit': 0},
                'dt': BASE_DT - shift(days=3)},
               {'completions': {'voice_unit': 10018, 'tts_unit': 2503},
                'dt': BASE_DT - shift(days=1)}],
     'expected_calculator': 'postpay'},

    # Test-case 5
    {'description': u'>, >',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 15204, 'tts_unit': 2000},
          'dt': END_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 10018, 'tts_unit': 2500},
          'dt': START_CURRENT_MONTH}],
     'expected_calculator': 'postpay_two_months'
     },

    # Test-case 6
    {'description': u'<, >',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 5000, 'tts_unit': 4999},
          'dt': END_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 0, 'tts_unit': 0}, 'dt': START_CURRENT_MONTH},
         {'completions': {'voice_unit': 4001, 'tts_unit': 6000},
          'dt': START_CURRENT_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_CURRENT_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months'
     },
    # Test-case 7
    {'description': u'>, <',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 5000, 'tts_unit': 5001},
          'dt': END_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 2000, 'tts_unit': 1000},
          'dt': START_CURRENT_MONTH},
         {'completions': {'voice_unit': 1000, 'tts_unit': 2000},
          'dt': START_CURRENT_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 2000, 'tts_unit': 1999},
          'dt': START_CURRENT_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months'
     },

    # Test-case 8
    {'description': u'=, =',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 4000, 'tts_unit': 6000},
          'dt': END_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 1000, 'tts_unit': 2000},
          'dt': START_CURRENT_MONTH},
         {'completions': {'voice_unit': 2000, 'tts_unit': 3000},
          'dt': START_CURRENT_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 1000, 'tts_unit': 1000},
          'dt': START_CURRENT_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months'
     },

    # Test-case 9
    {'description': u'0, 0',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0}, 'dt': END_PREVIOUS_MONTH},
         {'completions': {'voice_unit': 0, 'tts_unit': 0}, 'dt': START_CURRENT_MONTH},
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_CURRENT_MONTH + shift(hours=2)},
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_CURRENT_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months'
     },
]


@pytest.mark.skip('No tariffs')
@pytest.mark.parametrize('tariff', get_parameters(mobile_sdk_tariffs, )
    , ids=lambda x: '{}-{}'.format(x.service_id, x.name))
@pytest.mark.parametrize(
    'scenario', get_parameters(mobile_sdk_scenarios, )
    , ids=lambda x: x['description'])
def test_mobile_sdk(tariff, scenario, db_connection):
    current_scenario = aDict(scenario)
    service_id = tariff.service_id
    current_scenario.tariff = tariff.name
    if hasattr(current_scenario, 'expected_calculator'):
        calculator = CALCULATORS.get(current_scenario.expected_calculator)
        current_scenario.expected = calculator(current_scenario.stats, tariff)

    flow.Postpayment.basic(current_scenario, db_connection, service_id)


multikeys_scenarios = [
    # Test-case 0
    {'description': u'2 keys 2 days >',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         # key1
         {'completions': {'voice_unit': 0, 'tts_unit': 500}, 'dt': START_PREVIOUS_MONTH + shift(days=1),
          'key': 1},
         {'completions': {'voice_unit': 0, 'tts_unit': 1}, 'dt': START_PREVIOUS_MONTH + shift(days=2, hours=1),
          'key': 1},
         # key2
         {'completions': {'voice_unit': 1, 'tts_unit': 0}, 'dt': START_PREVIOUS_MONTH + shift(days=1, hours=1),
          'key': 1},
         {'completions': {'voice_unit': 500, 'tts_unit': 0}, 'dt': START_PREVIOUS_MONTH + shift(days=3),
          'key': 1},
     ],
     'expected_calculator': 'postpay',
     'close_month': True},

    # Test-case 1
    {'description': u'2 keys 2 days =',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         # key1
         {'completions': {'voice_unit': 499, 'tts_unit': 0}, 'dt': START_PREVIOUS_MONTH + shift(days=4, hours=1),
          'key': 1},
         {'completions': {'voice_unit': 0, 'tts_unit': 1}, 'dt':  START_PREVIOUS_MONTH + shift(days=5), 'key': 1},
         # key2
         {'completions': {'voice_unit': 0, 'tts_unit': 1}, 'dt':  START_PREVIOUS_MONTH + shift(days=4), 'key': 2},
         {'completions': {'voice_unit': 499, 'tts_unit': 0}, 'dt':  START_PREVIOUS_MONTH + shift(days=5, hours=1),
          'key': 2},
     ],
     'expected_calculator': 'postpay',
     'close_month': True},

    # Test-case 2
    {'description': u'2 keys 2 days  <',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         # key1
         {'completions': {'voice_unit': 248, 'tts_unit': 1},
          'dt': START_PREVIOUS_MONTH + shift(days=4), 'key': 1},
         {'completions': {'voice_unit': 248, 'tts_unit': 2},
          'dt': START_PREVIOUS_MONTH + shift(days=5, minutes=10), 'key': 1},
         # key2
         {'completions': {'voice_unit': 248, 'tts_unit': 2},
          'dt': START_PREVIOUS_MONTH + shift(days=4, minutes=10), 'key': 2},
         {'completions': {'voice_unit': 248, 'tts_unit': 2},
          'dt': START_PREVIOUS_MONTH + shift(days=5), 'key': 2},
     ],
     'expected_calculator': 'postpay'},

    # Test-case 3
    {'description': u'2 keys 0 0',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         # key1
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=4, minutes=10), 'key': 1},
         # key2
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=4), 'key': 2},
     ],
     'expected_calculator': 'postpay'},

    # Test-case 4
    {'description': u'2 keys > 0',
     'base_dt': START_PREVIOUS_MONTH,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         # key1
         {'completions': {'voice_unit': 500, 'tts_unit': 1},
          'dt': START_PREVIOUS_MONTH + shift(days=4), 'key': 1},
         # key2
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': START_PREVIOUS_MONTH + shift(days=4, minutes=10), 'key': 2},
     ],
     'expected_calculator': 'postpay'},
]


@pytest.mark.parametrize('tariff', get_parameters([clouds_general_tariffs[0]], )
    , ids=lambda x: '{}-{}'.format(x.service_id, x.name))
@pytest.mark.parametrize('scenario', get_parameters(multikeys_scenarios, )
    , ids=lambda x: x['description'])
@pytest.mark.good
def test_multikeys(tariff, scenario, db_connection):
    current_scenario = aDict(scenario)
    service_id = tariff.service_id
    current_scenario.tariff = tariff.name
    if hasattr(current_scenario, 'expected_calculator'):
        calculator = CALCULATORS.get(current_scenario.expected_calculator)
        current_scenario.expected = calculator(current_scenario.stats, tariff)

    flow.Postpayment.basic(current_scenario, db_connection, service_id)


faxed_to_signed_scenarios = [
    # Test-case 0
    {'description': u'2_months control test: [50k, 30k]',
     'faxed': {
         'base_dt': START_PREVIOUS_MONTH - shift(days=10),
         'is_faxed': START_PREVIOUS_MONTH - shift(days=10),
         'stats': [
             {'completions': {'voice_unit': 5000, 'tts_unit': 5000},
              'dt': START_PREVIOUS_MONTH - shift(days=10)},
             {'completions': {'voice_unit': 8000, 'tts_unit': 2000},
              'dt': START_PREVIOUS_MONTH - shift(days=5)},
             {'completions': {'voice_unit': 20000, 'tts_unit': 0},
              'dt': START_PREVIOUS_MONTH - shift(days=1)},
         ]},
     'signed': {
         'base_dt': START_PREVIOUS_MONTH + shift(days=21),
         'is_signed': START_PREVIOUS_MONTH + shift(days=21),
         'stats': [
             {'completions': {'voice_unit': 5000, 'tts_unit': 5000},
              'dt': START_PREVIOUS_MONTH + shift(days=21)},
             {'completions': {'voice_unit': 8000, 'tts_unit': 2000},
              'dt': START_PREVIOUS_MONTH + shift(days=22)},
             {'completions': {'voice_unit': 20000, 'tts_unit': 0},
              'dt': START_PREVIOUS_MONTH + shift(days=23)},
         ]},
     'expected_unit': 80000,
     'close_month': True
     },
]


@pytest.mark.parametrize('tariff', get_parameters(clouds_general_tariffs, )
    , ids=lambda x: '{}-{}'.format(x.service_id, x.name))
@pytest.mark.parametrize(
    'scenario', get_parameters(faxed_to_signed_scenarios, ), ids=lambda x: x['description'])
@pytest.mark.good
def test_faxed_to_signed(tariff, scenario, db_connection):
    current_scenario = aDict(scenario)
    service_id = tariff.service_id
    current_scenario.tariff = tariff.name
    current_scenario.expected = current_scenario.expected_unit * tariff.price
    flow.Postpayment.faxed_signed(current_scenario, db_connection, service_id)


trial_to_paid_tariffs = [
    # speechkitcloud
    aDict({
        'service_cc': 'speechkitcloud',
        'trial_tariff': 'apikeys_speechkitcloud_trial',
        'paid_tariff': 'apikeys_speechkitcloud_client_201702'
    })
]

trial_to_paid_scenarios = [
    # Test-case 0

    {'description': u'not locked trial',
     'trial': aDict({
         'base_dt': START_PREVIOUS_MONTH - shift(days=29),
         'signed_date': START_PREVIOUS_MONTH - shift(days=29),
         'stats': [
             {'completions': {'voice_unit': 1000, 'tts_unit': 0},
              'dt': START_PREVIOUS_MONTH - shift(days=29)},
             {'completions': {'voice_unit': 300, 'tts_unit': 0},
              'dt': START_PREVIOUS_MONTH - shift(days=15)},
             {'completions': {'voice_unit': 600, 'tts_unit': 0},
              'dt': START_PREVIOUS_MONTH}],
         'active_after_stats': True}),
     'paid': aDict({
         'base_dt': START_PREVIOUS_MONTH,
         'signed_date': START_PREVIOUS_MONTH,
         'stats': [
             {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 1000},
              'dt': START_PREVIOUS_MONTH + shift(days=1)},
             {'completions': {'voice_unit': 1, 'tts_unit': 0, 'ner_unit': 0},
              'dt': START_PREVIOUS_MONTH + shift(days=2)}],
         'expected': 200}),
     'close_month': True,
     },

    # Test-case 1

    {'description': u'locked trial',
     'trial': aDict({
         'base_dt': START_PREVIOUS_MONTH - shift(days=30),
         'signed_date': START_PREVIOUS_MONTH - shift(days=30),
         'stats': [
             {'completions': {'voice_unit': 1000, 'tts_unit': 0},
              'dt': START_PREVIOUS_MONTH - shift(days=30)},
             {'completions': {'voice_unit': 300, 'tts_unit': 0},
              'dt': START_PREVIOUS_MONTH - shift(days=15)},
             {'completions': {'voice_unit': 600, 'tts_unit': 0},
              'dt': START_PREVIOUS_MONTH}],
         'active_after_stats': False}),
     'paid': aDict({
         'base_dt': START_PREVIOUS_MONTH,
         'signed_date': START_PREVIOUS_MONTH,
         'stats': [
             {'completions': {'voice_unit': 0, 'tts_unit': 0, 'ner_unit': 1000},
              'dt': START_PREVIOUS_MONTH + shift(days=1)},
             {'completions': {'voice_unit': 1, 'tts_unit': 0, 'ner_unit': 0},
              'dt': START_PREVIOUS_MONTH + shift(days=2)}],
         'expected': 200}),
     'close_month': True,
     },
]


@pytest.mark.parametrize('tariff', get_parameters(trial_to_paid_tariffs, )
    , ids=lambda x: x.service_cc)
@pytest.mark.parametrize('scenario', get_parameters(trial_to_paid_scenarios, )
    , ids=lambda x: x['description'])
@pytest.mark.good
def test_trial_to_paid(scenario, tariff, free_passport):
    current_scenario = aDict(scenario)
    current_scenario.trial.service_cc = tariff.service_cc
    current_scenario.trial.tariff = tariff.trial_tariff
    current_scenario.paid.service_cc = tariff.service_cc
    current_scenario.paid.tariff = tariff.paid_tariff
    flow.Postpayment.trial_to_paid(current_scenario, free_passport)


custom_scenarios = [

    # Test-case 0
    {'description': u'1 day',
     'base_dt': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': BASE_DT}],
     'active_after_stats': True
     },

    # Test-case 1

    {'description': u'30 days',
     'base_dt': BASE_DT - shift(days=29),
     'stats': [
         {'completions': {'voice_unit': 1000, 'tts_unit': 0},
          'dt': BASE_DT - shift(days=29)},
         {'completions': {'voice_unit': 300, 'tts_unit': 0},
          'dt': BASE_DT - shift(days=15)},
         {'completions': {'voice_unit': 600, 'tts_unit': 0},
          'dt': BASE_DT}],
     'active_after_stats': True
     },

    # Test-case 2

    {'description': u'31 days',
     'base_dt': BASE_DT - shift(days=31),
     'stats': [
         {'completions': {'voice_unit': 1000, 'tts_unit': 0},
          'dt': BASE_DT - shift(days=31)},
         {'completions': {'voice_unit': 300, 'tts_unit': 0},
          'dt': BASE_DT - shift(days=15)},
         {'completions': {'voice_unit': 600, 'tts_unit': 0},
          'dt': BASE_DT}],
     'active_after_stats': True
     },

    # Test-case 3

    {'description': u'32 days',
     'base_dt': BASE_DT - shift(days=32),
     'stats': [
         {'completions': {'voice_unit': 1000, 'tts_unit': 0},
          'dt': BASE_DT - shift(days=32)},
         {'completions': {'voice_unit': 300, 'tts_unit': 0},
          'dt': BASE_DT - shift(days=15)},
         {'completions': {'voice_unit': 600, 'tts_unit': 0},
          'dt': BASE_DT - shift(days=2)},
         {'completions': {'voice_unit': 600, 'tts_unit': 0},
          'dt': BASE_DT - shift(days=1)},
         {'completions': {'voice_unit': 600, 'tts_unit': 0},
          'dt': BASE_DT},
     ],

     'active_after_stats': True
     },

    # Test-case 4

    {'description': u'999 + 1',
     'base_dt': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 999, 'tts_unit': 1},
          'dt': BASE_DT},
     ],

     'active_after_stats': True
     },

    # Test-case 5

    {'description': u'[1 + 1000]',
     'base_dt': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 1, 'tts_unit': 1000},
          'dt': BASE_DT},
     ],

     'active_after_stats': False
     },

    # Test-case 6

    {'description': u'[1000 + 1]',
     'base_dt': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 1000, 'tts_unit': 1},
          'dt': BASE_DT},
     ],

     'active_after_stats': False
     },

    # Test-case 7

    {'description': u'[1001 + 0]',
     'base_dt': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 1001, 'tts_unit': 0},
          'dt': BASE_DT},
     ],

     'active_after_stats': False
     },

    # Test-case 8

    {'description': u'[0 + 1001]',
     'base_dt': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 1001},
          'dt': BASE_DT},
     ],

     'active_after_stats': False
     },

    # Test-case 9

    {'description': u'[>,  <]',
     'base_dt': BASE_DT - shift(days=1),
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 1001},
          'dt': BASE_DT - shift(days=1)},
         {'completions': {'voice_unit': 0, 'tts_unit': 50},
          'dt': BASE_DT},
     ],

     'active_after_stats': True
     },
]


@pytest.mark.parametrize('tariff', get_parameters(custom_tariffs, )
    , ids=lambda x: '{}-{}'.format(x.service_id, x.name))
@pytest.mark.parametrize(
    'scenario', get_parameters(custom_scenarios, )
    # Для выбора отдельных сценариев нужно написать индексы чрез запятую
    , ids=lambda x: x['description'])
@pytest.mark.good
def test_custom(tariff, scenario, db_connection):
    current_scenario = aDict(scenario)
    service_id = tariff.service_id
    current_scenario.tariff = tariff.name
    flow.Postpayment.custom(current_scenario, db_connection, service_id)
