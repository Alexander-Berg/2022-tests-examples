from datetime import date
import uuid

import pytest

from intranet.trip.src.api.auth import (
    has_manager_perm,
    has_person_partial_perm,
    has_person_perm,
    has_person_trip_read_perm,
    has_service_read_perm,
    has_transaction_read_perm,
    has_trip_read_perm,
    LazyRolesMap,
    LazyManagersMapByTrip,
)
from intranet.trip.src.enums import ServiceStatus


pytestmark = pytest.mark.asyncio


"""
Ниже при тестировании используются именования:
- author - автор заявки (командировки)
- user - пользователь, от лица которого смотрим (проверяем права)
- person - пользователь, на ресурсы которого смотрим (проверяем права)
"""


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


@pytest.mark.parametrize(
    'person_id, user_id, is_coordinator, is_limited_access, should_have_perm', (
        (1, 2, True, False, True),      # 1
        (1, 2, True, True, True),       # 2
        (1, 2, False, False, True),     # 3
        (1, 2, False, True, False),     # 4
        (1, 1, False, False, True),     # 5
        (1, 1, False, True, True),      # 6
    )
)
async def test_has_person_read_perm(
    f,
    uow,
    person_id,
    user_id,
    is_coordinator,
    is_limited_access,
    should_have_perm,
):
    """
    1) координатор смотрит на профиль
    2) координатор из КПБ смотрит на профиль
    3) обычный пользователь смотрит на чужой профиль
    4) КПБ пользователь смотрит на чужой профиль
    5) обычный пользователь смотрит на свой профиль
    6) КПБ пользователь смотрит на свой профиль
    """
    await f.create_person(
        person_id=user_id,
        is_coordinator=is_coordinator,
        is_limited_access=is_limited_access,
    )
    if person_id != user_id:
        await f.create_person(person_id=person_id)
    user = await uow.persons.get_user(person_id=user_id)
    person = await uow.persons.get_person(person_id=person_id)
    roles_map = LazyRolesMap(uow, person_ids=[person_id])
    has_perm = await has_person_partial_perm(user=user, person=person, roles_map=roles_map)
    assert has_perm == should_have_perm


@pytest.mark.parametrize(
    'person_id, person_holding_id, user_id, user_holding_id, is_coordinator, is_support, should_have_perm', (  # noqa: E501
        (2, 2, 2, 2, False, False, (True, True)),      # 1
        (3, 3, 2, 3, False, False, (True, False)),     # 2
        (3, 4, 2, 3, False, False, (False, False)),    # 3
        (3, 3, 2, 3, True, False, (True, True)),       # 4
        (3, 4, 2, 3, True, False, (False, False)),     # 5
        (3, 1, 2, 1, True, False, (True, True)),       # 6
        (3, 4, 2, 1, True, False, (True, True)),       # 7
        (3, 4, 2, 1, False, True, (True, True)),       # 8
    )
)
async def test_has_person_perm(
    f,
    uow,
    person_id,
    person_holding_id,
    user_id,
    user_holding_id,
    is_support,
    is_coordinator,
    should_have_perm,
):
    """
    (partial, full):
    1) Пользователь смотрит на себя - True, True
    2) Пользователь смотрит другого из того же холдинга - True, False
    3) Пользователь смотрит другого из другого же холдинга - False, False
    4) Координатор холдинга смотрит на другого из того же холдинга - True, True
    5) Координатор холдинга смотрит на другого из другого холдинга - False, False
    6) Координатор яндекса смотрит на другого из того же холдинга - True, True
    7) Координатор яндекса смотрит на другого из другого холдинга - True, True
    8) Сотрудник ПЛП яндекса смотрит сотрудника - True, True
    """
    await f.create_holding(holding_id=person_holding_id)
    await f.create_company(
        company_id=person_holding_id,
        holding_id=person_holding_id,
    )
    if user_holding_id != person_holding_id:
        await f.create_holding(holding_id=user_holding_id)
        await f.create_company(
            company_id=user_holding_id,
            holding_id=user_holding_id,
        )

    await f.create_person(
        person_id=user_id,
        is_coordinator=is_coordinator,
        is_limited_access=False,
        company_id=user_holding_id,
    )
    await f.create_person(
        person_id=person_id,
        company_id=person_holding_id,
        support_id=user_id if is_support else None,
    )

    user = await uow.persons.get_user(person_id=user_id)
    person = await uow.persons.get_person(person_id=person_id)
    has_partial_perm = await has_person_partial_perm(
        user=user,
        person=person,
        roles_map=LazyRolesMap(uow, person_ids=[person_id]),
    )
    has_perm = has_person_perm(user=user, person=person)
    assert (has_partial_perm, has_perm) == should_have_perm, f'{user=} {person=}'


