# -*- coding: utf-8 -*-

import pytest

from balance.actions.acts.account import ActCreatorFilter


@pytest.mark.parametrize(
    'firm_id, service_ids, act_type, answer',
    [
        (1, [1], 'daily', False),
        (3, [3], 'daily', False),
        (3, [2, 3], 'monthly', False),
        (4, [1, 2, 3], 'daily', True),
    ],
)
def test_firm_ids(firm_id, service_ids, act_type, answer):
    act_creation_filter = [
        {'firm_ids': [1, 2], 'act_types': ['daily', 'monthly']},
        {'firm_ids': 3, 'service_ids': None}
    ]
    act_filter = ActCreatorFilter(act_creation_filter)
    assert act_filter.need2act(firm_id=firm_id, service_ids=service_ids, act_type=act_type) is answer


@pytest.mark.parametrize(
    'firm_id, service_ids, act_type, answer',
    [
        (1, [1], 'daily', False),
        (1, [3], 'monthly', False),
        (1, [2, 3], 'daily', False),
        (1, [4], 'monthly', True),
        (1, [1, 4], 'daily', False),
    ],
)
def test_service_ids(firm_id, service_ids, act_type, answer):
    act_creation_filter = [
        {'service_ids': [1, 2], 'act_types': None},
        {'service_ids': 3, 'firm_ids': None}
    ]
    act_filter = ActCreatorFilter(act_creation_filter)
    assert act_filter.need2act(firm_id=firm_id, service_ids=service_ids, act_type=act_type) is answer


@pytest.mark.parametrize(
    'firm_id, service_ids, act_type, answer',
    [
        (1, [3], 'daily', False),
        (1, [3], 'monthly', True),

        (2, [3], 'daily', True),
        (2, [3], 'monthly', False),

        (3, [1], 'daily', False),
        (3, [1], 'monthly', True),

        (3, [2], 'daily', True),
        (3, [2], 'monthly', False),
    ],
)
def test_act_types(firm_id, service_ids, act_type, answer):
    act_creation_filter = [
        {'firm_ids': 1, 'act_types': 'daily'},
        {'firm_ids': 2, 'act_types': 'monthly'},
        {'service_ids': 1, 'act_types': 'daily'},
        {'service_ids': 2, 'act_types': 'monthly'}
    ]
    act_filter = ActCreatorFilter(act_creation_filter)
    assert act_filter.need2act(firm_id=firm_id, service_ids=service_ids, act_type=act_type) is answer


@pytest.mark.parametrize(
    'firm_id, service_ids, act_type, answer',
    [
        (1, [1], 'daily', False),
        (2, [1], 'monthly', True),
        (3, [1], 'daily', False),
    ],
)
def test_exclude_firm_ids(firm_id, service_ids, act_type, answer):
    act_creation_filter = [
        {'exclude_firm_ids': [1, 2]},
        {'exclude_firm_ids': 2}
    ]
    act_filter = ActCreatorFilter(act_creation_filter)
    assert act_filter.need2act(firm_id=firm_id, service_ids=service_ids, act_type=act_type) is answer


@pytest.mark.parametrize(
    'firm_id, service_ids, act_type, answer',
    [
        (1, [1], 'daily', False),
        (1, [2], 'monthly', True),

        (3, [2, 3], 'daily', False),

        (1, [3], 'daily', False),
    ],
)
def test_exclude_service_ids(firm_id, service_ids, act_type, answer):
    act_creation_filter = [
        {'exclude_service_ids': [1, 2]},
        {'exclude_service_ids': 2}
    ]
    act_filter = ActCreatorFilter(act_creation_filter)
    assert act_filter.need2act(firm_id=firm_id, service_ids=service_ids, act_type=act_type) is answer


@pytest.mark.parametrize(
    'firm_id, service_ids, act_type, answer',
    [
        (4, [1], 'daily', False),
        (5, [3], 'monthly', True),
        (4, [3, 2], 'daily', False),
        (6, [2], 'monthly', True),
        (6, [3], 'daily', False),
        (6, [3, 1], 'monthly', False),
        (6, [3, 12], 'daily', False),
        (6, [4], 'daily', False),
        (6, [12], 'monthly', True),
        (7, [3], 'daily', False),
        (7, [4], 'daily', False),
        (8, [1, 2, 3, 12], 'daily', True),
        (8, [1, 2, 3, 4, 12], 'monthly', False),
    ],
)
def test_service_ids_firm_ids(firm_id, service_ids, act_type, answer):
    act_creation_filter = [
        {'firm_ids': [4, 5], 'service_ids': [1, 2]},
        {'firm_ids': 6, 'service_ids': 3},
        {'firm_ids': 7},
        {'service_ids': 4}
    ]
    act_filter = ActCreatorFilter(act_creation_filter)
    assert act_filter.need2act(firm_id=firm_id, service_ids=service_ids, act_type=act_type) is answer


