import pytest
from datetime import datetime
from mock import patch

from intranet.trip.src.enums import EmployeeStatus
from ..core.test_middlewares import user_data_from_bb_mock


pytestmark = pytest.mark.asyncio


company_id = 1
employees = [
    {
        'uid': '1',
        'person_id': 1,
        'first_name': 'Иван',
        'middle_name': 'Иванович',
        'last_name': 'Иванов',
        'email': 'ivan@ya.ru',
        'is_active': True,
        'is_dismissed': False,
        'is_coordinator': False,
        'company_id': company_id,
        'rejected_at': None,
    },
    {
        'person_id': 2,
        'first_name': 'Петр',
        'middle_name': 'Петрович',
        'last_name': 'Петров',
        'email': 'petr@ya.ru',
        'is_active': True,
        'is_dismissed': False,
        'is_coordinator': False,
        'company_id': company_id,
        'rejected_at': None,
    },
    {
        'person_id': 3,
        'first_name': 'Не',
        'middle_name': 'Активированный',
        'last_name': 'Пользователь',
        'email': 'passive@ya.ru',
        'is_active': False,
        'is_dismissed': False,
        'is_coordinator': False,
        'company_id': company_id,
        'rejected_at': datetime.now().isoformat(),
    },
    {
        'person_id': 4,
        'first_name': 'Ноунейм',
        'last_name': 'Ноунеймов',
        'email': 'nou@ya.ru',
        'is_active': False,
        'is_dismissed': True,
        'is_coordinator': False,
        'company_id': company_id,
        'rejected_at': None,
    },
    {
        'person_id': 5,
        'first_name': 'Ноунейм',
        'last_name': 'Ноунеймов',
        'email': 'nou@ya.ru',
        'is_active': False,
        'is_dismissed': False,
        'is_coordinator': False,
        'company_id': company_id,
        'rejected_at': None,
    },
    {
        'person_id': 6,
        'first_name': 'Ноунейм',
        'last_name': 'Ноунеймов',
        'email': 'nou@ya.ru',
        'is_active': True,
        'is_dismissed': False,
        'is_coordinator': True,
        'company_id': company_id,
        'rejected_at': None,
    },
    {
        'person_id': 7,
        'first_name': 'Ноунейм',
        'last_name': 'Ноунеймов',
        'email': 'nou@ya.ru',
        'is_active': True,
        'is_dismissed': False,
        'is_coordinator': False,
        'company_id': company_id,
        'rejected_at': None,
    },
]


async def _create_employees(f):
    for emp in employees:
        await f.create_company(emp['company_id'])
        await f.create_person(**emp)


async def test_employee_details(uow, f, client):
    user_data_from_bb_mocked = await user_data_from_bb_mock()
    user_data_from_bb_mocked['is_coordinator'] = True
    user_data_from_bb_mocked['uid']['value'] = '1'

    async def mock_get_user_from_bb(self, request):
        return user_data_from_bb_mocked

    await _create_employees(f)

    employee = employees[1].copy()
    employee_id = employee['person_id']
    with patch('intranet.trip.src.middlewares.auth.DevMiddleware.get_user_data_from_bb',
               mock_get_user_from_bb):
        response = await client.get(f'api/companies/{company_id}/employees/{employee_id}')
        data = response.json()

    data.pop('approver')
    data.pop('actions')
    data.pop('status')
    employee['employee_id'] = employee.pop('person_id')
    assert data == employee


async def test_employees_list(f, client):
    user_data_from_bb_mocked = await user_data_from_bb_mock()
    user_data_from_bb_mocked['is_coordinator'] = True
    user_data_from_bb_mocked['uid']['value'] = '1'

    async def mock_get_user_from_bb(self, request):
        return user_data_from_bb_mocked

    await _create_employees(f)
    employee_with_filter_params = {
        'is_coordinator': False,
        'limit': 5,
        'offset': 0,
    }
    with patch('intranet.trip.src.middlewares.auth.DevMiddleware.get_user_data_from_bb',
               mock_get_user_from_bb):
        response = await client.get(
            url=f'api/companies/{company_id}/employees',
            params=employee_with_filter_params,
        )
    assert response.status_code == 200

    data = response.json()
    assert data['count'] == 6
    assert data['limit'] == 5


