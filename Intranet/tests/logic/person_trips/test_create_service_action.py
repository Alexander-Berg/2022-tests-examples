import pytest
from mock import patch

from intranet.trip.src.enums import PTStatus
from intranet.trip.src.logic.person_trips import CreateServiceAction
from intranet.trip.src.exceptions import WorkflowError, PermissionDenied
from intranet.trip.src.api.schemas import ServiceCreate
from intranet.trip.src.enums import (
    ServiceType,
    ServiceStatus,
    OverpriceWorkflow,
)

from ...mocks import MockedAeroclubClient


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
    if author_id != fields['person_id']:
        await f.create_person(person_id=author_id)
    await f.create_person(person_id=fields['person_id'])
    await f.create_trip(trip_id=fields['trip_id'], author_id=author_id)
    await f.create_person_trip(**fields)


@pytest.mark.parametrize('person_trip_fields, exception, exc_message', (
    (
        {'person_id': 2, 'author_id': 2},
        PermissionDenied,
        '',
    ),  # Создаем услугу для другого пользователя и ты не автор
    (
        {'aeroclub_trip_id': None},
        WorkflowError,
        'Missing aeroclub ids',
    ),  # Создаем услугу, когда командировка еще не в АК
    (
        {'is_authorized': False},
        WorkflowError,
        'Person trip is not authorized in Aeroclub',
    ),  # Создаем услугу неавторизованной командировки
    (
        {'status': PTStatus.cancelled},
        WorkflowError,
        'Wrong person trip status',
    ),  # Создаем услугу отмененной командировки
    (
        {'is_offline': True},
        WorkflowError,
        'Services are not available in offline trips',
    ),  # Создаем услугу в оффлайн командировке
))
async def test_check_persmissions(f, uow, user, person_trip_fields, exception, exc_message):
    await _create_person_trip(f, **person_trip_fields)
    trip_id = person_trip_fields.get('trip_id', 1)
    person_id = person_trip_fields.get('person_id', 1)
    action = await CreateServiceAction.init(uow, user=user, trip_id=trip_id, person_id=person_id)
    with pytest.raises(exception) as exc_info:
        await action.check_permissions()
    assert str(exc_info.value) == exc_message


@pytest.mark.parametrize('person_id, author_id', (
    (1, 2),  # Добавлене себе услуги
    (2, 1),  # Добавлене не себе услуги, но ты автор заявки
))
async def test_create_service(f, uow, user, person_id, author_id):
    await _create_person_trip(f, author_id=author_id, person_id=person_id)

    action = await CreateServiceAction.init(uow, user=user, trip_id=1, person_id=person_id)
    service_create = ServiceCreate(
        trip_id=1,
        person_id=person_id,
        search_id=1,
        option_number=1,
        key='key',
        detail_index=0,
        type=ServiceType.avia,
    )
    with patch('intranet.trip.src.logic.person_trips.aeroclub', MockedAeroclubClient()):
        service_id = await action.execute(service_create)
        assert service_id is not None

    service = await uow.services.get_service(service_id)
    assert service.type == ServiceType.avia
    assert service.provider_order_id == 1
    assert service.provider_service_id == 1
    assert service.status == ServiceStatus.draft


@pytest.mark.parametrize('overprice_workflow, has_notify', (
    (OverpriceWorkflow.difficult_approve, True),
    (OverpriceWorkflow.extra_payment, True),
    (None, False),
))
async def test_create_service_with_price_overhead(
    f, uow, user, overprice_workflow, has_notify,
):
    await _create_person_trip(f)

    action = await CreateServiceAction.init(uow, user=user, trip_id=1, person_id=1)
    service_create = ServiceCreate(
        trip_id=1,
        person_id=1,
        search_id=1,
        option_number=1,
        key='key',
        detail_index=0,
        type=ServiceType.avia,
        overprice_workflow=overprice_workflow,
    )
    with patch('intranet.trip.src.logic.person_trips.aeroclub', MockedAeroclubClient()):
        service_id = await action.execute(service_create)
        assert service_id is not None

    if has_notify:
        assert 'notify_by_tracker_comment_task' == uow._jobs[0][0]
        assert 'overprice_service.jinja2' == uow._jobs[0][1]['template_name']
    else:
        assert uow._jobs == []
