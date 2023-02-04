import pytest
from fastapi import Request
from mock import patch

from intranet.trip.src.models import UserUid
from intranet.trip.src.logic.persons import PassportPersonalData


pytestmark = pytest.mark.asyncio


async def test_get_person_not_found(client):
    response = await client.get('api/persons/1/')
    assert response.status_code == 404


async def test_get_person(f, client):
    await f.create_person(person_id=1)
    response = await client.get('api/persons/1/')

    assert response.status_code == 200
    data = response.json()
    assert data['first_name'] == 'Test'
    assert data['gender'] == 'male'
    assert not data['is_dismissed']
    assert data['last_name'] == 'Test'
    assert data['person_id'] == 1
    assert data['uid'] == '1'
    assert data['has_date_of_birth']


async def test_get_person_by_limited_manager(f, client):
    from intranet.trip.src.middlewares.auth import DevMiddleware

    user_id = 3
    person_id = 1

    class MockRequest(Request):
        def __init__(self):
            pass

    blackbox_user_mocked = await DevMiddleware.get_user_data_from_bb(MockRequest())
    user = await DevMiddleware.get_user_or_error(MockRequest(), blackbox_user_mocked)
    user.person_id = user_id
    user.is_limited_access = True

    async def get_user_or_error_mock(self, request, blackbox_user):
        return user

    await f.create_person(person_id=person_id)
    await f.create_person(person_id=user_id, is_limited_access=True)
    await f.create_trip(
        trip_id=1,
        person_ids=[person_id],
        manager_ids=[user_id],
    )

    with patch(
        'intranet.trip.src.middlewares.auth.DevMiddleware.get_user_or_error',
        get_user_or_error_mock,
    ):
        response = await client.get(f'api/persons/{person_id}/')

    assert response.status_code == 200
    data = response.json()
    assert data['first_name'] == 'Test'


async def test_get_dismissed_person(f, client):
    await f.create_person(person_id=1, is_dismissed=True)
    response = await client.get('api/persons/1/')

    assert response.status_code == 200
    data = response.json()
    assert data == {
        'is_dismissed': True,
        'is_limited_access': False,
        'login': 'user',
        'person_id': 1,
        'uid': '1',
        'gender': None,
        'first_name': None,
        'first_name_ac_en': None,
        'last_name': None,
        'last_name_ac_en': None,
        'middle_name': None,
        'middle_name_ac_en': None,
        'is_offline_trip': False,
        'has_date_of_birth': False,
        'has_first_name_en': False,
        'has_last_name_en': False,
    }


async def test_person_document_list_person_not_found(client):
    response = await client.get('api/persons/1/documents')
    assert response.status_code == 404


async def test_person_document_list(f, client):
    await f.create_person(person_id=1)
    await f.create_person_document(person_id=1, document_id=1)
    await f.create_person_document(person_id=1, document_id=2, is_deleted=True)

    response = await client.get('api/persons/1/documents')

    assert response.status_code == 200
    data = response.json()
    assert len(data) == 1
    assert data[0]['document_id'] == 1


async def test_person_document_delete(f, client):
    await f.create_person(person_id=1)
    await f.create_person_document(person_id=1, document_id=1)

    response = await client.delete('api/persons/1/documents/1')
    assert response.status_code == 204

    response = await client.get('api/persons/1/documents')

    assert response.status_code == 200
    data = response.json()
    assert len(data) == 0


async def test_get_person_details(f, client):
    await f.create_person(
        person_id=1,
        first_name_ac_en='Test1',
        last_name_ac_en='Test2',
        middle_name_ac_en='Test3',
    )
    response = await client.get('api/persons/1/details/')
    assert response.status_code == 200
    data = response.json()
    assert data['date_of_birth'] == '1990-01-01'
    assert not data['is_dismissed']
    assert data['first_name_ac_en'] == 'Test1'
    assert data['last_name_ac_en'] == 'Test2'
    assert data['middle_name_ac_en'] == 'Test3'


async def test_get_dismissed_person_details(f, client):
    await f.create_person(
        person_id=1,
        first_name_ac_en='Test1',
        last_name_ac_en='Test2',
        middle_name_ac_en='Test3',
        is_dismissed=True,
    )
    response = await client.get('api/persons/1/details/')
    assert response.status_code == 200
    data = response.json()
    assert data == {
        'is_dismissed': True,
        'is_limited_access': False,
        'login': 'user',
        'person_id': 1,
        'uid': '1',
        'email': 'a@a.ru',
        'gender': None,
        'first_name': None,
        'first_name_ac_en': None,
        'last_name': None,
        'last_name_ac_en': None,
        'middle_name': None,
        'middle_name_ac_en': None,
        'is_offline_trip': False,
        'date_of_birth': None,
        'phone_number': None,
        'has_date_of_birth': False,
        'has_first_name_en': False,
        'has_last_name_en': False,
    }


