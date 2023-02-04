import requests
import json
import os
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY,
    fill_dummy_test_options,
    post_task_and_check_async_error_message,
    post_task_and_check_error_message,
    TASK_DIRECTORY,
    wait_task,
)


def _prepare_task(vehicle_ids):
    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), 'r') as f:
        task = json.load(f)

    fill_dummy_test_options(task)

    assert len(task['vehicles']) == len(vehicle_ids)
    for i, id in enumerate(vehicle_ids):
        if id is None:
            del task['vehicles'][i]['id']
        else:
            task['vehicles'][i]['id'] = id

    return task


def _solve_task(async_backend_url, task):
    response = requests.post(async_backend_url + '/add/mvrp?apikey={}'.format(API_KEY), json.dumps(task))
    assert response.ok, response.text
    j = response.json()
    assert 'id' in j
    task_id = j['id']

    j = wait_task(async_backend_url, task_id)
    assert 'status' in j

    return j


def _check_task_succeeded(async_backend_url, vehicle_ids):
    task = _prepare_task(vehicle_ids)

    j = _solve_task(async_backend_url, task)

    assert 'calculated' in j['status']
    assert j['message'] == 'Task successfully completed', j['message']
    assert [x['id'] for x in j['result']['vehicles']] == vehicle_ids
    assert {x['vehicle_id'] for x in j['result']['routes']} <= set(vehicle_ids)


def _check_task_failed(async_backend_url, vehicle_ids, expected_error):
    task = _prepare_task(vehicle_ids)
    post_task_and_check_async_error_message(async_backend_url, task, expected_error)


def _check_task_schema_failed(async_backend_url, vehicle_ids):
    task = _prepare_task(vehicle_ids)

    response = requests.post(async_backend_url + '/add/mvrp?apikey={}'.format(API_KEY), json.dumps(task))
    assert response.status_code == requests.codes.bad_request, response.text

    j = response.json()
    assert 'error' in j
    assert 'message' in j['error']
    msg = j['error']['message']
    assert 'Request parameters do not meet the requirements.' in msg
    assert 'Type of error: Required parameter is missing' in msg


def test_integer_ids(async_backend_url):
    _check_task_succeeded(async_backend_url, [1, 2, 3])
    _check_task_succeeded(async_backend_url, [0, 1, 2])
    _check_task_succeeded(async_backend_url, [3, 200, 0])


def test_string_ids(async_backend_url):
    _check_task_succeeded(async_backend_url, ['abc', 'z', '1'])
    _check_task_succeeded(async_backend_url, ['1', 'abc', 'z'])
    _check_task_succeeded(async_backend_url, ['', 'abc', '1'])


def test_missing_ids(async_backend_url):
    _check_task_schema_failed(async_backend_url, [None, 'z', '1'])
    _check_task_schema_failed(async_backend_url, [None, None, 'z'])


def test_fail_on_duplicate_ids(async_backend_url):
    _check_task_failed(async_backend_url, [1, 2, 1], "At least two vehicles have the same ID")
    _check_task_failed(async_backend_url, ['1', '2', '1'], "At least two vehicles have the same ID")
    _check_task_failed(async_backend_url, ['', '', 'A'], "At least two vehicles have the same ID")


def test_fail_on_mixed_integer_and_string_ids(async_backend_url):
    _check_task_failed(async_backend_url, ['A', 'B', 1], "All vehicle IDs should be either integers or strings. Found string ID 'A' and integer ID 1.")
    _check_task_failed(async_backend_url, [1, 'B', 'A'], "All vehicle IDs should be either integers or strings. Found integer ID 1 and string ID 'B'.")


def test_incorrect_shift_ids(async_backend_url):
    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), 'r') as f:
        task = json.load(f)
    fill_dummy_test_options(task)

    task['vehicles'][0]['shifts'] = [{"id": "0", "time_window": "10:00-15:00"},
                                     {"id": "0", "time_window": "15:00-20:00"}]
    post_task_and_check_error_message(async_backend_url, task, "vehicle with id 1: shift id 0 is defined more than once.")
