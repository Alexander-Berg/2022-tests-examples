import pytest

import random
from sqlalchemy import and_, or_

from src.budget_positions.models import budget_position_fact_table
from src.common import ContinuationTokenManager, ContinuationTokenException
from src.common.test_utils import get_random_date


def test_get_filter():
    table = budget_position_fact_table
    sorting = [_get_random_column() for _ in range(3)]
    bp_code = random.randint(12, 213132131)
    current_id = random.randint(300, 9878787)
    event_time = get_random_date()
    last_result = {
        'is': {
            'id': current_id,
            'event_time': event_time,
        },
        'bp_code': bp_code,
    }
    last_result_token = f"~{last_result['is']['event_time']}~{last_result['bp_code']}~{last_result['is']['id']}"
    continuation_token = '.'.join(sorting) + last_result_token

    result = ContinuationTokenManager().get_filter(table, continuation_token, sorting)

    assert str(result) == str(or_(
        table.c.event_time < event_time,
        and_(table.c.event_time == event_time, table.c.bp_code > bp_code),
        and_(table.c.event_time == event_time, table.c.bp_code == bp_code, table.c.id > current_id),
    ))


def test_get_filter_broken_sorting():
    sorting = [_get_random_column() for _ in range(random.randint(3, 5))]
    continuation_token = '.'.join([_get_random_column() for _ in range(len(sorting))]) + f'~{get_random_date()}~2~3'

    with pytest.raises(ContinuationTokenException):
        ContinuationTokenManager().get_filter(budget_position_fact_table, continuation_token, sorting)


@pytest.mark.parametrize(
    'event_time, bp_code, current_id',
    [
        ('test', 1, 2),
        (get_random_date(), 'test', 2),
        (get_random_date(), 1, 'test'),
    ],
)
def test_get_filter_broken_stamps(event_time, bp_code, current_id):
    sorting = [_get_random_column() for _ in range(3)]
    continuation_token = '.'.join(sorting) + f'~{event_time}~{bp_code}~{current_id}'

    with pytest.raises(ContinuationTokenException):
        ContinuationTokenManager().get_filter(budget_position_fact_table, continuation_token, sorting)


def test_get_filter_token_too_long():
    sorting = [_get_random_column() for _ in range(random.randint(4, 53))]
    continuation_token = '.'.join(sorting) + f'~{get_random_date()}~2~3'

    with pytest.raises(ContinuationTokenException):
        ContinuationTokenManager().get_filter(budget_position_fact_table, continuation_token, sorting)


def test_get_filter_token_too_short():
    sorting = [_get_random_column() for _ in range(random.randint(0, 2))]
    continuation_token = '.'.join(sorting) + f'~{get_random_date()}~2~3'

    with pytest.raises(ContinuationTokenException):
        ContinuationTokenManager().get_filter(budget_position_fact_table, continuation_token, sorting)


def test_get_continuation_token():
    sorting = [_get_random_column() for _ in range(random.randint(3, 5))]
    bp_code = random.randint(12, 213132131)
    current_id = random.randint(300, 9878787)
    event_time = get_random_date()
    last_result = {
        'is': {
            'id': current_id,
            'event_time': event_time,
        },
        'bp_code': bp_code,
    }
    last_result_token = f"~{last_result['is']['event_time']}~{last_result['bp_code']}~{last_result['is']['id']}"
    expected_continuation_token = '.'.join(sorting) + last_result_token

    assert ContinuationTokenManager().get_continuation_token([last_result], sorting, 1) == expected_continuation_token


def test_get_continuation_token_last_page():
    data = [{} for _ in range(random.randint(23, 654))]
    assert ContinuationTokenManager().get_continuation_token(data, [], len(data) + 1) is None


def _get_random_column():
    return f's-{random.randint(0, 9999999999)}'
