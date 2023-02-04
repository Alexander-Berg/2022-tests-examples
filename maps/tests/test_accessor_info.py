import pytest

from flask import g
from datetime import timedelta

from maps.b2bgeo.identity.libs.payloads.py import Company
from maps.b2bgeo.identity.libs.pytest_lib.identity_jwt import TEST_SERVICE

from maps.b2bgeo.libs.py_auth.user_role import UserRole
from maps.b2bgeo.libs.py_auth.resource import IdentityResource
from .conftest import make_user_payload, make_company_payload, make_route


class ResourceWithAccessorInfo(IdentityResource):
    route = '/<int:company_id>/resource-with-accessor-info'

    def get(self, company_id: int) -> dict:
        if hasattr(g, 'identity'):
            result = {
                'id': g.identity.id,
                'is_super': g.identity.is_super,
                'role': g.identity.get_role(company_id),
            }
            if isinstance(g.identity, Company):
                result['apikey'] = g.identity.apikey
            return result
        else:
            raise RuntimeError('No user info present')


@pytest.fixture()
def api_with_accessor_info(api):
    resources = [
        ResourceWithAccessorInfo,
    ]
    for cls in resources:
        api.add_resource(cls, cls.route)
    return api


def test_user_accessor_info_available(api_with_accessor_info, client, issuer):
    user_payload = make_user_payload()
    jwt_string = issuer.issue(user_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(ResourceWithAccessorInfo)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    assert response.status_code == 200
    assert response.json == {
        'id': user_payload.id,
        'is_super': user_payload.is_super,
        'role': user_payload.get_role(1),
    }


def test_company_accessor_info_available(api_with_accessor_info, client, issuer):
    company_payload = make_company_payload()
    jwt_string = issuer.issue(company_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(ResourceWithAccessorInfo)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    assert response.status_code == 200
    assert response.json == {
        'id': company_payload.id,
        'is_super': False,
        'role': int(UserRole.admin),
        'apikey': company_payload.apikey,
    }