async def test_change_person_details(f, client):
    await f.create_person(person_id=1)
    response = await client.put(
        'api/persons/1/details/',
        json={
            'first_name_ac_en': 'Test1',
            'last_name_ac_en': 'Test2',
            'middle_name_ac_en': 'Test3',
        })
    assert response.status_code == 204


async def test_ext_person_create(f, client):
    await f.create_person(person_id=1)
    response = await client.post(
        'api/persons/1/ext_persons',
        json={
            'name': 'alias',
            'email': 'kek@kek.kek',
        },
    )
    assert response.status_code == 201


async def test_ext_person_regenerate(f, client):
    await f.create_person(person_id=1)
    await f.create_ext_person(person_id=1, ext_person_id=1)
    response = await client.post(
        'api/persons/1/ext_persons/1/regenerate_secret/',
        json={},
    )
    assert response.status_code == 200


async def test_ext_person_list(f, client):
    await f.create_person(person_id=1)
    await f.create_ext_person(
        person_id=1,
        ext_person_id=1,
        name='name1',
        email='name1@kek.kek',
    )
    await f.create_ext_person(
        person_id=1,
        ext_person_id=2,
        name='name2',
        email='name2@kek.kek',
    )
    response = await client.get('api/persons/1/ext_persons/')
    assert response.status_code == 200


async def test_ext_person_delete(f, client):
    await f.create_person(person_id=1)
    await f.create_ext_person(person_id=1, ext_person_id=1)
    response = await client.delete('api/persons/1/ext_persons/1/',)
    assert response.status_code == 204


async def test_persons_suggest(f, client):
    await f.create_person(
        person_id=1,
        uid='1',
        first_name='Имя',
        last_name='Фамилия',
        middle_name='Отчество',
        login='kek',
    )
    await f.create_person(
        person_id=2,
        uid='2',
        first_name='нет',
        last_name='нет',
        middle_name='нет',
        login='no',
    )
    with patch('intranet.trip.src.api.endpoints.isearch.settings.IS_YA_TEAM', False):
        response = await client.get('api/isearch/persons?text=kek')
    assert response.status_code == 200
    data = response.json()
    assert data == {
        'people': {
            'result': [
                {
                    'uid': '1',
                    'name': {
                        'first': 'Имя',
                        'last': 'Фамилия',
                        'middle': 'Отчество',
                    },
                    'login': 'kek',
                    'position': 'Сотрудник',
                    'staff_id': 1,
                    'person_id': 1,
                    'is_dismissed': False,
                }
            ],
            'pagination': {
                'page': 0,
                'per_page': 8,
                'pages': 1,
                'count': 1,
            },
        }
    }


@patch('intranet.trip.src.api.endpoints.persons.get_personal_data_gateway')
async def send_mocked_create_request(get_personal_data_gateway_mocked, uow, client, uid, email):
    get_personal_data_gateway_mocked.return_value = PassportPersonalData(uow)

    async def get_user_or_error_mocked(self, request, blackbox_user):
        return UserUid(uid=uid)

    async def complement_user_contacts_mocked(self, user):
        return user

    with (
        patch('intranet.trip.src.middlewares.auth.DevMiddleware.get_user_or_error',
              get_user_or_error_mocked),
        patch('intranet.trip.src.logic.persons.PassportPersonalData.complement_user_contacts',
              complement_user_contacts_mocked),
    ):
        response = await client.post(
            'api/persons/create',
            json={'email': email},
        )
    return response


async def test_new_person_create(uow, client):
    uid = '131719'
    email = 'test@test.dev'

    async with uow:
        response = await send_mocked_create_request(
            uow=uow,
            client=client,
            uid=uid,
            email=email,
        )
    assert response.status_code == 201


async def test_existing_person_create(uow, client, f):
    uid = '131719'
    email = 'test@test.dev'

    await f.create_person(person_id=int(uid), uid=uid)
    async with uow:
        response = await send_mocked_create_request(
            uow=uow,
            client=client,
            uid=uid,
            email=email,
        )
    assert response.status_code == 404
