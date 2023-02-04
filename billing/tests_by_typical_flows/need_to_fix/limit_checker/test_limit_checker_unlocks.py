# coding: utf-8

from datetime import datetime

import pytest

from apikeys.apikeys_utils import get_test_cases, list_wrapper, merge_dicts, mark_xfail
from apikeys.tests_by_typical_flows import plain_function
from apikeys.tests_by_typical_flows import typical_flows as flow
from btestlib.utils import aDict

__author__ = 'mednor'

ADMIN = 1120000000011035
APIKEYS_SERVICE_ID = 129
PAYSYS_ID = 1001

BASE_DT = datetime.utcnow().replace(hour=5, minute=0, second=0, microsecond=0)

SERVICE_LIMITS = \
    {
        u'ydfimoder': {u'hits': 10000},
        # u'city': {u'hits': 500},
        # u'apimaps': {u'geocoder_hits': 25000, u'router_hits': 25000},
        # u'speechkitmobile': {u'tts_unit': 10000, u'voice_unit': 10000},
        # u'speechkit': {u'tts_unit': 100, u'voice_unit': 100},
        # u'ydfiduplic': {u'hits': 10000},
        # u'speechkitcloud': {u'tts_unit': 100, u'voice_unit': 100}
    }
SERVICE_LIMITS_TEST = \
    {
        # 'testspeechkitcloud': {'tts_unit': 100, 'voice_unit': 100},
        # 'testapimaps': {'geocoder_hits': 25000, 'router_hits': 25000},
        # 'testspeechkitmobile': {u'tts_unit': 10000, u'voice_unit': 10000}
    }

XFAILS = \
    [
        {'contains': 'apimaps', 'not': 'testapimaps'}
    ]


@pytest.mark.skip(reason='need to update')
@pytest.mark.parametrize('test_case',
                         get_test_cases(
                             [
                                 plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                             ],
                             merge_dicts(
                                 SERVICE_LIMITS_TEST,
                                 SERVICE_LIMITS
                             )
                         )
    , ids=lambda x: '{}-{}:[{}]'.format(x['service'], x['func'], x['stats']).replace('datetime.datetime', 'datetime'))
