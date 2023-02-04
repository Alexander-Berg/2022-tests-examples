"""
Tests for checking user's requests
"""
import requests
import pytest
import json


@pytest.fixture(scope='session')
def user(identity_app, superuser, default_company):
    request_user = {'login': 'test-admin-user'}
    headers = superuser.authenticate(identity_app)
    user = identity_app.checked_request(
        f'/companies/{default_company.id}/users', method='POST', headers=headers, data=json.dumps(request_user)
    )
    yield user
    id = user['id']
    identity_app.checked_request(f'/companies/{default_company.id}/users/{id}', method='DELETE', headers=headers)


def test_add_admin_user(identity_app, admin_user, default_company):
    request_user = {'login': 'test-admin'}
    headers = admin_user.authenticate(identity_app)
    test_user = identity_app.checked_request(
        f'/companies/{default_company.id}/users', method='POST', headers=headers, data=json.dumps(request_user)
    )

    assert test_user['login'] == request_user['login']
    assert 'id' in test_user
    assert test_user['role'] == 'admin'
    id = test_user['id']
    identity_app.checked_request(f'/companies/{default_company.id}/users/{id}', method='DELETE', headers=headers)


def test_add_existing_user(identity_app, admin_user, user, default_company):
    request_user = {
        'login': user['login'],
        'role': 'admin',
    }
    headers = admin_user.authenticate(identity_app)
    response = identity_app.checked_request(
        f'/companies/{default_company.id}/users',
        method='POST',
        headers=headers,
        data=json.dumps(request_user),
        expected_code=requests.codes.unprocessable_entity,
    )

    assert f'User \'{user["login"]}\' already registered. Choose another login.' in response['error']['message']


def test_get_list_of_users(identity_app, admin_user, default_company):
    headers = admin_user.authenticate(identity_app)
    users = identity_app.checked_request(f'/companies/{default_company.id}/users', headers=headers)
    assert len(users) > 0


def test_list_fail(identity_app, admin_user, default_company):
    headers = admin_user.authenticate(identity_app)

    response = identity_app.checked_request(
        f'/companies/{default_company.id}/users?page=0',
        headers=headers,
        expected_code=requests.codes.unprocessable_entity,
    )
    assert 'Must be greater than or equal to 1' in response['error']['message']

    response = identity_app.checked_request(
        f'/companies/{default_company.id}/users?page=-1',
        headers=headers,
        expected_code=requests.codes.unprocessable_entity,
    )
    assert 'Must be greater than or equal to 1' in response['error']['message']


def test_get_by_id(identity_app, admin_user, user, default_company):
    headers = admin_user.authenticate(identity_app)
    id = user['id']
    user_test = identity_app.checked_request(f'/companies/{default_company.id}/users/{id}', headers=headers)

    assert user_test['login'] == user['login']


def test_get_by_login_fail(identity_app, admin_user):
    headers = admin_user.authenticate(identity_app)
    response = identity_app.checked_request(
        '/user-info', expected_code=requests.codes.unprocessable_entity, headers=headers
    )
    assert 'Missing data for required field' in response['error']['message']


def test_get_by_login(identity_app, superuser, user, default_company):
    headers = superuser.authenticate(identity_app)

    def get_expected_response(user_data):
        expected_resp = {**user_data, 'company_ids': [default_company.id]}
        return expected_resp

    for login in [
        user['login'],
        '*' + user['login'][2:],
        '*' + user['login'][2:12] + '*',
        user['login'][:4] + '*',
        '*',
        '**',
        user['login'].replace('t', '?'),
    ]:
        response = identity_app.checked_request(f'/user-info?login={login}', headers=headers)
        assert get_expected_response(user) in response

    test_user = {'login': 'test_login_user'}
    test_user = identity_app.checked_request(
        f'/companies/{default_company.id}/users', method='POST', headers=headers, data=json.dumps(test_user)
    )

    login = (test_user['login'][:13] + '*').replace('t', '?')
    response = identity_app.checked_request(f'/user-info?login={login}', headers=headers)
    assert get_expected_response(test_user) in response

    login = 'test*'
    response = identity_app.checked_request(f'/user-info?login={login}', headers=headers)
    assert get_expected_response(user) in response
    assert get_expected_response(test_user) in response

    id = test_user['id']
    identity_app.checked_request(f'/companies/{default_company.id}/users/{id}', method='DELETE', headers=headers)

    login = 'test_login_user'
    response = identity_app.checked_request(f'/user-info?login={login}', headers=headers)
    assert response == []


