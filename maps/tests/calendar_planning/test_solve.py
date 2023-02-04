import requests

from maps.b2bgeo.mvrp_solver.backend.async_backend.tests_lib.cp_util import (
    post_cp_task, wait_cp_task, get_cp_task_request, get_cp_task_status, get_cp_task_result,
    get_cp_task_metadata,
)


def test_metadata(async_backend_url):
    j = post_cp_task(async_backend_url)
    task_id = j['id']

    wait_cp_task(async_backend_url, task_id)

    j = get_cp_task_metadata(async_backend_url, task_id, expected_status_code=requests.codes.ok)

    assert 'dates' in j
    assert 'vehicles' in j
    assert 'dates_vehicles' in j


def test_ok(async_backend_url):
    j = post_cp_task(async_backend_url)
    task_id = j['id']

    get_cp_task_result(async_backend_url, task_id, expected_status_code=requests.codes.not_found)

    j = wait_cp_task(async_backend_url, task_id)

    assert 'routes' in j
    assert '__localized_string__' not in str(j)


def test_unknown_task_id(async_backend_url):
    get_cp_task_status(async_backend_url, 'unknown_task_id', expected_status_code=requests.codes.not_found)
    get_cp_task_result(async_backend_url, 'unknown_task_id', expected_status_code=requests.codes.not_found)
    get_cp_task_request(async_backend_url, 'unknown_task_id', expected_status_code=requests.codes.not_found)


def test_wrong_apikey(async_backend_url):
    get_cp_task_result(async_backend_url, 'unknown_task_id', expected_status_code=requests.codes.not_found)
