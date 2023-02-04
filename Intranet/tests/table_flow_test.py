from typing import Dict

import mock
import pytest
from waffle.models import Switch

from staff.departments.controllers.tickets.helpers.table_flow import TableFlowAPI


def _fake_request(*args, **kwargs) -> Dict[str, str]:
    try:
        if kwargs['params']['department_id'] == 0:
            return {
                'analyst': 'analyst-login',
                'budget_owner': 'None',
                'budget_tag': 'None',
                'budget_notify': 'None',
                'ticket_access': 'None',
                'all_department_attrs': '1,2',
            }
        else:
            return {
                'analyst': 'test-user1',
                'budget_owner': 'test-user2',
                'budget_tag': 'Бюджетный тэг',
                'budget_notify': 'test-user3,test-user4',
                'ticket_access': 'test-user5',
                'all_department_attrs': 'None'
            }
    except KeyError:
        return {}


@pytest.mark.django_db
def test_get_departmentattrs_for_department():
    Switch.objects.get_or_create(name='enable_table_flow', active=True)

    with mock.patch('staff.departments.controllers.tickets.helpers.table_flow.TableFlowAPI._request', _fake_request):
        api = TableFlowAPI()
        result = api.get_department_attrs(2)

    assert result == {
        'department_id': 2,
        'analyst': 'test-user1',
        'budget_owner': 'test-user2',
        'budget_tag': 'Бюджетный тэг',
        'budget_notify': ('test-user3', 'test-user4'),
        'ticket_access': ('test-user5',),
    }


@pytest.mark.django_db
def test_get_all_departmentattr_ids():
    Switch.objects.get_or_create(name='enable_table_flow', active=True)

    with mock.patch('staff.departments.controllers.tickets.helpers.table_flow.TableFlowAPI._request', _fake_request):
        api = TableFlowAPI()
        result = api.get_all_department_attrs_ids()

    assert result == [1, 2]


@pytest.mark.django_db
def test_get_all_departmentattrs():
    Switch.objects.get_or_create(name='enable_table_flow', active=True)

    with mock.patch('staff.departments.controllers.tickets.helpers.table_flow.TableFlowAPI._request', _fake_request):
        api = TableFlowAPI()
        result = api.get_all_department_attrs()

    assert result == [
        {
            'department_id': 1,
            'analyst': 'test-user1',
            'budget_owner': 'test-user2',
            'budget_tag': 'Бюджетный тэг',
            'budget_notify': ('test-user3', 'test-user4'),
            'ticket_access': ('test-user5',),
        },
        {
            'department_id': 2,
            'analyst': 'test-user1',
            'budget_owner': 'test-user2',
            'budget_tag': 'Бюджетный тэг',
            'budget_notify': ('test-user3', 'test-user4'),
            'ticket_access': ('test-user5',),
        },
    ]


@pytest.mark.django_db
def test_get_base_analyst_login():
    Switch.objects.get_or_create(name='enable_table_flow', active=True)

    with mock.patch('staff.departments.controllers.tickets.helpers.table_flow.TableFlowAPI._request', _fake_request):
        api = TableFlowAPI()
        result = api.get_base_analyst_login()

    assert result == 'analyst-login'
