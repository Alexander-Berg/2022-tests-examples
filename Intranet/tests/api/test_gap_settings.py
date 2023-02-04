import datetime
import itertools
from unittest.mock import patch
import pytest

from fastapi import status

from watcher import enums
from watcher.logic.timezone import now


def test_retrieve_gap_settings(
    client, manual_gap_settings_factory, service_factory, schedule_factory, scope_session,
    assert_json_keys_value_equal, assert_count_queries
):
    gap_settings = manual_gap_settings_factory()
    gap_settings.services = [service_factory()]
    gap_settings.schedules = [schedule_factory()]
    scope_session.commit()

    gap_settings_id = gap_settings.id
    with assert_count_queries(2):
        # select intranet_staff.uid = 123
        # select manual_gap_settings
        response = client.get(
            f'/api/watcher/v1/settings/gap/{gap_settings_id}'
        )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()
    assert data['id'] == gap_settings.id
    assert len(data['services']) == 1
    assert data['services'][0]['slug'] == gap_settings.services[0].slug
    assert len(data['schedules']) == 1
    assert data['schedules'][0]['slug'] == gap_settings.schedules[0].slug


def test_list_gap_settings(client, manual_gap_settings_factory, service_factory, scope_session, assert_count_queries):
    gap_settings = [manual_gap_settings_factory() for _ in range(2)]
    gap_settings[0].services = [service_factory()]
    gap_settings[1].services = [gap_settings[0].services[0]]
    scope_session.commit()

    with assert_count_queries(2):
        # select intranet_staff.uid = 123
        # select manual_gap_settings
        response = client.get(
            '/api/watcher/v1/settings/gap/'
        )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()

    assert {item['id'] for item in data['result']} == {item.id for item in gap_settings}
    assert {item['services'][0]['id'] for item in data['result']} == {item.services[0].id for item in gap_settings}


def test_gap_settings_filter_service(client, manual_gap_settings_factory, service_factory, schedule_factory,
                                     member_factory, scope_session, assert_count_queries):
    services = [service_factory() for _ in range(3)]

    members = []
    members.extend([member_factory(service=services[0]) for _ in range(3)])
    members.extend([member_factory(service=services[1]) for _ in range(3)])
    members.extend([member_factory(service=services[2]) for _ in range(1)])
    members.extend([member_factory(staff=members[-1].staff, service=services[0]) for _ in range(1)])

    gap_settings = [manual_gap_settings_factory(staff=member.staff) for member in members]

    gap_settings[0].all_services = True
    gap_settings[1].services = [services[0]]
    gap_settings[2].schedules = [schedule_factory(service=services[0])]

    gap_settings[3].all_services = True
    gap_settings[4].services = [services[0]]
    gap_settings[5].schedules = [schedule_factory(service=services[0])]

    gap_settings[6].all_services = True
    gap_settings[7].services = [services[2]]

    scope_session.commit()

    filter_params = {'filter': f'service_id={services[0].id}'}
    expected_ids = {obj.id for obj in itertools.chain(gap_settings[0:3], [gap_settings[-2]])}

    with assert_count_queries(2):
        # select intranet_staff.uid = 123
        # select manual_gap_settings
        response = client.get(
            '/api/watcher/v1/settings/gap/', params=filter_params
        )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()['result']

    filtered_ids = {obj['id'] for obj in data}
    assert filtered_ids == expected_ids


@pytest.mark.parametrize('schedule_idx', (0, 1))
def test_gap_settings_filter_schedule(client, manual_gap_settings_factory, service_factory, schedule_factory,
                                      member_factory, scope_session, assert_count_queries, schedule_idx):
    services = [service_factory() for _ in range(2)]
    schedules = []
    schedules.extend([schedule_factory(service=services[0]) for _ in range(2)])
    schedules.extend([schedule_factory(service=services[1]) for _ in range(2)])

    members = []
    members.extend([member_factory(service=services[0]) for _ in range(5)])
    members.extend([member_factory(service=services[1]) for _ in range(3)])

    gap_settings = [manual_gap_settings_factory(staff=member.staff) for member in members]

    # Member-ы первого сервиса
    # этот ожидаем в выдаче
    gap_settings[0].all_services = True

    # этот ожидаем в выдаче - он должен показываться для любого расписания первого сервиса
    gap_settings[1].services = [services[0]]

    # этот ожидаем в выдаче
    gap_settings[2].schedules = [schedules[0], schedules[1]]

    # этот ожидаем в выдаче только для schedules[0]
    gap_settings[3].services = [services[0]]
    gap_settings[3].schedules = [schedules[0]]

    # этот ожидаем в выдаче только для schedules[1]
    gap_settings[4].services = [services[0]]
    gap_settings[4].schedules = [schedules[1]]

    # Member-ы второго сервиса - не ожидаем в выдаче, хотя каждый из них подходит по настройкам
    gap_settings[5].all_services = True
    gap_settings[6].services = [services[0]]
    gap_settings[7].schedules = [schedules[0]]

    scope_session.commit()

    filter_params = {'filter': f'schedule_id={schedules[schedule_idx].id}'}
    if schedule_idx == 0:
        expected_ids = {obj.id for obj in gap_settings[0:4]}
    else:
        expected_ids = {obj.id for obj in itertools.chain(gap_settings[0:3] + [gap_settings[4]])}

    with assert_count_queries(3):
        # select intranet_staff.uid = 123
        # select schedules
        # select manual_gap_settings
        response = client.get(
            '/api/watcher/v1/settings/gap/', params=filter_params
        )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()['result']

    filtered_ids = {obj['id'] for obj in data}
    assert filtered_ids == expected_ids