@pytest.mark.parametrize(
    'firm_id, service_ids, act_type, answer',
    [
        (4, [1], 'daily', True),
        (6, [3], 'daily', True),
        (5, [3], 'monthly', False),
        (6, [2], 'monthly', False),
        (4, [3, 2], 'daily', False),
        (6, [3, 1], 'monthly', False),
        (6, [1, 12], 'daily', False),
        (6, [12], 'monthly', True),
        (7, [12], 'daily', True),
    ],
)
def test_service_ids_exclude_firm_ids(firm_id, service_ids, act_type, answer):
    act_creation_filter = [
        {'service_ids': [1, 2], 'exclude_firm_ids': [4, 5]},
        {'service_ids': 3, 'exclude_firm_ids': 6}
    ]
    act_filter = ActCreatorFilter(act_creation_filter)
    assert act_filter.need2act(firm_id=firm_id, service_ids=service_ids, act_type=act_type) is answer


@pytest.mark.parametrize(
    'firm_id, service_ids, act_type, answer',
    [
        (4, [1], 'daily', True),
        (6, [3], 'daily', True),
        (5, [3], 'monthly', False),
        (6, [2], 'monthly', False),

        (4, [3, 2], 'daily', False),
        (6, [3, 1], 'monthly', False),

        (6, [1, 12], 'daily', False),
        (6, [12], 'monthly', False),
        (7, [1, 2, 3, 4, 12], 'daily', True),
    ],
)
def test_firm_ids_exclude_service_ids(firm_id, service_ids, act_type, answer):
    act_creation_filter = [
        {'firm_ids': [4, 5], 'exclude_service_ids': [1, 2]},
        {'firm_ids': 6, 'exclude_service_ids': 3}
    ]
    act_filter = ActCreatorFilter(act_creation_filter)
    assert act_filter.need2act(firm_id=firm_id, service_ids=service_ids, act_type=act_type) is answer


@pytest.mark.parametrize(
    'firm_id, service_ids, act_type, answer',
    [
        (4, [1], 'daily', False),
        (6, [3], 'daily', False),
        (5, [3], 'monthly', True),
        (6, [2], 'monthly', True),
        (4, [3, 1], 'daily', False),
        (6, [3, 2], 'monthly', False),
        (6, [12], 'monthly', False),

        (6, [1, 12], 'daily', False),
        (7, [1, 2, 3, 4, 12], 'daily', False),
    ],
)
def test_exclude_firm_ids_exclude_service_ids(firm_id, service_ids, act_type, answer):
    act_creation_filter = [
        {'exclude_firm_ids': [4, 5], 'exclude_service_ids': [1, 2]},
        {'exclude_firm_ids': 6, 'exclude_service_ids': 3}
    ]
    act_filter = ActCreatorFilter(act_creation_filter)
    assert act_filter.need2act(firm_id=firm_id, service_ids=service_ids, act_type=act_type) is answer


@pytest.mark.parametrize(
    'firm_id, service_ids, act_type, answer',
    [
        (1, [1], 'daily', True),
        (1, [1, 2], 'monthly', True),
    ],
)
def test_empty_list(firm_id, service_ids, act_type, answer):
    act_creation_filter = []
    act_filter = ActCreatorFilter(act_creation_filter)
    assert act_filter.need2act(firm_id=firm_id, service_ids=service_ids, act_type=act_type) is answer


@pytest.mark.parametrize(
    'firm_id, service_ids, act_type, answer',
    [
        (1, [1], 'daily', False),
        (1, [1, 2], 'monthly', False),
    ],
)
def test_empty_dict(firm_id, service_ids, act_type, answer):
    act_creation_filter = [{}]
    act_filter = ActCreatorFilter(act_creation_filter)
    assert act_filter.need2act(firm_id=firm_id, service_ids=service_ids, act_type=act_type) is answer
