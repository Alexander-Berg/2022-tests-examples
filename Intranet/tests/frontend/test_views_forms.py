# coding: utf-8

from __future__ import unicode_literals

from datetime import datetime
import pytz

import pytest


SOME_DATETIME = datetime(2018, 3, 14, 12, 0, 0, 0, tzinfo=pytz.utc)
SOME_DATETIME_STR = '2018-03-14T12:00Z'


# TODO: вынести формы из вьюх и тестить отдельно
@pytest.fixture
def combination():
    """
    Это костыль. В этом модуле с тестами формы из вьюх тестятся
    напрямую, что приводит к импорту get_user_ticket
    в незапатченном виде.
    Поэтому импортим вьюху прямо внутри теста.
    """
    from easymeeting.frontend.views import combination
    return combination


@pytest.mark.parametrize(
    'input_str,expected', [
        (
            '2018-03-14T12:00Z',
            SOME_DATETIME,
        ),
    ]
)
def test_post_combinations_form_dates(combination, input_str, expected):
    form_cls = combination.CombinationForm
    form = form_cls(
        data={
            'dateFrom': input_str,
            'dateTo': input_str,
            'participants': [
                {'login': 'volozh'},
            ]
        }
    )
    assert form.is_valid(), form.errors

    cleaned_data = form.cleaned_data
    assert cleaned_data['dateFrom'] == expected
    assert cleaned_data['dateTo'] == expected


@pytest.mark.parametrize(
    'participants', [
        [],
        [{}],
    ]
)
def test_post_combinations_form_participants_invalid(combination, participants):
    form_cls = combination.CombinationForm
    form = form_cls(
        data={
            'dateFrom': SOME_DATETIME_STR,
            'dateTo': SOME_DATETIME_STR,
            'participants': participants,
        }
    )
    assert not form.is_valid()
    assert 'participants' in form.errors


@pytest.mark.parametrize(
    'participants,expected', [
        (
            [{'login': 'volozh'}],
            [{'login': 'volozh', 'officeId': None}],
        ),
        (
            [{'login': 'volozh', 'officeId': '10'}],
            [{'login': 'volozh', 'officeId': 10}],
        ),
        (
            [{'officeId': '10'}],
            [{'login': '', 'officeId': 10}],
        ),
    ]
)
def test_post_combinations_form_participants_valid(combination, participants, expected):
    form_cls = combination.CombinationForm
    form = form_cls(
        data={
            'dateFrom': SOME_DATETIME_STR,
            'dateTo': SOME_DATETIME_STR,
            'participants': participants,
        }
    )
    assert form.is_valid()
    assert 'dateFrom' in form.cleaned_data
    assert 'dateTo' in form.cleaned_data
    assert form.cleaned_data['participants'] == expected
