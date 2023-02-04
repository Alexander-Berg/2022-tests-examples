import pytest
import logging
from datetime import timedelta

from maps.b2bgeo.identity.libs.pytest_lib.identity_jwt import TEST_SERVICE
from maps.b2bgeo.identity.libs.payloads.py import UserCompany

from maps.b2bgeo.libs.py_auth.resource import (
    IdentityResource,
    IdentitySecuredResource,
    SuperSecuredResource,
)
from maps.b2bgeo.libs.py_auth.user_role import UserRole
from .conftest import make_user_payload, make_company_payload, make_route


class TestSecuredResourceAnyCompany(IdentityResource):
    route = '/companies/<int:company_id>/secured-resource-any-company'

    def get(self, company_id: int) -> dict:
        return {}


class TestSecuredResource(IdentitySecuredResource):
    route = '/companies/<int:company_id>/secured-resource'

    def get(self, company_id: int) -> dict:
        return {}


class TestSuperSecuredResource(SuperSecuredResource):
    route = '/companies/<int:company_id>/super-secured-resource'

    def get(self, company_id: int) -> dict:
        return {}


ALL_RESOURCES = [
    TestSecuredResourceAnyCompany,
    TestSecuredResource,
    TestSuperSecuredResource,
]


@pytest.fixture()
def secured_api(api):
    resources = ALL_RESOURCES
    for cls in resources:
        api.add_resource(cls, cls.route)
    return api


@pytest.mark.parametrize("resource", ALL_RESOURCES)
def test_access_without_jwt_header_forbidden(secured_api, client, resource):
    route = make_route(resource)
    response = client.get(route)
    assert response.status_code == 401


# TODO: write test access with invalid JWT type (not user and not company)


@pytest.mark.parametrize(
    "resource_and_code",
    [
        (TestSecuredResourceAnyCompany, 200),
        (TestSecuredResource, 200),
        (TestSuperSecuredResource, 403),
    ],
)
def test_secured_resource_with_valid_jwt_user_header(secured_api, client, issuer, resource_and_code):
    resource, code = resource_and_code
    test_payload = make_user_payload()
    jwt_string = issuer.issue(test_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(resource)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    assert response.status_code == code


@pytest.mark.parametrize("resource", ALL_RESOURCES)
def test_secured_resource_with_wrong_audience(secured_api, client, issuer, resource):
    test_payload = make_user_payload()
    jwt_string = issuer.issue(test_payload, TEST_SERVICE + '_other', timedelta(minutes=15))

    route = make_route(resource)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    assert response.status_code == 500


@pytest.mark.parametrize(
    "resource_and_code",
    [
        (TestSecuredResourceAnyCompany, 200),
        (TestSecuredResource, 403),
        (TestSuperSecuredResource, 403),
    ],
)
def test_secured_resource_with_other_company_jwt_user_header(secured_api, client, issuer, resource_and_code):
    resource, code = resource_and_code
    test_payload = make_user_payload(
        companies=[
            UserCompany(
                id=3,
                apikey='6516c288-b8f9-4d4b-a59e-dd1eb9f47b26',
                role=UserRole.admin,
            )
        ]
    )
    jwt_string = issuer.issue(test_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(resource, company_id=1)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    assert response.status_code == code


@pytest.mark.parametrize("resource", ALL_RESOURCES)
@pytest.mark.parametrize("is_super", [False, True])
def test_secured_resource_access_with_empty_company_ids(secured_api, client, issuer, resource, is_super):
    test_payload = make_user_payload(companies=[], is_super=is_super)
    jwt_string = issuer.issue(test_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(resource)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    assert response.status_code == 403


@pytest.mark.parametrize("is_super", [False, True])
@pytest.mark.parametrize("role", [UserRole.admin, UserRole.manager])
def test_only_super_is_allowed_to_access_super_secured_resource(secured_api, client, issuer, is_super, role):
    test_payload = make_user_payload(
        company_ids=[
            UserCompany(
                id=1,
                apikey='d3cb0ed9-4659-43ed-8a03-ad2298b64af1',
                role=role,
            )
        ],
        is_super=is_super,
    )
    jwt_string = issuer.issue(test_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(TestSuperSecuredResource, company_id=2)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    if is_super:
        assert response.status_code == 200
    else:
        assert response.status_code == 403


@pytest.mark.parametrize(
    "resource",
    [
        TestSecuredResource,
    ],
)
def test_super_allowed_to_access_other_companies(secured_api, client, issuer, resource):
    test_payload = make_user_payload(
        company_ids=[
            UserCompany(
                id=1,
                apikey='13af7d1c-73f5-4459-ae11-a42b1c6ea814',
                role=UserRole.admin,
            )
        ],
        is_super=True,
    )
    jwt_string = issuer.issue(test_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(resource, company_id=2)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    assert response.status_code == 200


@pytest.mark.parametrize(
    "resource",
    [
        TestSecuredResource,
    ],
)
def test_admin_not_allowed_to_access_other_companies(secured_api, client, issuer, resource):
    test_payload = make_user_payload(
        company_ids=[
            UserCompany(
                id=1,
                apikey='6c28bf06-4722-4e3a-ae7e-04553a5b27c4',
                role=UserRole.admin,
            )
        ],
        is_super=False,
    )
    jwt_string = issuer.issue(test_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(resource, company_id=2)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    assert response.status_code == 403


@pytest.mark.parametrize(
    "resource_and_code",
    [
        (TestSecuredResourceAnyCompany, 200),
        (TestSecuredResource, 200),
        (TestSuperSecuredResource, 403),
    ],
)
def test_secured_resource_access_with_apikey(secured_api, client, issuer, resource_and_code):
    resource, code = resource_and_code
    company_payload = make_company_payload()
    jwt_string = issuer.issue(company_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(resource)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    logging.info(response.data)
    assert response.status_code == code


@pytest.mark.parametrize(
    "resource_and_code",
    [
        (TestSecuredResourceAnyCompany, 200),
        (TestSecuredResource, 403),
        (TestSuperSecuredResource, 403),
    ],
)
def test_secured_resource_access_with_apikey_for_wrong_company(secured_api, client, issuer, resource_and_code):
    resource, code = resource_and_code
    company_payload = make_company_payload(id=1)
    jwt_string = issuer.issue(company_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(resource, company_id=2)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    logging.info(response.data)
    assert response.status_code == code
