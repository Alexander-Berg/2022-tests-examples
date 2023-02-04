import pytest
from mock import patch, AsyncMock, MagicMock

import random

from src.common import AsyncContextManager
from src.common.access import has_permissions
from src.config import settings


@pytest.mark.parametrize(
    'staff_api_response, expected_result',
    [
        ({'result': []}, False),
        ({'result': [1]}, True),
    ],
)
@pytest.mark.asyncio
async def test_has_permissions(staff_api_response, expected_result):
    login = f'login-{random.random()}'
    user = {'login': login}
    role = f'role-{random.random()}'
    ticket = f'ticket{random.random()}'

    response = AsyncMock()
    response.json.return_value = staff_api_response
    session = MagicMock()
    session.get.return_value = AsyncContextManager(response)

    with patch('src.common.access.get_tvm_service_ticket', return_value=ticket) as get_tvm_service_ticket_patch:
        with patch(
            'src.common.access.aiohttp.ClientSession',
            return_value=AsyncContextManager(session),
        ) as session_patch:
            result = await has_permissions(user, role)

            assert result == expected_result

            session_patch.assert_called_once_with(headers={settings.TVM_SERVICE_TICKET_HEADER: ticket})
            get_tvm_service_ticket_patch.assert_called_once_with('staff-api')
            session.get.assert_called_once_with(
                url=f'https://{settings.STAFF_API_HOST}/v3/departmentstaff',
                params={'person.login': login, 'role': role},
            )
            response.json.assert_called_once_with()
