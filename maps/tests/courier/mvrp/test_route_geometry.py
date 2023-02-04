from http import HTTPStatus
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get


GEOMETRY_ID_RUNNING = 'dummy_geometry_task_id_running'
GEOMETRY_ID_COMPLETED = 'dummy_geometry_task_id_completed'
ROUTE_DATA = {'task_id': '8fbfc297-d0adc65a-a55412b1-2d9af710', 'run_number' : 1, 'vehicle_id' : '42516023897'}


def test_route_post_task(env: Environment):
    path = f'/api/v1/vrs/route?company_id={env.default_company.id}'
    local_post(env.client, path, data=ROUTE_DATA, headers=env.user_auth_headers, expected_status=HTTPStatus.ACCEPTED)


def test_route_status_task(env: Environment):
    path = f'/api/v1/vrs/route/{GEOMETRY_ID_RUNNING}/status'
    local_get(env.client, path, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)

    path = f'/api/v1/vrs/route/{GEOMETRY_ID_COMPLETED}/status'
    resp = env.client.get(path, headers=env.user_auth_headers)
    assert resp.status_code == HTTPStatus.SEE_OTHER
    assert 'Location' in resp.headers
    assert f'{GEOMETRY_ID_COMPLETED}/result' in resp.headers.get('Location', type=str)


def test_route_result_task(env: Environment):
    path = f'/api/v1/vrs/route/{GEOMETRY_ID_COMPLETED}/result'
    local_get(env.client, path, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)


def test_route_cancel_task(env: Environment):
    path = f'/api/v1/vrs/route/{GEOMETRY_ID_RUNNING}/cancel'
    local_post(env.client, path, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)
