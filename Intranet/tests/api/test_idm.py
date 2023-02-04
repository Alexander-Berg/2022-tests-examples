import pytest


pytestmark = pytest.mark.asyncio


async def test_info(client):
    response = await client.get('api/idm/info')
    assert response.status_code == 200


@pytest.mark.parametrize('action, initial_value, expected_value', (
    ('add', False, True),
    ('add', True, True),
    ('remove', True, False),
    ('remove', False, False),
))
async def test_add_remove_role(f, uow, client, action, initial_value, expected_value):
    await f.create_person(person_id=1, login='login', is_coordinator=initial_value)
    role_request = {
        'login': 'login',
        'role': '{"role": "coordinator"}',
    }
    response = await client.post(f'api/idm/{action}-role', data=role_request)

    assert response.status_code == 200

    user = await uow.persons.get_user(person_id=1)
    assert user.is_coordinator is expected_value


@pytest.mark.parametrize('action', ('add', 'remove'))
async def test_bad_role(f, client, action):
    await f.create_person(person_id=1, login='login', is_coordinator=False)
    role_request = {
        'login': 'login',
        'role': '{"role": "admin"}',
    }
    response = await client.post(f'api/idm/{action}-role', data=role_request)
    assert response.status_code == 400


async def test_get_all_roles(f, client):
    await f.create_person(
        person_id=1, login='login1', is_coordinator=False, is_limited_access=False,
    )
    await f.create_person(
        person_id=2, login='login2', is_coordinator=True, is_limited_access=False,
    )
    await f.create_person(
        person_id=3, login='login3', is_coordinator=False, is_limited_access=True,
    )
    await f.create_person(
        person_id=4, login='login4', is_coordinator=True, is_limited_access=True,
    )
    await f.create_person(
        person_id=5,
        login='login5',
        is_coordinator=True,
        is_limited_access=True,
        is_dismissed=True,
    )

    response = await client.get('api/idm/get-all-roles')
    assert response.status_code == 200

    data = response.json()
    assert data == {
        'code': 0,
        'users': [
            {
                'login': 'login2',
                'roles': [{'role': 'coordinator'}],
            },
            {
                'login': 'login3',
                'roles': [{'role': 'limited_access'}],
            },
            {
                'login': 'login4',
                'roles': [{'role': 'coordinator'}, {'role': 'limited_access'}],
            },
        ]
    }


@pytest.mark.parametrize('action', ('add', 'remove'))
async def test_modify_role_for_unknown_login(client, action):
    response = await client.post(f'api/idm/{action}-role', data={
        'login': 'login',
        'role': '{"role": "coordinator"}',
    })
    assert response.status_code == 200

    data = response.json()
    assert data == {
        'code': 1,
        'error': 'No person found with login "login"'
    }
