# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime

import pytest

import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
from balance.features import Features
from btestlib import utils
from btestlib.matchers import equal_to_casted_dict

# смотрим курс на вчера
DATE_OF_RATE = datetime.datetime.today().replace(hour=0, minute=0, second=0, microsecond=0) - datetime.timedelta(
    days=2)

TODAY = datetime.datetime.today().replace(hour=0, minute=0, second=0, microsecond=0)


# тест на проверку параметра base_cc в GetCurrencyRate
@pytest.mark.smoke
@reporter.feature(Features.GET_CURRENCY_RATE, Features.XMLRPC)
@pytest.mark.tickets('BALANCE-23475')
@pytest.mark.parametrize('rate_src_id, currency, base_cc',
                         [
                             (1005, 'USD', 'AMD'),
                             (1000, 'USD', None),
                         ],
                         ids=['With base_cc'
                             , 'W/O base_cc'
                              ]
                         )
def test_get_currency_rate(rate_src_id, currency, base_cc):
    # вызываем метод GetCurrencyRate
    if base_cc:
        result = api.medium().GetCurrencyRate(currency, DATE_OF_RATE, rate_src_id, base_cc)
    else:
        result = api.medium().GetCurrencyRate(currency, DATE_OF_RATE)
        base_cc = 'RUR'

    db_result = get_db_currency_rate(rate_src_id, base_cc, currency, DATE_OF_RATE, False)

    expected_result = {
        'currency': db_result[0]['cc'],
        'date': utils.Date.nullify_time_of_date(db_result[0]['rate_dt']),
        'rate': db_result[0]['rate']
    }

    utils.check_that(result[2], equal_to_casted_dict(expected_result))


# тест на фиксированные курсы
@reporter.feature(Features.GET_CURRENCY_RATE, Features.XMLRPC)
@pytest.mark.parametrize('currency, expected_rate', [('USD', '315.35'), ('EUR', '343.98'), ('RUB', '5.52')])
@pytest.mark.parametrize('rate_src_id', [1007, None])
@pytest.mark.parametrize('base_cc', ['KZT'])
def test_get_fixed_currency_rates(rate_src_id, expected_rate, currency, base_cc):
    # дёргаем ручку GetCurrencyRates
    result = api.medium().GetCurrencyRate(currency, DATE_OF_RATE, rate_src_id, base_cc)

    expected_result = {'currency': currency,
                       'date': datetime.datetime(2017, 5, 4, 0, 0),
                       'rate': expected_rate}
    utils.check_that(result[2], equal_to_casted_dict(expected_result), step='Сравним курс с ожидаемым "прибитым".')


# тест на нефиксированные курсы
@reporter.feature(Features.GET_CURRENCY_RATE, Features.XMLRPC)
@pytest.mark.parametrize('currency, rate_src_id, base_cc',
                         [
                             ('USD', 1007, 'KZT'),
                             ('EUR', 1002, 'TRY'),
                             ('EUR', 1003, 'UAH'),
                             ('RUB', 1002, 'TRY'),
                             ('RUB', 1007, 'KZT'),
                         ]
                         )
def test_unfixed_currency_rates(currency, rate_src_id, base_cc):
    result = api.medium().GetCurrencyRate(currency, DATE_OF_RATE, rate_src_id, base_cc, True)

    db_result = get_db_currency_rate(rate_src_id, base_cc, currency, DATE_OF_RATE, True)
    expected_result = {
        'currency': db_result[0]['cc'],
        'date': utils.Date.nullify_time_of_date(db_result[0]['rate_dt']),
        'rate': db_result[0]['rate']
    }

    utils.check_that(result[2], equal_to_casted_dict(expected_result))


# отдельно тесты для пустого RUB c разными параметрами
@pytest.mark.parametrize('rate_dt', ['', '2011-11-28'], ids=['Empty date', 'Fixed date'])
@pytest.mark.parametrize('rate_src_id', [None, 1000], ids=['Without rate source', 'Rate source = 1000'])
@pytest.mark.parametrize('iso_base', [True, False], ids=['iso_base = True', 'iso_base = False'])
def test_rub_rate_without_params(rate_dt, rate_src_id, iso_base):
    result = api.medium().GetCurrencyRate('RUB', rate_dt, rate_src_id, None, iso_base)
    expected_result = {
        'currency': 'RUB',
        'date': TODAY,
        'rate': '1'
    }
    utils.check_that(result[2], equal_to_casted_dict(expected_result))


# utils
def get_db_currency_rate(rate_src_id, base_cc, currency, rate_dt, iso_base):
    query = "SELECT * FROM t_currency_rate_v2 " \
            "WHERE rate_src_id = :rate_src_id AND base_cc = :base_cc AND cc = :cc " \
            "AND rate_dt = (SELECT max(rate_dt) FROM t_currency_rate_v2 " \
            "WHERE rate_src_id = :rate_src_id AND base_cc = :base_cc AND cc = :cc AND rate_dt <= :dt)"

    b_cc = base_cc.lower() if iso_base else base_cc

    if currency == 'RUB' and iso_base:
        cc = 'RUR'
    else:
        cc = currency
    params = {'rate_src_id': rate_src_id, 'base_cc': b_cc, 'cc': cc, 'dt': rate_dt}
    res = db.balance().execute(query, params)

    if res[0]['cc'] == 'RUR':
        res[0]['cc'] = 'RUB'

    return res