def test_autolock_autounlock(test_case, free_passport):
    scenario = aDict({'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])}})
    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    mark_xfail(XFAILS, testname)
    flow.LimitChecker.autolock_autounlock(scenario, free_passport, test_case['service'], True)


@pytest.mark.skip(reason='need to update')
@pytest.mark.parametrize('test_case',
                         get_test_cases(
                             [
                                 plain_function.below_limit_in_each_unit_over_limit_in_sum,
                                 # plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 # plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 # plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                             ],
                             merge_dicts(
                                 SERVICE_LIMITS_TEST,
                                 SERVICE_LIMITS
                             )
                         )
    , ids=lambda x: '{}-{}:[{}]'.format(x['service'], x['func'], x['stats']).replace('datetime.datetime', 'datetime'))
def test_manuallock_autounlock(test_case, free_passport):
    scenario = aDict({'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])}})
    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    mark_xfail(XFAILS, testname)
    flow.LimitChecker.manuallock_autounlock(scenario, free_passport, test_case['service'], False)


@pytest.mark.skip(reason='need to update')
@pytest.mark.parametrize('test_case',
                         get_test_cases(
                             [
                                 #plain_function.below_limit_in_each_unit_over_limit_in_sum,
                                  plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 # plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 # plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                             ],
                             merge_dicts(
                                 SERVICE_LIMITS_TEST,
                                 SERVICE_LIMITS
                             )
                         )
    , ids=lambda x: '{}-{}:[{}]'.format(x['service'], x['func'], x['stats']).replace('datetime.datetime', 'datetime'))
def test_manuallock_manualunlock(test_case, free_passport):
    scenario = aDict({'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])}})
    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    mark_xfail(XFAILS, testname)
    flow.LimitChecker.manuallock_manualunlock(scenario, free_passport, test_case['service'], True)


@pytest.mark.skip(reason='need to update')
@pytest.mark.parametrize('test_case',
                         get_test_cases(
                             [
                                 #plain_function.below_limit_in_each_unit_over_limit_in_sum,
                                  plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 # plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 # plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                             ],
                             merge_dicts(
                                 SERVICE_LIMITS_TEST,
                                 SERVICE_LIMITS
                             )
                         )
    , ids=lambda x: '{}-{}:[{}]'.format(x['service'], x['func'], x['stats']).replace('datetime.datetime', 'datetime'))
def test_manuallock_update_to_autolock(test_case, free_passport):
    scenario = aDict({'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])}})
    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    mark_xfail(XFAILS, testname)
    flow.LimitChecker.manuallock_update_to_autolock(scenario, free_passport, test_case['service'], True)


@pytest.mark.skip(reason='need to update')
@pytest.mark.parametrize('test_case',
                         get_test_cases(
                             [
                                 #plain_function.below_limit_in_each_unit_over_limit_in_sum,
                                  plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 # plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 # plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                             ],
                             merge_dicts(
                                 SERVICE_LIMITS_TEST,
                                 SERVICE_LIMITS
                             )
                         )
    , ids=lambda x: '{}-{}:[{}]'.format(x['service'], x['func'], x['stats']).replace('datetime.datetime', 'datetime'))
def test_autolock_manualunlock(test_case, free_passport):
    scenario = aDict({'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])}})
    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    mark_xfail(XFAILS, testname)
    flow.LimitChecker.autolock_manualunlock(scenario, free_passport, test_case['service'], True)


@pytest.mark.skip(reason='need to update')
@pytest.mark.parametrize('test_case',
                         get_test_cases(
                             [
                                 #plain_function.below_limit_in_each_unit_over_limit_in_sum,
                                  plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 # plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 # plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                             ],
                             merge_dicts(
                                 SERVICE_LIMITS_TEST,
                                 SERVICE_LIMITS
                             )
                         )
    , ids=lambda x: '{}-{}:[{}]'.format(x['service'], x['func'], x['stats']).replace('datetime.datetime', 'datetime'))
def test_autolock_manuallock(test_case, free_passport):
    scenario = aDict({'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])}})
    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    mark_xfail(XFAILS, testname)
    flow.LimitChecker.autolock_manuallock(scenario, free_passport, test_case['service'], True)


@pytest.mark.skip(reason='need to update')
@pytest.mark.parametrize('test_case',
                         get_test_cases(
                             [
                                 plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                             ],
                             merge_dicts(
                                 SERVICE_LIMITS_TEST,
                                 SERVICE_LIMITS
                             )
                         )
    , ids=lambda x: '{}-{}:[{}]'.format(x['service'], x['func'], x['stats']).replace('datetime.datetime', 'datetime'))
def test_autolock_autounlock_unblockable(test_case, free_passport):
    scenario = aDict({'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])}})
    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    mark_xfail(XFAILS, testname)
    flow.LimitChecker.autolock_autounlock_unblockable(scenario, free_passport, test_case['service'], True)


@pytest.mark.skip(reason='need to update')
@pytest.mark.parametrize('test_case',
                         get_test_cases(
                             [
                                 plain_function.below_limit_in_each_unit_over_limit_in_sum,
                                 # plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 # plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 # plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                             ],
                             merge_dicts(
                                 SERVICE_LIMITS_TEST,
                                 SERVICE_LIMITS
                             )
                         )
    , ids=lambda x: '{}-{}:[{}]'.format(x['service'], x['func'], x['stats']).replace('datetime.datetime', 'datetime'))
def test_manuallock_autounlock_unblockable(test_case, free_passport):
    scenario = aDict({'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])}})
    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    mark_xfail(XFAILS, testname)
    flow.LimitChecker.manuallock_autounlock_unblockable(scenario, free_passport, test_case['service'], False)


@pytest.mark.skip(reason='need to update')
@pytest.mark.parametrize('test_case',
                         get_test_cases(
                             [
                                 plain_function.below_limit_in_each_unit_over_limit_in_sum,
                                 # plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 # plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 # plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                             ],
                             merge_dicts(
                                 SERVICE_LIMITS_TEST,
                                 SERVICE_LIMITS
                             )
                         )
    , ids=lambda x: '{}-{}:[{}]'.format(x['service'], x['func'], x['stats']).replace('datetime.datetime', 'datetime'))
def test_manuallock_unblockable_update_to_blockable(test_case, free_passport):
    scenario = aDict({'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])}})
    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    mark_xfail(XFAILS, testname)
    flow.LimitChecker.manuallock_unblockable_update_to_blockable(scenario, free_passport, test_case['service'], False)
