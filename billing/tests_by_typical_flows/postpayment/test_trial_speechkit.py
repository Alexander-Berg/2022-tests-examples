# coding: utf-8

from datetime import datetime, timedelta as shift
from collections import namedtuple
from decimal import Decimal as D
import pytest

from apikeys.tests_by_typical_flows import typical_flows as flow
from apikeys.apikeys_utils import get_parameters, trunc_date
from btestlib import utils
from btestlib.utils import aDict

__author__ = 'kostya-karpus'

Tariff = namedtuple('Tariff',
                    ['name', 'service_id', 'price', 'min_quantum', 'free_call'])

TEST_CLOUD_SERVICE = 'testspeechkitcloud'
CLOUD_SERVICE = 'speechkitcloud'
JSAPI_SERVICE = 'speechkitjsapi'

trial_tariffs = [
    Tariff('apikeys_speechkitcloud_trial', CLOUD_SERVICE, D('0'), 1000, 0),
    Tariff('apikeys_speechkitjsapi_trial', CLOUD_SERVICE, D('0'), 1000, 0),
]

BASE_DT = trunc_date(datetime.utcnow().replace(hour=5),'hour')
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)

trial_scenarios = [

    # Test-case 0
    {'description': u'1 day',
     'base_dt': BASE_DT,
     'signed_date': START_CURRENT_MONTH,
     'stats': [
         {'completions': {'voice_unit': 0, 'tts_unit': 0},
          'dt': BASE_DT}],
     'active_after_stats': True
     },

    # Test-case 1
    {'description': u'30 days',
     'base_dt': BASE_DT - shift(days=29),
     'signed_date': BASE_DT - shift(days=29),
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
     'signed_date': BASE_DT - shift(days=31),
     'stats': [
         {'completions': {'voice_unit': 1000, 'tts_unit': 0},
          'dt': BASE_DT - shift(days=31)},
         {'completions': {'voice_unit': 300, 'tts_unit': 0},
          'dt': BASE_DT - shift(days=15)},
         {'completions': {'voice_unit': 600, 'tts_unit': 0},
          'dt': BASE_DT}],
     'active_after_stats': False
     },

    # Test-case 3
    {'description': u'32 days',
     'base_dt': BASE_DT - shift(days=32),
     'signed_date': BASE_DT - shift(days=32),
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

     'active_after_stats': False
     },

]


@pytest.mark.parametrize('tariff', get_parameters(trial_tariffs, )
    , ids=lambda x: '{}-{}'.format(x.service_id, x.name))
@pytest.mark.parametrize(
    'scenario', get_parameters(trial_scenarios, )
    # Для выбора отдельных сценариев нужно написать индексы чрез запятую
    , ids=lambda x: x['description'])
@pytest.mark.good
@pytest.mark.smoke
def test_trial(tariff, scenario, db_connection):
    current_scenario = aDict(scenario)
    service_id = tariff.service_id
    current_scenario.tariff = tariff.name
    flow.Postpayment.trial(current_scenario, service_id,db_connection)
