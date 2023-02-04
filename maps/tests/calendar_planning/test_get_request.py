import requests

from maps.b2bgeo.mvrp_solver.backend.async_backend.tests_lib.cp_util import (
    post_cp_task, wait_cp_task, get_cp_task_request, load_cp_task,
)

from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import API_KEY, EMPTY_API_KEY


def test_get_request(async_backend_url):
    response = post_cp_task(async_backend_url)
    task_id = response['id']

    wait_cp_task(async_backend_url, task_id)

    response = get_cp_task_request(async_backend_url, task_id, apikey=API_KEY)
    assert response == load_cp_task()

    response = get_cp_task_request(
        async_backend_url, task_id, apikey=EMPTY_API_KEY, expected_status_code=requests.codes.not_found)
    assert 'error' in response
    assert 'message' in response['error']
    assert 'incident_id' in response['error']


def test_override_task_options_with_cgi_parameters(async_backend_url):
    new_solver_time_limit_s = 0
    new_matrix_router = 'geodesic'

    response = post_cp_task(async_backend_url, solver_time_limit_s=new_solver_time_limit_s, matrix_router=new_matrix_router)
    task_id = response['id']

    wait_cp_task(async_backend_url, task_id)
    j = get_cp_task_request(async_backend_url, task_id)

    assert j['options']['matrix_router'] == new_matrix_router
    assert j['options']['solver_time_limit_s'] == new_solver_time_limit_s
