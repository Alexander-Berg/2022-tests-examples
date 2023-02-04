import random

import pytest
from unittest.mock import MagicMock, patch, AsyncMock

from src.change_registry.models import change_record
from src.change_registry.pull_from_registry import PullData
from src.common import AsyncContextManager
from src.common.test_utils import get_random_date
from src.config import settings
from src.tvm import TVM_SERVICE_TICKET_HEADER


@pytest.mark.asyncio
async def test_pull_data_no_source_changes():
    app = MagicMock()
    ticket = f'ticket{random.random()}'
    response = AsyncMock()
    response.json.return_value = {'data': []}
    session = MagicMock()
    session.get.return_value = AsyncContextManager(response)
    connection = MagicMock()
    app.state.engine.acquire.return_value = AsyncContextManager(connection)
    executor = AsyncMock()
    executor.get_last_processed_source_date.return_value = get_random_date()
    pull_data = PullData(executor=executor).run

    with patch('src.change_registry.pull_from_registry.get_tvm_ticket', return_value=ticket) as get_tvm_ticket_patch:
        with patch(
            'src.change_registry.pull_from_registry.aiohttp.ClientSession',
            return_value=AsyncContextManager(session),
        ) as session_patch:
            result = await pull_data(app)
            session_patch.assert_called_once_with(headers={TVM_SERVICE_TICKET_HEADER: ticket})
            get_tvm_ticket_patch.assert_called_once_with('staff')
            session.get.assert_called_once_with(
                url=f'https://{settings.STAFF_HOST}/budget-position-api/export-change-registry/',
                params={'continuation_token': executor.get_last_processed_source_date.return_value.isoformat()}
            )
            response.json.assert_called_once_with()
            executor.get_last_processed_source_date.assert_called_once_with(connection, change_record)
            assert result


@pytest.mark.asyncio
async def test_pull_data():
    app = MagicMock()
    ticket = f'ticket{random.random()}'
    response = AsyncMock()
    response.json.return_value = {'data': [MagicMock()]}
    session = MagicMock()
    session.get.return_value = AsyncContextManager(response)

    connection = MagicMock()
    app.state.engine.acquire.return_value = AsyncContextManager(connection)

    mapper = MagicMock()
    executor = AsyncMock()
    executor.get_last_processed_source_date.return_value = get_random_date()
    mapper.map.return_value = MagicMock()
    pull_data = PullData(mapper, executor).run

    with patch('src.change_registry.pull_from_registry.get_tvm_ticket', return_value=ticket) as get_tvm_ticket_patch:
        with patch(
            'src.change_registry.pull_from_registry.aiohttp.ClientSession',
            return_value=AsyncContextManager(session),
        ) as session_patch:
            result = await pull_data(app)
            session_patch.assert_called_once_with(headers={TVM_SERVICE_TICKET_HEADER: ticket})
            get_tvm_ticket_patch.assert_called_once_with('staff')
            session.get.assert_called_once_with(
                url=f'https://{settings.STAFF_HOST}/budget-position-api/export-change-registry/',
                params={'continuation_token': executor.get_last_processed_source_date.return_value.isoformat()}
            )
            response.json.assert_called_once_with()
            executor.get_last_processed_source_date.assert_called_once_with(connection, change_record)
            executor.insert.assert_called_once_with(connection, change_record, mapper.map.return_value)
            mapper.map.assert_called_once_with(
                'change-registry',
                response.json.return_value['data'][0],
                change_record,
                custom={
                    executor.source_date_field_name: 'workflow_confirmed_at',
                    'started_at': 'workflow_created_at',
                    'resolved_at': 'workflow_confirmed_at',
                },
            )
            assert executor.insert.await_count == 1
            assert not result


@pytest.mark.asyncio
async def test_pull_data_no_last_id():
    app = MagicMock()
    ticket = f'ticket{random.random()}'
    response = AsyncMock()
    response.json.return_value = {'data': [MagicMock()]}
    session = MagicMock()
    session.get.return_value = AsyncContextManager(response)

    connection = MagicMock()
    app.state.engine.acquire.return_value = AsyncContextManager(connection)

    mapper = MagicMock()
    executor = AsyncMock()
    executor.get_last_processed_source_date.return_value = None
    mapper.map.return_value = MagicMock()

    pull_data = PullData(mapper, executor).run

    with patch('src.change_registry.pull_from_registry.get_tvm_ticket', return_value=ticket) as get_tvm_ticket_patch:
        with patch(
            'src.change_registry.pull_from_registry.aiohttp.ClientSession',
            return_value=AsyncContextManager(session),
        ) as session_patch:
            result = await pull_data(app)
            session_patch.assert_called_once_with(headers={TVM_SERVICE_TICKET_HEADER: ticket})
            get_tvm_ticket_patch.assert_called_once_with('staff')
            session.get.assert_called_once_with(
                url=f'https://{settings.STAFF_HOST}/budget-position-api/export-change-registry/',
                params={}
            )
            response.json.assert_called_once_with()
            executor.get_last_processed_source_date.assert_called_once_with(connection, change_record)
            executor.insert.assert_called_once_with(connection, change_record, mapper.map.return_value)
            mapper.map.assert_called_once_with(
                'change-registry',
                response.json.return_value['data'][0],
                change_record,
                custom={
                    executor.source_date_field_name: 'workflow_confirmed_at',
                    'started_at': 'workflow_created_at',
                    'resolved_at': 'workflow_confirmed_at',
                },
            )
            assert executor.insert.await_count == 1
            assert not result
