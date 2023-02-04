"""
Tests for /companies/... route handles
"""

import pytest
from typing import Union, Iterator

from util import Company, KeycloakUser, SECOND_TEST_APIKEY


@pytest.fixture(scope='session', params=['admin_user', 'company'])
def admin_identity(request, admin_user, default_company) -> Iterator[Union[Company, KeycloakUser]]:
    if request.param == 'admin_user':
        yield admin_user
    else:
        yield default_company


@pytest.fixture(scope='session', params=['manager', 'dispatcher', 'app'])
def nonadmin_user(request, manager_user, dispatcher_user, app_user) -> Iterator[KeycloakUser]:
    if request.param == 'manager':
        yield manager_user
    elif request.param == 'dispatcher':
        yield dispatcher_user
    else:
        yield app_user


def test_admins_can_access_and_change_company(identity_app, admin_identity, default_company, second_company):
    headers = admin_identity.authenticate(identity_app)
    resp = identity_app.checked_request('/companies', headers=headers)
    assert resp == [{'id': default_company.id, 'apikey': default_company.apikey}]

    resp = identity_app.checked_request(f'/companies/{default_company.id}', headers=headers)
    assert resp == {'id': default_company.id, 'apikey': default_company.apikey}

    new_apikey = '1786cdf1-08f2-43ad-a90b-05008d366f48'
    resp = identity_app.checked_request(
        f'/companies/{default_company.id}', headers=headers, method='PATCH', json={'apikey': new_apikey}
    )
    assert resp == {'id': default_company.id, 'apikey': new_apikey}

    resp = identity_app.checked_request(
        f'/companies/{default_company.id}', headers=headers, method='PATCH', json={'apikey': default_company.apikey}
    )
    assert resp == {'id': default_company.id, 'apikey': default_company.apikey}

    identity_app.checked_request(
        f'/companies/{default_company.id}', headers=headers, method='PATCH', json={'apikey': 'wrong'}, expected_code=422
    )


def test_identity_cannot_access_another_company(identity_app, admin_identity, second_company):
    headers = admin_identity.authenticate(identity_app)
    resp = identity_app.checked_request(f'/companies/{second_company.id}', headers=headers, expected_code=404)
    assert 'incident_id' in resp['error']
    assert resp['error']['message'] == 'Company doesn\'t exist'

    resp = identity_app.checked_request(
        f'/companies/{second_company.id}', headers=headers, method='PATCH', expected_code=422
    )
    assert 'incident_id' in resp['error']
    assert 'Error in request structure' in resp['error']['message']


def test_non_admin_users_cannot_see_apikey(identity_app, nonadmin_user, default_company, second_company):
    headers = nonadmin_user.authenticate(identity_app)
    resp = identity_app.checked_request('/companies', headers=headers)
    assert resp == [{'id': default_company.id}]

    resp = identity_app.checked_request(f'/companies/{default_company.id}', headers=headers)
    assert resp == {'id': default_company.id}


def test_non_admin_users_cannot_patch_company(identity_app, nonadmin_user, default_company):
    headers = nonadmin_user.authenticate(identity_app)
    json = {'apikey': '1786cdf1-08f2-43ad-a90b-05008d366f48'}
    resp = identity_app.checked_request(
        f'/companies/{default_company.id}', headers=headers, method='PATCH', json=json, expected_code=403
    )
    assert 'incident_id' in resp['error']
    assert resp['error']['message'] == 'Your role doesn\'t allow to make this request'


def test_normal_users_and_companies_cannot_create_and_delete_companies(identity_app, admin_identity, default_company):
    headers = admin_identity.authenticate(identity_app)
    resp = identity_app.checked_request(
        '/companies', headers=headers, method='POST', json={'apikey': SECOND_TEST_APIKEY}, expected_code=403
    )
    assert 'incident_id' in resp['error']
    assert resp['error']['message'] == 'Superuser access required'

    headers = admin_identity.authenticate(identity_app)
    resp = identity_app.checked_request(
        f'/companies/{default_company.id}', headers=headers, method='DELETE', expected_code=403
    )
    assert 'incident_id' in resp['error']
    assert resp['error']['message'] == 'Superuser access required'


def test_access_to_non_existent_company_is_not_found(identity_app, superuser):
    headers = superuser.authenticate(identity_app)
    json = {'apikey': '7b61d45b-2627-4ea2-811a-4cc07221a678'}
    identity_app.checked_request('/companies/0', headers=headers, expected_code=404)
    identity_app.checked_request('/companies/0', headers=headers, method='PATCH', json=json, expected_code=404)
    identity_app.checked_request('/companies/0', headers=headers, method='DELETE', expected_code=404)


def test_superuser_receives_all_companies(identity_app, superuser, default_company, second_company):
    headers = superuser.authenticate(identity_app)
    assert identity_app.checked_request('/companies', headers=headers) == [
        {'id': default_company.id, 'apikey': default_company.apikey},
        {'id': second_company.id, 'apikey': second_company.apikey},
    ]


def test_superuser_can_create_and_delete_companies(identity_app, superuser):
    headers = superuser.authenticate(identity_app)
    json = {'apikey': '7732f91b-278b-4846-b283-83c433499637'}
    resp = identity_app.checked_request('/companies', headers=headers, method='POST', json=json)
    id = resp['id']

    identity_app.checked_request(f'/companies/{id}', headers=headers, expected_code=200)
    identity_app.checked_request(f'/companies/{id}', headers=headers, method='DELETE', expected_code=200)
    identity_app.checked_request(f'/companies/{id}', headers=headers, expected_code=404)


def test_apikey_is_unique_across_companies(identity_app, superuser, admin_user, default_company, second_company):
    json = {'apikey': second_company.apikey}

    headers = superuser.authenticate(identity_app)
    resp = identity_app.checked_request('/companies', headers=headers, method='POST', json=json, expected_code=422)
    assert resp['error']['message'] == 'There already is a company with this apikey'

    headers = superuser.authenticate(identity_app)
    resp = identity_app.checked_request(
        f'/companies/{default_company.id}', headers=headers, method='PATCH', json=json, expected_code=422
    )
    assert resp['error']['message'] == 'There already is a company with this apikey'


def test_incorrect_schema(identity_app, superuser, default_company):
    headers = superuser.authenticate(identity_app)

    json = {'apikeyy': '7732f91b-278b-4846-b283-83c433499637'}
    identity_app.checked_request('/companies', headers=headers, method='POST', json=json, expected_code=422)
    identity_app.checked_request(
        f'/companies/{default_company.id}', headers=headers, method='PATCH', json=json, expected_code=422
    )

    json = {}
    identity_app.checked_request('/companies', headers=headers, method='POST', json=json, expected_code=422)
    identity_app.checked_request(
        f'/companies/{default_company.id}', headers=headers, method='PATCH', json=json, expected_code=422
    )

    json = {'apikey': '7732f91b-278b-4846-b283-83c433499637', 'ku': 'ku'}
    identity_app.checked_request('/companies', headers=headers, method='POST', json=json, expected_code=422)
    identity_app.checked_request(
        f'/companies/{default_company.id}', headers=headers, method='PATCH', json=json, expected_code=422
    )
