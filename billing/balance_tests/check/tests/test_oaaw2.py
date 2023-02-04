# coding: utf-8
__author__ = 'chihiro'

from datetime import datetime, timedelta
from decimal import Decimal
import time

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string, is_in, empty, has_length, equal_to

import btestlib.reporter as reporter
from btestlib import utils as butils
from check import shared_steps, utils
from check.shared import CheckSharedBefore

CONTRACT_START_DT = butils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))
DEFAULT_SERVICE_MIN_COST = Decimal('100')

CHECK_CODE_NAME = 'oaaw2'

class TestOAAW2(object):
    DIFFS_COUNT = 11
    DATE = datetime.now() - timedelta(days=1)
    SHOWS_ACCEPTED = 50
    SHOWS_REALIZED = 50
    BUDGET_PLAN = 10
    BUDGET_REALIZED = 10
    PRODUCT_TYPE_NMB = 1

    @staticmethod
    def format_order(count):
        return '{!s}-{:x}'.format(count, int(time.time()))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_number', 'ado_data', 'awaps_data']) as before:
            before.validate()
            order_number = self.format_order(1)
            ado_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }
            awaps_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['order_nmb'], row['state'])
                  for row in cmp_data if row['order_nmb'] == order_number]

        butils.check_that(result, empty())

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_not_found_in_ado(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_number', 'awaps_data']) as before:
            before.validate()
            order_number = self.format_order(2)
            awaps_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['order_nmb'], row['state'])
                  for row in cmp_data if row['order_nmb'] == order_number]

        butils.check_that((order_number, 2), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_not_found_in_awaps(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_number', 'ado_data']) as before:
            before.validate()
            order_number = self.format_order(3)
            ado_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['order_nmb'], row['state'])
                  for row in cmp_data if row['order_nmb'] == order_number]

        butils.check_that((order_number, 1), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_shows_accepted_mismatch(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_number', 'ado_data', 'awaps_data']) as before:
            before.validate()
            order_number = self.format_order(4)
            ado_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED + 10,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }
            awaps_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['order_nmb'], row['state'])
                  for row in cmp_data if row['order_nmb'] == order_number]

        butils.check_that((order_number, 3), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_shows_realized_mismatch(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_number', 'ado_data', 'awaps_data']) as before:
            before.validate()
            order_number = self.format_order(5)
            ado_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }
            awaps_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED + 10,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['order_nmb'], row['state'])
                  for row in cmp_data if row['order_nmb'] == order_number]

        butils.check_that((order_number, 4), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_budget_plan_mismatch(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_number', 'ado_data', 'awaps_data']) as before:
            before.validate()
            order_number = self.format_order(6)
            ado_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN + 7,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }
            awaps_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED ,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['order_nmb'], row['state'])
                  for row in cmp_data if row['order_nmb'] == order_number]

        butils.check_that((order_number, 5), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_budget_realized_mismatch(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_number', 'ado_data', 'awaps_data']) as before:
            before.validate()
            order_number = self.format_order(7)
            ado_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }
            awaps_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED + 3,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['order_nmb'], row['state'])
                  for row in cmp_data if row['order_nmb'] == order_number]

        butils.check_that((order_number, 6), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_date_begin_mismatch(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_number', 'ado_data', 'awaps_data']) as before:
            before.validate()
            order_number = self.format_order(8)
            ado_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': (datetime.now() - timedelta(days=2)).strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }
            awaps_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['order_nmb'], row['state'])
                  for row in cmp_data if row['order_nmb'] == order_number]

        butils.check_that((order_number, 7), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_date_end_mismatch(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_number', 'ado_data', 'awaps_data']) as before:
            before.validate()
            order_number = self.format_order(9)
            ado_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }
            awaps_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': (datetime.now() - timedelta(days=2)).strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['order_nmb'], row['state'])
                  for row in cmp_data if row['order_nmb'] == order_number]

        butils.check_that((order_number, 8), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_product_type_nmb_mismatch(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_number', 'ado_data', 'awaps_data']) as before:
            before.validate()
            order_number = self.format_order(10)
            ado_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': 9
            }
            awaps_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['order_nmb'], row['state'])
                  for row in cmp_data if row['order_nmb'] == order_number]

        butils.check_that((order_number, 9), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_double_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_number', 'ado_data', 'awaps_data']) as before:
            before.validate()
            order_number = self.format_order(11)
            ado_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED + 10,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }
            awaps_data = {
                'order_nmb': order_number,
                'shows_accepted': self.SHOWS_ACCEPTED,
                'shows_realized': self.SHOWS_REALIZED,
                'budget_plan': self.BUDGET_PLAN + 9,
                'budget_realized': self.BUDGET_REALIZED,
                'date_begin': self.DATE.strftime('%d.%m.%Y'),
                'date_end': self.DATE.strftime('%d.%m.%Y'),
                'product_type_nmb': self.PRODUCT_TYPE_NMB
            }

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')


        result = [(row['order_nmb'], row['state'])
                  for row in cmp_data if row['order_nmb'] == order_number]

        butils.check_that((order_number, 3), is_in(result))
        butils.check_that((order_number, 5), is_in(result))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_oaaw2_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        butils.check_that(cmp_data, has_length(self.DIFFS_COUNT))

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OAAW2)
    def test_check_autoanalyzer_comments(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_oaaw2(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
        reporter.log("CMP_DATA = %s" % cmp_data)

        cmp_id = cmp_data[0]['cmp_id']

        ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)
        rationale = u'Выясняем причину расхождения на стороне сервиса'

        comments = list(ticket.comments.get_all(expand='attachments'))

        for comment in comments:
            if rationale in comment.text:
                attachment_name = comment.attachments[0].name

                butils.check_that(
                    attachment_name, contains_string(str(u'.xls'))
                )
                break
        else:
            assert False, u'Комментарий авторазбора не найден'
