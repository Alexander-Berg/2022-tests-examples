# coding: utf-8

from __future__ import unicode_literals

import json
from decimal import Decimal

from tests.data import review_data

from cab import const
from cab.sources import review

import pytest


PRECISION = 10 ** -4


def test_merge_uniform_options_keys():
    merged = review.merge_uniform_options(review_data.OPTIONS_ONE)
    assert merged.keys() == ['N1R']


def test_merge_uniform_options_amount():
    merged = review.merge_uniform_options(review_data.OPTIONS_ONE)
    merged = merged.values()[0]
    assert merged['amount'] == sum(
        grant['grantAmount'] for grant in review_data.OPTIONS_ONE
    )


def test_merge_uniform_options_vesting_amount():
    merged = review.merge_uniform_options(review_data.OPTIONS_ONE)
    merged = merged.values()[0]
    assert merged['amount'] == sum(
        vesting_event['amount'] for vesting_event in
        merged['vesting']
    )


def test_merge_options_with_money_amount():
    merged = review.merge_options_with_money(review_data.OPTIONS_ONE)
    n1r_merged = merged['N1R']
    expected_amount = sum(
        grant_event['grantAmount'] for grant_event in review_data.OPTIONS_ONE
    )
    merged_amount = sum(
        event['amount'] for event in n1r_merged
    )
    assert expected_amount == merged_amount


@pytest.mark.parametrize(
    'currency', const.KNOWN_CURRENCIES
)
def test_merge_options_with_money_cost(currency):
    merged = review.merge_options_with_money(review_data.OPTIONS_ONE, currency)
    n1r_merged = merged['N1R']
    expected_sum = sum(
        independent_vesting_sum(grant_event['vesting'], currency)
        for grant_event in review_data.OPTIONS_ONE
    )
    merged_sum = sum(
        event['value'] for event in n1r_merged
    )
    assert Decimal(expected_sum) - merged_sum < PRECISION


def independent_vesting_sum(vesting, currency):
    value_rub = value_usd = 0
    for event in vesting:
        exchange_rate = event['exchangeRate'] or 0
        share_cost = event['shareCost'] or 0
        value_usd += share_cost * event['vestAmount']
        value_rub += exchange_rate * share_cost * event['vestAmount']
    return {
        const.RUB_CURRENCY: value_rub,
        const.USD_CURRENCY: value_usd,
    }[currency]


def test_latest_currency_change_not_found():
    oebs_data = json.loads(review_data.FINANCE_EXAMPLE_ONE)
    result = review.get_latest_salary_or_bonus_currency_change(oebs_data)
    assert result is None


def test_latest_currency_change_found():
    oebs_data = review_data.FINANCE_EXAMPLE_CHANGED_CURRENCIES
    oebs_data = json.loads(oebs_data)
    result = review.get_latest_salary_or_bonus_currency_change(oebs_data)

    bonus_date = None
    previous_item = None
    for item in oebs_data['bonus_history']:
        if previous_item and previous_item['currency'] != item['currency']:
            bonus_date = item['effectiveDate']
        previous_item = item

    salary_date = None
    previous_item = None
    for item in oebs_data['salary_history']:
        if previous_item and previous_item['currency'] != item['currency']:
            salary_date = item['dateFrom']
        previous_item = item

    assert result.isoformat() == max(bonus_date, salary_date)


def test_latest_currency_change_cab_301():
    oebs_data = review_data.FINANCE_EXAMPLE_THREE
    oebs_data = json.loads(oebs_data)
    result = review.get_latest_salary_or_bonus_currency_change(oebs_data)

    assert result.isoformat() == '2015-04-07'
