import pytest
from copy import deepcopy
from fastapi import Request
from mock import patch

from intranet.trip.src.enums import Citizenship, PTStatus, ServiceStatus

from ..factories import date_from_str, date_middle_str, date_to_str, date_to_modified_str

pytestmark = pytest.mark.asyncio


base_trip_form_data = {
    'city_from': 'Ivanovo',
    'city_to': 'Moscow',
    'country_from': 'Russia',
    'country_to': 'Russia',
    'date_from': date_from_str,
    'date_to': date_to_str,
    'purposes': [2],
    'description': 'updated description',
    'with_days_off': True,
    'persons': ['2'],
    'conf_details': {
        'conf_date_from': date_from_str,
        'conf_date_to': date_to_str,
        'conference_name': 'conference name',
        'conference_url': 'conference url',
        'program_url': 'program url',
        'price': '123',
        'is_another_city': True,
        'promo_code': 'promo code',
        'ticket_type': 'ticket type',
    },
    'comment': 'test comment',
}
trip_form_data = {
    **base_trip_form_data,
    'provider_city_from_id': 123,
    'provider_city_to_id': 456,
}
trip_form_data_deprecated = {
    **base_trip_form_data,
    'provider_city_from_id': 123,
    'provider_city_to_id': 456,
}
expected_simple_route = [{
    'city': {
        'name': {
            'ru': 'Ivanovo',
            'en': None,
        },
    },
    'country': {
        'name': {
            'ru': 'Russia',
            'en': None,
        },
    },
    'aeroclub_city_id': 123,
    'provider_city_id': '123',
    'date': date_from_str,
    'need_hotel': True,
    'need_transfer': True,
}, {
    'city': {
        'name': {
            'ru': 'Moscow',
            'en': None,
        },
    },
    'country': {
        'name': {
            'ru': 'Russia',
            'en': None,
        },
    },
    'aeroclub_city_id': 456,
    'provider_city_id': '456',
    'date': date_to_str,
    'need_hotel': True,
    'need_transfer': True,
}]

complex_route_in = [{
    'city': 'Moscow',
    'country': 'Russia',
    'provider_city_id': 1,
    'date': date_from_str,
}, {
    'city': 'London',
    'country': 'United Kingdom',
    'provider_city_id': 2,
    'date': date_middle_str,
    'need_transfer': False,
}, {
    'city': 'Paris',
    'country': 'France',
    'provider_city_id': 3,
    'date': date_to_str,
    'need_hotel': False,
}]
complex_route_in = [{
    'city': 'Moscow',
    'country': 'Russia',
    'provider_city_id': 1,
    'date': date_from_str,
}, {
    'city': 'London',
    'country': 'United Kingdom',
    'provider_city_id': 2,
    'date': date_middle_str,
    'need_transfer': False,
}, {
    'city': 'Paris',
    'country': 'France',
    'provider_city_id': 3,
    'date': date_to_str,
    'need_hotel': False,
}]
complex_route_in_deprecated = [{
    'city': 'Moscow',
    'country': 'Russia',
    'aeroclub_city_id': 1,
    'date': date_from_str,
}, {
    'city': 'London',
    'country': 'United Kingdom',
    'aeroclub_city_id': 2,
    'date': date_middle_str,
    'need_transfer': False,
}, {
    'city': 'Paris',
    'country': 'France',
    'aeroclub_city_id': 3,
    'date': date_to_str,
    'need_hotel': False,
}]
expected_complex_route = [{
    'city': {
        'name': {
            'ru': 'Moscow',
            'en': None,
        },
    },
    'country': {
        'name': {
            'ru': 'Russia',
            'en': None,
        },
    },
    'aeroclub_city_id': 1,
    'provider_city_id': '1',
    'date': date_from_str,
    'need_hotel': True,
    'need_transfer': True,
}, {
    'city': {
        'name': {
            'ru': 'London',
            'en': None,
        },
    },
    'country': {
        'name': {
            'ru': 'United Kingdom',
            'en': None,
        },
    },
    'aeroclub_city_id': 2,
    'provider_city_id': '2',
    'date': date_middle_str,
    'need_hotel': True,
    'need_transfer': False,
}, {
    'city': {
        'name': {
            'ru': 'Paris',
            'en': None,
        },
    },
    'country': {
        'name': {
            'ru': 'France',
            'en': None,
        },
    },
    'aeroclub_city_id': 3,
    'provider_city_id': '3',
    'date': date_to_str,
    'need_hotel': False,
    'need_transfer': True,
}]


