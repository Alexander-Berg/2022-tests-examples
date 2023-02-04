import pytest

import random
from datetime import date
from typing import Optional

from staff.lib.testing import get_random_date

from staff.oebs.controllers.rolluppers.reward_rollupper import RewardMapper


def test_reward_mapper():
    source_object = {
        'name': f'name-{random.random()}',
        'description': f'description-{random.random()}',
        'start_date': get_random_date(),
        'end_date': get_random_date(),
    }

    target = RewardMapper(source_object)

    result = dict(target)
    for field in source_object.keys():
        assert result[field] == source_object[field]


@pytest.mark.parametrize(
    'category, expected_category',
    [
        (None, None),
        ('', None),
        ('test', None),
        ('Mass positions', 'Mass positions'),
        ('Business support', 'Business support'),
        ('Professionals', 'Professionals'),
    ],
)
def test_reward_mapper_category(category: Optional[str], expected_category: Optional[str]):
    target = RewardMapper({'category': category})

    result = dict(target)
    assert result['category'] == expected_category


@pytest.mark.parametrize(
    'end_date, expected_intranet_status',
    [
        (None, 1),
        (date(1990, 1, 1), 0),
        (date(2990, 1, 1), 1),
    ],
)
def test_reward_mapper_intranet_status(end_date: Optional[date], expected_intranet_status: int):
    target = RewardMapper({'end_date': end_date})

    result = dict(target)
    assert result['intranet_status'] == expected_intranet_status
