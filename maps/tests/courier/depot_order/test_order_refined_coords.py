from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_patch
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user

from ya_courier_backend.models import UserRole


TEST_ORDER = {
    "number": "test_order",
    "time_interval": "11:00 - 23:00",
    "address": "ул. Льва Толстого, 16",
    "lat": 55.7447,
    "lon": 37.6728,
    "customer_number": "224",
    "route_number": "test_route",
}


def _add_route(env, route_number=None, depot_number=None):
    if not route_number:
        route_number = "test_route"
    if not depot_number:
        depot_number = env.default_depot.number

    path = f"/api/v1/companies/{env.default_company.id}/routes"
    data = {
        "number": route_number,
        "courier_number": env.default_courier.number,
        "depot_number": depot_number,
        "date": "2020-11-30",
    }
    return local_post(env.client, path, headers=env.user_auth_headers, data=data)


@skip_if_remote
def test_refined_coords(env: Environment):
    route = _add_route(env)
    path = f"/api/v1/companies/{env.default_company.id}/orders"
    order = local_post(env.client, path, headers=env.user_auth_headers, data=TEST_ORDER)
    assert order['refined_lat'] is None
    assert order['refined_lon'] is None

    courier_login = "test_app"
    courier_id, courier_auth = add_user(env, courier_login, UserRole.app)

    path = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/orders/{order['id']}"
    patched_order = local_patch(env.client, path, headers=courier_auth,
                                data={'refined_lat': 55.74, 'refined_lon': 37.67})
    assert patched_order['refined_lat'] == 55.74
    assert patched_order['refined_lon'] == 37.67