async def _create_trip(f, client, data):
    await f.create_purpose()
    return await client.post('api/trips', json=data)


@pytest.mark.parametrize('trip_data', (trip_form_data, trip_form_data_deprecated))
async def test_trip_create(f, client, trip_data):
    form_data = {
        **trip_data,
        'purposes': [1],
        'person_trips': [{'person_uid': '1'}],
    }
    await f.create_person()
    response = await _create_trip(f, client, form_data)

    assert response.status_code == 201
    data = response.json()
    trip_id = data['trip_id']
    assert trip_id is not None

    response = await client.get(f'api/trips/{trip_id}')
    assert response.status_code == 200
    data = response.json()
    assert 'created_at' in data
    assert 'updated_at' in data
    del data['created_at']
    del data['updated_at']
    assert 'author' in data
    assert 'conf_details' in data
    assert 'person_trips' in data
    assert len(data['person_trips']) == 1
    person_id = data['person_trips'][0]['person']['person_id']
    assert 'purposes' in data
    assert len(data['purposes']) == 1

    assert 'route' in data
    assert data['route'] == expected_simple_route

    response = await client.get(f'api/trips/{trip_id}/persons/{person_id}')
    assert response.status_code == 200
    data = response.json()
    assert 'person' in data
    assert 'conf_details' in data

    assert 'route' in data
    assert data['route'] == expected_simple_route


async def _create_environment(f, persons):
    for company_id in (1, 2):
        await f.create_holding(holding_id=company_id)
        await f.create_company(company_id=company_id, holding_id=company_id)
    for p in persons:
        await f.create_person(
            person_id=p['person_id'],
            company_id=p['company_id'],
        )


async def test_group_trip_create_correct(f, client):
    form_data = {
        **trip_form_data,
        'purposes': [1],
        'person_trips': [{'person_uid': '1'}, {'person_uid': '2'}],
    }
    await _create_environment(
        f,
        persons=[
            {'person_id': '1', 'company_id': '1', },
            {'person_id': '2', 'company_id': '1', },
        ],
    )
    response = await _create_trip(f, client, form_data)
    assert response.status_code == 201


async def test_group_trip_create_incorrect(f, client):
    form_data = {
        **trip_form_data,
        'purposes': [1],
        'person_trips': [{'person_uid': '1'}, {'person_uid': '2'}, {'person_uid': '3'}],
    }
    await _create_environment(
        f,
        persons=[
            {'person_id': '1', 'company_id': '1', },
            {'person_id': '2', 'company_id': '2', },
            {'person_id': '3', 'company_id': '1', },
        ],
    )
    with patch('intranet.trip.src.api.endpoints.trips.settings.YANDEX_HOLDING_ID', 999):
        response = await _create_trip(f, client, form_data)
        assert response.status_code == 400


@pytest.mark.parametrize('route_data', (complex_route_in_deprecated, complex_route_in))
async def test_complex_route_trip_create(f, uow, client, route_data):
    form_data = {
        **base_trip_form_data,
        'purposes': [1],
        'person_trips': [{'person_uid': '1'}],
        'route': route_data,
    }
    await f.create_person()
    response = await _create_trip(f, client, form_data)

    assert response.status_code == 201, response.json()
    data = response.json()
    trip_id = data['trip_id']
    assert trip_id is not None

    response = await client.get(f'api/trips/{trip_id}')
    assert response.status_code == 200
    data = response.json()
    person_id = data['person_trips'][0]['person']['person_id']

    assert 'route' in data
    assert data['route'] == expected_complex_route

    trip = await uow.trips.get_detailed_trip(trip_id)
    assert trip.aeroclub_city_from_id == 1
    assert trip.aeroclub_city_to_id == 3
    assert trip.provider_city_from_id == '1'
    assert trip.provider_city_to_id == '3'

    response = await client.get(f'api/trips/{trip_id}/persons/{person_id}')
    assert response.status_code == 200
    data = response.json()

    assert 'route' in data
    assert data['route'] == expected_complex_route


