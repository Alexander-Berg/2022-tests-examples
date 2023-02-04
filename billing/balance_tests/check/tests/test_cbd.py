# -*- coding: utf-8 -*-

import pytest
from six.moves import range
from dateutil.relativedelta import relativedelta

from btestlib import utils
from check import shared_steps
from check.shared import CheckSharedBefore

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)
END_DT = START_DT + relativedelta(months=1)

DEFAULT_CACHE_VARS = ['start_dt', 'end_dt']
COMPLETIONS_VARS = ['billing_completions', 'yt_completions']


class States(object):
    MISSING_IN_DRIVE = 1
    MISSING_IN_BILLING = 2
    AMOUNT_MISMATCH = 3


def rd(days):
    return START_DT + relativedelta(days=days - 1)


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CBD)
def test_cbd_success(shared_data, switch_to_pg):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=DEFAULT_CACHE_VARS + COMPLETIONS_VARS) as before:
        before.validate()

        # TODO: вынести в инициализацию модуля
        start_dt = START_DT
        end_dt = END_DT

        billing_completions = [
            {'amount': '199.16', 'dt': rd(1)},
            {'amount': '0.84', 'dt': rd(1)},
            {'amount': '66.6', 'dt': rd(2)},
            {'amount': '10', 'dt': rd(3)},
            # Возможен допуск на копейку
            {'amount': '12.34', 'dt': rd(4)},
        ]

        yt_completions = [
            {'amount': '0.84', 'dt': rd(1)},
            {'amount': '10', 'dt': rd(3)},
            {'amount': '199.16', 'dt': rd(1)},
            {'amount': '100', 'promocode_sum': '33.4', 'dt': rd(2)},
            # Возможен допуск на копейку
            {'amount': '12.35', 'dt': rd(4)},
        ]

    cmp_data = shared_steps.SharedBlocks.run_cbd(
        shared_data, before, pytest.active_tests)
    if cmp_data is None:
        cmp_data = shared_data.cache.get('cmp_data')

    days = set(d['day'] for d in cmp_data)
    not_expected = set(rd(x) for x in range(1, 5))
    assert days.intersection(not_expected) == set()


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CBD)
def test_cbd_amount_mismatch(shared_data, switch_to_pg):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=DEFAULT_CACHE_VARS + COMPLETIONS_VARS) as before:
        before.validate()

        # TODO: вынести в инициализацию модуля
        start_dt = START_DT
        end_dt = END_DT

        billing_completions = [
            {'amount': '199.16', 'dt': rd(5)},
            {'amount': '10', 'dt': rd(6)},
            {'amount': '12.34', 'dt': rd(7)},
        ]

        yt_completions = [
            {'amount': '200.16', 'dt': rd(5)},
            {'amount': '10', 'dt': rd(6)},
            {'amount': '12.36', 'dt': rd(7)},
        ]

    cmp_data = shared_steps.SharedBlocks.run_cbd(
        shared_data, before, pytest.active_tests)
    if cmp_data is None:
        cmp_data = shared_data.cache.get('cmp_data')

    def format_tuple(d):
        return (
            d.get('billing_amount'),
            d.get('drive_amount'),
            d.get('state'),
        )

    answer = [format_tuple(d) for d in cmp_data]
    assert ('199.16', '200.16', States.AMOUNT_MISMATCH) in answer
    assert ('12.34', '12.36', States.AMOUNT_MISMATCH) in answer


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CBD)
def test_cbd_missing_in_drive(shared_data, switch_to_pg):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=DEFAULT_CACHE_VARS + ['billing_completions']) as before:
        before.validate()

        # TODO: вынести в инициализацию модуля
        start_dt = START_DT
        end_dt = END_DT

        billing_completions = [
            {'amount': '125.1', 'dt': rd(8)},
            {'amount': '125.2', 'dt': rd(9)},
        ]

    cmp_data = shared_steps.SharedBlocks.run_cbd(
        shared_data, before, pytest.active_tests)
    if cmp_data is None:
        cmp_data = shared_data.cache.get('cmp_data')

    def format_tuple(d):
        return (
            d.get('day'),
            d.get('billing_amount'),
            d.get('state'),
        )

    answer = [format_tuple(d) for d in cmp_data]
    assert (rd(8), '125.1', States.MISSING_IN_DRIVE) in answer
    assert (rd(9), '125.2', States.MISSING_IN_DRIVE) in answer


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CBD)
def test_cbd_missing_in_billing(shared_data, switch_to_pg):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=DEFAULT_CACHE_VARS + ['yt_completions']) as before:
        before.validate()

        # TODO: вынести в инициализацию модуля
        start_dt = START_DT
        end_dt = END_DT

        yt_completions = [
            {'amount': '521.1', 'dt': rd(10)},
            {'amount': '521.2', 'dt': rd(11)},
        ]

    cmp_data = shared_steps.SharedBlocks.run_cbd(
        shared_data, before, pytest.active_tests)
    if cmp_data is None:
        cmp_data = shared_data.cache.get('cmp_data')

    def format_tuple(d):
        return (
            d.get('day'),
            d.get('drive_amount'),
            d.get('state'),
        )

    answer = [format_tuple(d) for d in cmp_data]
    assert (rd(10), '521.1', States.MISSING_IN_BILLING) in answer
    assert (rd(11), '521.2', States.MISSING_IN_BILLING) in answer

# vim:ts=4:sts=4:sw=4:tw=79:et:
