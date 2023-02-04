import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_get_request,
    create_tmp_users
)
from ya_courier_backend.models import UserRole


def test_documentation_is_available_only_to_superusers(system_env_with_db):
    user_roles = [
        UserRole.admin,
        UserRole.manager,
        UserRole.dispatcher,
        UserRole.app,
    ]
    with create_tmp_users(system_env_with_db, [system_env_with_db.company_id] * len(user_roles), user_roles) as users:
        for user in users:
            for path in ['doc', 'spec']:
                response = env_get_request(system_env_with_db, path=path, auth=system_env_with_db.get_user_auth(user))
                assert response.status_code == requests.codes.forbidden

    response = env_get_request(system_env_with_db, path='doc', auth=system_env_with_db.auth_header_super)
    assert response.status_code == requests.codes.ok
    # Check that response body is not a garbage
    assert '<html>' in response.text
    assert response.headers['Content-Type'].startswith('text/html')

    response = env_get_request(system_env_with_db, path='spec', auth=system_env_with_db.auth_header_super)
    assert response.status_code == requests.codes.ok
    # Check that response body is not a garbage
    j = response.json()
    assert j['components']['schemas']['CourierCreate']['required'] == ['number']
