import requests
import pytest
import datetime
import dateutil.tz
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    create_route_env,
    env_get_request, env_patch_request, env_post_request, env_delete_request,
    create_tmp_users,
    create_tmp_company,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import UserKind
from ya_courier_backend.models import UserRole


_ROUTE_DATE_ISO = datetime.datetime.now(tz=dateutil.tz.gettz('Europe/Moscow')).date().isoformat()


@pytest.fixture(scope='module')
def env_with_orders(system_env_with_db):
    env = system_env_with_db
    user_types = {
        UserKind.admin: UserRole.admin,
        UserKind.trusted_manager: UserRole.manager,
        UserKind.manager: UserRole.manager,
        UserKind.trusted_dispatcher: UserRole.dispatcher,
        UserKind.dispatcher: UserRole.dispatcher,
        UserKind.app: UserRole.app,
    }
    with create_tmp_company(env, "Test company test_order_access") as company_id:
        with create_tmp_users(env, [company_id] * len(user_types), list(user_types.values())) as users:
            user_info = dict(zip(user_types.keys(), users))
            with create_route_env(
                    env,
                    'test_confirm_first_order_with_interval_change',
                    route_date=_ROUTE_DATE_ISO,
                    order_locations=[{'lat': 55.791928, 'lon': 37.841492}],
                    time_intervals=['08:00-18:00'],
                    company_id=company_id,
                    auth=env.auth_header_super) as route_env:

                # Provide access to the depot to the trusted manager/dispatcher (the other manager/dispatcher has no access)
                env_patch_request(
                    env,
                    api_path_with_company_id(env, 'user_depot', user_info[UserKind.trusted_manager]['id'], company_id=company_id),
                    data=[route_env['depot']['id']],
                    auth=env.auth_header_super
                )
                env_patch_request(
                    env,
                    api_path_with_company_id(env, 'user_depot', user_info[UserKind.trusted_dispatcher]['id'], company_id=company_id),
                    data=[route_env['depot']['id']],
                    auth=env.auth_header_super
                )

                yield {
                    'dbenv': env,
                    'company_id': company_id,
                    'users': user_info,
                    'route_env': route_env
                }


def _api_path(env_with_orders, order_id=None):
    return api_path_with_company_id(
        env_with_orders['dbenv'],
        'orders',
        object_id=order_id,
        company_id=env_with_orders['company_id'])


def _get_order_count(env_with_orders):
    env = env_with_orders['dbenv']
    response = env_get_request(env, _api_path(env_with_orders), auth=env.auth_header_super)
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert isinstance(j, list)
    return len(j)


def _get_orders(env_with_orders, user_kind):
    return env_get_request(
        env_with_orders['dbenv'],
        _api_path(env_with_orders),
        caller=env_with_orders['users'][user_kind])


def _get_order(env_with_orders, user_kind, order_id):
    assert order_id is not None
    return env_get_request(
        env_with_orders['dbenv'],
        _api_path(env_with_orders, order_id),
        caller=env_with_orders['users'][user_kind])


def _patch_order(env_with_orders, user_kind, order_id, data):
    assert order_id is not None
    return env_patch_request(
        env_with_orders['dbenv'],
        _api_path(env_with_orders, order_id),
        data=data,
        caller=env_with_orders['users'][user_kind])


def _add_order(env_with_orders, user_kind, data):
    return env_post_request(
        env_with_orders['dbenv'],
        _api_path(env_with_orders),
        data=data,
        caller=env_with_orders['users'][user_kind])


def _delete_order(env_with_orders, user_kind, order_id):
    assert order_id is not None
    return env_delete_request(
        env_with_orders['dbenv'],
        _api_path(env_with_orders, order_id),
        caller=env_with_orders['users'][user_kind])


def test_get_order(env_with_orders):
    order_id = env_with_orders['route_env']['orders'][0]['id']

    for user_kind in [UserKind.admin, UserKind.trusted_manager]:
        assert _get_order_count(env_with_orders) == 1

        response = _get_order(env_with_orders, user_kind, order_id)
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert j['id'] == order_id

    for user_kind in [UserKind.manager, UserKind.app]:
        assert _get_order(env_with_orders, user_kind, order_id).status_code == requests.codes.forbidden


def test_get_orders(env_with_orders):
    order_id = env_with_orders['route_env']['orders'][0]['id']

    for user_kind in [UserKind.admin, UserKind.trusted_manager, UserKind.trusted_dispatcher, UserKind.app]:
        assert _get_order_count(env_with_orders) == 1

        response = _get_orders(env_with_orders, user_kind)
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert isinstance(j, list) and len(j) == 1
        assert j[0]['id'] == order_id

    for user_kind in [UserKind.manager, UserKind.dispatcher]:
        response = _get_orders(env_with_orders, user_kind)
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert isinstance(j, list) and len(j) == 0


def test_admin_and_trusted_manager_can_patch_order(env_with_orders):
    assert _get_order_count(env_with_orders) == 1

    order_id = env_with_orders['route_env']['orders'][0]['id']

    for user_kind in [UserKind.admin, UserKind.trusted_manager]:
        response = _patch_order(env_with_orders, user_kind, order_id, {'comments': 'Abc'})
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert j['comments'] == 'Abc'

    for user_kind in [UserKind.manager, UserKind.trusted_dispatcher, UserKind.dispatcher, UserKind.app]:
        assert _patch_order(env_with_orders, user_kind, order_id, {'comments': 'Abc'}).status_code == requests.codes.forbidden


def test_admin_and_trusted_manager_can_add_and_delete_order(env_with_orders):
    assert _get_order_count(env_with_orders) == 1
    order_id = env_with_orders['route_env']['orders'][0]['id']

    def _get_new_order_json(user_kind):
        response = _get_order(env_with_orders, user_kind, order_id)
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert j['id'] == order_id
        for field in ['id', 'confirmed_at', 'time_interval_secs', 'time_window', 'partner_id', 'shared_with_company_id',
                      'shared_with_companies', 'delivered_at', 'notifications', 'company_id', 'history', 'status_log',
                      'order_status_comments', 'refined_lat', 'refined_lon']:
            j.pop(field, None)

        j['number'] = j['number'] + 'x'
        return j

    for user_kind in [UserKind.admin, UserKind.trusted_manager]:
        response = _add_order(env_with_orders, user_kind, _get_new_order_json(user_kind))
        assert response.status_code == requests.codes.ok
        new_order = response.json()
        assert new_order['id'] != order_id

        assert _get_order_count(env_with_orders) == 2
        assert _delete_order(env_with_orders, user_kind, new_order['id']).status_code == requests.codes.ok
        assert _get_order_count(env_with_orders) == 1

    new_order = _get_new_order_json(UserKind.admin)
    for user_kind in [UserKind.manager, UserKind.trusted_dispatcher, UserKind.dispatcher, UserKind.app]:
        assert _add_order(env_with_orders, user_kind, new_order).status_code == requests.codes.forbidden
        assert _delete_order(env_with_orders, user_kind, order_id).status_code == requests.codes.forbidden
