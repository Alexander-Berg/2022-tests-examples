import json
import requests
import os
import pytest
import common_mrapi
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import wait_task, API_KEY, TASK_DIRECTORY, generate_task
from yatest.common import source_path, output_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.conftest import (
    bring_up_backend,
    create_pg_database,
    SOLVER_BACKEND_BINARY_PATH,
)


def _create_specs(routing_mode, height=None, width=None, length=None, max_weight=None):
    res = {"routing_mode": routing_mode}

    if height or width or length or max_weight:
        res["specs"] = {}
    if height:
        res["specs"]["height"] = height
    if width:
        res["specs"]["width"] = width
    if length:
        res["specs"]["length"] = length
    if max_weight:
        res["specs"]["max_weight"] = max_weight
    return res


def check_sync_handler_mrapi(async_backend_url, explicit_specs):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    task_value["options"]["solver_time_limit_s"] = 1
    task_value["locations"] = task_value["locations"][0:2]
    for i, specs in enumerate(explicit_specs):
        task_value["vehicles"][i].update(specs)

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok

    task_id = response.json()['id']
    response = requests.get(async_backend_url + "/result/" + task_id)
    assert response.ok

    j = wait_task(async_backend_url, task_id)

    assert j["id"] == task_id
    assert j["message"] == "Task successfully completed"
    for routing_mode in j["matrix_statistics"]:
        assert j["matrix_statistics"][routing_mode]["slice_count"] > 1


@pytest.mark.parametrize(
    "explicit_specs",
    [
        [],
        [_create_specs('driving'), _create_specs('truck')],
        [_create_specs('truck'), _create_specs('truck')],
        [_create_specs('truck', 1.0, 2.0, 3.0, 4.0), _create_specs('truck', max_weight=100.0)]
    ]
)
def test_sync_handler_mrapi(async_backend_url, explicit_specs):
    check_sync_handler_mrapi(async_backend_url, explicit_specs)


def check_mrapi_async(async_backend_url, task):
    task["options"]["matrix_router"] = "main"

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.ok

    task_id = response.json()['id']

    j = wait_task(async_backend_url, task_id, 200)
    assert j["id"] == task_id
    assert j["message"] == "Task successfully completed"

    backend_logs_path = output_path("stderr")
    with open(backend_logs_path, 'r') as backend_logs_f:
        assert 'Download matrix async with router mrapi' in backend_logs_f.read()


def test_mrapi_async(async_backend_url):
    task = generate_task(300, 0.1, 300, coordinate_step=0.01, matrix_router_type="mrapi")
    check_mrapi_async(async_backend_url, task)


def test_mrapi_cached_link(async_backend_url):
    task = generate_task(300, 0.1, 300, coordinate_step=0.01, matrix_router_type="mrapi")
    common_mrapi.check_cached_link(async_backend_url, task)


def test_no_caching_with_different_cgi_params(async_backend_url):
    task = generate_task(200, 0.1, 200, coordinate_step=0.01, matrix_router_type="mrapi")
    task["options"]["routing_mode"] = "driving"
    check_mrapi_async(async_backend_url, task)

    task["options"]["routing_mode"] = "truck"
    check_mrapi_async(async_backend_url, task)

    backend_logs_path = output_path("stderr")
    with open(backend_logs_path, 'r') as backend_logs_f:
        logs = backend_logs_f.read()
        assert 'Use cached download link' not in logs


def test_mrapi_split_cached_link():
    with create_pg_database() as pg_conn:
        env = {'YC_COST_MATRIX_REQUEST_DIMENSION_LIMIT': '300'}
        with bring_up_backend(SOLVER_BACKEND_BINARY_PATH, pg_conn, env_params=env) as (backend_url, process):
            task = generate_task(598, 0.1, 598, coordinate_step=0.01, matrix_router_type="mrapi", time_window='07:00:00-07:59:59')
            common_mrapi.check_cached_link(backend_url, task)
