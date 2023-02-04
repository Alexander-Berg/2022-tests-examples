import pytest

from maps.b2bgeo.identity.libs.pytest_lib.identity_jwt import TEST_SERVICE

from maps.b2bgeo.libs.py_auth.resource import IdentitySecuredResource
from .conftest import make_user_payload, make_route
from datetime import timedelta


class TestCourierSecuredResource(IdentitySecuredResource):
    route = '/couriers/<int:instance_id>/courier-secured-resource'

    def get(self, instance_id: int) -> dict:
        return {}


@pytest.fixture()
def api_with_courier_resource(api):
    cls = TestCourierSecuredResource
    api.add_resource(cls, cls.route)
    return api


def test_courier_secured_resource_is_not_implemented(api_with_courier_resource, client, issuer):
    test_payload = make_user_payload()
    jwt_string = issuer.issue(test_payload, TEST_SERVICE, timedelta(minutes=15))

    route = make_route(TestCourierSecuredResource)
    response = client.get(route, headers={'Authorization': f'Bearer {jwt_string}'})

    # Courier access check is not implemented. Accessing such routes must case an error.
    assert response.status_code == 500
