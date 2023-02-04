# coding=utf-8
from datetime import datetime, timedelta
from decimal import Decimal
import pytest

import pytz

from billing.apikeys.apikeys.tarifficator import Tariffication
from billing.apikeys.apikeys.tarifficator.consumers import (
    DailyStatisticRangeConsumerUnit,
    DailyStatisticRangeConsumerWithTrialUnit,
    FirstPeriodScaleByDayFixerUnit,
    PeriodicalConsumerUnit,
    PrepayPeriodicallyWithTrialUnit,
    PostpaySubscribePeriodicallyRangeConsumerUnit,
    WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit,
    PrepayPeriodicallyDiscountedUnit,
    PrepayPeriodicallyForgiveIncompleteUnit,
    BanLinkPeriodicallyReachingLimitUnit,
)


class TestDailyStatisticRangeConsumerUnit:

    @pytest.fixture()
    def tariffication(self, fake_statistic_getter):
        return Tariffication([], fake_statistic_getter({
            datetime(2018, 4,  1, tzinfo=pytz.utc): {'value': 100000},
            datetime(2018, 4, 13, tzinfo=pytz.utc): {'value': 100000},
            datetime(2018, 4, 30, tzinfo=pytz.utc): {'value': 10000000},
            datetime(2018, 5,  1, tzinfo=pytz.utc): {'value': 10000},
            datetime(2018, 5, 13, tzinfo=pytz.utc): {'value': 90000},
            datetime(2018, 5, 30, tzinfo=pytz.utc): {'value': 10000000},
        }))

    def test_complex(self, tariffication):
        unit = DailyStatisticRangeConsumerUnit(tariffication=tariffication, **{
            "time_zone": "UTC",
            "product_id": "508229",
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 1, 12, tzinfo=pytz.utc),  # активация не в начале дня, но мы должны учесть текущий день полностью
            'last_run': datetime(2018, 4, 1, 12, tzinfo=pytz.utc),
            'now': datetime(2018, 6, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        value = 100000 + 100000 + 10000000 + 10000 + 90000 + 10000000
        assert Decimal(state['products']['508229']['consumed']) == Decimal(value)


class TestDailyStatisticRangeConsumerWithTrialUnit:

    @pytest.fixture()
    def tariffication(self, fake_statistic_getter):
        return Tariffication([], fake_statistic_getter({
            datetime(2022, 1, 20, tzinfo=pytz.utc): {'value': 100},
            datetime(2022, 1, 21, tzinfo=pytz.utc): {'value': 200},
            datetime(2022, 1, 22, tzinfo=pytz.utc): {'value': 300},
            datetime(2022, 1, 23, tzinfo=pytz.utc): {'value': 400},
            datetime(2022, 1, 24, tzinfo=pytz.utc): {'value': 500},
            datetime(2022, 1, 25, tzinfo=pytz.utc): {'value': 600},
        }))

    def test_complex(self, tariffication):
        unit = DailyStatisticRangeConsumerWithTrialUnit(tariffication=tariffication, **{
            'time_zone': 'UTC',
            'product_id': '508229',
            'subscription_product_id': '508200'
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2022, 1, 19, 12, tzinfo=pytz.utc),
            'last_run': datetime(2022, 1, 19, tzinfo=pytz.utc),
            'products': {
                '508200': {
                    'trial_period_until': datetime(2022, 1, 22, tzinfo=pytz.utc)
                }
            }
        }

        # 'now' before trial end
        state['now'] = datetime(2022, 1, 21, tzinfo=pytz.utc)
        unit(prev_state, state)
        assert state['products'].get('508229') is None

        # 'now' after trial end
        state['now'] = datetime(2022, 1, 26, tzinfo=pytz.utc)
        unit(state, state)
        value = 300 + 400 + 500 + 600
        assert Decimal(state['products']['508229']['consumed']) == Decimal(value)


class TestFirstPeriodScaleByDayFixerUnit:

    def test_half_product(self):
        unit = FirstPeriodScaleByDayFixerUnit(tariffication=None, **{
            "period_mask": "0 0 1 * * *", "time_zone": "Europe/Moscow",
            "product_id": "508899", "product_value": "120"
        })
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'last_run': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'now': datetime(2018, 4, 17, tzinfo=pytz.utc),
        }
        unit({}, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('-60')

    def test_whole_product(self):
        unit = FirstPeriodScaleByDayFixerUnit(tariffication=None, **{
            "period_mask": "0 0 1 * * *", "time_zone": "Europe/Moscow",
            "product_id": "508899", "product_value": "120"
        })
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 1, tzinfo=pytz.utc),
            'last_run': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'now': datetime(2018, 4, 17, tzinfo=pytz.utc),
        }
        unit({}, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('0')

    def test_no_double_fix(self):
        unit = FirstPeriodScaleByDayFixerUnit(tariffication=None, **{
            "period_mask": "0 0 1 * * *", "time_zone": "Europe/Moscow",
            "product_id": "508899", "product_value": "120"
        })
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'last_run': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'now': datetime(2018, 4, 17, tzinfo=pytz.utc),
        }
        unit({}, state)
        state['now'] = datetime(2018, 4, 18, tzinfo=pytz.utc)
        unit({}, state)
        state['now'] = datetime(2018, 5, 17, tzinfo=pytz.utc)
        unit({}, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('-60')


class TestPeriodicalConsumerUnit:

    def test_close_previous(self):
        unit = PeriodicalConsumerUnit(tariffication=None, **{
            "period_mask": "0 0 1 * * *", "time_zone": "UTC",
            "product_id": "508899", "product_value": "120"
        })
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'last_run': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'now': datetime(2018, 4, 17, tzinfo=pytz.utc),
        }

        unit({}, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('120')

        state['now'] = datetime(2018, 5, 1, tzinfo=pytz.utc)
        unit({}, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('120')

        state['now'] = datetime(2018, 5, 1, 0, 1, tzinfo=pytz.utc)
        unit({}, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('240')


class TestPostpaySubscribePeriodicallyRangeConsumerUnit:

    @pytest.fixture()
    def tariffication(self, fake_statistic_getter):
        return Tariffication([], fake_statistic_getter({
            datetime(2018, 4,  1, tzinfo=pytz.utc): {'value': 100000},
            datetime(2018, 4, 13, tzinfo=pytz.utc): {'value': 100000},
            datetime(2018, 4, 30, tzinfo=pytz.utc): {'value': 10000000},
            datetime(2018, 5, 1, tzinfo=pytz.utc): {'value': 10000},
            datetime(2018, 5, 13, tzinfo=pytz.utc): {'value': 90000},
            datetime(2018, 5, 30, tzinfo=pytz.utc): {'value': 10000000},
        }))

    def test_consume_single_period_per_day(self, tariffication):
        unit = PostpaySubscribePeriodicallyRangeConsumerUnit(tariffication=tariffication, **{
            "period_mask": "0 0 x * * *", "time_zone": "UTC",
            "range_from": "10000",
            "subscription_product_id": "509584", "product_id": "509586"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 1, tzinfo=pytz.utc),
            'last_run': datetime(2018, 4, 1, tzinfo=pytz.utc),
            'now': datetime(2018, 4, 1, tzinfo=pytz.utc) + timedelta(hours=1),  # Начало первого дня месяца
            'products': {
                '509584': {
                    'next_consume_date': datetime(2018, 5, 1, tzinfo=pytz.utc),
                },
            },
        }
        unit(prev_state, state)
        assert Decimal(state['products']['509586']['consumed']) == Decimal('90000')

        prev_state = state.copy()
        state['now'] = datetime(2018, 4, 1, tzinfo=pytz.utc) + timedelta(days=13)
        unit(prev_state, state)
        assert Decimal(state['products']['509586']['consumed']) == Decimal('190000')

        prev_state = state.copy()
        state['now'] = datetime(2018, 5, 1, tzinfo=pytz.utc)
        unit(prev_state, state)
        assert Decimal(state['products']['509586']['consumed']) == Decimal('10190000')

    def test_consume_two_periods(self, tariffication):
        range_from = 10000
        unit = PostpaySubscribePeriodicallyRangeConsumerUnit(tariffication=tariffication, **{
            "period_mask": "0 0 x * * *", "time_zone": "UTC",
            "range_from": str(range_from),
            "subscription_product_id": "509584", "product_id": "509586"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 1, tzinfo=pytz.utc),
            'last_run': datetime(2018, 4, 1, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),  # Месяц целиком
            'products': {
                '509584': {
                    'next_consume_date': datetime(2018, 5, 1, tzinfo=pytz.utc),
                },
            },
        }
        unit(prev_state, state)
        first_month_value = 100000 + 100000 + 10000000 - range_from
        assert Decimal(state['products']['509586']['consumed']) == Decimal(first_month_value)

        prev_state = state.copy()
        state['last_run'] = state['now']
        state['now'] = datetime(2018, 6, 1, tzinfo=pytz.utc)
        state['products']['509584']['next_consume_date'] = datetime(2018, 6, 1, tzinfo=pytz.utc)
        unit(prev_state, state)
        second_month_value = 100000 + 10000000 - range_from
        assert Decimal(state['products']['509586']['consumed']) == Decimal(first_month_value + second_month_value)

    def test_consume_two_periods_subscription_longer_than_period(self, tariffication):
        range_from = 10000
        unit = PostpaySubscribePeriodicallyRangeConsumerUnit(tariffication=tariffication, **{
            "period_mask": "0 0 x * * *", "time_zone": "UTC",
            "range_from": str(range_from),
            "subscription_product_id": "509584", "product_id": "509586"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 1, tzinfo=pytz.utc),
            'last_run': datetime(2018, 4, 1, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),  # Месяц целиком
            'products': {
                '509584': {
                    'next_consume_date': datetime(2019, 4, 1, tzinfo=pytz.utc),
                },
            },
        }
        unit(prev_state, state)
        first_month_value = 100000 + 100000 + 10000000 - range_from
        assert Decimal(state['products']['509586']['consumed']) == Decimal(first_month_value)

        prev_state = state.copy()
        state['last_run'] = state['now']
        state['now'] = datetime(2018, 6, 1, tzinfo=pytz.utc)
        state['products']['509584']['next_consume_date'] = datetime(2018, 6, 1, tzinfo=pytz.utc)
        unit(prev_state, state)
        second_month_value = 100000 + 10000000 - range_from
        assert Decimal(state['products']['509586']['consumed']) == Decimal(first_month_value + second_month_value)


class TestWithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit:

    @pytest.fixture()
    def tariffication(self, fake_statistic_getter):
        return Tariffication([], fake_statistic_getter({
            datetime(2018, 3, 16, tzinfo=pytz.utc): {'value': 100000},
            datetime(2018, 4, 30, tzinfo=pytz.utc): {'value': 10000000}
        }))

    def test_first_period_whole(self, tariffication):
        unit = WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit(tariffication=tariffication, **{
            "range_from": "30000", "time_zone": "UTC",
            "product_id": "508899"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 1, tzinfo=pytz.utc),  # 30 days from 30
            'last_run': datetime(2018, 4, 1, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('9970000')

    def test_second_period_whole(self, tariffication):
        unit = WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit(tariffication=tariffication, **{
            "range_from": "30000", "time_zone": "UTC",
            "product_id": "508899"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 3, 19, tzinfo=pytz.utc),  # 1.4 month
            'last_run': datetime(2018, 3, 19, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('9970000')

    def test_two_periods_whole(self, tariffication):
        unit = WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit(tariffication=tariffication, **{
            "range_from": "30000", "time_zone": "UTC",
            "product_id": "508899"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 3, 1, tzinfo=pytz.utc),  # 2 months
            'last_run': datetime(2018, 3, 1, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('10040000')  # 9970000 + 70000

    def test_first_period_part_secod_period_whole(self, tariffication):
        unit = WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit(tariffication=tariffication, **{
            "range_from": "30000", "time_zone": "UTC",
            "product_id": "508899"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 3, 11, tzinfo=pytz.utc),  # 1.66 months
            'last_run': datetime(2018, 3, 11, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('10049677')  # 9970000 + (100000 - 30000*21/31)

    def test_scale_rage_from_2_to_30_days(self, tariffication):
        unit = WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit(tariffication=tariffication, **{
            "range_from": "30000", "time_zone": "UTC",
            "product_id": "508899"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 2, tzinfo=pytz.utc),  # 29 days from 30
            'last_run': datetime(2018, 4, 2, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('9971000')  # 10000000-(30000/30*28)

    def test_scale_rage_from_15_to_30_days(self, tariffication):
        unit = WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit(tariffication=tariffication, **{
            "range_from": "30000", "time_zone": "UTC",
            "product_id": "508899"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 16, tzinfo=pytz.utc),  # 15 days from 30
            'last_run': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('9985000')  # 10000000-(30000/2)

    def test_scale_rage_from_18_to_30_days(self, tariffication):
        unit = WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit(tariffication=tariffication, **{
            "range_from": "30000", "time_zone": "UTC",
            "product_id": "508899"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 18, tzinfo=pytz.utc),  # 13 days from 30
            'last_run': datetime(2018, 4, 18, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('9987000')  # 10000000-(30000/30*13)

    def test_scale_rage_from_30_to_30_days(self, tariffication):
        unit = WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit(tariffication=tariffication, **{
            "range_from": "30000", "time_zone": "UTC",
            "product_id": "508899"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 30, tzinfo=pytz.utc),  # 1 day from 30
            'last_run': datetime(2018, 4, 30, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('9999000')  # 10000000-(30000/30*1)

    def test_scale_range_to(self, tariffication):
        unit = WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit(tariffication=tariffication, **{
            "range_to": "30000", "time_zone": "UTC",
            "product_id": "508899"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 16, tzinfo=pytz.utc),  # 15 days from 30
            'last_run': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('15000')

    def test_scale_precision_zero(self, tariffication):
        unit = WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit(tariffication=tariffication, **{
            "range_to": "50000", "time_zone": "UTC",
            "product_id": "508899", "precision": ".0001"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 14, tzinfo=pytz.utc),  # 16 days from 30
            'last_run': datetime(2018, 4, 14, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('28333')

    def test_scale_precision_three(self, tariffication):
        unit = WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit(tariffication=tariffication, **{
            "scale_precision": ".001", "range_to": "50000", "time_zone": "UTC",
            "product_id": "508899", "precision": ".0001"
        })
        prev_state = {
            'is_active': True,
        }
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 14, tzinfo=pytz.utc),  # 16 days from 30
            'last_run': datetime(2018, 4, 14, tzinfo=pytz.utc),
            'now': datetime(2018, 5, 1, tzinfo=pytz.utc),
        }
        unit(prev_state, state)
        assert Decimal(state['products']['508899']['consumed']) == Decimal('28333.333')


class TestPrepayPeriodicallyWithTrialUnit:
    unit_id = 'PrepayPeriodicallyWithTrialUnit_yearly'
    product_id = '508206'

    @pytest.fixture()
    def unit_being_tested_factory(self):
        return lambda product_value, postpone_days: PrepayPeriodicallyWithTrialUnit(tariffication=None, **{
            'period_mask': '0 0 x * * *', 'time_zone': '+0300',
            'product_id': self.product_id, 'product_value': product_value,
            'trial_period_days': 10, 'postpone_consume_days': postpone_days,
            'ban_reason': 1, 'unban_reason': 18, 'scope': 'yearly'
        })

    def test_first_consume(self, unit_being_tested_factory):
        unit = unit_being_tested_factory(product_value='100', postpone_days='0')
        state = {
            'is_active': True,
            'activated_date': datetime(2022, 1, 17, tzinfo=pytz.utc),
            'now': datetime(2022, 1, 17, tzinfo=pytz.utc),
            'products': {
                self.product_id: {
                    'consumed': '0',
                    'credited': '100'
                }
            },
        }

        # turn on the tariff
        unit({}, state)
        assert state['products'][self.product_id] == {
            'consumed': '100',
            'credited': '100',
            'next_consume_value': '100',
            'next_consume_date': datetime(2022, 2, 26, 21, 0, tzinfo=pytz.utc),
            'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc)
        }

        # 'now' before trial end - skip the billing
        state['now'] = datetime(2022, 1, 18, tzinfo=pytz.utc)
        unit({}, state)
        assert state['products'][self.product_id] == {
            'consumed': '100',
            'credited': '100',
            'next_consume_value': '100',
            'next_consume_date': datetime(2022, 2, 26, 21, 0, tzinfo=pytz.utc),
            'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc)
        }

        # 'now' after trial end, before next consume - skip the billing
        state['now'] = datetime(2022, 1, 28, tzinfo=pytz.utc)
        unit({}, state)
        assert state['products'][self.product_id] == {
            'consumed': '100',
            'credited': '100',
            'next_consume_value': '100',
            'next_consume_date': datetime(2022, 2, 26, 21, 0, tzinfo=pytz.utc),
            'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc)
        }

    def test_second_consume(self, unit_being_tested_factory):
        unit = unit_being_tested_factory(product_value='100', postpone_days='0')
        state = {
            'is_active': True,
            'activated_date': datetime(2022, 1, 17, tzinfo=pytz.utc),
            'now': datetime(2022, 2, 27, tzinfo=pytz.utc),
            'products': {
                self.product_id: {
                    'consumed': '100',
                    'credited': '200',
                    'next_consume_value': '100',
                    'next_consume_date': datetime(2022, 2, 26, 21, 0, tzinfo=pytz.utc),
                    'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc)
                }
            },
        }

        unit({}, state)
        assert state['products'][self.product_id] == {
            'consumed': '200',
            'credited': '200',
            'next_consume_value': '100',
            'next_consume_date': datetime(2022, 3, 26, 21, 0, tzinfo=pytz.utc),
            'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc)
        }

    def test_second_consume_after_postpone(self, unit_being_tested_factory):
        """
        Consume was postponed for several days and today 'credited' is enought to
        consume and set new 'next_consume_date'
        """
        unit = unit_being_tested_factory(product_value='100', postpone_days='10')
        state = {
            'is_active': True,
            'activated_date': datetime(2022, 1, 17, tzinfo=pytz.utc),
            'now': datetime(2022, 3, 2, tzinfo=pytz.utc),
            'products': {
                self.product_id: {
                    'consumed': '100',
                    'credited': '200',
                    'next_consume_value': '100',
                    'next_consume_date': datetime(2022, 2, 26, 21, 0, tzinfo=pytz.utc),
                    'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc)
                }
            },
        }

        unit({}, state)
        assert state['products'][self.product_id] == {
            'consumed': '200',
            'credited': '200',
            'next_consume_value': '100',
            'next_consume_date': datetime(2022, 3, 26, 21, 0, tzinfo=pytz.utc),
            'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc),
            'postpone_payment_until': datetime(2022, 3, 8, 21, 0, tzinfo=pytz.utc),
        }

    @pytest.mark.parametrize("postpone_days,is_banned",
                             [("0", True),
                              ("10", False)])
    def test_postpone_consume(self, unit_being_tested_factory, postpone_days, is_banned):
        unit = unit_being_tested_factory(product_value='100', postpone_days=postpone_days)
        state = {
            'is_active': True,
            'activated_date': datetime(2022, 1, 17, tzinfo=pytz.utc),
            'now': datetime(2022, 2, 27, tzinfo=pytz.utc),
            'products': {
                self.product_id: {
                    'consumed': '100',
                    'credited': '100',
                    'next_consume_value': '100',
                    'next_consume_date': datetime(2022, 2, 26, 21, 0, tzinfo=pytz.utc),
                    'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc)
                }
            }
        }

        unit({}, state)
        expected = {
            'consumed': '100',
            'credited': '100',
            'next_consume_value': '100',
            'next_consume_date': datetime(2022, 2, 26, 21, 0, tzinfo=pytz.utc),
            'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc),
        }
        if postpone_days == "10":
            expected['postpone_payment_until'] = datetime(2022, 3, 8, 21, 0, tzinfo=pytz.utc)
        expected['credited_deficit'] = '100'
        assert state['products'][self.product_id] == expected
        if is_banned:
            assert state['ban_units'] == {'PrepayPeriodicallyWithTrialUnit_yearly': True}
            assert state['ban']
            assert state['ban_reason'] == 1
        else:
            assert state.get('ban_units', {}) == {}
            assert not state.get('ban', False)

    def test_can_consume_with_postpone(self, unit_being_tested_factory):
        postpone_days = 10
        unit = unit_being_tested_factory(product_value='100', postpone_days=postpone_days)
        state = {
            'is_active': True,
            'activated_date': datetime(2022, 1, 17, tzinfo=pytz.utc),
            'now': datetime(2022, 2, 27, tzinfo=pytz.utc),
            'products': {
                self.product_id: {
                    'consumed': '100',
                    'credited': '200',
                    'next_consume_value': '100',
                    'next_consume_date': datetime(2022, 2, 26, 21, 0, tzinfo=pytz.utc),
                    'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc)
                }
            }
        }

        unit({}, state)
        expected = {
            'consumed': '200',
            'credited': '200',
            'next_consume_value': '100',
            'next_consume_date': datetime(2022, 3, 26, 21, 0, tzinfo=pytz.utc),
            'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc),
            'postpone_payment_until': datetime(2022, 3, 8, 21, 0, tzinfo=pytz.utc),
        }
        assert state['products'][self.product_id] == expected


class TestPrepayPeriodicallyDiscountedUnit:

    unit_id = 'PrepayPeriodicallyDiscountedUnit_yearly'

    @pytest.fixture()
    def unit_being_tested_factory(self):
        return lambda product_value: PrepayPeriodicallyDiscountedUnit(tariffication=None, **{
            "period_mask": "0 0 x x * *", "truncate_period_mask": "0 0 * * * *", "time_zone": "+0300",
            "product_id": "508206", "product_value": product_value,
            "ban_reason": 1, "unban_reason": 18, 'scope': 'yearly'
        })

    def test_first_consume(self, unit_being_tested_factory):
        unit = unit_being_tested_factory(product_value='1500000')
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'last_run': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'now': datetime(2018, 4, 17, tzinfo=pytz.utc),
            'products': {
                '508206': {
                    'consumed': '0',
                    'credited': '1500000'
                }
            },
        }

        unit({}, state)
        next_tariff_data = state['next_tariff_state']['products']['508206'][unit.id]
        assert Decimal(next_tariff_data['prev_consume_value']) == Decimal('1500000')
        assert Decimal(next_tariff_data['consume_discount']) == Decimal('1500000')

    def test_calculating_discount_every_run(self, unit_being_tested_factory):
        unit = unit_being_tested_factory(product_value='1500000')
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 16, 0, 0, tzinfo=pytz.utc),
            'last_run': datetime(2018, 4, 16, 0, 0, tzinfo=pytz.utc),
            'now': datetime(2018, 10, 16, 0, 0, tzinfo=pytz.utc),
            'products': {
                '508206': {
                    'consumed': '1500000',
                    'credited': '1500000',
                    'next_consume_date': datetime(2019, 4, 16, 21, 0, tzinfo=pytz.utc),
                    'next_consume_value': '1500000'
                }
            }
        }

        unit({}, state)
        next_tariff_data = state['next_tariff_state']['products']['508206'][unit.id]
        assert Decimal(next_tariff_data['prev_consume_value']) == Decimal('1500000')
        assert Decimal(next_tariff_data['consume_discount']) == Decimal('752055')  # rounded 1500000/365*182

    def test_first_consume_with_discount_applied_but_later_wihout_one(self, unit_being_tested_factory):
        unit = unit_being_tested_factory(product_value='2000000')
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'last_run': datetime(2018, 10, 15, tzinfo=pytz.utc),
            'now': datetime(2018, 10, 16, 0, 0, tzinfo=pytz.utc),
            'products': {
                '508206': {
                    'consumed': '0',
                    'credited': '2000000',
                    unit.id: {
                        'prev_consume_value': '1500000',
                        'consume_discount': '747945.21',
                    }
                }
            },
        }

        unit({}, state)
        assert Decimal(state['products']['508206']['consumed']) == Decimal('1252054.79')
        next_tariff_data = state['next_tariff_state']['products']['508206'][unit.id]
        assert Decimal(next_tariff_data['prev_consume_value']) == Decimal('2000000')
        assert Decimal(next_tariff_data['consume_discount']) == Decimal('2000000')
        # assert Decimal(next_tariff_data['consume_discount']) == Decimal('1994520.55')  # one day already used

        state['products']['508206']['credited'] = '4000000'
        state['now'] = datetime(2019, 10, 16, 0, 0, tzinfo=pytz.utc)
        unit({}, state)
        assert Decimal(state['products']['508206']['consumed']) == Decimal('3252054.79')
        next_tariff_data = state['next_tariff_state']['products']['508206'][unit.id]
        assert Decimal(next_tariff_data['prev_consume_value']) == Decimal('2000000')

    def test_first_consume_without_discount_applied(self, unit_being_tested_factory):
        unit = unit_being_tested_factory(product_value='1000000')
        state = {
            'is_active': True,
            'activated_date': datetime(2018, 4, 16, tzinfo=pytz.utc),
            'last_run': datetime(2018, 10, 15, tzinfo=pytz.utc),
            'now': datetime(2018, 10, 16, 0, 0, tzinfo=pytz.utc),
            'products': {
                '508206': {
                    'consumed': '0',
                    'credited': '1000000',
                    unit.id: {
                        'prev_consume_value': '1500000',
                        'consume_discount': '747945.21',
                    }
                }
            },
        }

        unit({}, state)
        assert Decimal(state['products']['508206']['consumed']) == Decimal('1000000')
        next_tariff_data = state['next_tariff_state']['products']['508206'][unit.id]
        assert Decimal(next_tariff_data['prev_consume_value']) == Decimal('1000000')
        assert Decimal(next_tariff_data['consume_discount']) == Decimal('1000000')

    def test_usage_of_cron_quarter_planner_leads_to_incorrect_discount_calculation(self):
        state = {
            "is_active": True,
            "activated_date": datetime(2020, 5, 26, 10, 4, 35, tzinfo=pytz.utc),
            "last_run": datetime(2020, 5, 26, 21, 1, 14, tzinfo=pytz.utc),
            'now': datetime(2020, 5, 27, 3, 30, 0, tzinfo=pytz.utc),
            "products": {
                "510973": {
                    "consumed": "0",
                    "credited": "23513",
                    "autocharge_personal_account": True,
                    "credited_deficit": "23513",
                    "next_consume_date": datetime(2020, 8, 25, 21, 0, 0, tzinfo=pytz.utc),
                    "next_consume_value": "23513"
                }
            }
        }
        unit = PrepayPeriodicallyDiscountedUnit(tariffication=None, **{
            "period_mask": "0 0 x x/3 * *", "time_zone": "Europe/Moscow",
            "product_id": "510973", "product_value": "23513", "autocharge_personal_account": True,
            "ban_reason": 169, "unban_reason": 170, "scope": "vrp_threemonths_2020"
        })
        unit({}, state)
        next_tariff_data = state['next_tariff_state']['products']['510973'][unit.id]
        assert Decimal(next_tariff_data['consume_discount']) == Decimal('7809')

    def test_relativedelta_quarter_planner_calculates_discount_correctly(self):
        state = {
            "is_active": True,
            "activated_date": datetime(2020, 5, 26, 10, 4, 35, tzinfo=pytz.utc),
            "last_run": datetime(2020, 5, 26, 21, 1, 14, tzinfo=pytz.utc),
            'now': datetime(2020, 5, 27, 3, 30, 0, tzinfo=pytz.utc),
            "products": {
                "510973": {
                    "consumed": "0",
                    "credited": "23513",
                    "autocharge_personal_account": True,
                    "credited_deficit": "23513",
                    "next_consume_date": datetime(2020, 8, 25, 21, 0, 0, tzinfo=pytz.utc),
                    "next_consume_value": "23513"
                }
            }
        }

        unit = PrepayPeriodicallyDiscountedUnit(tariffication=None, **{
            "period_delta": {'months': 3, 'hour': 0, 'minute': 0}, "time_zone": "Europe/Moscow",
            "product_id": "510973", "product_value": "23513", "autocharge_personal_account": True,
            "ban_reason": 169, "unban_reason": 170, "scope": "vrp_threemonths_2020"
        })
        unit({}, state)
        next_tariff_data = state['next_tariff_state']['products']['510973'][unit.id]
        assert Decimal(next_tariff_data['consume_discount']) == Decimal('23257')

    def test_overdraft_limit(self, unit_being_tested_factory):
        unit = PrepayPeriodicallyDiscountedUnit(tariffication=None, **{
            "period_mask": "0 0 x x * *", "truncate_period_mask": "0 0 * * * *", "time_zone": "+0300",
            "product_id": "508206", "product_value": "10",
            "autocharge_personal_account": True, "overdraft_personal_account": True,
            "overdraft_limit": "10",
            "ban_reason": 1, "unban_reason": 18
        })

        state = {
            'is_active': True,
            'activated_date': datetime(2021, 8, 25, tzinfo=pytz.utc),
            'last_run': datetime(2021, 8, 25, tzinfo=pytz.utc),
            'now': datetime(2021, 8, 26, tzinfo=pytz.utc),
            'products': {
                '508206': {
                    'consumed': '0',
                    'credited': '10'
                }
            },
        }

        unit({}, state)
        assert state['products']['508206'] == {
            'consumed': '10',
            'credited': '10',
            'autocharge_personal_account': True,
            'overdraft_personal_account': True,
            'overdraft_limit': '10',
            'next_consume_date': datetime(2022, 8, 25, 21, 0, 0, tzinfo=pytz.utc),
            'next_consume_value': '10'
        }
        assert state['ban_units'] == {}
        assert state.get('ban', False) is False

    def test_overdraft_limit_fail(self, unit_being_tested_factory):
        unit = PrepayPeriodicallyDiscountedUnit(tariffication=None, **{
            "period_mask": "0 0 x x * *", "truncate_period_mask": "0 0 * * * *", "time_zone": "+0300",
            "product_id": "508206", "product_value": "20",
            "autocharge_personal_account": True, "overdraft_personal_account": True,
            "overdraft_limit": "9",
            "ban_reason": 1, "unban_reason": 18
        })

        state = {
            'is_active': True,
            'activated_date': datetime(2021, 8, 25, tzinfo=pytz.utc),
            'last_run': datetime(2021, 8, 25, tzinfo=pytz.utc),
            'now': datetime(2021, 8, 26, tzinfo=pytz.utc),
            'products': {
                '508206': {
                    'consumed': '0',
                    'credited': '10'
                }
            },
        }

        unit({}, state)
        assert state['products']['508206'] == {
            'consumed': '0',
            'credited': '10',
            'autocharge_personal_account': True,
            'overdraft_personal_account': True,
            'overdraft_limit': '9',
            'credited_deficit': '10'  # 'credited_deficit' doesn't take 'overdraft_limit' into account
        }
        assert state['ban_units'] == {'PrepayPeriodicallyDiscountedUnit': True}
        assert state['ban'] is True
        assert state['ban_reason'] == 1


class TestPrepayPeriodicallyForgiveIncompleteUnit:
    product_id = "510973"

    @pytest.fixture()
    def unit_being_tested_factory(self):
        return lambda product_value, postpone_days: PrepayPeriodicallyForgiveIncompleteUnit(tariffication=None, **{
            'period_mask': '0 0 22 7 * *', 'time_zone': '+0300',
            'product_id': self.product_id, 'product_value': product_value,
            'trial_period_days': 10, 'postpone_consume_days': postpone_days,
            "ban_reason": 1, "unban_reason": 18
        })

    @pytest.mark.parametrize("now_date,expected_forgive",
                             [("2021-07-22", False),
                              ("2021-07-22T11:40:00", False),
                              ("2021-07-22T22:40:00", False),
                              ("2021-07-23", True),
                              ("2021-07-24", True),
                              ("2022-07-21", True)])
    def test_first_consume_incomplete_period(self, unit_being_tested_factory, now_date, expected_forgive):
        product_value = '10'
        unit = unit_being_tested_factory(product_value=product_value, postpone_days='0')
        if len(now_date) == 10:
            now_date = datetime.strptime(now_date, "%Y-%m-%d").replace(tzinfo=pytz.timezone("Europe/Moscow"))
        else:
            now_date = datetime.strptime(now_date, "%Y-%m-%dT%H:%M:%S").replace(tzinfo=pytz.timezone("Europe/Moscow"))
        state = {
            'is_active': True,
            'now': now_date,
            'products': {
                self.product_id: {
                    'consumed': '0',
                    'credited': product_value
                }
            }
        }

        unit({}, state)
        next_year = now_date.year if now_date.day < 22 else now_date.year + 1
        assert state['products'] == {
            self.product_id: {
                'consumed': '0' if expected_forgive else product_value,
                'credited': product_value,
                'next_consume_date': datetime(next_year, 7, 21, 21, 0, tzinfo=pytz.utc),
                'next_consume_value': product_value
            }
        }
        assert state['ban_units'] == {}

    def test_consume_incomplete_period(self, unit_being_tested_factory):
        product_value = '10'
        unit = unit_being_tested_factory(product_value=product_value, postpone_days='0')

        start_date = "2021-07-20"
        start_date = datetime.strptime(start_date, "%Y-%m-%d").replace(tzinfo=pytz.timezone("Europe/Moscow"))
        state = {
            'is_active': True,
            'now': start_date,
            'products': {
                self.product_id: {
                    'consumed': '0',
                    'credited': product_value
                }
            }
        }
        prev_state = {}

        for day in range(5):
            now_date = start_date + timedelta(days=day)
            state['now'] = now_date
            unit(prev_state, state)

            next_year = now_date.year if now_date.day < 22 else now_date.year + 1
            consumed = 0 if now_date.day < 22 else product_value
            assert state['products'] == {
                self.product_id: {
                    'consumed': str(consumed),
                    'credited': product_value,
                    'next_consume_date': datetime(next_year, 7, 21, 21, 0, tzinfo=pytz.utc),
                    'next_consume_value': product_value
                }
            }

            prev_state = state

    def test_consume_banned_client(self, unit_being_tested_factory):
        product_value = '10'
        unit = unit_being_tested_factory(product_value=product_value, postpone_days='0')

        now_date = "2021-07-24"
        now_date = datetime.strptime(now_date, "%Y-%m-%d").replace(tzinfo=pytz.timezone("Europe/Moscow"))
        state = {
            'is_active': True,
            'now': now_date,
            'products': {
                self.product_id: {
                    'consumed': '0',
                    'credited': '0'
                }
            },
            "ban": True
        }

        unit({}, state)
        assert state['products'] == {
            self.product_id: {
                'consumed': '0',
                'credited': '0',
                'credited_deficit': product_value
            }
        }
        assert state['ban']
        assert state['ban_units'] != {}

    def test_second_consume_after_postpone(self, unit_being_tested_factory):
        """
        (!) Copy from TestPrepayPeriodicallyWithTrialUnit
        Consume was postponed for several days and today 'credited' is enought to
        consume and set new 'next_consume_date'
        """
        unit = unit_being_tested_factory(product_value='100', postpone_days='10')
        state = {
            'is_active': True,
            'activated_date': datetime(2022, 1, 17, tzinfo=pytz.utc),
            'now': datetime(2022, 7, 22, tzinfo=pytz.utc),
            'products': {
                self.product_id: {
                    'consumed': '100',
                    'credited': '200',
                    'next_consume_value': '100',
                    'next_consume_date': datetime(2022, 7, 21, 21, 0, tzinfo=pytz.utc),
                }
            },
        }

        unit({}, state)
        assert state['products'][self.product_id] == {
            'consumed': '200',
            'credited': '200',
            'next_consume_value': '100',
            'next_consume_date': datetime(2023, 7, 21, 21, 0, tzinfo=pytz.utc),
            'postpone_payment_until': datetime(2023, 7, 31, 21, 0, tzinfo=pytz.utc),
        }

    @pytest.mark.parametrize("postpone_days,is_banned",
                             [("0", True),
                              ("10", False)])
    def test_postpone_consume(self, unit_being_tested_factory, postpone_days, is_banned):
        """
        (!) Copy from TestPrepayPeriodicallyWithTrialUnit
        """
        unit = unit_being_tested_factory(product_value='100', postpone_days=postpone_days)
        state = {
            'is_active': True,
            'activated_date': datetime(2022, 1, 17, tzinfo=pytz.utc),
            'now': datetime(2022, 2, 27, tzinfo=pytz.utc),
            'products': {
                self.product_id: {
                    'consumed': '100',
                    'credited': '100',
                    'next_consume_value': '100',
                    'next_consume_date': datetime(2022, 2, 26, 21, 0, tzinfo=pytz.utc),
                    'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc)
                }
            }
        }

        unit({}, state)
        expected = {
            'consumed': '100',
            'credited': '100',
            'next_consume_value': '100',
            'next_consume_date': datetime(2022, 2, 26, 21, 0, tzinfo=pytz.utc),
            'trial_period_until': datetime(2022, 1, 27, 0, 0, tzinfo=pytz.utc),
        }
        if postpone_days == "10":
            expected['postpone_payment_until'] = datetime(2022, 3, 8, 21, 0, tzinfo=pytz.utc)
        expected['credited_deficit'] = '100'
        assert state['products'][self.product_id] == expected
        if is_banned:
            assert state['ban_units'] == {unit.id: True}
            assert state['ban']
            assert state['ban_reason'] == 1
        else:
            assert state.get('ban_units', {}) == {}
            assert not state.get('ban', False)

    def test_can_consume_with_postpone(self, unit_being_tested_factory):
        """
        (!) Copy from TestPrepayPeriodicallyWithTrialUnit
        """
        postpone_days = 10
        unit = unit_being_tested_factory(product_value='100', postpone_days=postpone_days)
        state = {
            'is_active': True,
            'activated_date': datetime(2022, 1, 17, tzinfo=pytz.utc),
            'now': datetime(2022, 7, 22, tzinfo=pytz.utc),
            'products': {
                self.product_id: {
                    'consumed': '100',
                    'credited': '200',
                    'next_consume_value': '100',
                    'next_consume_date': datetime(2022, 7, 21, 21, 0, tzinfo=pytz.utc),
                }
            }
        }

        unit({}, state)
        expected = {
            'consumed': '200',
            'credited': '200',
            'next_consume_value': '100',
            'next_consume_date': datetime(2023, 7, 21, 21, 0, tzinfo=pytz.utc),
            'postpone_payment_until': datetime(2023, 7, 31, 21, 0, tzinfo=pytz.utc),
        }
        assert state['products'][self.product_id] == expected

    def test_consume_multiple_units(self):
        """
        Check state for two products with different consume dates
        """
        product_id1 = "510973"
        product_id2 = "510974"
        product_value1 = "10"
        product_value2 = "10"
        unit1 = PrepayPeriodicallyForgiveIncompleteUnit(tariffication=None, **{
            "period_mask": "0 0 22 7 * *", "time_zone": "Europe/Moscow",
            "product_id": product_id1, "product_value": int(product_value2),
            "ban_reason": 169, "unban_reason": 170
        })
        unit2 = PrepayPeriodicallyForgiveIncompleteUnit(tariffication=None, **{
            "period_mask": "0 0 24 7 * *", "time_zone": "Europe/Moscow",
            "product_id": product_id2, "product_value": int(product_value2),
            "ban_reason": 169, "unban_reason": 170
        })

        start_date = "2021-07-21"
        start_date = datetime.strptime(start_date, "%Y-%m-%d").replace(tzinfo=pytz.timezone("Europe/Moscow"))
        state = {
            'is_active': True,
            'now': start_date,
            'products': {
                product_id1: {
                    'consumed': '0',
                    'credited': product_value1
                },
                product_id2: {
                    'consumed': '0',
                    'credited': product_value2
                }
            }
        }
        prev_state = {}

        for day in range(5):
            now_date = start_date + timedelta(days=day)
            state['now'] = now_date
            unit1(prev_state, state)
            unit2(prev_state, state)

            next_year1 = now_date.year if now_date.day < 22 else now_date.year + 1
            next_year2 = now_date.year if now_date.day < 24 else now_date.year + 1
            consumed1 = 0 if now_date.day < 22 else product_value1
            consumed2 = 0 if now_date.day < 24 else product_value2
            assert state['products'] == {
                product_id1: {
                    'consumed': str(consumed1),
                    'credited': product_value1,
                    'next_consume_date': datetime(next_year1, 7, 21, 21, 0, tzinfo=pytz.utc),
                    'next_consume_value': product_value1
                },
                product_id2: {
                    'consumed': str(consumed2),
                    'credited': product_value2,
                    'next_consume_date': datetime(next_year2, 7, 23, 21, 0, tzinfo=pytz.utc),
                    'next_consume_value': product_value2
                }
            }
            prev_state = state


class TestBanLinkPeriodicallyReachingLimitUnit:
    subscription_product_id = '509584'

    @staticmethod
    def _get_datetime(date_str):
        return pytz.utc.normalize(pytz.timezone('Europe/Moscow').localize(datetime.strptime(date_str, "%Y-%m-%d")))

    @pytest.fixture()
    def tariffication(self, fake_statistic_getter):
        return Tariffication([], fake_statistic_getter({
            datetime(2022, 6, 25, tzinfo=pytz.utc): {'hits': 10},
            datetime(2022, 7, 1, tzinfo=pytz.utc): {'hits': 10},
            datetime(2022, 7, 10, tzinfo=pytz.utc): {'hits': 1},
        }))

    @pytest.fixture()
    def unit(self, tariffication):
        return BanLinkPeriodicallyReachingLimitUnit(tariffication=tariffication, **{
            'period_delta': {'months': 1, 'month': 'x', 'day': 'x'}, 'time_zone': 'Europe/Moscow',
            'product_id': None,
            'subscription_product_id': self.subscription_product_id, 'limit': '20',
            'statistic_aggregator': 'hits', 'limit_statistic_aggregator': 'hits',
            'ban_reason': 122, 'unban_reason': 123
        })

    def test_not_paid_tarifficator_state_not_affected(self, unit):
        prev_state = {
            'is_active': True,
            'now': self._get_datetime("2022-06-21"),
            'products': {
                self.subscription_product_id: {
                    'consumed': '0',
                    'credited': '0'
                },
            }
        }
        state = prev_state.copy()
        unit(prev_state, state)
        assert prev_state == state

    def test_add_to_tarifficator_state_with_no_consume_empty_ban_units(self, unit):
        prev_state = {
            'is_active': True,
            'now': self._get_datetime("2022-06-21"),
            'products': {
                self.subscription_product_id: {
                    'consumed': '100',
                    'credited': '100',
                    'next_consume_date': self._get_datetime("2022-07-21")
                },
            }
        }
        state = prev_state.copy()
        unit(prev_state, state)
        assert state['ban_units'] == {}
        assert state['tariff_limits'] == {unit.id: {
            'limit': '20', 'from_date': self._get_datetime("2022-06-21"), 'statistic_aggregator': 'hits'
        }}

    def test_add_to_tarifficator_state_with_little_consume_empty_ban_units(self, unit):
        prev_state = {
            'is_active': True,
            'now': self._get_datetime("2022-07-01"),
            'products': {
                self.subscription_product_id: {
                    'consumed': '100',
                    'credited': '100',
                    'next_consume_date': self._get_datetime("2022-07-21")
                },
            }
        }
        state = prev_state.copy()
        unit(prev_state, state)
        assert state['ban_units'] == {}
        assert state['tariff_limits'] == {unit.id: {
            'limit': '20', 'from_date': self._get_datetime("2022-06-21"), 'statistic_aggregator': 'hits'
        }}

    def test_mark_tarifficator_state_banned(self, unit):
        prev_state = {
            'is_active': True,
            'now': self._get_datetime("2022-07-11"),
            'products': {
                self.subscription_product_id: {
                    'consumed': '100',
                    'credited': '100',
                    'next_consume_date': self._get_datetime("2022-07-21")
                },
            }
        }
        state = prev_state.copy()
        unit(prev_state, state)
        assert state['ban_units'] == {unit.id: True}
        assert state['ban']
        assert state['ban_reason'] == 122
        assert state['tariff_limits'] == {}
