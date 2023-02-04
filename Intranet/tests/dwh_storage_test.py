import pytest
from unittest.mock import MagicMock

from collections import defaultdict
from datetime import datetime
from random import randint, random

from src.budget_positions.dwh_storage import DwhStorage, GetBudgetPositionsParams, FilterParams
from src.budget_positions.models import budget_position_fact_table
from src.common import AsyncContextManager, AsyncIterator
from src.common.test_utils import get_random_date


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'department_id, login, budget_position_code, event_time__ge, event_time__le',
    [
        (randint(100, 200), f'login{randint(10, 20)}', randint(1000, 2000), get_random_date(), get_random_date()),
        (None, None, None, None, None),

        (randint(100, 200), f'login{randint(10, 20)}', None, None, None),
        (randint(100, 200), None, randint(1000, 2000), None, None),
        (None, f'login{randint(10, 20)}', randint(1000, 2000), None, None),

        (randint(100, 200), None, None, None, None),
        (None, f'login{randint(10, 20)}', None, None, None),
        (None, None, randint(1000, 2000), None, None),

        (None, None, None, get_random_date(), None),
        (None, None, None, None, get_random_date()),
    ]
)
async def test_get_budget_positions(
    department_id: int,
    login: str,
    budget_position_code: int,
    event_time__ge: datetime,
    event_time__le: datetime,
):
    sorting = ['-event_time', 'bp_code', 'id']
    where_clause = _construct_where_clause(
        department_id,
        login,
        budget_position_code,
        event_time__ge,
        event_time__le,
    )
    continuation_token = f'continuation-token-{random()}'
    new_continuation_token = f'new-continuation-token-{random()}'
    limit = randint(100, 1000)

    app = MagicMock()
    connection = MagicMock()
    app.state.engine.acquire.return_value = AsyncContextManager(connection)

    executor = MagicMock()
    bp1 = randint(100, 300)
    bp2 = randint(1000, 3000)
    facts = [
        {'bp_code': bp1, 'value': random()},
        {'bp_code': bp1, 'value': random()},
        {'bp_code': bp2, 'value': random()},
    ]
    resulting_data = [
        {'bp_code': bp1, 'was': facts[1], 'is': facts[0], 'debug_info': ''},
        {'bp_code': bp1, 'was': None, 'is': facts[1], 'debug_info': ''},
        {'bp_code': bp2, 'was': None, 'is': facts[2], 'debug_info': ''},
    ]
    executor.select.return_value = AsyncIterator(facts)
    executor.get_continuation_token.return_value = new_continuation_token

    target = DwhStorage(app.state.engine, executor)
    result = await target.get_budget_positions(
        GetBudgetPositionsParams(
            continuation_token=continuation_token,
            limit=limit,
            filter_params=FilterParams(
                department_id=department_id,
                login=login,
                budget_position_code=budget_position_code,
                event_time__ge=event_time__ge,
                event_time__le=event_time__le,
                event_types=[],
            ),
        ),
    )

    app.state.engine.acquire.assert_called_once_with()
    executor.select.assert_called_once_with(
        connection,
        budget_position_fact_table,
        where_clause,
        sorting,
        limit,
        continuation_token,
    )
    executor.get_continuation_token.assert_called_once_with(resulting_data, sorting, limit)

    assert result == {'data': resulting_data, 'continuation_token': new_continuation_token}


def _construct_where_clause(
    department_id,
    login,
    budget_position_code,
    event_time__ge,
    event_time__le,
):
    where_clause = defaultdict()

    if department_id is not None:
        where_clause['department_id'] = {'eq': department_id}

    if login is not None:
        where_clause['login'] = {'eq': login}

    if budget_position_code is not None:
        where_clause['bp_code'] = {'eq': budget_position_code}

    if event_time__ge is not None:
        where_clause['event_time'] = {'ge': event_time__ge}

    if event_time__le is not None:
        where_clause.setdefault('event_time', {})['le'] = event_time__le

    return where_clause