async def test_trip_create_bad_conf_details(f, client):
    conf_details = {
        **trip_form_data['conf_details'],
        'conference_name': 'conf' * 128,
    }
    persons = ['1']
    form_data = {
        **trip_form_data,
        'conf_details': conf_details,
        'purposes': [1],
        'persons': persons,
    }
    await f.create_company(company_id=1, aeroclub_company_id=None)
    await f.create_person(person_id=1, company_id=1)
    await f.create_purpose()
    response = await client.post('api/trips', json=form_data)
    assert response.status_code == 422


async def test_trip_create_bad_company(f, client):
    persons = ['1']
    form_data = {**trip_form_data, 'purposes': [1], 'persons': persons}
    await f.create_company(company_id=1, aeroclub_company_id=None)
    await f.create_person(person_id=1, company_id=1)
    await f.create_purpose()
    response = await client.post('api/trips', json=form_data)
    assert response.status_code == 422


async def test_trip_list(f, client):
    await f.create_purpose(purpose_id=1)
    await f.create_person()
    await f.create_trip(person_ids=[1])

    response = await client.get('api/trips/')
    assert response.status_code == 200
    data = response.json()

    assert data['count'] == 1
    assert data['page'] == 1
    assert len(data['data']) == 1


@pytest.mark.parametrize('user_id, count', (
    (1, 3),  # user without is_limited_access sees all person-trips
    (2, 1),  # is_limited_access sees only own person-trips
    (3, 3),  # manager sees all person-trips where he is a manager
))
async def test_person_trips_list(user_id, count, f, client):
    from intranet.trip.src.middlewares.auth import DevMiddleware

    class MockRequest(Request):
        def __init__(self):
            pass

    blackbox_user_mocked = await DevMiddleware.get_user_data_from_bb(MockRequest())
    user = await DevMiddleware.get_user_or_error(MockRequest(), blackbox_user_mocked)
    user.person_id = user_id
    user.is_limited_access = user_id in (2, 3)

    async def get_user_or_error_mock(self, request, blackbox_user):
        return user

    await f.create_person(person_id=1)
    await f.create_person(person_id=2, is_limited_access=True)
    await f.create_person(person_id=3, is_limited_access=True)
    await f.create_person(person_id=4)

    await f.create_trip(
        trip_id=1,
        person_ids=[1, 2, 4],
        manager_ids=[3, 3, None],
    )
    with patch(
        'intranet.trip.src.middlewares.auth.DevMiddleware.get_user_or_error',
        get_user_or_error_mock,
    ):
        response = await client.get('api/trips/1')
    assert response.status_code == 200
    data = response.json()
    assert len(data['person_trips']) == count


@pytest.mark.parametrize('trip_id, status', (
    (1, 200),
    (123, 404),
))
async def test_trip_detail_not_found(f, client, trip_id, status):
    await f.create_person()
    await f.create_trip()

    response = await client.get(f'api/trips/{trip_id}')
    assert response.status_code == status


@pytest.mark.parametrize('trip_id, status', (
    (1, 200),
    (123, 404),
))
async def test_trip_update(f, client, trip_id, status):
    await f.create_person()
    await f.create_trip()
    await f.create_person(person_id=2, uid='2')
    await f.create_purpose(purpose_id=2)
    response = await client.put(f'api/trips/{trip_id}', json=trip_form_data)
    assert response.status_code == status


async def test_person_trip_detail_200(f, client):
    person_id = 1
    trip_id = 1
    await f.create_person(person_id=person_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])

    response = await client.get(f'api/trips/{trip_id}/persons/{person_id}')
    assert response.status_code == 200
    data = response.json()
    assert data['person']


async def test_person_trip_detail_404_on_wrong_trip(f, client):
    person_id = 1
    trip_id = 1
    await f.create_person(person_id=person_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])

    response = await client.get(f'api/trips/{trip_id+100500}/persons/{person_id}')
    assert response.status_code == 404


async def test_person_trip_detail_404_on_wrong_person(f, client):
    person_id = 1
    trip_id = 1
    await f.create_person(person_id=person_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])

    response = await client.get(f'api/trips/{trip_id}/persons/{person_id+100500}')
    assert response.status_code == 404


