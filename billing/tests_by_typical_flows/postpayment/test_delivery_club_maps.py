# coding: utf-8

from datetime import datetime, timedelta as shift
from collections import namedtuple
from decimal import Decimal as D
import pytest

from apikeys.tests_by_typical_flows import plain_function
from apikeys.tests_by_typical_flows import typical_flows as flow
from apikeys.apikeys_utils import get_parameters,trunc_date
from btestlib import utils
from btestlib.utils import aDict

__author__ = 'kostya-karpus'

Tariff = namedtuple('Tariff',
                    ['name', 'service_id', 'price', 'min_quantum', 'free_call'])

delivery_club_tariff = [Tariff('apikeys_apimaps_100000_noban_noprepay', 'apimaps', D('11'), 0, 100000)]

CALCULATORS = {'postpay': plain_function.calculate_expected_postpay,
               'postpay_two_months': plain_function.calculate_expected_postpay_two_months}

BASE_DT = trunc_date(datetime.utcnow().replace(hour=5),'hour')
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)

scenarios = [

    # Test-case 0
    {'description': u'Control test: [>, >]',
     'base_dt': BASE_DT,
     'signed_date': START_CURRENT_MONTH,
     'stats': [{'completions': {'total': 100001},
                'dt': BASE_DT - shift(days=2)},
               {'completions': {'total': 100001},
                'dt': BASE_DT - shift(days=1)}
               ],
     'expected': 2},

    # Test-case 1
    {'description': u'Control test: [>, <, >]',
     'base_dt': BASE_DT,
     'signed_date': START_CURRENT_MONTH,
     'stats': [{'completions': {'total': 111111},
                'dt': BASE_DT - shift(days=3)},
               {'completions': {'total': 99999},
                'dt': BASE_DT - shift(days=2)},
               {'completions': {'total': 100001},
                'dt': BASE_DT - shift(days=1)}],
     'expected': 13,
     'close_month': True},

    # Test-case 2
    {'description': u'Control test: [100000]',
     'base_dt': BASE_DT,
     'signed_date': START_CURRENT_MONTH,
     'stats': [{'completions': {'total': 100000},
                'dt': BASE_DT - shift(days=1)}],
     'expected': 0},

    # Test-case 3
    {'description': u'Control test: [>, _, >]',
     'base_dt': BASE_DT,
     'signed_date': START_CURRENT_MONTH,
     'stats': [{'completions': {'total': 100001},
                'dt': BASE_DT - shift(days=3)},
               {'completions': {'total': 101000},
                'dt': BASE_DT - shift(days=1)}],
     'expected': 2},

    # Test-case 4
    {'description': u'<, >',
     'base_dt': BASE_DT,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'total': 5000},
          'dt': END_PREVIOUS_MONTH},
         {'completions': {'total': 0}, 'dt': START_CURRENT_MONTH},
         {'completions': {'total': 1000000},
          'dt': START_CURRENT_MONTH + shift(hours=2)},
         {'completions': {'total': 0},
          'dt': START_CURRENT_MONTH + shift(hours=4)},
     ],
     'expected': 900},

    # Test-case 5
    {'description': u'>, <',
     'base_dt': BASE_DT,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'total': 999999},
          'dt': END_PREVIOUS_MONTH},
         {'completions': {'total': 1000},
          'dt': START_CURRENT_MONTH},
         {'completions': {'total': 2000},
          'dt': START_CURRENT_MONTH + shift(hours=2)},
         {'completions': {'total': 1999},
          'dt': START_CURRENT_MONTH + shift(hours=4)},
     ],
     'expected': 900},

    # Test-case 6
    {'description': u'=, =',
     'base_dt': BASE_DT,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'total': 6000},
          'dt': END_PREVIOUS_MONTH},
         {'completions': {'total': 2000},
          'dt': START_CURRENT_MONTH},
         {'completions': {'total': 3000},
          'dt': START_CURRENT_MONTH + shift(hours=2)},
         {'completions': {'total': 1000},
          'dt': START_CURRENT_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months'
     },

    # Test-case 7
    {'description': u'0, 0',
     'base_dt': BASE_DT,
     'signed_date': START_PREVIOUS_MONTH,
     'stats': [
         {'completions': {'total': 0}, 'dt': END_PREVIOUS_MONTH},
         {'completions': {'total': 0}, 'dt': START_CURRENT_MONTH},
         {'completions': {'total': 0},
          'dt': START_CURRENT_MONTH + shift(hours=2)},
         {'completions': {'total': 0},
          'dt': START_CURRENT_MONTH + shift(hours=4)},
     ],
     'expected_calculator': 'postpay_two_months'
     },
]


@pytest.mark.parametrize(
    'tariff', get_parameters(delivery_club_tariff, ),
    ids=lambda x: '{}-{}'.format(x.service_id, x.name))
@pytest.mark.parametrize(
    'scenario', get_parameters(scenarios, )
    , ids=lambda x: x['description'])
@pytest.mark.good
@pytest.mark.smoke
def test_delivery_club(tariff, scenario, db_connection):
    current_scenario = aDict(scenario)
    service_id = tariff.service_id
    current_scenario.tariff = tariff.name
    if hasattr(current_scenario, 'expected_calculator'):
        calculator = CALCULATORS.get(current_scenario.expected_calculator)
        current_scenario.expected = calculator(current_scenario.stats, tariff)

    flow.Postpayment.basic(current_scenario, db_connection, service_id, tariff.price)
