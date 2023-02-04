from datetime import datetime, timedelta
from freezegun import freeze_time

import pytest
from mock import patch, Mock

from waffle.models import Switch

from staff.lib.testing import StaffFactory
from staff.rfid.constants import STATE
from staff.rfid.tasks import SyncBadgeChanges, ExportBadgeChanges
from staff.rfid.tests.views_test import EmployeeFactory, AnonymFactory


@pytest.mark.django_db
@patch('staff.rfid.tasks.tvm2')
@patch('staff.rfid.tasks.requests.post')
def test_anonym_badge_changes(mock_request, mock_deploy, rfid_cmp):
    Switch.objects.get_or_create(name='enable_badge_changes_for_helpdesk', active=True)
    mock_request.return_value = Mock(status_code=200)
    mock_deploy.return_value = 'tvm_ticket'

    datetimes = (datetime.now() + timedelta(seconds=seconds) for seconds in range(100))

    employee_person = StaffFactory()
    with freeze_time(datetimes):
        anonym_badge = AnonymFactory(state=STATE.ACTIVE)
        employee_badge = EmployeeFactory(state=STATE.LOST, person=employee_person)

    SyncBadgeChanges()
    SyncBadgeChanges()

    ExportBadgeChanges()
    assert sorted(mock_request.call_args_list[0][1]['json'], key=rfid_cmp) == sorted([
        {
            'type': 'Guest',
            'id': anonym_badge.id,
            'badges': [{'status': 'inactive', 'rfid': int(anonym_badge.rfid.code)}]
        },
        {
            'type': 'Employee',
            'firstName': employee_person.first_name,
            'lastName': employee_person.last_name,
            'middleName': '',
            'phone': None,
            'login': employee_badge.person.login,
            'badges': [{'status': 'inactive', 'rfid': int(employee_badge.rfid.code)}],
        }
    ], key=rfid_cmp)

    with freeze_time(datetimes):
        anonym_badge.anonym_food_allowed = True
        anonym_badge.save()
        employee_badge.state = STATE.ACTIVE
        employee_badge.save()

    SyncBadgeChanges()

    ExportBadgeChanges()
    assert sorted(mock_request.call_args_list[2][1]['json'], key=rfid_cmp) == sorted([
        {
            'type': 'Guest',
            'id': anonym_badge.id,
            'badges': [{'status': 'active', 'rfid': int(anonym_badge.rfid.code)}]
        },
        {
            'type': 'Employee',
            'firstName': employee_person.first_name,
            'lastName': employee_person.last_name,
            'middleName': '',
            'phone': None,
            'login': employee_badge.person.login,
            'badges': [{'status': 'active', 'rfid': int(employee_badge.rfid.code)}],
        }
    ], key=rfid_cmp)

    with freeze_time(datetimes):
        anonym_badge2 = AnonymFactory(state=STATE.ACTIVE)

    SyncBadgeChanges()

    anonym_badge2.anonym_food_allowed = True
    anonym_badge2.save()
    SyncBadgeChanges()

    ExportBadgeChanges()
    assert mock_request.call_args_list[4][1]['json'] == [{
        'type': 'Guest',
        'id': anonym_badge2.id,
        'badges': [{'status': 'active', 'rfid': int(anonym_badge2.rfid.code)}]
    }]


@pytest.mark.django_db
@patch('staff.rfid.tasks.tvm2')
@patch('staff.rfid.tasks.requests.post')
def test_badge_deletion(mock_request, mock_deploy, rfid_cmp):
    mock_request.return_value = Mock(status_code=200)
    mock_deploy.return_value = 'tvm_ticket'

    badge1 = AnonymFactory(state=STATE.ACTIVE)
    employee_person = StaffFactory()
    badge2 = EmployeeFactory(state=STATE.INACTIVE, person=employee_person)

    SyncBadgeChanges()

    ExportBadgeChanges()
    assert sorted(mock_request.call_args_list[0][1]['json'], key=rfid_cmp) == sorted([
        {
            'type': 'Guest',
            'id': badge1.id,
            'badges': [{'status': 'inactive', 'rfid': int(badge1.rfid.code)}]
        },
        {
            'type': 'Employee',
            'firstName': employee_person.first_name,
            'lastName': employee_person.last_name,
            'middleName': '',
            'phone': None,
            'login': badge2.person.login,
            'badges': [{'status': 'inactive', 'rfid': int(badge2.rfid.code)}]
        }
    ], key=rfid_cmp)