def test_create_gap_settings(client, staff_factory, service_factory, schedule_factory, assert_count_queries):

    staff = staff_factory()
    service = service_factory()
    schedules = [schedule_factory(service=service) for _ in range(2)]

    current_now = now()

    gap_settings_data = {
        'title': 'something title',
        'recurrence': 'fortnight',
        'staff_id': staff.id,
        'start': (current_now + datetime.timedelta(hours=1)).isoformat(),
        'end': (current_now + datetime.timedelta(hours=9)).isoformat(),
        'services': [service.id],
        'schedules': [schedule.id for schedule in schedules],
    }

    with assert_count_queries(9):
        # select intranet_staff.uid = 123
        # select schedules in _validate_schema
        # select services in _validate_schema
        # insert manual_gap_settings
        # insert manual_gap_settings_services
        # insert manual_gap_settings_schedules
        # select manual_gap_settings
        # select services
        # select schedules
        with patch('watcher.tasks.manual_gap.update_manual_gaps.delay'):
            response = client.post(
                '/api/watcher/v1/settings/gap/',
                json=gap_settings_data
            )
    assert response.status_code == status.HTTP_201_CREATED, response.text
    data = response.json()

    assert len(data['services']) == 1
    assert data['services'][0]['id'] == service.id

    assert len(data['schedules']) == len(schedules)
    schedule_ids = {schedule['id'] for schedule in data['schedules']}
    expected_ids = {schedule.id for schedule in schedules}
    assert schedule_ids == expected_ids


def test_create_gap_settings_not_all_exist(client, staff_factory, assert_count_queries):
    staff = staff_factory()
    current_now = now()

    gap_settings_data = {
        'title': 'something title',
        'recurrence': 'fortnight',
        'staff_id': staff.id,
        'start': (current_now + datetime.timedelta(hours=1)).isoformat(),
        'end': (current_now + datetime.timedelta(hours=9)).isoformat(),
        'services': [123, 456],
    }

    with assert_count_queries(2):
        # select intranet_staff.uid = 123
        # select services in _validate_schema
        response = client.post(
            '/api/watcher/v1/settings/gap/',
            json=gap_settings_data
        )
    assert response.status_code == status.HTTP_404_NOT_FOUND, response.text
    assert response.json()['error'] == 'not_found'


def test_create_gap_settings_validation_error(client, staff_factory, service_factory, schedule_factory,
                                              assert_count_queries):
    staff = staff_factory()
    service = service_factory()
    schedule = schedule_factory(service=service)

    current_now = now()

    gap_settings_data = {
        'title': 'something title',
        'recurrence': 'fortnight',
        'staff_id': staff.id,
        'start': (current_now + datetime.timedelta(hours=1)).isoformat(),
        'end': (current_now + datetime.timedelta(hours=9)).isoformat(),
        'services': [],
        'schedules': [schedule.id],
    }

    with assert_count_queries(3):
        # select intranet_staff.uid = 123
        # select schedules in _validate_schema
        # select services in _validate_schema
        response = client.post(
            '/api/watcher/v1/settings/gap/',
            json=gap_settings_data
        )
    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text
    assert response.json()['error'] == 'schedule_service_not_in_the_list'


