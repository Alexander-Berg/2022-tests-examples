import json
import pytest
import requests
from urllib.parse import urlencode
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import wait_task, get_task_value, API_KEY, LIMITED_API_KEY
from maps.b2bgeo.mvrp_solver.backend.tests_lib.conftest import (
    bring_up_backend,
    create_pg_database,
    SOLVER_BACKEND_BINARY_PATH,
)


@pytest.fixture()
def backend_url_small_concurrent_tasks_limit():
    with create_pg_database() as pg_conn:
        env = {'YC_TASK_HASH_TTL': '30', 'YC_CONCURRENT_TASKS_DEFAULT_LIMIT': '2'}
        with bring_up_backend(SOLVER_BACKEND_BINARY_PATH, pg_conn, env_params=env) as (url, process):
            yield url


@pytest.fixture()
def default_task():
    return get_task_value('10_locs.json')


def _add_task(task, backend_url, params):
    response = requests.post(f'{backend_url}/add/mvrp?{urlencode(params)}', json.dumps(task))
    assert response.status_code == requests.codes.accepted
    return response.json()


def test_same_task_posting_generates_the_same_id(async_backend_url_with_hashing, default_task):
    backend_url = async_backend_url_with_hashing
    params = {'apikey': API_KEY}

    task_id = _add_task(default_task, backend_url, params)['id']

    assert _add_task(default_task, backend_url, params)['id'] == task_id

    j = wait_task(backend_url, task_id)
    assert j['message'] == 'Task successfully completed', j['message']

    j = _add_task(default_task, backend_url, params)
    assert j['id'] == task_id
    assert j['message'] == 'Task successfully completed', j['message']
    assert set(j) == {'id', 'message', 'status', 'yt_operations'}


def test_different_ids_are_returned_for_different_parents(async_backend_url_with_hashing, default_task):
    backend_url = async_backend_url_with_hashing
    params = {'apikey': API_KEY}

    first_id = _add_task(default_task, backend_url, params)['id']
    second_id = _add_task(default_task, backend_url, {'parent_task_id': first_id, **params})['id']
    third_id = _add_task(default_task, backend_url, {'parent_task_id': second_id, **params})['id']

    assert len(set({first_id, second_id, third_id})) == 3

    assert _add_task(default_task, backend_url, params)['id'] == first_id
    assert _add_task(default_task, backend_url, {'parent_task_id': first_id, **params})['id'] == second_id
    assert _add_task(default_task, backend_url, {'parent_task_id': second_id, **params})['id'] == third_id


def test_different_ids_are_returned_for_different_apikeys(async_backend_url_with_hashing, default_task):
    backend_url = async_backend_url_with_hashing
    first_id = _add_task(default_task, backend_url, {'apikey': API_KEY})['id']
    second_id = _add_task(default_task, backend_url, {'apikey': LIMITED_API_KEY})['id']

    assert len(set({first_id, second_id})) == 2

    assert _add_task(default_task, backend_url, {'apikey': API_KEY})['id'] == first_id
    assert _add_task(default_task, backend_url, {'apikey': LIMITED_API_KEY})['id'] == second_id


def test_different_ids_are_returned_for_different_buckets(async_backend_url_with_hashing, default_task):
    backend_url = async_backend_url_with_hashing
    params = {'apikey': API_KEY}

    first_id = _add_task(default_task, backend_url, params)['id']
    second_id = _add_task(default_task, backend_url, {'bucket': 'b2bgeo-test', **params})['id']

    assert first_id != second_id

    assert _add_task(default_task, backend_url, params)['id'] == first_id
    assert _add_task(default_task, backend_url, {'bucket': 'b2bgeo-test', **params})['id'] == second_id


def test_latest_available_same_task_is_returned(default_task, postgres_database_connection):
    solver_path = SOLVER_BACKEND_BINARY_PATH
    pg_conn = postgres_database_connection
    params = {'apikey': API_KEY}

    env = {'YC_TASK_HASH_TTL': '0'}
    with bring_up_backend(solver_path, pg_conn, kv_disk_directory='.', env_params=env) as (backend_url, _):
        task_ids = [_add_task(default_task, backend_url, params)['id'] for _ in range(3)]

    assert len(set(task_ids)) == len(task_ids)

    env = {'YC_TASK_HASH_TTL': '30'}
    with bring_up_backend(solver_path, pg_conn, kv_disk_directory='.', env_params=env) as (backend_url, _):
        assert _add_task(default_task, backend_url, params)['id'] == task_ids[-1]


def test_new_task_is_created_if_previous_is_in_error_state(async_backend_url_with_hashing, default_task):
    backend_url = async_backend_url_with_hashing
    default_task['options']['thread_count'] = 5
    default_task['options']['task_count'] = 5

    task_id = _add_task(default_task, backend_url, {'apikey': API_KEY})['id']

    # Failed to solve on YT clusters (it's not configured)
    j = wait_task(backend_url, task_id)
    assert j['message'] == 'Error executing task.', j['message']

    assert task_id != _add_task(default_task, backend_url, {'apikey': API_KEY})['id']


def test_concurrent_tasks_limit_is_not_exceeded_for_same_tasks(backend_url_small_concurrent_tasks_limit, default_task):
    params = {'apikey': API_KEY}

    first_id = _add_task(default_task, backend_url_small_concurrent_tasks_limit, params)['id']

    default_task['options']['solver_time_limit_s'] = 2
    _add_task(default_task, backend_url_small_concurrent_tasks_limit, params)['id']

    default_task['options']['solver_time_limit_s'] = 1
    assert _add_task(default_task, backend_url_small_concurrent_tasks_limit, params)['id'] == first_id
