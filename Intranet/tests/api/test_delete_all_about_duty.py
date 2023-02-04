from fastapi import status
from unittest.mock import patch

from watcher.crud.composition import query_compositions_by_services
from watcher.crud.interval import query_intervals_by_service
from watcher.crud.revision import query_revisions_by_service
from watcher.crud.schedule import query_schedules_by_service
from watcher.crud.shift import query_all_shifts_by_service
from watcher.crud.slot import query_slots_by_service
from watcher.tasks.generating_shifts import initial_creation_of_shifts
from watcher.tasks.shift import start_shifts


def test_environment(scope_session, set_production_for_unit_tests, client, schedule_data):
    """
    Эта ручка исключительно для тестовых данных
    set_production_for_unit_tests
    """
    service = schedule_data.schedule.service
    response = client.delete(f'/api/watcher/frontend/service/all_about_duty/{service.id}')

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    data = response.json()['context']['message']
    assert data['ru'] == 'В продакшн окружении нельзя использовать эту ручку'
    assert data['en'] == 'You cannot use this router in a production environment'


def test_delete_all(scope_session, client, schedule_data_with_composition):
    schedule = schedule_data_with_composition.schedule
    service = schedule.service
    initial_creation_of_shifts(schedule.id)

    response = client.delete(f'/api/watcher/frontend/service/all_about_duty/{service.id}')
    assert response.status_code == status.HTTP_204_NO_CONTENT

    assert len(query_schedules_by_service(db=scope_session, service_id=service.id).all()) == 0
    assert len(query_all_shifts_by_service(db=scope_session, service_id=service.id).all()) == 0
    assert len(query_intervals_by_service(db=scope_session, service_id=service.id).all()) == 0
    assert len(query_slots_by_service(db=scope_session, service_id=service.id).all()) == 0
    assert len(query_revisions_by_service(db=scope_session, service_id=service.id).all()) == 0
    assert len(query_compositions_by_services(db=scope_session, service_ids=[service.id]).all()) == 0


def test_called_finish(scope_session, client, schedule_data_with_composition):
    schedule = schedule_data_with_composition.schedule
    service = schedule.service
    initial_creation_of_shifts(schedule.id)

    with patch('watcher.logic.member.abc_client.request_role'):
        start_shifts()

    with patch('watcher.api.routes.service.finish_shift') as mock_finish_shift:
        response = client.delete(f'/api/watcher/frontend/service/all_about_duty/{service.id}')
        assert response.status_code == status.HTTP_204_NO_CONTENT

    assert mock_finish_shift.called
