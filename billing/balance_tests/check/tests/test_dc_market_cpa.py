# coding: utf-8
__author__ = 'chihiro'
import random
from decimal import Decimal as D
from datetime import datetime, timedelta

import pytest
from dateutil.relativedelta import relativedelta

from btestlib.data import defaults
from check.steps import COMPLETION_DT
from check.shared import CheckSharedBefore
from check import shared_steps, steps as check_steps
from balance.distribution.distribution_types import DistributionType

NOT_TODAY = (datetime.now() + timedelta(days=6)).strftime("%d.%m.%y 00:00:00")
TWO_MONTH_AGO = datetime.now() - relativedelta(months=2)
REVERSE_DATE = (datetime.now() - relativedelta(months=1)).replace(day=3)


class TestDcMarketCPA(object):
    COMPLETION_TYPE = DistributionType.MARKET_CPA
    SOURCE_ID = DistributionType.MARKET_CPA.source_id
    PAGE_ID = 10004
    SHOWS = defaults.Distribution.DEFAULT_REVSHARE_SHOWS
    CLICKS = defaults.Distribution.DEFAULT_REVSHARE_CLICKS
    BUCKS = D(DistributionType.MARKET_CPA.default_amount) / DistributionType.MARKET_CPA.units_type_rate
    DATE = COMPLETION_DT
    DATE_STR = DATE.strftime('%Y-%m-%d')

    DIFFS_COUNT = 4  # Стало на два расхождения меньше. Было 6 - CHECK-2762

    @staticmethod
    def additional_cond(test_name):
        return 'MarketCPA' in test_name

    def format_data(self, tag_id):
        return {
            'date': self.DATE_STR,
            'tag_id': tag_id,
            'clicks': self.CLICKS,
            'orders': self.SHOWS,
            "bucks": self.BUCKS,
            'bucks_rs': ''
        }

    def run_cmp(self, shared_data, before):
        cmp_data = shared_steps.SharedBlocks.run_dc(
            shared_data, before, pytest.active_tests, additional_conditions=self.additional_cond,
            block_name=shared_steps.SharedBlocks.RUN_DC_MARKET_CPA
        )
        return cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC_MARKET_CPA)
    def test_dc_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = self.format_data(tag_id)

        cmp_data = self.run_cmp(shared_data, before)
        assert tag_id not in [row['tag_id'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC_MARKET_CPA)
    def test_dc_not_found_in_market_cpa(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

        cmp_data = self.run_cmp(shared_data, before)
        result = [(row['tag_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
        assert len(result) == 1
        assert (tag_id, 1) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC_MARKET_CPA)
    def test_dc_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            tag_id = random.randint(100000, 999999)
            source_id = self.SOURCE_ID

            dc_data = self.format_data(tag_id)

        cmp_data = self.run_cmp(shared_data, before)
        result = [(row['tag_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
        assert len(result) == 1
        assert (tag_id, 2) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC_MARKET_CPA)
    def test_dc_shows_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = self.format_data(tag_id)
            dc_data['orders'] **= 2

        cmp_data = self.run_cmp(shared_data, before)
        result = [(row['tag_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
        assert len(result) == 0  # Теперь тут нет расхождений - CHECK-2762

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC_MARKET_CPA)
    def test_dc_clicks_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = self.format_data(tag_id)
            dc_data['clicks'] **= 2

        cmp_data = self.run_cmp(shared_data, before)
        result = [(row['tag_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
        assert len(result) == 0  # Теперь тут нет расхождений - CHECK-2762

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC_MARKET_CPA)
    def test_dc_bucks_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = self.format_data(tag_id)
            dc_data['bucks'] **= 2

        cmp_data = self.run_cmp(shared_data,before)
        result = [(row['tag_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
        assert len(result) == 1
        assert (tag_id, 5) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC_MARKET_CPA)
    def test_dc_hits_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = self.format_data(tag_id)
            dc_data['bucks_rs'] = dc_data['bucks']

        cmp_data = self.run_cmp(shared_data,before)
        result = [(row['tag_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
        assert len(result) == 1
        assert (tag_id, 6) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC_MARKET_CPA)
    def test_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = self.run_cmp(shared_data,before)
        assert len(cmp_data) == self.DIFFS_COUNT
