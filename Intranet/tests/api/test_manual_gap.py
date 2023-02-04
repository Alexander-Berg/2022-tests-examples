import pytest

from fastapi import status


def test_retrieve_gap(client, manual_gap_settings_factory, service_factory, schedule_factory, scope_session,
                      manual_gap_factory, assert_count_queries):
    gap_settings = manual_gap_settings_factory()
    gap_settings.services = [service_factory()]
    gap_settings.schedules = [schedule_factory(service=gap_settings.services[0])]
    gap = manual_gap_factory(
        gap_settings=gap_settings, staff=gap_settings.staff
    )

    gap_id = gap.id
    with assert_count_queries(2):
        # select intranet_staff.uid = 123
        # select manual_gap joined with settings, services, schedules
        response = client.get(
            f'/api/watcher/v1/manual_gaps/{gap_id}'
        )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()
    assert data['id'] == gap_id
    assert len(data['services']) == 1
    assert data['services'][0] == gap_settings.services[0].id
    assert len(data['schedules']) == 1
    assert data['schedules'][0] == gap_settings.schedules[0].id


@pytest.mark.parametrize('filter_by', ('service', 'standalone_service', 'service_in', 'staff'))
def test_filter_service_list_gap(client, manual_gap_settings_factory, service_factory, schedule_factory, scope_session,
                                 manual_gap_factory, staff_factory, member_factory, assert_count_queries, filter_by):
    service = service_factory()
    standalone_service = service_factory()
    schedule = schedule_factory(service=service)

    staff = [staff_factory() for _ in range(4)]
    [member_factory(staff=staff[i], service=service) for i in range(3)]
    member_factory(staff=staff[2], service=standalone_service)

    gap_settings_all_services = manual_gap_settings_factory(staff=staff[0])
    gap_settings_all_schedules = manual_gap_settings_factory(staff=staff[1])
    gap_settings_schedule = manual_gap_settings_factory(staff=staff[2])
    gap_settings_standalone = manual_gap_settings_factory(staff=staff[2])
    gap_settings_not_member = manual_gap_settings_factory(staff=staff[3])

    gap_settings_standalone.services = [standalone_service]

    gap_settings_schedule.services = [service]
    gap_settings_schedule.schedules = [schedule]

    gap_settings_all_schedules.services = [service]
    gap_settings_all_schedules.schedules = []

    gap_settings_all_services.all_services = True
    gap_settings_not_member.all_services = True

    gaps = [
        manual_gap_factory(staff=gap_settings_all_services.staff, gap_settings=gap_settings_all_services),
        manual_gap_factory(staff=gap_settings_all_schedules.staff, gap_settings=gap_settings_all_schedules),
        manual_gap_factory(staff=gap_settings_schedule.staff, gap_settings=gap_settings_schedule),
        manual_gap_factory(staff=gap_settings_standalone.staff, gap_settings=gap_settings_standalone),
        manual_gap_factory(staff=gap_settings_not_member.staff, gap_settings=gap_settings_not_member),
    ]

    filter_params = {}
    expected_gap_ids = {}
    if filter_by == 'staff':
        filter_params = {'filter': f'staff_id={staff[0].id},service_id={service.id},service_id={standalone_service.id}'}
        expected_gap_ids = {gaps[0].id}
    elif filter_by == 'service':
        filter_params = {'filter': f'service_id={service.id}'}
        expected_gap_ids = {gaps[0].id, gaps[1].id, gaps[2].id}
    elif filter_by == 'standalone_service':
        filter_params = {'filter': f'service_id={standalone_service.id}'}
        expected_gap_ids = {gaps[3].id}
    elif filter_by == 'service_in':
        filter_params = {'filter': f'service_id={service.id},service_id={standalone_service.id}'}
        expected_gap_ids = {gaps[0].id, gaps[1].id, gaps[2].id, gaps[3].id}

    with assert_count_queries(2):
        # select intranet_staff.uid = 123
        # select manual_gap filtered and joined with settings, services, schedules
        response = client.get(
            '/api/watcher/v1/manual_gaps/', params=filter_params,
        )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()['result']

    filtered_gap_ids = {obj['id'] for obj in data}
    assert filtered_gap_ids == expected_gap_ids
