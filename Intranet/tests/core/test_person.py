# coding: utf-8

from __future__ import unicode_literals

import pytest

from easymeeting.core import person


@pytest.mark.vcr
def test_get_persons_by_logins():
    logins = ['mokhov', 'sibirev', 'kiparis']
    actual = person.get_person_by_login_dict(logins)
    expected = {
        'mokhov': {
            'login': 'mokhov',
            'location': {'office': {'id': 2}},
            'official': {'is_dismissed': False},
        },
        'sibirev': {
            'login': 'sibirev',
            'location': {'office': {'id': 2}},
            'official': {'is_dismissed': False},
        },
        'kiparis': {
            'login': 'kiparis',
            'location': {'office': {'id': 1}},
            'official': {'is_dismissed': False},
        }
    }
    assert actual == expected
