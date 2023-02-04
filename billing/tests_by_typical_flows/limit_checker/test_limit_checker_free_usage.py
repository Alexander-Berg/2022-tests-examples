# coding: utf-8

import pytest

from apikeys import apikeys_defaults as defaults
from apikeys.apikeys_utils import get_test_cases, list_wrapper, merge_dicts, mark_xfail, mark_skip
from apikeys.tests_by_typical_flows import multikey_plain_function_modifier
from apikeys.tests_by_typical_flows import plain_function
from apikeys.tests_by_typical_flows import typical_flows as flow

__author__ = 'mednor'

KEYS = 2

SERVICE_LIMITS = \
    {
        u'market': {u'light_hits': 100, u'heavy_hits': 100},
        u'ydfimoder': {u'hits': 10000},
        u'city': {u'hits': 500},
        u'apimaps': {u'total': 1000},
        u'staticmaps': {u'hits': 1000},
        u'routingmatrix': {u'total': 10000},
        u'mapkit': {u'total': 1000},
        u'speechkitmobile': {u'tts_unit': 10000, u'voice_unit': 10000},
        u'speechkit': {u'tts_unit': 100, u'voice_unit': 100},
        u'ydfiduplic': {u'hits': 10000},
        u'speechkitcloud': {u'tts_unit': 100, u'voice_unit': 100},
        u'pogoda': {u'hits': 5000}
    }
SERVICE_LIMITS_TEST = \
    {
        # 'testspeechkitcloud_trial': {'tts_unit': 100, 'voice_unit': 100},
        # 'testspeechkitcloud': {'tts_unit': 100, 'voice_unit': 100},
        # 'testapimaps': {'geocoder_hits': 25000, 'router_hits': 25000},
        # 'testcity': {'hits': 500},
        # 'testmarket': {'light_hits': 100, 'heavy_hits': 100},
        # 'testspeechkitmobile': {u'tts_unit': 10000, u'voice_unit': 10000}
    }

XFAILS = \
    [
        # {'contains': 'apimaps', 'not': 'testapimaps'}
    ]

SKIP = \
    [
        {'contains': 'testmarket', 'not': 'apimaps'},
    ]

SKIP_ABOVE = \
    [
        {'contains': 'speechkitcloud', 'not': ''},
        {'contains': 'market', 'not': ''},
    ]


@pytest.fixture(scope='module')
def idfn_test_case(val):
    return '{}-{}:[{}]'.format(val['service'], val['func'], val['stats']).replace('datetime.datetime', 'datetime')


@pytest.fixture(scope='module')
def idfn_keys(val):
    return 'keys:[{}]'.format(val)


@pytest.fixture(scope='module')
def idfn_modifier(val):
    return '{}'.format(val.__doc__)


def get_scenario(keys, modifier, test_case):
    scenario = {'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': defaults.BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])}}
    scenario = modifier(scenario, keys)
    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    return scenario, testname


@pytest.mark.parametrize('test_case',
                         get_test_cases(
                             [
                                 plain_function.null_in_each_unit,
                                 plain_function.below_limit_in_each_unit_over_limit_in_sum,
                                 plain_function.limit_minus_one_in_each_unit_PR_multi_processed_below_limit_in_others_PR,
                                 plain_function.limit_minus_one_in_every_unit_PR_below_limit_in_others_PR,
                                 plain_function.limit_minus_one_in_every_unit_P_zero_in_others_R,
                                 plain_function.limit_minus_one_in_every_unit_R_zero_in_others_P
                             ],
                             merge_dicts(
                                 SERVICE_LIMITS_TEST,
                                 SERVICE_LIMITS
                             )
                         ),
                         ids=idfn_test_case)
@pytest.mark.parametrize('modifier',
                         [
                             multikey_plain_function_modifier.split_by_keys,
                             multikey_plain_function_modifier.zero_in_one_key,
                         ],
                         ids=idfn_modifier)
@pytest.mark.parametrize('keys', [1, 2], ids=idfn_keys)
@pytest.mark.good
def test_multikeys_limit_check_in_limit(test_case, modifier, keys, free_passport):
    scenario, testname = get_scenario(keys, modifier, test_case)
    mark_skip(SKIP, testname)
    flow.LimitChecker.multikeys_free_usage(scenario, free_passport, test_case['service'], False)


@pytest.mark.parametrize('test_case',
                         get_test_cases(
                             [
                                 plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                                 plain_function.limit_in_each_unit_PR_multi_processed_below_limit_in_others_PR
                             ],
                             merge_dicts(
                                 SERVICE_LIMITS_TEST,
                                 SERVICE_LIMITS
                             )
                         ),
                         ids=idfn_test_case)
@pytest.mark.parametrize('modifier',
                         [
                             multikey_plain_function_modifier.split_by_keys,
                             multikey_plain_function_modifier.zero_in_one_key,
                         ],
                         ids=idfn_modifier)
@pytest.mark.parametrize('keys', [1, 2], ids=idfn_keys)
@pytest.mark.good
def test_multikeys_limit_check_above_limit(test_case, modifier, keys, free_passport):
    scenario, testname = get_scenario(keys, modifier, test_case)
    mark_xfail(XFAILS, testname)
    mark_skip(SKIP_ABOVE, testname)
    flow.LimitChecker.multikeys_free_usage(scenario, free_passport, test_case['service'], True)
