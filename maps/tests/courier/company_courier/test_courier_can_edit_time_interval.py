from http import HTTPStatus

import pytest as pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_patch, local_post


@skip_if_remote
@pytest.mark.parametrize("courier_can_patch_time_interval,expected_status", [(True, HTTPStatus.OK),
                                                                             (False, HTTPStatus.FORBIDDEN)])
def test_courier_can_patch_time_window(env: Environment, courier_can_patch_time_interval, expected_status):
    company_path = f'/api/v1/companies/{env.default_company.id}'
    local_patch(env.client, company_path, data={'courier_can_patch_time_interval': courier_can_patch_time_interval},
                headers=env.user_auth_headers)

    order = {
        "number": "test",
        "time_interval": "00:00-23:59",
        "address": "some address",
        "lat": 55.791928,
        "lon": 37.841492,
        "route_id": env.default_route.id,
    }
    path_orders = f"/api/v1/companies/{env.default_company.id}/orders"
    order_dict = local_post(env.client, path_orders, headers=env.user_auth_headers, data=order)

    patch_order = f'/api/v1/couriers/{env.default_courier.id}/routes/{env.default_route.id}/orders/{order_dict["id"]}'
    local_patch(env.client, patch_order, headers=env.user_auth_headers,
                data={"time_interval": "10:00-1.23:59"}, expected_status=expected_status)
