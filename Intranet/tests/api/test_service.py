import pytest

from watcher.db import ScheduleShowInServices, Role


@pytest.mark.parametrize('is_responsible', [True, False])
def test_change_related_schedules(
    client, member_factory, role_factory,
    test_request_user, scope_session, service_factory,
    schedule_factory, is_responsible
):
    service = service_factory()
    schedule = schedule_factory()

    assert scope_session.query(ScheduleShowInServices).count() == 0

    if is_responsible:
        member_factory(
            staff=test_request_user,
            service=service,
            role=role_factory(code=Role.RESPONSIBLE)
        )
    response = client.put(
        f'/api/watcher/v1/service/{service.id}/related_schedules/',
        json={
            'related_schedules': [schedule.id]
        },
    )

    if is_responsible:
        assert response.status_code == 204, response.text
        schedules_show = scope_session.query(ScheduleShowInServices).all()
        assert len(schedules_show) == 1
        assert schedules_show[0].schedule_id == schedule.id
        assert schedules_show[0].service_id == service.id
    else:
        assert response.status_code == 403, response.text
        assert scope_session.query(ScheduleShowInServices).count() == 0


def test_remove_related_schedules(
    client, scope_session, service_factory, schedule_factory,
    member_factory, test_request_user, role_factory
):
    service = service_factory()
    schedule = schedule_factory()
    service.related_schedules.append(schedule)
    scope_session.commit()
    member_factory(
        staff=test_request_user,
        service=service,
        role=role_factory(code=Role.RESPONSIBLE)
    )

    assert scope_session.query(ScheduleShowInServices).count() == 1

    response = client.put(
        f'/api/watcher/v1/service/{service.id}/related_schedules/',
        json={
            'related_schedules': []
        },
    )

    assert response.status_code == 204, response.text
    assert scope_session.query(ScheduleShowInServices).count() == 0
