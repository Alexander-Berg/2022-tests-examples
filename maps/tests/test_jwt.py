import os
import requests
import json
import pytest
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    fill_dummy_test_options,
    generate_task,
    API_KEY,
    BANNED_API_KEY,
    TASK_DIRECTORY,
    wait_task,
    SYNC_SOLVER_URL,
)
from maps.b2bgeo.identity.libs.pytest_lib.identity_jwt import (
    user_auth_headers,
    company_auth_headers,
)
from maps.b2bgeo.identity.libs.payloads.py import UserRole


def _generate_svrp(location_count=None):
    if location_count is None:
        location_count = 10
    task_value = generate_task(location_count=location_count, solver_time_limit_s=0.01)
    task_value['vehicle'] = task_value['vehicles'][0]
    del task_value['vehicles']
    return task_value


def _generate_task():
    return generate_task(location_count=2, solver_time_limit_s=0.01)


@pytest.mark.parametrize(
    'response_code,headers,apikey',
    [
        (requests.codes.accepted, company_auth_headers(), ''),
        (requests.codes.accepted, user_auth_headers(role=UserRole.admin), ''),
        (requests.codes.accepted, user_auth_headers(role=UserRole.manager), ''),
        (requests.codes.accepted, user_auth_headers(role=UserRole.dispatcher), ''),
        (requests.codes.accepted, {}, API_KEY),
        (requests.codes.accepted, user_auth_headers(), BANNED_API_KEY),
        (requests.codes.accepted, company_auth_headers(), BANNED_API_KEY),
        (requests.codes.forbidden, user_auth_headers(role=UserRole.app), ''),
        (
            requests.codes.forbidden,
            user_auth_headers(role=UserRole.app),
            API_KEY,
        ),
        (requests.codes.unauthorized, {}, ''),
        (requests.codes.unauthorized, {}, 'abc'),
    ],
)
def test_add_mvrp_with_jwt(async_backend_url_jwt, response_code, headers, apikey):
    task_value = _generate_task()
    response = requests.post(
        f"{async_backend_url_jwt}/add/mvrp{'?apikey=' + apikey if apikey else ''}",
        data=json.dumps(task_value),
        headers=headers,
    )
    assert response.status_code == response_code


def test_good_jwt_without_authlib_unacceptable(async_backend_url):
    task_value = _generate_task()
    response = requests.post(
        f'{async_backend_url}/add/mvrp',
        data=json.dumps(task_value),
        headers=user_auth_headers(),
    )
    assert response.status_code == requests.codes.unauthorized


def test_svrp_with_jwt(async_backend_url_jwt):
    task_value = _generate_svrp()
    cgi = f'?apikey={API_KEY}&origin=ya_courier'
    response = requests.post(
        f'{async_backend_url_jwt}/solve/svrp{cgi}',
        data=json.dumps(task_value),
        headers=user_auth_headers(),
        allow_redirects=False,
    )
    assert response.status_code == requests.codes.temporary_redirect
    assert response.headers['Location'] == f'{SYNC_SOLVER_URL}{cgi}'


def test_expression_validate_with_jwt(async_backend_url_jwt):
    expression = {'expression': '100 * duration_h + 8 * distance_km'}
    resp = requests.post(
        f'{async_backend_url_jwt}/validate/vehicle_cost',
        json.dumps(expression),
        headers=user_auth_headers(),
    )
    assert resp.status_code == requests.codes.ok
    assert resp.json() == {}


def test_children_with_jwt(async_backend_url_jwt):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, 'r') as f:
        task_value = json.load(f)

    fill_dummy_test_options(task_value)
    headers = user_auth_headers()
    response = requests.post(
        f'{async_backend_url_jwt}/add/mvrp',
        data=json.dumps(task_value),
        headers=headers,
    )
    assert response.ok
    parent_task_id = response.json()['id']
    j = wait_task(async_backend_url_jwt, parent_task_id)
    assert 'calculated' in j.get('status', {})

    response = requests.post(
        f'{async_backend_url_jwt}/add/mvrp?parent_task_id={parent_task_id}',
        data=json.dumps(task_value),
        headers=headers,
    )
    assert response.ok
    child_task_id = response.json()['id']
    j = wait_task(async_backend_url_jwt, child_task_id)
    assert 'calculated' in j.get('status', {})

    response = requests.get(
        f'{async_backend_url_jwt}/children?parent_task_id={parent_task_id}',
        headers=headers,
    )
    assert response.ok
    assert child_task_id in [item['task_id'] for item in response.json()]


@pytest.mark.parametrize('resource', ['status', 'result', 'request'])
def test_calendar_planning_jwt(async_backend_url_jwt, resource):
    task_id = 'bf35cff0-5432-47b1-8196-b910279366f5'  # random task id (not existing)
    status_path = (
        f'{async_backend_url_jwt}/calendar_planning/tasks/{task_id}/{resource}'
    )
    headers = user_auth_headers()
    response = requests.get(status_path, headers=headers)
    assert response.status_code == requests.codes.not_found
