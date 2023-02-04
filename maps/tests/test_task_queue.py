import requests
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import wait_task, API_KEY, \
    TASK_QUEUE_LARGEST_TASK_LOCATIONS, DEFAULT_SOLVER_WORKER_THREADS
from maps.b2bgeo.test_lib.sanitizers_utils import sanitizer_aware_timeout
from maps.b2bgeo.mvrp_solver.backend.tests_lib.conftest import (
    bring_up_backend, create_pg_database,
    SOLVER_BACKEND_BINARY_PATH,
)
from maps.b2bgeo.mvrp_solver.backend.tests_lib.db_util import db_connection, get_tasks_queue_row_count

from util import run_task

TASK_SIZE_LIMIT = TASK_QUEUE_LARGEST_TASK_LOCATIONS ** 2
DEFAULT_PARAMS = {'solver_time_limit_s': 10, 'quality': 'low'}


def _get_unistat(async_backend_url):
    response = requests.get(async_backend_url + "/unistat")
    assert response.ok, response.text
    unistat = response.json()
    unistat = {x[0]: x[1] for x in unistat}
    return unistat


def _send_task(async_backend_url, api_key, params):
    params = {**DEFAULT_PARAMS, **params}

    # run_task() will add one more location (depot), so fix the value we pass
    if 'location_count' in params:
        params['location_count'] -= 1

    return run_task(async_backend_url, api_key, **params)


def _run_tasks(async_backend_url, api_key, params_list):
    task_ids = []

    for index, params in enumerate(params_list):
        response = _send_task(async_backend_url, api_key, params)
        assert response.ok
        task_id = response.json()['id']

        unistat = _get_unistat(async_backend_url)

        assert unistat['task_size_limit_ammx'] == TASK_SIZE_LIMIT
        if unistat['pending_task_count_ammx'] == 0:
            assert unistat['free_worker_count_ammx'] + unistat['mvrp_queue_size_ammx'] \
                + unistat['svrp_queue_size_ammx'] == DEFAULT_SOLVER_WORKER_THREADS
        else:
            assert unistat['free_worker_count_ammx'] == 0

        response = requests.get(async_backend_url + "/result/" + task_id)
        assert response.ok

        if response.status_code == requests.codes.created:
            if index == 0 and unistat['mvrp_queue_size_ammx'] == 1:
                task_size = params['location_count'] ** 2
                assert unistat['task_size_used_ammx'] == task_size

        task_ids.append(task_id)

    results = []

    for task_id in task_ids:
        j = wait_task(async_backend_url, task_id, timeout=sanitizer_aware_timeout(60))
        assert "status" in j
        results.append(j)

    unistat = _get_unistat(async_backend_url)
    assert unistat['free_worker_count_ammx'] == DEFAULT_SOLVER_WORKER_THREADS
    assert unistat['mvrp_queue_size_ammx'] + unistat['svrp_queue_size_ammx'] + unistat['pending_task_count_ammx'] == 0

    return results


def _check_results_are_good(results):
    for j in results:
        assert "calculated" in j["status"]
        assert j["message"] == 'Task successfully completed', j["message"]


def test_tasks_under_limit(async_backend_url_task_queue_size_limit):
    url = async_backend_url_task_queue_size_limit

    tasks = [{'location_count': 10}] * 3
    results = _run_tasks(url, API_KEY, tasks)
    _check_results_are_good(results)

    # Pending tasks picked by a worker but not yet executed have 'started'
    # status, so we check 'matrix_downloaded' status time instead.
    matrix_downloaded_times = [r['status']['matrix_downloaded'] for r in results]
    completed_times = [r['status']['completed'] for r in results]

    # All tasks in this test should run simultaneously
    assert max(matrix_downloaded_times) < min(completed_times)


def test_tasks_above_limit(async_backend_url_task_queue_size_limit):
    url = async_backend_url_task_queue_size_limit

    # Limit is 100 locs => task size 10000
    # Sizes for these tasks are:
    # 10 locs => 100
    # 99 locs => 9801
    # 20 locs => 400
    location_count = [10, 99, 20, 19]
    tasks = [{'location_count': p} for p in location_count]
    results = _run_tasks(url, API_KEY, tasks)
    _check_results_are_good(results)

    # Pending tasks picked by a worker but not yet executed have 'started'
    # status, so we check 'matrix_downloaded' status time instead.
    matrix_downloaded_times = [r['status']['matrix_downloaded'] for r in results]
    started_times = [r['status']['started'] for r in results]
    completed_times = [r['status']['completed'] for r in results]

    # Tasks 0 and 1 (10 and 99 locs) should be able to run simultaneously
    assert max(matrix_downloaded_times[0:2]) < min(completed_times[0:2])

    # Task 2 (15 locs) should be able to start only after task 1 (99 locs) completes
    assert matrix_downloaded_times[2] > completed_times[1]

    # The queue is expected to not pick more than one "extra" task it cannot
    # solve right away. Therefore task 3 (19 locs) should not be picked until
    # the task 1 (99 locs) is finished.
    assert started_times[3] >= completed_times[1]


def test_single_task_above_limit():
    solver_path = SOLVER_BACKEND_BINARY_PATH
    with create_pg_database() as pg_conn:
        env = {
            'YC_TASK_QUEUE_LARGEST_TASK_LOCATIONS': '1'
        }
        with bring_up_backend(solver_path, pg_conn, env_params=env) as (url, _):
            # Bad request is sent to user in case of too big task to handle
            response = _send_task(url, API_KEY, {'location_count': 10})
            assert response.status_code == 400
            error_msg = response.json()['error']
            assert error_msg['message'].startswith('The task is too big')
            assert error_msg['incident_id']

            # Task is still saved to the DB to have information on how many such tasks do we have
            response = requests.get(f'{url}/result/{error_msg["incident_id"]}')
            assert response.ok
            assert response.json()['error']['message'].startswith('The task is too big')
            assert response.json()['message'] == 'Error executing task.'

            # Task is not saved to tasks.queue
            with db_connection(pg_conn) as conn:
                assert get_tasks_queue_row_count(conn) == 0
