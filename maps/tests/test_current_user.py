"""
Tests for checking /current_user
"""


import jwt
import datetime

from util import load_private_key, IDENTITY_KEY_ID


# TODO: Refactor to using C2S token route
def test_current_user(identity_app, admin_user):
    value = {
        'id': 1,
        'is_super': True,
        'companies': [
            {'id': 1, 'role': 'admin', 'apikey': 'd71c1711-66ea-46a4-89c5-09ec556b623d'},
            {'id': 2, 'role': 'admin', 'apikey': 'd71c1711-66ea-46a4-89c5-09ec556b623d'},
        ],
    }
    subject = {'type': 'user', 'value': value}

    now = datetime.datetime.now(tz=datetime.timezone.utc)
    token = jwt.encode(
        {
            'subject': subject,
            'iss': 'identity',
            'nbf': now,
            'exp': now + datetime.timedelta(minutes=10),
            'apikey': 'd71c1711-66ea-46a4-89c5-09ec556b623d',
            'aud': ['account'],
        },
        load_private_key(),
        algorithm='ES256',
        headers={'kid': IDENTITY_KEY_ID},
    )

    header = {'Authorization': f'Bearer {token}'}

    response = identity_app.checked_request('/current_user', headers=header)
    assert response == {
        'id': 1,
        'role': 'admin',
        'is_super': True,
        'company_users': [{'company_id': 1, 'role': 'admin'}, {'company_id': 2, 'role': 'admin'}],
    }


def test_current_user_from_s2s(identity_app, default_company):
    headers = default_company.authenticate(identity_app)
    identity_app.checked_request('/current_user', headers=headers, expected_code=403)