async def test_employee_status(uow, f, client):
    user_data_from_bb_mocked = await user_data_from_bb_mock()
    user_data_from_bb_mocked['is_coordinator'] = True
    user_data_from_bb_mocked['uid']['value'] = '1'

    async def mock_get_user_from_bb(self, request):
        return user_data_from_bb_mocked

    await _create_employees(f)
    with patch('intranet.trip.src.middlewares.auth.DevMiddleware.get_user_data_from_bb',
               mock_get_user_from_bb):
        response = await client.get(
            url=f'api/companies/{company_id}/employees',
            params={
                'limit': 5,
                'offset': 0,
            },
        )
    assert response.status_code == 200
    data = response.json()
    for employee in data['data']:
        if employee['rejected_at'] is not None:
            assert employee['status'] == EmployeeStatus.rejected
        elif employee['is_dismissed']:
            assert employee['status'] == EmployeeStatus.blocked
        elif not employee['is_active']:
            assert employee['status'] == EmployeeStatus.wait_activation
        else:
            assert employee['status'] == EmployeeStatus.activated


async def test_employee_without_filter(uow, f, client):
    user_data_from_bb_mocked = await user_data_from_bb_mock()
    user_data_from_bb_mocked['is_coordinator'] = True
    user_data_from_bb_mocked['uid']['value'] = '1'

    async def mock_get_user_from_bb(self, request):
        return user_data_from_bb_mocked

    await _create_employees(f)
    with patch('intranet.trip.src.middlewares.auth.DevMiddleware.get_user_data_from_bb',
               mock_get_user_from_bb):
        response = await client.get(
            url=f'api/companies/{company_id}/employees',
            params={},
        )
    assert response.status_code == 200
    data = response.json()
    assert data['count'] == len(employees)


@pytest.mark.parametrize('status, is_coordinator, count', (
    (EmployeeStatus.wait_activation, False, 1),
    (EmployeeStatus.activated, False, 3),
    (EmployeeStatus.activated, True, 1),
    (EmployeeStatus.blocked, False, 1),
    (EmployeeStatus.rejected, False, 1),
))
async def test_employee_filter_one_status(uow, f, client, status, is_coordinator, count):
    user_data_from_bb_mocked = await user_data_from_bb_mock()
    user_data_from_bb_mocked['is_coordinator'] = True
    user_data_from_bb_mocked['uid']['value'] = '1'

    async def mock_get_user_from_bb(self, request):
        return user_data_from_bb_mocked

    await _create_employees(f)
    with patch('intranet.trip.src.middlewares.auth.DevMiddleware.get_user_data_from_bb',
               mock_get_user_from_bb):
        response = await client.get(
            url=f'api/companies/{company_id}/employees',
            params={
                'limit': 10,
                'offset': 0,
                'statuses': [status.value],
                'is_coordinator': is_coordinator,
            },
        )
    assert response.status_code == 200
    data = response.json()
    assert data['count'] == count


async def test_employee_filter_several_statuses(uow, f, client):
    user_data_from_bb_mocked = await user_data_from_bb_mock()
    user_data_from_bb_mocked['is_coordinator'] = True
    user_data_from_bb_mocked['uid']['value'] = '1'

    async def mock_get_user_from_bb(self, request):
        return user_data_from_bb_mocked

    await _create_employees(f)
    with patch('intranet.trip.src.middlewares.auth.DevMiddleware.get_user_data_from_bb',
               mock_get_user_from_bb):
        response = await client.get(
            url=f'api/companies/{company_id}/employees',
            params={
                'limit': 10,
                'offset': 0,
                'statuses': [
                    EmployeeStatus.rejected.value,
                    EmployeeStatus.blocked.value,
                    EmployeeStatus.wait_activation.value,
                ],
            },
        )
    assert response.status_code == 200
    data = response.json()
    assert data['count'] == 3
