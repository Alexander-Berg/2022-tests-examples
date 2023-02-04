import pytest

from intranet.trip.src.enums import PTStatus, ServiceStatus
from intranet.trip.src.logic.person_trips import ExecuteServicesAction
from intranet.trip.src.exceptions import WorkflowError, PermissionDenied


pytestmark = pytest.mark.asyncio


@pytest.fixture
async def user(f, uow):
    await f.create_person(person_id=1)
    user = await uow.persons.get_user(person_id=1)
    return user


async def _create_person_trip(f, **fields):
    fields['trip_id'] = fields.get('trip_id', 1)
    fields['person_id'] = fields.get('person_id', 1)
    author_id = fields.pop('author_id', 1)
    date_of_birth = fields.pop('date_of_birth', '1990-01-01')
    if author_id != fields['person_id']:
        await f.create_person(person_id=author_id)
    await f.create_person(person_id=fields['person_id'], date_of_birth=date_of_birth)
    await f.create_trip(trip_id=fields['trip_id'], author_id=author_id)
    await f.create_person_trip(**fields)


async def _create_service(f, service_id, **fields):
    fields = {
        'service_id': service_id,
        'person_id': 1,
        'trip_id': 1,
        'provider_order_id': service_id,
        'provider_service_id': service_id,
        'status': ServiceStatus.draft,
        **fields
    }
    await f.create_service(**fields)


@pytest.mark.parametrize('pt_fields, service_fields, exception, exc_message', (
    (
        {'person_id': 2, 'author_id': 2},
        {'person_id': 2, 'service_id': 1},
        PermissionDenied,
        '',
    ),  # Оформляем чужие услуги и ты не автор
    (
        {'aeroclub_trip_id': None},
        None,
        WorkflowError,
        'Missing aeroclub ids',
    ),  # Оформляем услуги, когда командировка не в АК
    (
        {'is_authorized': False},
        None,
        WorkflowError,
        'Person trip is not authorized in Aeroclub',
    ),  # Оформляем услуги неавторизованной командировки
    (
        {'person_id': 3, 'date_of_birth': None},
        None,
        WorkflowError,
        'Birth date is empty',
    ),  # Оформляем услуги при незаполненной дате рождения
    (
        {},
        {'service_id': 1, 'status': ServiceStatus.deleted},
        WorkflowError,
        'Empty service list',
    ),  # Оформляем пустой список услуг
    (
        {'status': PTStatus.executing},
        {'service_id': 1, 'status': ServiceStatus.in_progress},
        WorkflowError,
        'Wrong person trip status',
    ),  # Оформляем услуги уже оформляемой командировки
    (
        {},
        {'service_id': 1, 'provider_document_id': None},
        WorkflowError,
        'Service 1 is without document'
    ),  # Оформляем услуги без документов
))
async def test_check_persmissions(f, uow, user, pt_fields, service_fields, exception, exc_message):
    await _create_person_trip(f, **pt_fields)
    if service_fields is not None:
        await _create_service(f, **service_fields)
    trip_id = pt_fields.get('trip_id', 1)
    person_id = pt_fields.get('person_id', 1)
    action = await ExecuteServicesAction.init(uow, user=user, trip_id=trip_id, person_id=person_id)
    with pytest.raises(exception) as exc_info:
        await action.check_permissions()
    assert str(exc_info.value) == exc_message


@pytest.mark.parametrize('person_id, author_id', (
    (1, 2),  # Отправление услуг себе
    (2, 1),  # Отправление услуг не себе, но ты автор заявки
))
async def test_execute_services(f, uow, user, person_id, author_id):
    await _create_person_trip(f, person_id=person_id, author_id=author_id)
    await _create_service(f, service_id=1, person_id=person_id)

    action = await ExecuteServicesAction.init(uow, user=user, trip_id=1, person_id=person_id)
    await action.execute()

    person_trip = await uow.person_trips.get_person_trip(trip_id=1, person_id=person_id)
    assert person_trip.status == PTStatus.executing