@pytest.mark.parametrize(
    'user_id, is_chief, is_coordinator, is_limited_access, author_id', (
        (1, False, False, False, 3),    # 1
        (2, False, True, False, 3),     # 2
        (2, True, False, False, 3),     # 3
        (3, False, False, False, 3),    # 4
        (4, False, True, True, 3),      # 5
    )
)
async def test_has_service_read_perm(
    f,
    uow,
    user_id,
    is_chief,
    is_coordinator,
    is_limited_access,
    author_id,
):
    """
    1) обычный пользователь просматривает свою услугу
    2) координатор просматривает услуги
    3) шеф просматривает услугу
    4) автор командировки просматривает услугу
    5) координатор из КПБ смотрит на профиль
    """
    trip_id = person_id = service_id = 1
    await _create_person_trip(f, trip_id=trip_id, person_id=person_id, author_id=author_id)
    await f.create_service(service_id=service_id, trip_id=trip_id, person_id=person_id)

    service = await uow.services.get_service(service_id)

    if user_id != person_id:
        await f.create_person(
            person_id=user_id,
            is_coordinator=is_coordinator,
            is_limited_access=is_limited_access,
        )

    if is_chief:
        await f.create_chief_relation(chief_id=user_id, person_id=person_id)

    user_has_perm = await uow.persons.get_user(person_id=user_id)

    roles_map = LazyRolesMap(uow, person_ids=[person_id])
    managers_map = LazyManagersMapByTrip(uow, trip_ids=[trip_id])
    assert await has_service_read_perm(
        user_has_perm,
        service,
        roles_map=roles_map,
        managers_map=managers_map,
    )


@pytest.mark.parametrize('is_limited_access', (True, False))
async def test_not_has_service_read_perm(f, uow, is_limited_access):
    trip_id = service_id = 1
    person_id = 1
    await _create_person_trip(f, trip_id=trip_id, person_id=person_id)
    await f.create_service(service_id=service_id, trip_id=trip_id, person_id=person_id)

    service = await uow.services.get_service(service_id)

    await f.create_person(person_id=2, is_limited_access=is_limited_access)
    user_has_not_perm = await uow.persons.get_user(person_id=2)

    roles_map = LazyRolesMap(uow, person_ids=[person_id])
    managers_map = LazyManagersMapByTrip(uow, trip_ids=[trip_id])
    assert not await has_service_read_perm(
        user_has_not_perm, service, roles_map, managers_map
    )


async def test_author_has_service_read_perm(f, client):
    await f.create_person(person_id=1)
    await f.create_person(person_id=2)
    await f.create_trip(trip_id=2, person_ids=[1, 2], author_id=2)
    await f.create_trip(trip_id=1, person_ids=[1, 2], author_id=1)
    await f.create_service(
        service_id=1,
        trip_id=1,
        person_id=2,
        status=ServiceStatus.draft,
        provider_document_id=None,
    )
    await f.create_service(
        service_id=2,
        trip_id=1,
        person_id=2,
        status=ServiceStatus.draft,
    )

    response = await client.get('api/trips/1/persons/2')
    data = response.json()
    assert len(data['services']) == 2
    data['services'] = sorted(data['services'], key=lambda service: service['service_id'])
    assert data['services'][0]['actions']['add_document']
    assert not data['services'][0]['actions']['change_document']
    assert not data['services'][1]['actions']['add_document']
    assert data['services'][1]['actions']['change_document']
    assert data['services'][0]['actions']['delete']


