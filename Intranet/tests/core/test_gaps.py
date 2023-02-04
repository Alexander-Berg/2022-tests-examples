# coding: utf-8

from __future__ import unicode_literals

import datetime
import pytz
import pytest

from easymeeting.core import gaps

from tests import helpers


@pytest.mark.vcr
def test_get_gaps():
    gaps_list = gaps.get_gaps(
        logins=[
            'imperator',
            'volozh'
        ],
        date_from=datetime.date(2018, 2, 20),
        date_to=datetime.date(2018, 2, 20),
    )

    helpers.assert_is_substructure(
        [
            {
                'person_login': 'imperator',
                'to_notify': [],
                'full_day': True,
            },
        ],
        gaps_list,
    )


@pytest.mark.vcr
def test_get_persons_gap_by_logins():
    persons_gap_by_logins = gaps.get_persons_gap_by_logins(
        logins=['mokhov', 'zhigalov'],
        date_from=datetime.date(2018, 3, 14),
        date_to=datetime.date(2018, 3, 15),
    )
    helpers.assert_is_substructure(
        {
            'mokhov': [],
            'zhigalov': [
                {
                    'person_login': 'zhigalov',
                    'to_notify': [],
                    'date_from': datetime.datetime(2018, 3, 14, 0, 0, tzinfo=pytz.utc),
                    'date_to': datetime.datetime(2018, 3, 15, 0, 0, tzinfo=pytz.utc),
                    'full_day': True,
                    'work_in_absence': True,
                    'workflow': 'absence',
                },
            ],
        },
        persons_gap_by_logins,
    )
