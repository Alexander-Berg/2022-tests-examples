import pytest

from mock import patch

from intranet.trip.src.enums import ServiceStatus, ServiceType
from ..mocks import MockedAeroclubClient


pytestmark = pytest.mark.asyncio


async def _create_person_trip(f, trip_id, person_id, author_id=None):
    await f.create_person(person_id=person_id)
    author_id = author_id or person_id
    if author_id != person_id:
        await f.create_person(person_id=author_id)
    await f.create_trip(trip_id=trip_id, author_id=author_id)
    await f.create_person_trip(
        trip_id=trip_id,
        person_id=person_id,
        aeroclub_journey_id=1,
        aeroclub_trip_id=1,
        is_authorized=True,
    )


mocked_aeroclub = MockedAeroclubClient()


async def test_service_create(f, client, uow):
    trip_id = person_id = 1
    await _create_person_trip(f, trip_id=trip_id, person_id=person_id)
    with patch('intranet.trip.src.logic.person_trips.aeroclub', mocked_aeroclub):
        response = await client.post(
            url='api/services/',
            json={
                'trip_id': trip_id,
                'person_id': person_id,
                'search_id': 1,
                'option_number': 1,
                'key': 'KEY',
                'detail_index': None,
                'type': 'avia',
            }
        )
    data = response.json()
    assert response.status_code == 201
    assert 'service_id' in data
    service_id = data['service_id']
    assert service_id is not None


async def test_service_detail(f, client):
    trip_id = person_id = service_id = 1
    await _create_person_trip(f, trip_id=trip_id, person_id=person_id)
    await f.create_service(service_id=service_id, trip_id=trip_id, person_id=person_id)

    response = await client.get(f'api/services/{service_id}')
    data = response.json()
    assert response.status_code == 200
    assert data['service_id'] == service_id


@pytest.mark.parametrize('status', (
    ServiceStatus.draft,
    ServiceStatus.new,
    ServiceStatus.in_progress,
    ServiceStatus.reserved,
    ServiceStatus.executed,
    ServiceStatus.verification,
))
async def test_service_remove(f, client, status):
    trip_id = person_id = service_id = 1
    await _create_person_trip(f, trip_id=trip_id, person_id=person_id)
    await f.create_service(
        service_id=service_id,
        trip_id=trip_id,
        person_id=person_id,
        status=status,
    )
    with patch('intranet.trip.src.logic.services.aeroclub', mocked_aeroclub):
        response = await client.delete(f'api/services/{service_id}')
        assert response.status_code == 204


@pytest.mark.parametrize('status', (
    ServiceStatus.deleted,
    ServiceStatus.cancelled,
))
async def test_service_remove_workflow_error(f, client, status):
    trip_id = person_id = service_id = 1
    await _create_person_trip(f, trip_id=trip_id, person_id=person_id)
    await f.create_service(
        service_id=service_id,
        trip_id=trip_id,
        person_id=person_id,
        status=status,
    )
    with patch('intranet.trip.src.logic.services.aeroclub', mocked_aeroclub):
        response = await client.delete(f'api/services/{service_id}')
        assert response.status_code == 422


@pytest.mark.parametrize('params', (
    {
        'provider_profile_id': 1,
        'provider_document_id': 1,
    },
    {
        'provider_profile_id': 1,
        'provider_document_id': 1,
    },
))
async def test_service_document_add(f, client, params):
    trip_id = person_id = service_id = 1
    await _create_person_trip(f, trip_id=trip_id, person_id=person_id)
    await f.create_service(
        service_id=service_id,
        trip_id=trip_id,
        person_id=person_id,
        status=ServiceStatus.draft,
        provider_document_id=None,
    )
    with patch('intranet.trip.src.logic.services.aeroclub', mocked_aeroclub):
        response = await client.post(
            url=f'api/services/{service_id}/add_document',
            json=params,
        )
    assert response.status_code == 204


PROVIDERS = [
    {'service_type': ServiceType.avia, 'code': 'OV', 'name': 'Salam Air', 'name_en': 'Salam Air'},
    {'service_type': ServiceType.avia, 'code': 'S7', 'name': 'Эс как доллар', 'name_en': 'S seven'},
    {'service_type': ServiceType.rail, 'code': 'RZD', 'name': 'РЖД Бонус', 'name_en': 'RZD'},
    {'service_type': ServiceType.hotel, 'code': 'UZ', 'name': 'PEGASUS', 'name_en': 'PEGASUS'},
]


async def create_service_providers(factory, providers_fields: list[dict]) -> None:
    for provider_fields in providers_fields:
        await factory.create_service_provider(**provider_fields)


async def test_service_provider_suggest(f, client):
    await create_service_providers(f, PROVIDERS)
    response = await client.get('/api/services/providers')
    assert response.status_code == 200
    data = response.json()
    assert len(data) == 4


@pytest.mark.parametrize('search_query', ('am', 'Am', 'aM'))
async def test_service_provider_suggest_by_query(f, client, search_query):
    await create_service_providers(f, PROVIDERS)
    response = await client.get(f'/api/services/providers?search_query={search_query}')
    assert response.status_code == 200
    data = response.json()
    assert len(data) == 1

    assert data[0] == PROVIDERS[0]


async def test_service_provider_suggest_by_type(f, client):
    await create_service_providers(f, PROVIDERS)
    response = await client.get('/api/services/providers?service_type=avia')
    assert response.status_code == 200
    data = response.json()
    assert len(data) == 2

    assert data[0] == PROVIDERS[0]
    assert data[1] == PROVIDERS[1]