def test_get_by_uppercase_login(identity_app, superuser, default_company):
    headers = superuser.authenticate(identity_app)
    test_user = {'login': 'test_login_user'}
    test_user = identity_app.checked_request(
        f'/companies/{default_company.id}/users', method='POST', headers=headers, data=json.dumps(test_user)
    )
    login = test_user['login'].upper()
    response = identity_app.checked_request(f'/user-info?login={login}', headers=headers)
    assert len(response) > 0

    id = test_user['id']
    identity_app.checked_request(f'/companies/{default_company.id}/users/{id}', method='DELETE', headers=headers)


def test_patch(identity_app, admin_user, user, default_company):
    patched_user = {'role': 'manager'}
    headers = admin_user.authenticate(identity_app)
    id = user['id']
    updated_user = identity_app.checked_request(
        f'/companies/{default_company.id}/users/{id}', method='PATCH', headers=headers, data=json.dumps(patched_user)
    )

    assert updated_user['login'] == user['login']
    assert updated_user['role'] == 'manager'


def test_remove(identity_app, admin_user, user, default_company):
    headers = admin_user.authenticate(identity_app)
    request_user = {'login': 'temp-user'}

    temp_user = identity_app.checked_request(
        f'/companies/{default_company.id}/users', method='POST', headers=headers, data=json.dumps(request_user)
    )

    id = temp_user['id']
    identity_app.checked_request(f'/companies/{default_company.id}/users/{id}', method='DELETE', headers=headers)

    response = identity_app.checked_request(f'/companies/{default_company.id}/users', headers=headers)

    assert id not in [x['id'] for x in response]


def test_add_user_role(identity_app, admin_user, default_company):
    user_data = {'login': 'test-manager-user', 'role': 'manager'}
    headers = admin_user.authenticate(identity_app)
    test_user = identity_app.checked_request(
        f'/companies/{default_company.id}/users', method='POST', headers=headers, data=json.dumps(user_data)
    )

    assert test_user['role'] == 'manager'
    id = test_user['id']

    identity_app.checked_request(f'/companies/{default_company.id}/users/{id}', method='DELETE', headers=headers)


def test_add_app_user(identity_app, admin_user, default_company):
    request_user = {'login': 'test-app-user'}
    headers = admin_user.authenticate(identity_app)
    response = identity_app.request(
        f'/companies/{default_company.id}/app-user', method='POST', headers=headers, data=json.dumps(request_user)
    )
    assert response.status_code == 200


def test_the_user_that_does_not_exist(identity_app, admin_user, default_company):
    headers = admin_user.authenticate(identity_app)
    identity_app.checked_request(
        f'/companies/{default_company.id}/users/{10000}',
        method='DELETE',
        headers=headers,
        expected_code=requests.codes.not_found,
    )
    patched_user = {'role': 'manager'}
    identity_app.checked_request(
        f'/companies/{default_company.id}/users/{10000}',
        method='PATCH',
        headers=headers,
        data=json.dumps(patched_user),
        expected_code=requests.codes.not_found,
    )
    identity_app.checked_request(
        f'/companies/{default_company.id}/users/{10000}',
        method='GET',
        headers=headers,
        expected_code=requests.codes.not_found,
    )


def test_incorrect_schema(identity_app, admin_user, default_company):
    headers = admin_user.authenticate(identity_app)
    patched_user = {'role_': 'manager'}
    identity_app.checked_request(
        f'/companies/{default_company.id}/users/{10000}',
        method='PATCH',
        headers=headers,
        data=json.dumps(patched_user),
        expected_code=requests.codes.unprocessable_entity,
    )

    request_user = {'login': 'test-app-user', 'additional_login': 'test'}
    identity_app.checked_request(
        f'/companies/{default_company.id}/app-user',
        method='POST',
        headers=headers,
        data=json.dumps(request_user),
        expected_code=requests.codes.unprocessable_entity,
    )

    request_user = {}
    identity_app.checked_request(
        f'/companies/{default_company.id}/app-user',
        method='POST',
        headers=headers,
        data=json.dumps(request_user),
        expected_code=requests.codes.unprocessable_entity,
    )

    request_user = {'role': 'admin'}
    identity_app.checked_request(
        f'/companies/{default_company.id}/users',
        method='POST',
        headers=headers,
        data=json.dumps(request_user),
        expected_code=requests.codes.unprocessable_entity,
    )

    request_user = {'login': 'test-user', 'home': 'bad'}
    identity_app.checked_request(
        f'/companies/{default_company.id}/users',
        method='POST',
        headers=headers,
        data=json.dumps(request_user),
        expected_code=requests.codes.unprocessable_entity,
    )