@pytest.mark.parametrize('is_creation', (False, True))
async def test_person_trip_create_and_update(f, client, is_creation):
    trip_id = 1
    person_id = 1
    purpose_id = 2
    await f.create_city(city_id=1)
    await f.create_person(person_id=person_id)
    await f.create_purpose(purpose_id=purpose_id)
    person_ids = [] if is_creation else [person_id]
    await f.create_trip(trip_id=trip_id, purpose_ids=[purpose_id], person_ids=person_ids)
    await f.create_person_document(person_id=person_id, document_id=1)

    response = await client.put(
        url=f'api/trips/{trip_id}/persons/{person_id}',
        json={
            'purposes': [2],
            'travel_details': None,
            'conf_details': None,
            'documents': [1],
            'is_hidden': False,
            'with_days_off': False,
        },
    )
    assert response.status_code == 200


@pytest.mark.parametrize('holding_id, is_coordinator, status', (
    (1, False, 200),
    (2, True, 404),
))
async def test_group_trip_add_person(f, client, holding_id, is_coordinator, status):
    """
    В одну командировку:
    - можно добавить людей из разных организаций, но одного холдинга
    - нельзя добавить людей из разных холдингов даже с правами координатора Яндекс
    """
    await f.create_city(city_id=1)
    await f.create_holding(holding_id=1)
    await f.create_company(company_id=1, holding_id=1)
    await f.create_person(person_id=1, company_id=1, is_coordinator=is_coordinator)
    await f.create_purpose(purpose_id=2)
    await f.create_trip(trip_id=1, purpose_ids=[2], person_ids=[1])

    await f.create_holding(holding_id=holding_id)
    await f.create_company(company_id=2, holding_id=holding_id)
    await f.create_person(person_id=2, company_id=2)
    response = await client.put(
        url='api/trips/1/persons/2',
        json={
            'purposes': [],
            'documents': [],
            'is_hidden': False,
            'with_days_off': False,
            'description': '',
        },
    )
    assert response.status_code == status


async def test_person_trip_partial_update(f, client):
    trip_id = 1
    person_id = 1
    await f.create_person(person_id=person_id)
    await f.create_trip(trip_id=trip_id)

    await f.create_person_trip(
        trip_id=trip_id,
        person_id=person_id,
        with_days_off=True,
        description='description',
    )
    response = await client.patch(
        url=f'api/trips/{trip_id}/persons/{person_id}',
        json={
            'description': 'description2',
        },
    )
    assert response.status_code == 200

    response = await client.get(url=f'api/trips/{trip_id}/persons/{person_id}')
    data = response.json()
    assert data['description'] == 'description2'
    assert data['with_days_off'] is True


@pytest.mark.parametrize('update_fields', (
    {
        'date': date_to_modified_str,
    },
    {
        'date': date_to_modified_str,
        'need_hotel': True,
        'need_transfer': True,
    },
    {
        'date': date_to_modified_str,
        'need_hotel': False,
        'need_transfer': False,
    },
    {
        'date': date_to_modified_str,
        'need_hotel': True,
        'need_transfer': False,
    },
    {
        'date': date_to_modified_str,
        'need_hotel': False,
        'need_transfer': True,
    },
    {
        'need_transfer': False,
    },
    {
        'need_hotel': False,
    },
    {
        'need_hotel': False,
        'need_transfer': True,
    },
    {
        'need_hotel': True,
        'need_transfer': False,
    },
))
async def test_person_trip_partial_update_route(f, client, update_fields):
    trip_id = 1
    person_id = 1
    await f.create_city(city_id=1)
    await f.create_person(person_id=person_id)
    await f.create_trip(trip_id=trip_id, route=complex_route_in)

    await f.create_person_trip(
        trip_id=trip_id,
        person_id=person_id,
        with_days_off=True,
        description='description',
        route=complex_route_in,
    )

    modified_route = deepcopy(complex_route_in)
    modified_route[-1] |= update_fields
    response = await client.patch(
        url=f'api/trips/{trip_id}/persons/{person_id}',
        json={
            'route': modified_route,
        },
    )
    assert response.status_code == 200

    expected_modified = deepcopy(expected_complex_route)
    expected_modified[-1] |= update_fields
    response = await client.get(url=f'api/trips/{trip_id}/persons/{person_id}')
    data = response.json()

    assert 'route' in data
    assert data['route'] == expected_modified
    if update_fields.get('date'):
        assert data['gap_date_to'] == update_fields['date']


