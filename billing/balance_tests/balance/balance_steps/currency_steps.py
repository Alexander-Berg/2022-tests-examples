# coding=utf-8
__author__ = 'igogor'

import datetime

from decimal import Decimal
import balance.balance_api as api
import balance.balance_db as db
import btestlib.utils as utils

log_align = 30

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class CurrencySteps(object):
    @staticmethod
    def get_currency_rate(dt, currency, base_cc, rate_src_id, iso_base=False):
        if currency == base_cc:
            return Decimal('1')

        rate_data = api.medium().GetCurrencyRate(currency, dt, rate_src_id, base_cc, iso_base)
        if rate_data[1] == 'SUCCESS':
            return Decimal(rate_data[2]['rate'])

        # пробуем обратный курс
        rate_data = api.medium().GetCurrencyRate(base_cc, dt, rate_src_id, currency, iso_base)
        if rate_data[1] == 'SUCCESS':
            return Decimal('1.') / Decimal(rate_data[2]['rate'])

        raise Exception("Currency Rate not found for date {}".format(dt))

    @staticmethod
    def get_iso_code_by_num_code(num_code):
        # handle kzt, try, uah
        num_code = str(num_code)
        num_code = num_code if num_code not in ['398', '949', '980'] else '10{}'.format(num_code)

        query = "SELECT iso_code  FROM t_currency WHERE num_code = :num_code"
        return db.balance().execute(query, {'num_code': num_code}, single_row=True)['iso_code']