@pytest.mark.parametrize(
    (
        'author_id, user_id, person_id,'
        'is_support, is_coordinator, is_limited_access, should_have_perm'
    ),
    (
        (1, 2, 1, False, True, False, True),       # 1
        (1, 2, 1, False, True, True, True),        # 2
        (1, 2, 1, False, False, False, False),     # 3
        (1, 2, 1, False, False, True, False),      # 4
        (2, 2, 2, False, False, False, True),      # 5
        (2, 2, 2, False, False, True, True),       # 6
        (2, 2, 1, False, False, False, True),      # 7
        (2, 2, 1, False, False, True, False),      # 8
        (2, 2, 1, True, False, True, True),        # 9
    )
)
async def test_has_person_trip_read_perm(
    f,
    uow,
    author_id,
    user_id,
    person_id,
    is_support,
    is_coordinator,
    is_limited_access,
    should_have_perm,
):
    """
    1) координатор смотрит заявку
    2) координатор из КПБ смотрит заявку
    3) обычный пользователь смотрит чужую заявку
    4) КПБ пользователь смотрит чужую заявку
    5) обычный пользователь смотрит свою заявку
    6) КПБ пользователь смотрит свою заявку
    7) обычный пользователь смотрит созданную им чужую заявку
    8) КПБ пользователь смотрит созданную им чужую заявку
    9) КПБ из ПЛП смотрит созданную свою заявку
    """
    trip_id = 1
    await f.create_person(
        person_id=user_id,
        is_coordinator=is_coordinator,
        is_limited_access=is_limited_access,
    )
    for identifier in {person_id, author_id}:
        if identifier != user_id:
            await f.create_person(
                person_id=identifier,
                support_id=user_id if is_support else None,
            )
    await f.create_trip(trip_id=trip_id, author_id=author_id, person_ids=[person_id])

    user = await uow.persons.get_user(person_id=user_id)
    person_trip = await uow.person_trips.get_detailed_person_trip(
        trip_id=trip_id,
        person_id=person_id,
    )
    roles_map = LazyRolesMap(uow, person_ids=[person_id])
    has_perm = await has_person_trip_read_perm(user, person_trip, roles_map)
    assert has_perm == should_have_perm


async def test_has_not_perm_trip_list(f, client):
    await f.create_purpose(purpose_id=1)
    await f.create_person(person_id=2)
    await f.create_person()
    await f.create_trip(person_ids=[2], author_id=2)

    response = await client.get('api/trips/')
    assert response.status_code == 200
    data = response.json()

    assert data['count'] == 0
    assert data['page'] == 1
    assert len(data['data']) == 0


async def test_has_perm_trip_list_for_author(f, client):
    await f.create_purpose(purpose_id=1)
    await f.create_person()
    await f.create_person(person_id=2)
    await f.create_trip(person_ids=[2], author_id=1)

    response = await client.get('api/trips/')
    assert response.status_code == 200
    data = response.json()

    assert data['count'] == 1
    assert data['page'] == 1
    assert len(data['data']) == 1


@pytest.mark.parametrize(
    (
        'author_id, user_id, person_id, is_support, is_coordinator,'
        'is_chief, is_manager, is_limited_access, should_have_perm'
    ),
    (
        (1, 1, 1, False, False, False, False, False, True),       # 1
        (1, 2, 3, False, True, False, False, False, True),        # 2
        (1, 2, 2, False, False, False, False, False, True),       # 3
        (1, 2, 3, False, False, False, True, False, True),        # 4
        (1, 2, 3, False, False, True, False, False, True),        # 5
        (1, 1, 2, False, False, False, False, False, True),       # 6
        (1, 2, 3, False, False, False, False, False, False),      # 7
        (1, 2, 3, False, False, False, False, True, False),       # 8
        (1, 2, 3, False, False, False, True, True, True),         # 9
        (1, 2, 3, True, False, False, False, True, True),         # 10
    )
)
async def test_has_trip_read_perm(
    f,
    uow,
    author_id,
    user_id,
    person_id,
    is_support,
    is_coordinator,
    is_chief,
    is_manager,
    is_limited_access,
    should_have_perm,
):
    """
    1) Автор смотрит на свою командировку
    2) Координатор смотрит на командировку другого пользователя
    3) Один из участников смотрит на свою командировку
    4) Менеджер смотрит на командировку пользователя
    5) Шеф одного из участников смотрит на командировку
    6) Автор заявки смотрит на командировку другого пользователя
    7) Обычный пользователь смотрит на чужую командировку
    8) КПБ смотрит на чужую командировку
    9) КПБ-мендежер смотрит на чужую командировку
    10) КПБ-поддержка смотрит на свою командировку
    """
    participant_ids = [person_id, person_id + 10, person_id + 11]
    await f.create_person(
        person_id=user_id,
        is_coordinator=is_coordinator,
        is_limited_access=is_limited_access,
    )
    for pid in set([author_id, person_id] + participant_ids):
        if pid != user_id:
            await f.create_person(
                person_id=pid,
                support_id=user_id if is_support else None,
            )

    trip_id = 1
    await f.create_trip(trip_id=trip_id, author_id=author_id)
    for participant_id in participant_ids:
        await f.create_person_trip(
            trip_id=trip_id,
            person_id=participant_id,
            aeroclub_journey_id=1,
            aeroclub_trip_id=1,
            is_authorized=True,
            manager_id=user_id if is_manager and participant_id == person_id else None,
        )

    if is_chief:
        await f.create_chief_relation(chief_id=user_id, person_id=person_id)

    roles_map = LazyRolesMap(uow, person_ids=participant_ids)
    user = await uow.persons.get_user(person_id=user_id)
    trip = await uow.trips.get_detailed_trip(trip_id=trip_id)
    has_perm = await has_trip_read_perm(user, trip, roles_map)

    assert has_perm == should_have_perm


