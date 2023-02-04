import json
import pytest
import os
import requests
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import API_KEY, TASK_DIRECTORY, wait_task
from yatest.common import source_path


def _check_unistat(backend_url, request_type, total_slice_count, has_walking=False):
    response = requests.get(backend_url + "/unistat")
    assert response.ok, response.text

    unistat = response.json()
    unistat = {x[0]: x[1] for x in unistat}

    expected_slice_count_sync = total_slice_count if request_type == "sync" else 0
    expected_slice_count_async = total_slice_count - expected_slice_count_sync
    expected_slice_count_walking_async = 0
    if has_walking:
        expected_slice_count_walking_async = 1

    assert unistat["mrapi_sync_requests_summ"] == expected_slice_count_sync
    assert unistat["mrapi_async_requests_summ"] == expected_slice_count_async
    assert unistat["mrapi_mt_async_pedestrian_requests_summ"] == expected_slice_count_walking_async

    request_count = 0
    for bucket in unistat["sync_matrix_download_time_ms_hgram"]:
        request_count += bucket[1]
    assert request_count == expected_slice_count_sync

    request_count = 0
    for bucket in unistat["async_matrix_download_time_ms_hgram"]:
        request_count += bucket[1]
    assert request_count == expected_slice_count_async


@pytest.mark.parametrize('allow_walking', [False, True])
def test_sync_router_request(async_backend_function_scope_url, allow_walking):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task = json.load(f)
    if allow_walking:
        task["vehicles"][0]["walking_courier"] = {"cost": {"hour": 1}}
    task["options"] = {"date": "2020-09-30", "time_zone": 3, "quality": "low"}
    task["locations"] = [
        {
            "time_window": "10:00-18:00",
            "point": {
                "lat": 59.81 + delta * 0.01,
                "lon": 35.48 + delta * 0.01
            }
        } for delta in range(10)
    ]

    response = requests.post(f"{async_backend_function_scope_url}/add/mvrp?apikey={API_KEY}", json.dumps(task))
    assert response.ok, response.text

    j = wait_task(async_backend_function_scope_url, response.json()['id'])
    assert j["message"] == 'Task successfully completed', j

    _check_unistat(async_backend_function_scope_url, "sync", 10, allow_walking)


@pytest.mark.parametrize('allow_walking', [False, True])
def test_async_router_request(async_backend_function_scope_url, allow_walking):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task = json.load(f)
    if allow_walking:
        task["vehicles"][0]["walking_courier"] = {"cost": {"hour": 1}}
    task["options"] = {
        "date": "2020-09-30",
        "time_zone": 3,
        "solver_time_limit_s": 0.01,
        "thread_count": 1,
        "task_count": 1,
    }
    task["depot"]["time_window"] = "10:00-11:00"
    task["locations"] = [
        {
            "time_window": "10:00-11:00",
            "point": {
                "lat": 55.646931 + delta * 0.01,
                "lon": 37.451529 + delta * 0.01,
            }
        } for delta in range(1000)
    ]

    response = requests.post(f"{async_backend_function_scope_url}/add/mvrp?apikey={API_KEY}", json.dumps(task))
    assert response.ok, response.text

    j = wait_task(async_backend_function_scope_url, response.json()['id'], timeout=150)
    assert j["message"] == 'Task successfully completed', j

    _check_unistat(async_backend_function_scope_url, "async", 2, allow_walking)
