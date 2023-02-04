import contextlib
import json
import pytest
import requests
import time
from urllib.parse import urlencode
from werkzeug.wrappers import Response

from maps.b2bgeo.mvrp_solver.backend.tests_lib.db_util import db_connection, load_attempt_count
from maps.b2bgeo.mvrp_solver.backend.tests_lib.conftest import (
    bring_up_backend,
    SOLVER_BACKEND_BINARY_PATH,
)
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import API_KEY, wait_task, get_task_value
from maps.b2bgeo.test_lib.http_server import mock_http_server
from yandex.maps.test_utils.common import wait_until


MATRIX_ROUTER_URL = 'http://core-driving-matrix-router.maps.yandex.net'


@contextlib.contextmanager
def mock_errored_router():
    def _handler(environ, start_response):
        time.sleep(0.1)
        return Response("Internal error", status=500)(environ, start_response)

    with mock_http_server(_handler) as url:
        yield url


def _task_started(backend_url, task_id):
    resp = requests.get(f'{backend_url}/result/{task_id}')
    if resp.status_code == 202:
        return False
    if resp.status_code == 201:
        return True
    raise Exception(f'Expected queued or started task status but got: code {resp.status_code}, body {resp.json()}')


def _get_task(matrix_router):
    task = get_task_value('10_locs.json')
    task['options'].update({'time_zone': 3, 'solver_time_limit_s': 10, 'task_count': 5, 'matrix_router': matrix_router})
    return task


@pytest.mark.parametrize('wait_time', [0.1, 0.5, 1])
def test_task_is_solved_with_one_attempt_even_if_shutdown_happens(postgres_database_connection, wait_time):
    env = {"YC_MAX_LOCAL_TASK_COUNT": "10"}
    solver_path = SOLVER_BACKEND_BINARY_PATH
    pg_conn = postgres_database_connection

    with bring_up_backend(solver_path, pg_conn, kv_disk_directory='.', env_params=env) as (backend_url, _):
        task = _get_task('geodesic')
        response = requests.post(backend_url + '/add/mvrp?' + urlencode({'apikey': API_KEY}), json.dumps(task))
        assert response.status_code == requests.codes.accepted
        task_id = response.json()['id']

        wait_until(lambda: _task_started(backend_url, task_id))
        time.sleep(wait_time)

    with db_connection(pg_conn) as conn:
        assert load_attempt_count(conn, task_id) == 0

    with bring_up_backend(solver_path, pg_conn, kv_disk_directory='.', env_params=env) as (backend_url, _):
        j = wait_task(backend_url, task_id)
        assert j['message'] == 'Task successfully completed', j['message']

    with db_connection(pg_conn) as conn:
        assert load_attempt_count(conn, task_id) == 1


@pytest.mark.parametrize('wait_time', [0.1, 0.5, 1])
def test_task_is_solved_with_one_attempt_even_if_shutdown_happens_on_router_downloading(postgres_database_connection, wait_time):
    # We will be stuck in router downloading because of errors when we get shutdown request
    with mock_errored_router() as mock_router:
        env = {"YC_MAX_LOCAL_TASK_COUNT": "10", 'YC_MATRIX_ROUTER_URL': mock_router}
        solver_path = SOLVER_BACKEND_BINARY_PATH
        pg_conn = postgres_database_connection

        with bring_up_backend(solver_path, pg_conn, kv_disk_directory='.', env_params=env) as (backend_url, _):
            task = _get_task('main')
            response = requests.post(backend_url + '/add/mvrp?' + urlencode({'apikey': API_KEY}), json.dumps(task))
            assert response.status_code == requests.codes.accepted
            task_id = response.json()['id']

            wait_until(lambda: _task_started(backend_url, task_id))
            time.sleep(wait_time)

    with db_connection(pg_conn) as conn:
        assert load_attempt_count(conn, task_id) == 0

    env = {"YC_MAX_LOCAL_TASK_COUNT": "10"}
    with bring_up_backend(solver_path, pg_conn, kv_disk_directory='.', env_params=env) as (backend_url, _):
        j = wait_task(backend_url, task_id)
        assert j['message'] == 'Task successfully completed', j['message']

    with db_connection(pg_conn) as conn:
        assert load_attempt_count(conn, task_id) == 1
