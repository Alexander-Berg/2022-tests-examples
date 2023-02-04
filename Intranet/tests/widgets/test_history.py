# coding: utf-8

from __future__ import unicode_literals

import pytest

from cab.widgets import history


REVIEW_EVENT_OCTOBER = {
    'type': 'review',
    'date': '2020-10-15',
}

BONUS_EVENT_SEPTEMBER = {
    'type': 'bonus',
    'date': '2020-09-25'
}

BONUS_EVENT_OCTOBER = {
    'type': 'bonus',
    'date': '2020-10-25'
}

BONUS_EVENT_MAY = {
    'type': 'bonus',
    'date': '2020-05-01'
}


@pytest.mark.parametrize(
    'event, review_event',
    [
        (BONUS_EVENT_OCTOBER, REVIEW_EVENT_OCTOBER),
        (BONUS_EVENT_SEPTEMBER, REVIEW_EVENT_OCTOBER),
    ]
)
def test_bonus_is_relevant_to_review(event, review_event):
    assert history.is_relevant_to_review(event, review_event)


@pytest.mark.parametrize(
    'event, review_event',
    [
        (BONUS_EVENT_MAY, REVIEW_EVENT_OCTOBER),
    ]
)
def test_bonus_is_not_relevant_to_review(event, review_event):
    assert not history.is_relevant_to_review(event, review_event)
