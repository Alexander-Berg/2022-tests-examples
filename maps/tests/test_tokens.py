"""
Tests for checking identity service's token issuing
"""

import jwt
import uuid

from util import (
    load_public_key,
    KeycloakUser,
    TEST_APIKEY,
    TEST_AUDIENCE,
    IDENTITY_KEY_ID,
)


def _decode_token(response: dict):
    options = {'require': ['iss', 'exp', 'nbf']}
    return jwt.decode(
        response['token'],
        load_public_key(),
        algorithms=['ES256'],
        issuer='identity',
        audience=TEST_AUDIENCE,
        options=options,
    )


def test_company_token(identity_app):
    response_json = identity_app.checked_request(f'/internal/tokens/company?apikey={TEST_APIKEY}')
    assert 'token' in response_json

    assert jwt.get_unverified_header(response_json['token'])['kid'] == IDENTITY_KEY_ID

    decoded = _decode_token(response_json)
    assert decoded['subject']['type'] == 'company'
    assert decoded['subject']['value']['id'] == 1
    assert decoded['subject']['value']['apikey'] == TEST_APIKEY


def test_company_token_wrong_apikey(identity_app):
    route = '/internal/tokens/company?apikey={}'

    identity_app.checked_request(route.format(''), expected_code=401)
    identity_app.checked_request(route.format('wrong'), expected_code=401)
    identity_app.checked_request(route.format(uuid.uuid4()), expected_code=403)


def test_user_token(identity_app, admin_user, manager_user):
    headers = admin_user.jwt_headers()

    response = identity_app.checked_request('/internal/tokens/user', headers=headers)
    assert 'token' in response

    decoded = _decode_token(response)
    assert decoded['subject']['type'] == 'user'
    assert decoded['subject']['value']['id'] == 1
    assert not decoded['subject']['value']['is_super']
    assert len(decoded['subject']['value']['companies']) == 1
    assert decoded['subject']['value']['companies'][0] == {'apikey': TEST_APIKEY, 'id': 1, 'role': 'admin'}

    headers = KeycloakUser(admin_user.login, 'weirder_uid').jwt_headers()

    response = identity_app.checked_request('/internal/tokens/user', headers=headers, expected_code=403)
    assert 'There already is a different account with that login' in response['error']['message']

    headers = KeycloakUser('changed_admin_login', admin_user.uid).jwt_headers()

    response = identity_app.checked_request('/internal/tokens/user', headers=headers)
    assert 'token' in response


def test_user_token_wrong_user(identity_app):
    headers = KeycloakUser('user4', 'some_uid').jwt_headers()

    response_json = identity_app.checked_request('/internal/tokens/user', headers=headers, expected_code=403)
    assert 'User not found' in response_json['error']['message']


def test_user_token_invalid_auth(identity_app):
    response_json = identity_app.checked_request('/internal/tokens/user', expected_code=401)
    assert 'Authorization token is missing' in response_json['error']['message']

    headers = {"Authorization": "Bearer Garbage"}

    response_json = identity_app.checked_request('/internal/tokens/user', headers=headers, expected_code=401)
    assert 'Incorrect authorization token' in response_json['error']['message']