def test_update_gap_settings_all_services(client, staff_factory, service_factory, schedule_factory, scope_session,
                                          manual_gap_settings_factory, assert_json_keys_value_equal, assert_count_queries):
    current_now = now()

    gap_settings = manual_gap_settings_factory(
        start=current_now,
        end=current_now + datetime.timedelta(hours=4),
        recurrence=enums.ManualGapRecurrence.week,
    )
    gap_settings.services = [service_factory() for _ in range(2)]
    gap_settings.schedules = [schedule_factory(service=gap_settings.services[0])]
    scope_session.commit()

    gap_settings_patch_data = {
        'all_services': True
    }
    gap_settings_id = gap_settings.id
    with assert_count_queries(6):
        # select intranet_staff.uid = 123
        # select manual_gap_settings joined services, schedules
        # update manual_gap_settings
        # delete manual_gap_settings_services
        # delete manual_gap_settings_schedules
        # select manual_gap_settings joined
        with patch('watcher.tasks.manual_gap.update_manual_gaps.delay'):
            response = client.patch(
                f'/api/watcher/v1/settings/gap/{gap_settings_id}',
                json=gap_settings_patch_data
            )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()

    assert len(data['services']) == 0
    assert len(data['schedules']) == 0


def test_update_gap_settings(client, manual_gap_settings_factory, assert_json_keys_value_equal, service_factory,
                             schedule_factory, staff_factory, scope_session, assert_count_queries):

    service_for_schedules = service_factory()
    schedules = [
        schedule_factory(service=service_for_schedules),
        schedule_factory(service=service_for_schedules)
    ]

    current_now = now()

    gap_settings = manual_gap_settings_factory(
        start=current_now,
        end=current_now + datetime.timedelta(hours=4),
        recurrence=enums.ManualGapRecurrence.week,
    )
    gap_settings.services = [service_for_schedules] + [service_factory()]
    gap_settings.schedules = [schedules[0]]
    scope_session.commit()

    gap_settings_patch_data = {
        'recurrence': 'day',
        'schedules': [schedules[0].id, schedules[1].id],
        'services': [service_for_schedules.id]
    }

    gap_settings_id = gap_settings.id
    with assert_count_queries(10):
        # select intranet_staff.uid = 123
        # select manual_gap_settings joined services, schedules
        # select schedules in _validate_schema
        # select services in _validate_schema
        # update manual_gap_settings
        # select current manual_gap_settings_services
        # delete manual_gap_settings_services
        # select current manual_gap_settings_schedules
        # insert manual_gap_settings_schedules
        # select manual_gap_settings joined
        with patch('watcher.tasks.manual_gap.update_manual_gaps.delay'):
            response = client.patch(
                f'/api/watcher/v1/settings/gap/{gap_settings_id}',
                json=gap_settings_patch_data
            )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()

    assert data['recurrence'] == 'day'
    assert len(data['services']) == 1
    assert data['services'][0]['id'] == service_for_schedules.id

    assert len(data['schedules']) == len(schedules)
    for i in range(len(schedules)):
        assert data['schedules'][i]['id'] == schedules[i].id


def test_update_gap_settings_validation_error(
    client, staff_factory, service_factory, schedule_factory, scope_session, manual_gap_settings_factory,
    assert_json_keys_value_equal, assert_count_queries
):
    current_now = now()

    gap_settings = manual_gap_settings_factory(
        start=current_now,
        end=current_now + datetime.timedelta(hours=4),
        recurrence=enums.ManualGapRecurrence.week,
    )
    gap_settings.services = [service_factory()]
    gap_settings.schedules = [schedule_factory(service=gap_settings.services[0])]
    scope_session.commit()

    new_schedule = schedule_factory()
    gap_settings_patch_data = {
        'recurrence': 'day',
        'schedules': [gap_settings.schedules[0].id, new_schedule.id],
        'services': [gap_settings.services[0].id],
    }

    gap_settings_id = gap_settings.id
    with assert_count_queries(4):
        # select intranet_staff.uid = 123
        # select manual_gap_settings joined services, schedules
        # select schedules in _validate_schema
        # select services in _validate_schema
        response = client.patch(
            f'/api/watcher/v1/settings/gap/{gap_settings_id}',
            json=gap_settings_patch_data
        )
    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text
    assert response.json()['error'] == 'schedule_service_not_in_the_list'


def test_delete_gap_settings(client, manual_gap_settings_factory, service_factory, schedule_factory, scope_session,
                             manual_gap_factory, assert_count_queries):
    gap_settings = manual_gap_settings_factory()
    gap_settings.services = [service_factory()]
    gap_settings.schedules = [schedule_factory()]

    manual_gap_factory(gap_settings=gap_settings)

    gap_settings_id = gap_settings.id
    with assert_count_queries(3):
        # select intranet_staff.uid = 123
        # select manual_gap_settings joined services, schedules, gaps
        # update manual_gap_settings is_active = False
        with patch('watcher.tasks.manual_gap.update_manual_gaps.delay'):
            response = client.delete(
                f'/api/watcher/v1/settings/gap/{gap_settings_id}',
            )
    assert response.status_code == status.HTTP_204_NO_CONTENT, response.text

    scope_session.refresh(gap_settings)
    assert gap_settings.is_active is False
