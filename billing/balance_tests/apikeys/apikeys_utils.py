# coding: utf-8
import calendar
import datetime
import itertools
import operator
import re
from collections import namedtuple
from decimal import Decimal as D, ROUND_HALF_UP, ROUND_FLOOR

import pytest

import apikeys_steps
from apikeys.apikeys_defaults import APIKEYS_LOGIN_POOL as USERS
from apikeys_api import UI, TEST, API
from balance import balance_db as db
from btestlib import utils

__author__ = 'torvald'

LIMIT_CHECKER_MEMO_PATTERN = re.compile('\[LIMIT\] .*')

Person_type = namedtuple('Person_type', ['type', 'paysys_ls', 'paysys_invoice', 'nds'])


class TaskerErrorException(Exception):
    pass
    # data = None
    #
    # def __init__(self, data):
    #     self.data = data
    #
    # def __repr__(self):
    #     print 'TaskerErrorException: {}'.format(self.data)


class Verifier():
    @staticmethod
    def assert_order_status(client_id):
        orders = db.get_order_by_client(client_id)
        assert len(orders) == 0

    @staticmethod
    def assert_limit_checker(result, expected_banned):
        assert result["result"] != expected_banned  # is banned status service response expected

    @staticmethod
    def assert_project(project_id, expected_banned):
        project_service_link = TEST().mongo_find('project_service_link', {"project_id": project_id})
        assert project_service_link[0]["config"]["banned"] == expected_banned  # is banned status valid in the db
        assert project_service_link[0]['config']['approved'] is True
        if project_service_link[0]["config"]["banned"] and \
                'ban_memo' in project_service_link[0]["config"].keys() and \
                LIMIT_CHECKER_MEMO_PATTERN.match(project_service_link[0]["config"]['ban_memo']):
            limit_config_list = apikeys_steps.get_limit_configs_by_service_id(project_service_link[0]['service_id'])
            assert long(project_service_link[0]['config']['ban_reason_id']) in [long(limit['lock_reason']) for limit in
                                                                                limit_config_list]

    @staticmethod
    def assert_check_key(expected_banned, key, token):
        try:
            API().check_key(token, key, '127.0.0.1')
        except:
            assert expected_banned == True
        else:
            assert expected_banned == False


# todo-architect в чем назначение модуля apikeys_utils? Контрольный вопрос - метод может быть использован только для apikeys?
def to_timestamp(dt):
    # return int((dt - datetime.datetime(1970, 1, 1)).total_seconds()*1000)
    return calendar.timegm(dt.timetuple()) * 1000


def from_timestamp(timestamp):
    return (timestamp / 1000)


def rounded_delta(n, ratio, quant):
    actual = ratio
    rounded = ratio.quantize(D('0.00'))

    delta_1 = actual - rounded
    actual_n = actual * n
    delta_n = actual_n - actual_n.quantize(D('0.00'), ROUND_FLOOR)
    if abs(delta_n) < abs(delta_1):
        return rounded + (quant * (delta_1 / abs(delta_1)))
    else:
        return rounded


def rounded_delta_billing(total_parts, prev_part_num, cur_part_num, total_value, quantum=D('0.01')):
    if total_parts == 0 and total_value == 0 or prev_part_num == cur_part_num:
        return 0
    return (D(total_value) * cur_part_num / total_parts).quantize(quantum, ROUND_HALF_UP) - \
           (D(total_value) * prev_part_num / total_parts).quantize(quantum, ROUND_HALF_UP)


def r_delta_sum(from_dt, till_dt):
    total_parts = calendar.monthrange(from_dt.year, from_dt.month)[1]
    prev_part_num = from_dt.day - 1
    cur_part_num = till_dt.day - 1 or total_parts
    return rounded_delta_billing(total_parts, prev_part_num, cur_part_num, D('30'), D('0.01'))


def activate_all_users():
    for user in USERS:
        user_id = user[0]
        UI.get_user_info(user_id)


