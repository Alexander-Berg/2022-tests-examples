from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_post, local_get


@skip_if_remote
def test_create_and_get(env):
    data = {
        "provider": "yandex_taxi_cargo",
        "order_id": "rented courier order",
    }
    path = f"/api/v1/companies/{env.default_company.id}/rented-couriers"
    response = local_post(env.client, path, headers=env.user_auth_headers, data=data)
    courier_id = response['id']

    path = f"/api/v1/companies/{env.default_company.id}/rented-couriers/{courier_id}"
    response = local_get(env.client, path, headers=env.user_auth_headers)
    del response["created_at"]

    assert response == {
        "provider": "yandex_taxi_cargo",
        "order_id": "rented courier order",
        "company_id": env.default_company.id,
        "id": courier_id,
    }


@skip_if_remote
def test_unknown_provider(env):
    data = {
        "provider": "not taxi",
        "order_id": "rented courier order",
    }
    path = f"/api/v1/companies/{env.default_company.id}/rented-couriers"
    local_post(env.client, path, headers=env.user_auth_headers, data=data,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
