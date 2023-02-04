# -*- coding: utf-8 -*-
import pytest

from apikeys import apikeys_utils, apikeys_defaults as defaults
from apikeys.apikeys_utils import get_test_cases_for_paid, list_wrapper, merge_dicts, mark_xfail
from apikeys.tests_by_typical_flows import plain_function
from apikeys.tests_by_typical_flows import typical_flows as flow

__author__ = 'mednor'



SERVICE_TARIFFS = \
    {
        'apimaps': {
            'apikeys_apimaps_1000_ban': {'geocoder_hits': 1000, 'router_hits': 1000},
            'apikeys_apimaps_25000_ban': {'geocoder_hits': 25000, 'router_hits': 25000},
            'apikeys_apimaps_500000_ban': {'geocoder_hits': 500000, 'router_hits': 500000},
        }
    }
SERVICE_TARIFFS_TEST = \
    {
        'testapimaps': {'apikeys_testapimaps_1000_ban': {'geocoder_hits': 1000, 'router_hits': 1000},
                        'apikeys_testapimaps_25000_ban': {'geocoder_hits': 25000, 'router_hits': 25000},
                        'apikeys_testapimaps_500000_ban': {'geocoder_hits': 500000, 'router_hits': 500000},
                        }
    }

XFAILS = \
    [
        # {'contains': 'test', 'not': ''}
    ]

pytestmark = [pytest.mark.docpath('https://wiki.yandex-team.ru/testirovanie/functesting/billing/apikeys')]


# @pytest.mark.skip(reason='need to update')
@pytest.mark.parametrize('test_case',
                         get_test_cases_for_paid(
                             [
                                 plain_function.null_in_each_unit,
                                 plain_function.below_limit_in_each_unit_over_limit_in_sum,
                                 plain_function.limit_minus_one_in_each_unit_PR_multi_processed_below_limit_in_others_PR,
                                 plain_function.limit_minus_one_in_every_unit_PR_below_limit_in_others_PR,
                                 plain_function.limit_minus_one_in_every_unit_P_zero_in_others_R,
                                 plain_function.limit_minus_one_in_every_unit_R_zero_in_others_P
                             ],
                             merge_dicts(SERVICE_TARIFFS,
                                         SERVICE_TARIFFS_TEST)
                         )
                         +
                         get_test_cases_for_paid(
                             [
                                 plain_function.null_in_each_unit,
                                 plain_function.below_limit_in_each_unit_over_limit_in_sum,
                                 plain_function.limit_minus_one_in_each_unit_PR_multi_processed_below_limit_in_others_PR,
                                 plain_function.limit_minus_one_in_every_unit_PR_below_limit_in_others_PR,
                                 plain_function.limit_minus_one_in_every_unit_P_zero_in_others_R,
                                 plain_function.limit_minus_one_in_every_unit_R_zero_in_others_P,
                                 plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                                 plain_function.limit_in_each_unit_PR_multi_processed_below_limit_in_others_PR
                             ],
                             {'apimaps': {
                                 'apikeys_apimaps_1000_noban': {'geocoder_hits': 1000, 'router_hits': 1000}}}
                         )

    , ids=lambda x: '{}-{}-{}:[{}]'.format(x['service'], x['tariff'], x['func'], x['stats']).replace(
        'datetime.datetime', 'datetime'))
def test_limit_check_in_limit(test_case, free_passport):
    scenario = {'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': defaults.BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])},
                'tariff': test_case['tariff']}
    # Days shipment calculation
    from_dt = min([row['dt'] for row in scenario['processed']['stats']])
    scenario['days'] = apikeys_utils.total_r_delta_sum(from_dt, scenario['processed']['base_dt'])

    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    mark_xfail(XFAILS, testname)

    flow.LimitChecker.paid(scenario, free_passport, test_case['service'], expected_banned=False)


@pytest.mark.skip(reason='need to update')
@pytest.mark.parametrize('test_case',
                         get_test_cases_for_paid(
                             [
                                 plain_function.equal_to_limit_in_every_unit_PR_below_limit_in_others_PR,
                                 plain_function.equal_to_limit_in_every_unit_P_zero_in_others_R,
                                 plain_function.equal_to_limit_in_every_unit_R_zero_in_others_P,
                                 plain_function.limit_in_each_unit_PR_multi_processed_below_limit_in_others_PR
                             ],
                             merge_dicts(SERVICE_TARIFFS,
                                         SERVICE_TARIFFS_TEST)
                         )
    , ids=lambda x: '{}-{}-{}:[{}]'.format(x['service'], x['tariff'], x['func'], x['stats']).replace(
        'datetime.datetime', 'datetime'))
def test_limit_check_above_limit(test_case, free_passport):
    scenario = {'raw': {'stats': list_wrapper({'completions': test_case['stats']['raw']})},
                'processed': {'base_dt': defaults.BASE_DT, 'stats': list_wrapper(test_case['stats']['processed'])},
                'tariff': test_case['tariff']}
    # Days shipment calculation
    from_dt = min([row['dt'] for row in scenario['processed']['stats']])
    scenario['days'] = apikeys_utils.total_r_delta_sum(from_dt, scenario['processed']['base_dt'])

    testname = '{}-{}:[{}]'.format(test_case['service'], test_case['func'], test_case['stats']).replace(
        'datetime.datetime', 'datetime')
    mark_xfail(XFAILS, testname)

    flow.LimitChecker.paid(scenario, free_passport, test_case['service'], expected_banned=True)