def total_r_delta_sum(from_date, till_date):
    total = 0
    for (from_dt, till_dt) in utils.Date.date_period_cutter(from_date, till_date):
        total += r_delta_sum(from_dt, till_dt)
    return total


def join(sequence):
    return reduce(operator.add, sequence)


def get_test_cases(function_list, service_list):
    test_cases = join(
        [join(
            [
                [{'func': func.__name__, 'service': service, 'stats': result} for result in func(service_list[service])]
                for func in function_list
            ])
            for service in service_list
        ])
    return test_cases


def get_test_cases_for_paid(function_list, tariff_list):
    test_cases = join(
        [
            join(
                [
                    [{'func': func.__name__, 'service': service, 'stats': result, 'tariff': tariff} for result in
                     func(tariff_list[service][tariff])]
                    for func in function_list
                ]
            )
            for service, service_tariff_list in tariff_list.iteritems() for tariff in service_tariff_list
        ]
    )
    return test_cases


def list_wrapper(value):
    return value if isinstance(value, list) else [value]


def merge_dicts(dictionary, *args):
    merged_dict = dictionary.copy()
    for arg in args:
        merged_dict.update(arg)
    return merged_dict


def mark_xfail_regex(xfail_list, testname):
    for xfail_regex in xfail_list:
        if re.search(xfail_regex, testname) is not None:
            return pytest.xfail('xfail pattern')


def mark_xfail(xfail_list, testname):
    for xfail in xfail_list:
        if xfail['contains'] in testname and (xfail['not'] not in testname if len(xfail['not']) > 0 else True):
            return pytest.xfail('xfail pattern')


def mark_skip(skip_list, testname):
    for skip in skip_list:
        if skip['contains'] in testname and (skip['not'] not in testname if len(skip['not']) > 0 else True):
            return pytest.skip('skip pattern')


def get_deposit_money(scenario, tariff):
    if hasattr(scenario, 'prepaid_days'):
        return D(rounded_delta_billing(31, 0, scenario.prepaid_days, tariff.price)+1)
    elif hasattr(scenario, 'shift_money'):
        return D(rounded_delta_billing(31, 0, 1, tariff.price) +
                 D(scenario.shift_money)).quantize(D('0.01'), ROUND_HALF_UP)
    elif hasattr(scenario, 'prepaid_money'):
        return D(scenario.prepaid_money)
    elif hasattr(tariff, 'year_price'):
        return D(tariff.year_price)
    elif hasattr(scenario, 'prepay_sum_multiplier'):
        return D(tariff.price*scenario.prepay_sum_multiplier)
    else:
        return D(tariff.price)


def get_parameters(data_list, *select_by_index):
    if not select_by_index:
        return data_list
    else:
        selected_list = []
        for index in select_by_index:
            selected_list.append(data_list[index])
        return selected_list


def remove_key_service_config_without_project():
    for_remove = []

    apikeys_steps.clean_up(apikeys_steps.TRASH_UID)

    for project in [x['link_id'] for x in TEST.mongo_find('key_service_config')]:

        if not TEST.mongo_find('project', {'_id': project.split('_')[0]}):
            for_remove.append(project)

    for item in for_remove:
        TEST.mongo_remove('key_service_config', {'link_id': item})

def trunc_date(dt, component):
    fields = ['year', 'month', 'day', 'hour', 'minute', 'second', 'microsecond']
    if component not in fields[:-1]:
        raise ValueError("cannot make trunc_dt to component '%s'" % component)
    return dt.replace(**dict(zip(fields[fields.index(component) + 1:], itertools.cycle([0]))))


if __name__ == "__main__":
    from_dt = datetime.datetime(2016, 6, 25)
    till_dt = datetime.datetime(2016, 7, 1)
    a = utils.Date.date_period_cutter(from_dt, till_dt)
    r_delta_sum(from_dt, till_dt)
    print rounded_delta_billing(31, 0, 31, 150000)
    print rounded_delta_billing(31, 0, 9, 20000)
