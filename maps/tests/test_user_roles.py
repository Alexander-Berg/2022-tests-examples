import pytest
import requests
import json
from util import KeycloakUser


@pytest.mark.parametrize(
    'user_role, expected_status_code',
    [
        ('admin', requests.codes.forbidden),
        ('manager', requests.codes.forbidden),
        ('dispatcher', requests.codes.forbidden),
        ('app', requests.codes.ok),
    ],
)
def test_user_deleting_himself(identity_app, admin_user, default_company, user_role, expected_status_code):
    headers = admin_user.authenticate(identity_app)
    test_user = {'login': 'test_login_user', 'role': user_role}
    test_user = identity_app.checked_request(
        f'/companies/{default_company.id}/users', method='POST', headers=headers, data=json.dumps(test_user)
    )

    id = test_user['id']
    new_headers = KeycloakUser(test_user['login'], 'test-uid').authenticate(identity_app)
    identity_app.checked_request(
        f'/companies/{default_company.id}/users/{id}',
        method='DELETE',
        headers=new_headers,
        expected_code=expected_status_code,
    )
    if expected_status_code != requests.codes.ok:
        identity_app.checked_request(f'/companies/{default_company.id}/users/{id}', method='DELETE', headers=headers)


@pytest.mark.parametrize(
    'user_role, another_user_role, expected_status_code',
    [
        ('admin', 'admin', requests.codes.ok),
        ('admin', 'manager', requests.codes.ok),
        ('admin', 'dispatcher', requests.codes.ok),
        ('admin', 'app', requests.codes.ok),
        ('manager', 'admin', requests.codes.forbidden),
        ('manager', 'manager', requests.codes.forbidden),
        ('manager', 'dispatcher', requests.codes.forbidden),
        ('manager', 'app', requests.codes.ok),
        ('dispatcher', 'admin', requests.codes.forbidden),
        ('dispatcher', 'manager', requests.codes.forbidden),
        ('dispatcher', 'dispatcher', requests.codes.forbidden),
        ('dispatcher', 'app', requests.codes.forbidden),
        ('app', 'admin', requests.codes.forbidden),
        ('app', 'manager', requests.codes.forbidden),
        ('app', 'dispatcher', requests.codes.forbidden),
        ('app', 'app', requests.codes.forbidden),
    ],
)
def test_deleting_other_user(
    identity_app, admin_user, default_company, user_role, another_user_role, expected_status_code
):
    admin_headers = admin_user.authenticate(identity_app)
    test_user = {'login': 'test_login_user', 'role': user_role}
    test_user = identity_app.checked_request(
        f'/companies/{default_company.id}/users', method='POST', headers=admin_headers, data=json.dumps(test_user)
    )
    headers = KeycloakUser(test_user['login'], 'test-uid').authenticate(identity_app)

    another_test_user = {'login': 'test_another_login_user', 'role': another_user_role}
    another_test_user = identity_app.checked_request(
        f'/companies/{default_company.id}/users',
        method='POST',
        headers=admin_headers,
        data=json.dumps(another_test_user),
    )

    id = another_test_user['id']
    identity_app.checked_request(
        f'/companies/{default_company.id}/users/{id}',
        method='DELETE',
        headers=headers,
        expected_code=expected_status_code,
    )

    if expected_status_code != requests.codes.ok:
        identity_app.checked_request(
            f'/companies/{default_company.id}/users/{id}', method='DELETE', headers=admin_headers
        )
    id = test_user['id']
    identity_app.checked_request(f'/companies/{default_company.id}/users/{id}', method='DELETE', headers=admin_headers)


@pytest.mark.parametrize('user_role', ['admin', 'manager', 'dispatcher', 'app'])
def test_deleting_user_from_other_company(identity_app, superuser, admin_user, second_company, user_role):
    superuser_headers = superuser.authenticate(identity_app)
    admin_headers = admin_user.authenticate(identity_app)

    company_id = second_company.id
    test_user = {'login': 'test_login_user', 'role': user_role}
    test_user = identity_app.checked_request(
        f'/companies/{company_id}/users',
        method='POST',
        headers=superuser_headers,
        data=json.dumps(test_user),
    )

    id = test_user['id']
    identity_app.checked_request(
        f'/companies/{company_id}/users/{id}',
        method='DELETE',
        headers=admin_headers,
        expected_code=requests.codes.forbidden,
    )

    identity_app.checked_request(f'/companies/{company_id}/users/{id}', method='DELETE', headers=superuser_headers)
