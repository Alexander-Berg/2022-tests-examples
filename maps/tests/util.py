"""
This file has collection of useful utilities
"""

import jwt
import os

from dataclasses import dataclass
from datetime import datetime, timezone, timedelta
from yatest.common import source_path


TEST_AUDIENCE = 'reference-book'  # Could be any other acceptable audience

TEST_APIKEY = 'e3080197-05f7-4b2d-86a9-6ce258db9205'
SECOND_TEST_APIKEY = '23d8c6fa-0f2b-4ac5-8190-4865eeaf595c'

KEY_DIRECTORY = 'maps/b2bgeo/identity/backend/bin/tests/keys/'
IDENTITY_KEY_ID = 'id1'
KEYCLOAK_KEY_ID = 'kc1'
KEYCLOAK_JWT_LIFETIME = timedelta(minutes=10)


def _load_key(path):
    file_path = source_path(os.path.join(KEY_DIRECTORY, path))
    with open(file_path, "r") as f:
        public_key = f.read()
    return public_key


def load_public_key():
    return _load_key('identity-public-test.pem')


def load_private_key():
    return _load_key('identity-private-test')


def load_keycloak_public_key():
    return _load_key('keycloak-public-test.pem')


def load_keycloak_private_key():
    return _load_key('keycloak-private-test')


@dataclass(frozen=True)
class Company:
    id: int
    apikey: str

    def authenticate(self, identity_app) -> dict[str, str]:
        response = identity_app.request(f'/internal/tokens/company?apikey={self.apikey}')
        assert response.status_code == 200, response.text
        return {'Authorization': f'Bearer {response.json()["token"]}'}


@dataclass(frozen=True)
class KeycloakUser:
    login: str
    uid: str

    def jwt_payload(self):
        return {
            'login': self.login,
            'sub': self.uid,
            'aud': 'account',
            'iss': 'keycloak',
            'nbf': datetime.now(tz=timezone.utc),
            'exp': datetime.now(tz=timezone.utc) + KEYCLOAK_JWT_LIFETIME,
        }

    def jwt_headers(self):
        payload = self.jwt_payload()
        return {
            'Authorization': 'Bearer '
            + jwt.encode(payload, load_keycloak_private_key(), algorithm='ES256', headers={'kid': KEYCLOAK_KEY_ID})
        }

    def authenticate(self, identity_app) -> dict[str, str]:
        resp = identity_app.checked_request('/internal/tokens/user', headers=self.jwt_headers())
        return {'Authorization': f'Bearer {resp["token"]}'}
