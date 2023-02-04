import rstr
import pytest
import uuid
from datetime import timedelta
from http import HTTPStatus

from maps.b2bgeo.identity.libs.payloads.py import User, UserCompany, Company
from maps.b2bgeo.libs.py_auth.user_role import UserRole


def _create_vehicle(local_request, company_id, jwt, expected_status):
    path_vehicle = f'/api/v1/reference-book/companies/{company_id}/vehicles'
    vehicle_data = [
        {'name': 'Some vehicle 1', 'number': 'vehicle_id1', 'routing_mode': 'driving'},
    ]
    local_request(
        'POST',
        path_vehicle,
        data=vehicle_data,
        headers={'Authorization': f'bearer {jwt}'},
        expected_status=expected_status,
    )


def test_authorization_with_bad_jwt(local_request, company_id):
    bad_jwt = rstr.urlsafe(30)
    _create_vehicle(local_request, company_id, bad_jwt, HTTPStatus.INTERNAL_SERVER_ERROR)


@pytest.mark.parametrize(
    "role,expected_status",
    [
        (UserRole.admin, HTTPStatus.OK),
        (UserRole.manager, HTTPStatus.OK),
        (UserRole.dispatcher, HTTPStatus.FORBIDDEN),
        (UserRole.app, HTTPStatus.FORBIDDEN),
        (None, HTTPStatus.FORBIDDEN),
    ],
)
def test_authorization_with_role_jwt(local_request, audience, user_id, company_id, issuer, role, expected_status):
    companies = []
    if role is not None:
        companies.append(UserCompany(id=company_id, apikey=uuid.uuid4(), role=role))
    user_payload = User(
        id=user_id,
        is_super=False,
        companies=companies,
    )
    role_jwt = issuer.issue(user_payload, audience, timedelta(minutes=15))

    _create_vehicle(local_request, company_id, role_jwt, expected_status)


def test_authorization_with_s2s_jwt(local_request, audience, company_id, issuer):
    s2s_jwt = issuer.issue(Company(id=company_id, apikey=uuid.uuid4()), audience, timedelta(minutes=15))
    _create_vehicle(local_request, company_id, s2s_jwt, HTTPStatus.OK)


def test_authorization_with_other_company_s2s_jwt(local_request, audience, company_id, issuer):
    other_company_id = company_id + 1
    s2s_jwt = issuer.issue(
        Company(id=other_company_id, apikey=uuid.uuid4()), audience, timedelta(minutes=15)
    )
    _create_vehicle(local_request, company_id, s2s_jwt, HTTPStatus.FORBIDDEN)