@pytest.mark.parametrize('cancel_reason_json', (
    {},
    {'cancellation_reason': None},
    {'cancellation_reason': ''},
    {'cancellation_reason': '\n\n\t хз \n'},
))
async def test_person_trip_cancel_incorrect(f, client, cancel_reason_json):
    trip_id = 1
    person_id = 1
    await f.create_person(person_id=person_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])
    await f.create_person_conf_details(trip_id=trip_id, person_id=person_id)

    response = await client.post(
        url=f'api/trips/{trip_id}/persons/{person_id}/cancel',
        json=cancel_reason_json,
    )
    assert response.status_code == 422


async def test_person_trip_cancel_correct(f, client):
    trip_id = 1
    person_id = 1
    await f.create_person(person_id=person_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])
    await f.create_person_conf_details(trip_id=trip_id, person_id=person_id)

    response = await client.post(
        url=f'api/trips/{trip_id}/persons/{person_id}/cancel',
        json={
            'cancellation_reason': 'some reason',
        },
    )
    assert response.status_code == 204


@pytest.mark.parametrize('trip_id, document_id, status', (
    (100, 1, 404),
    (1, 100, 404),
    (100, 100, 404),
    (1, 1, 204),
))
async def test_person_trip_document_add(f, client, trip_id, document_id, status):
    await f.create_person(person_id=1)
    await f.create_trip(trip_id=1, person_ids=[1])
    await f.create_person_document()

    response = await client.put(f'api/trips/{trip_id}/documents/{document_id}')
    assert response.status_code == status


async def test_person_trip_document_add_duplicate(f, client):
    await f.create_person(person_id=1)
    await f.create_trip(trip_id=1, person_ids=[1])
    await f.create_person_document(person_id=1, document_id=1)
    await f.create_person_trip_document(trip_id=1, person_id=1, document_id=1)

    response = await client.put('api/trips/1/documents/1')
    assert response.status_code == 204


@pytest.mark.parametrize('trip_id, document_id, status', (
    (100, 1, 204),
    (1, 100, 204),
    (100, 100, 204),
    (1, 1, 204),
))
async def test_person_trip_document_remove(f, client, trip_id, document_id, status):
    await f.create_person(person_id=1)
    await f.create_trip(trip_id=1, person_ids=[1])
    await f.create_person_document(document_id=1, person_id=1)
    await f.create_person_trip_document(trip_id=1, person_id=1, document_id=1)

    response = await client.delete(f'api/trips/{trip_id}/documents/{document_id}')
    assert response.status_code == status


@pytest.mark.parametrize('country, status, pt_status, service_status', (
    (Citizenship.KZ, 204, PTStatus.verification, ServiceStatus.verification),
    (Citizenship.RU, 422, PTStatus.new, ServiceStatus.draft),
    (None, 422, PTStatus.new, ServiceStatus.draft),
))
async def test_person_trip_check_services(
        f,
        uow,
        client,
        country,
        status,
        pt_status,
        service_status,
):
    trip_id = 1
    person_id = 1
    await f.create_company(company_id=1, country=country)
    await f.create_person(person_id=person_id, company_id=1)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])
    await f.create_service(service_id=1, trip_id=trip_id, person_id=1)

    response = await client.post(
        url=f'api/trips/{trip_id}/persons/{person_id}/send_to_verification',
    )

    assert response.status_code == status

    person_trip = await uow.person_trips.get_detailed_person_trip(trip_id, person_id)
    assert person_trip.status == pt_status
    assert person_trip.services[0].status == service_status


async def test_person_trip_approve(f, uow, client):
    await f.create_company(company_id=1)
    trip_id = 1
    person_id = 2
    await f.create_person(person_id=1, is_coordinator=True)
    await f.create_person(person_id=person_id, company_id=1)
    await f.create_approver_relation(approver_id=1, person_id=person_id)
    await f.create_trip(trip_id=trip_id)
    await f.create_person_trip(trip_id=trip_id, person_id=person_id, is_approved=False)

    with patch('intranet.trip.src.logic.person_trips.settings.IS_YA_TEAM', False):
        response = await client.post(url=f'/api/trips/{trip_id}/persons/{person_id}/approve')
    assert response.status_code == 204
