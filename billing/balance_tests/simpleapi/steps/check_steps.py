# -*- coding: utf-8 -*-
import time
import urllib
from collections import Iterable

from hamcrest import is_not, is_, equal_to, contains_string, empty

import btestlib.reporter as reporter
from btestlib import environments
from btestlib import utils as butils
from simpleapi.common import logger
from simpleapi.steps import balance_test_steps as balance_test
from simpleapi.steps import db_steps

__author__ = 'fellow'

log = logger.get_logger()

'''
Модуль для базовых проверок
'''


def check_dicts_equals(actual, expected, compare_only=None, compare_expect_of=None):
    if not (isinstance(actual, dict) and isinstance(expected, dict)):
        log.debug('actual and expected must be of dict type')
    actual_filtered = actual.copy()
    expected_filtered = expected.copy()

    if compare_only:
        actual_filtered = {key: actual.get(key) for key in compare_only}
        expected_filtered = {key: expected.get(key) for key in compare_only}

    if compare_expect_of:
        for key in compare_expect_of:
            # dpath.delete(actual_filtered, key, separator='->')
            # dpath.delete(expected_filtered, key, separator='->')
            actual_filtered = {key: actual.get(key) for key in actual if key not in compare_expect_of}
            expected_filtered = {key: expected.get(key) for key in expected if key not in compare_expect_of}

    log.debug('Compare dictionaries. \n Actual: \n {} \n Expected: \n {}'.format(reporter.pformat(actual_filtered),
                                                                                 reporter.pformat(expected_filtered)))
    assert actual_filtered == expected_filtered


def check_iterable_contains(actual, must_contains):
    if not isinstance(actual, Iterable):
        log.debug('actual must be of iterable type')

    for field in must_contains:
        assert field in actual


def check_iterable_not_contains(actual, must_not_contains):
    if not isinstance(actual, Iterable):
        log.debug('actual must be of iterable type')

    for field in must_not_contains:
        assert field not in actual


def check_bs_query(query_bs, service, compare_field=None, compare_value=None, convert_to_decimal=False):
    with reporter.step(u'Проверяем запрос в BS'):
        result_bs = db_steps.bs_or_ng_by_service(service).execute_query(query_bs)
        if compare_field:
            if convert_to_decimal:
                from decimal import Decimal
                actual = Decimal(result_bs.get(compare_field))
                expected = Decimal(compare_value)
            else:
                actual = result_bs.get(compare_field)
                expected = compare_value
            check_that(actual, is_(equal_to(expected)),
                       step=u'Проверяем что результат запроса BS совпадает с проверяемым значением',
                       error='Query result in bs is not equals to expected')
        else:
            check_that(result_bs, is_not(None), step=u'Проверяем что результат запроса в BS не пуст',
                       error='Bs query has got an empty result')


def queries_bo_bs_got_same_data(query_bo, query_bs, service):
    with reporter.step(u'Проверяем что два запроса в BO и BS возвращают одинаковый результат'):
        result_bo = db_steps.bo().execute_query_all(query_bo)
        result_bs = db_steps.bs_or_ng_by_service(service).execute_query_all(query_bs)
        check_that(result_bo, is_not(None), step=u'Проверяем что результат запроса в BO не пуст',
                   error='Bs query has got an empty result')
        check_that(result_bs, is_not(None), step=u'Проверяем что результат запроса в BS не пуст',
                   error='Bs query has got an empty result')
        check_that(result_bo, is_(equal_to(result_bo)), step=u'Проверяем что результаты запросов в BO и BS совпадают',
                   error='Different queries results in bo and bs')


def check_trust_refunds(orders, trust_payment_id):
    refunds = db_steps.bs().get_all_refunds_of_payment(trust_payment_id)
    butils.check_that(len(orders), is_(equal_to(len(refunds))),
                      step=u'Проверяем что по каждому заказу проставился trust_refund_id',
                      error=u'По одному или нескольким заказам не проставился trust_refund_id')


def check_log(user, trust_payment_id, pass_params):
    # todo переделать когда поправят багу
    with reporter.step('Проверяем, был ли переданы pass_params в платежный шлюз'):
        path = '/var/remote-log/{}/yb/yb-trust-queue.log'.format(environments.simpleapi_env().trust_log_url)
        time_str = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(time.time() - 60))
        regexp = '.+sending body: user_id={}&merchant_order_id={}+'.format(user.uid, trust_payment_id)
        resp = balance_test.find_in_log(path, time_str, regexp)
        check_that(resp.get('lines'), is_not(empty()), error=u'В результате поиска по логу ничего не найдено')
        url = urllib.unquote(resp['lines'][0]).decode('utf-8')
        for param in pass_params.values():
            check_that(url, contains_string(param), error=u'Запрос в scrat не содержит параметр: {}'.format(param))


def check_that(value, matcher, step=u'Выполняем проверку', error=u''):
    butils.check_that(value, matcher, step=step, error=error)
