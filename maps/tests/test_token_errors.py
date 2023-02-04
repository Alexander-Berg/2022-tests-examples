"""
Tests for checking errors for malformed JWT tokens from keycloak and identity.
For keycloak JWT we shall:
    * return 500 for the case of correct token and signature, but incorrect body,
      because keycloak responding incorrect payloads, which can be an error in keycloak configuration;
    * return 401 for any other incorrect tokens.
Any error in identity JWT shall indicate about identity or auth-proxy problems, so, we should return 500 in this case.
For debugging purposes we still expect 401 in either JWTs.
"""

import jwt
import pytest

from util import (
    load_keycloak_private_key,
    KEYCLOAK_KEY_ID,
)


@pytest.mark.parametrize('route', ['/internal/tokens/user', '/companies/0'])
@pytest.mark.parametrize('headers', [{'Authorization': ''}, {}])
def test_empty_jwt_returns_401(identity_app, admin_user, headers, route):
    resp = identity_app.checked_request(route, headers=headers, expected_code=401)
    assert resp['error']['message'] == 'Authorization token is missing'


def test_malformed_identity_jwt_returns_500(identity_app, admin_user):
    headers = admin_user.authenticate(identity_app)
    headers['Authorization'] += 'malformed'
    resp = identity_app.checked_request('/companies/0', headers=headers, expected_code=500)
    assert resp['error']['message'] == 'System error, please contact support'


def test_malformed_keycloak_jwt_returns_401(identity_app, admin_user):
    headers = admin_user.jwt_headers()
    headers['Authorization'] += 'malformed'
    resp = identity_app.checked_request('/internal/tokens/user', headers=headers, expected_code=401)
    assert resp['error']['message'] == 'Incorrect authorization token'


def test_wrong_keycloak_key_returns_401(identity_app, admin_user):
    payload = admin_user.jwt_payload()
    headers = {
        'Authorization': 'Bearer '
        + jwt.encode(payload, load_keycloak_private_key(), algorithm='ES256', headers={'kid': 'wrong-key-id'})
    }
    resp = identity_app.checked_request('/internal/tokens/user', headers=headers, expected_code=401)
    assert resp['error']['message'] == 'Incorrect authorization token'


def test_correct_keycloak_jwt_with_wrong_payload_returns_500(identity_app, admin_user):
    payload = admin_user.jwt_payload()
    del payload['login']
    headers = {
        'Authorization': 'Bearer '
        + jwt.encode(payload, load_keycloak_private_key(), algorithm='ES256', headers={'kid': KEYCLOAK_KEY_ID})
    }
    resp = identity_app.checked_request('/internal/tokens/user', headers=headers, expected_code=500)
    assert resp['error']['message'] == 'System error, please contact support'
