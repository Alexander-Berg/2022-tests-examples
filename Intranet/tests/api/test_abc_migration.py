import responses

from fastapi import status
from unittest.mock import patch

from watcher.db import AbcMigration
from watcher.config import settings


def test_get_migration(client, abc_migration_factory):
    migration = abc_migration_factory()

    response = client.get(
        f'/api/watcher/v1/abc_migration/{migration.id}'
    )
    assert response.status_code == status.HTTP_200_OK
    assert response.text
    data = response.json()

    assert data['id'] == migration.id
    assert data['abc_schedule_id'] == migration.abc_schedule_id


def test_get_interval_list(client, service_factory, abc_migration_factory):
    service = service_factory()
    migration_in_service_1 = abc_migration_factory(service=service)
    migration_in_service_2 = abc_migration_factory(service=service)
    abc_migration_factory()

    response = client.get(
        '/api/watcher/v1/abc_migration/',
        params={'filter': f'service_id={service.id}'}
    )
    assert response.status_code == status.HTTP_200_OK
    assert response.text
    data = response.json()['result']

    assert len(data) == 2
    assert {element['id'] for element in data} == {migration_in_service_1.id, migration_in_service_2.id}


@responses.activate
def test_prepare_abc_migration_schedules(
    client, schedule_factory, staff_factory, scope_session,
    assert_count_queries, service_factory, member_factory
):
    service = service_factory()
    abc_schedule_id = 123456

    responses.add(
        responses.GET,
        f'{settings.ABC_API_HOST}api/v4/duty/schedules/'
        f'?service={service.id}&fields=id%2Cslug%2Cconsider_other_schedules&page_size=10',
        status=200,
        json={
            "next": None,
            "results": [
                {
                    "id": abc_schedule_id,
                    "consider_other_schedules": True,
                    "slug": "service"
                }
            ]
        }
    )

    with patch('watcher.tasks.abc_migration.prepare_migration_task.delay'):
        response = client.post(
            f'/api/watcher/v1/abc_migration/{service.id}/prepare',
        )
    assert response.status_code == status.HTTP_204_NO_CONTENT, response.text

    migrations = scope_session.query(AbcMigration).all()
    assert len(migrations) == 1
