from datetime import datetime
import json
import random
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from starlette.datastructures import QueryParams

from src.common.test_utils import get_random_date
from src.budget_positions.dwh_storage import FilterParams, GetBudgetPositionsParams
from src.budget_positions.views import _parse_get_budget_positions_params
from src.main_webapp import get_budget_positions
from src.tests.utils import create_request


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'limit, actual_limit',
    [
        (None, 1000),
        ('10', 10),
        ('1001', 1000),
    ],
)
@patch('src.budget_positions.views.has_permissions', return_value=True)
async def test_get_budget_positions(has_permissions_mock, limit, actual_limit):
    test_login = 'test_login'
    department_id = random.randint(3, 60000000)
    budget_position_code = random.randint(60000000, 90000000)
    login = f'login{random.randint(3, 6)}'
    budget_positions = [{f'test{random.randint(3, 6)}': random.randint(3, 6)}]
    event_time__ge = get_random_date()
    event_time__le = get_random_date()
    continuation_token = f'token-{random.random()}'

    query_params = QueryParams([
        ('department_id', str(department_id)),
        ('budget_position_code', str(budget_position_code)),
        ('login', login),
        ('event_time__ge', event_time__ge.isoformat()),
        ('event_time__le', event_time__le.isoformat()),
        ('continuation_token', continuation_token),
        ('limit', limit or ''),
    ])
    request = create_request(test_login, query_params)

    storage = MagicMock()
    storage.get_budget_positions = AsyncMock(return_value=budget_positions)
    with patch('src.budget_positions.views.BudgetPositionsStorage', return_value=storage) as storage_patch:
        result = await get_budget_positions(request)
        assert result.status_code == 200
        assert json.loads(result.body) == budget_positions

        storage_patch.assert_called_once_with(request.app.state.engine)
        storage.get_budget_positions.assert_called_once_with(
            GetBudgetPositionsParams(
                continuation_token=continuation_token,
                limit=actual_limit,
                filter_params=FilterParams(
                    department_id=department_id,
                    login=login,
                    budget_position_code=budget_position_code,
                    event_time__ge=event_time__ge,
                    event_time__le=event_time__le,
                    event_types=None,
                ),
            )
        )

    has_permissions_mock.assert_called_once_with({'login': test_login}, role='hr_analyst')


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'department_id, budget_position_code, event_time__ge, event_time__le, limit',
    [
        ('100', '', None, None, None),
        ('', '100', None, None, None),
        ('', '', None, None, None),
        ('100', 'test', None, None, None),
        ('test', '100', None, None, None),
        ('test', 'test', None, None, None),
        (None, None, 'test', None, None),
        (None, None, None, 'test', None),
        (None, None, None, None, 'test'),
        (None, None, None, None, '0'),
        (None, None, None, None, '-10'),
    ]
)
@patch('src.budget_positions.views.has_permissions', return_value=True)
async def test_get_budget_positions_invalid_parameters(
    has_permissions_mock,
    department_id,
    budget_position_code,
    event_time__ge,
    event_time__le,
    limit,
):
    test_login = 'test_login'
    query_params = QueryParams([
        ('department_id', department_id),
        ('budget_position_code', budget_position_code),
        ('event_time__ge', event_time__ge),
        ('event_time__le', event_time__le),
        ('login', f'login{random.randint(3, 6)}'),
        ('limit', limit),
    ])
    request = create_request(test_login, query_params)
    with patch('src.budget_positions.views.BudgetPositionsStorage') as storage_patch:
        result = await get_budget_positions(request)
        storage_patch.assert_not_called()
        assert result.status_code == 400

    has_permissions_mock.assert_called_once_with({'login': test_login}, role='hr_analyst')


@pytest.mark.asyncio
async def test_get_budget_positions_no_permissions():
    test_login = 'test_login'
    request = create_request(test_login, QueryParams())

    with patch('src.budget_positions.views.has_permissions', return_value=False) as has_permissions_mock:
        result = await get_budget_positions(request)

        assert result.status_code == 403
        has_permissions_mock.assert_called_once_with(request.state.user, role='hr_analyst')


def test_parse_get_budget_positions_params():
    # given
    q = QueryParams([
        ('department_id', '100'),
        ('budget_position_code', '1'),
        ('event_time__ge', '2021-09-17'),
        ('event_time__le', '2021-10-01'),
        ('continuation_token', '100500'),
        ('login', 'pg'),
        ('event_type', '5'),
        ('event_type', '6'),
        ('limit', '1000')
    ])

    # when
    result = _parse_get_budget_positions_params(q)

    # then
    assert result.filter_params.department_id == 100
    assert result.filter_params.budget_position_code == 1
    assert result.filter_params.event_time__ge == datetime(year=2021, month=9, day=17)
    assert result.filter_params.event_time__le == datetime(year=2021, month=10, day=1)
    assert result.filter_params.login == 'pg'
    assert result.filter_params.event_types == ['5', '6']
    assert result.continuation_token == '100500'
    assert result.limit == 1000
