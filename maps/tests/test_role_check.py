import pytest
import logging

from maps.b2bgeo.identity.libs.payloads.py import UserCompany
from maps.b2bgeo.identity.libs.pytest_lib.identity_jwt import TEST_SERVICE

from maps.b2bgeo.libs.py_auth.user_role import UserRole
from maps.b2bgeo.libs.py_auth.access import allowed_for_roles
from maps.b2bgeo.libs.py_auth.resource import IdentityResource
from .conftest import make_user_payload, make_company_payload, make_route
from datetime import timedelta


class ResourceWithRoleCheck(IdentityResource):
    route = '/<int:company_id>/secured/any-company'

    @allowed_for_roles([UserRole.manager], accept_company=False)
    def get(self, company_id: int) -> dict:
        return {}


class ResourceWithRoleOrApikeyCheck(IdentityResource):
    route = '/<int:company_id>/secured/any-company-apikey'

    @allowed_for_roles([UserRole.manager])
    def get(self, company_id: int) -> dict:
        return {}


class ResourceWithRoleOrApikeyCheckAndNoCompanyId(IdentityResource):
    route = '/resource-with-role-check'

    @allowed_for_roles([UserRole.manager])
    def get(self) -> dict:
        return {}


@pytest.fixture()
def api_with_role_check(api):
    resources = [
        ResourceWithRoleCheck,
        ResourceWithRoleOrApikeyCheck,
        ResourceWithRoleOrApikeyCheckAndNoCompanyId,
    ]
    for cls in resources:
        api.add_resource(cls, cls.route)
    return api


@pytest.mark.parametrize(
    "resource",
    [
        ResourceWithRoleCheck,
        ResourceWithRoleOrApikeyCheck,
    ],
)
@pytest.mark.parametrize("role", [UserRole.manager, UserRole.dispatcher])
@pytest.mark.parametrize("is_super", [False, True])
def test_secured_resource_with_role_check(api_with_role_check, client, issuer, resource, role, is_super):
    test_payload = make_user_payload(
        companies=[
            UserCompany(
                id=1,
                apikey='cc400763-5d69-4b65-8ffb-caf4b0e9effb',
                role=role,
            )
        ],
        is_super=is_super,
    )
    jwt_string = issuer.issue(test_payload, TEST_SERVICE, timedelta(minutes=15))
    logging.info('JWT: %s', jwt_string)
    logging.info('Role: %s', role)

    route = make_route(resource)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})
    logging.info(response.data)

    if is_super or role == UserRole.manager:
        expected_code = 200
    else:
        expected_code = 403

    assert response.status_code == expected_code


def test_role_check_without_company_id_fails(api_with_role_check, client, issuer):
    test_payload = make_user_payload()
    jwt_string = issuer.issue(test_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(ResourceWithRoleOrApikeyCheckAndNoCompanyId)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    assert response.status_code == 500


@pytest.mark.parametrize(
    "resource_and_code",
    [
        (ResourceWithRoleCheck, 403),
        (ResourceWithRoleOrApikeyCheck, 200),
        (ResourceWithRoleOrApikeyCheckAndNoCompanyId, 500),
    ],
)
def test_secured_resource_with_role_check_apikey(api_with_role_check, client, issuer, resource_and_code):
    resource, code = resource_and_code
    company_payload = make_company_payload()
    jwt_string = issuer.issue(company_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(resource)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    assert response.status_code == code