@pytest.mark.parametrize(
    (
        'holding_id, company_id, observer_company_id, observer_holding_id, '
        'is_manager, is_coordinator, should_have_perm'
    ),
    (
        (1, 1, 2, 2, False, False, False),  # 1
        (1, 1, 2, 2, True, False, True),    # 2
        (2, 2, 2, 3, False, True, True),    # 3
        (2, 2, 3, 3, False, True, False),   # 4
        (2, 2, 1, 1, False, True, True),    # 5
    ),
)
async def test_has_transaction_read_perm(
    f,
    uow,
    holding_id,
    company_id,
    observer_company_id,
    observer_holding_id,
    is_manager,
    is_coordinator,
    should_have_perm,
):
    """
    1) Не координатор и не менеджер смотрят транзакцию
    2) Менеджер смотрит транзакцию, в которой он менеджер
    3) Координатор смотрит транзакцию, относящуюся к его холдингу
    4) Координатор смотрит транзакцию, не относящуюся к его холдингу
    5) Координатор Яндекса смотрит транзакцию
    """
    trip_id = person_id = 1
    observer_id = 2
    transaction_id = uuid.uuid4()

    for hid in (holding_id, observer_holding_id):
        await f.create_holding(holding_id=hid)

    for cid, hid in ((company_id, holding_id), (observer_company_id, observer_holding_id)):
        await f.create_company(company_id=cid, holding_id=hid)

    await f.create_person(person_id=person_id, company_id=company_id)
    await f.create_person(
        person_id=observer_id,
        company_id=observer_company_id,
        is_coordinator=is_coordinator,
    )

    await f.create_trip(trip_id=trip_id, author_id=person_id)
    await f.create_person_trip(
        trip_id=trip_id,
        person_id=person_id,
        aeroclub_journey_id=1,
        aeroclub_trip_id=1,
        is_authorized=True,
        manager_id=observer_id if is_manager else None,
    )

    await f.create_transaction(
        transaction_id,
        company_id,
        person_id,
        trip_id,
        execution_date=date(2022, 2, 2),
    )

    user = await uow.persons.get_user(person_id=observer_id)
    transaction = await uow.billing_transactions.get_transaction(transaction_id)
    managers_map = LazyManagersMapByTrip(uow, trip_ids=[trip_id])
    has_perm = await has_transaction_read_perm(uow, user, transaction, managers_map)
    assert has_perm == should_have_perm


async def test_has_manager_perm(uow, f):
    user_id = 11
    person_id = 13
    trip_id = 1

    await f.create_person(person_id=user_id, is_limited_access=True, is_active=False)
    await f.create_person(person_id=person_id)
    await f.create_trip(trip_id=trip_id, author_id=person_id)
    await f.create_person_trip(
        trip_id=trip_id,
        person_id=person_id,
        aeroclub_journey_id=1,
        aeroclub_trip_id=1,
        manager_id=user_id,
    )

    user = await uow.persons.get_user(person_id=user_id)
    person = await uow.persons.get_person(person_id=person_id)
    roles_map = LazyRolesMap(uow, person_ids=[person_id])
    has_perm = await has_manager_perm(user, person, roles_map)
    assert has_perm
