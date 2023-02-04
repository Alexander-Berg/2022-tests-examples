import json
import requests
import common_mrapi
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import wait_task, API_KEY, generate_task
from yatest.common import output_path


def test_sync_handler_core_mt(async_backend_url):
    task_value = generate_task(location_count=11, solver_time_limit_s=0.1)
    task_value["vehicles"][0]["routing_mode"] = "walking"
    task_value["vehicles"][1]["routing_mode"] = "transit"
    task_value["vehicles"][2]["routing_mode"] = "bicycle"
    task_value["options"]["matrix_router"] = "main"

    response = requests.post(f"{async_backend_url}/add/mvrp?apikey={API_KEY}", json.dumps(task_value))
    assert response.ok

    task_id = response.json()['id']
    response = requests.get(f"{async_backend_url}/result/{task_id}")
    assert response.ok

    j = wait_task(async_backend_url, task_id)

    assert j["id"] == task_id
    assert j["message"] == "Task successfully completed"


def test_async_handler_core_mt(async_backend_url):
    task_value = generate_task(location_count=100, solver_time_limit_s=0.1)
    task_value["vehicles"][0]["routing_mode"] = "walking"
    task_value["vehicles"][1]["routing_mode"] = "transit"
    task_value["vehicles"][2]["routing_mode"] = "bicycle"
    task_value["options"]["matrix_router"] = "main"

    response = requests.post(f"{async_backend_url}/add/mvrp?apikey={API_KEY}", json.dumps(task_value))
    assert response.ok

    task_id = response.json()['id']
    response = requests.get(f"{async_backend_url}/result/{task_id}")
    assert response.ok

    j = wait_task(async_backend_url, task_id, timeout=200)

    assert j["id"] == task_id
    assert j["message"] == "Task successfully completed"

    backend_logs_path = output_path("stderr")
    with open(backend_logs_path, 'r') as backend_logs_f:
        logs = backend_logs_f.read()
        assert 'Download matrix async with router coreMt' in logs
        assert 'Download matrix async with router coreBicycle' in logs


def test_cached_link_core_mt(async_backend_url):
    task_value = generate_task(location_count=100, solver_time_limit_s=0.1)
    task_value["vehicles"][0]["routing_mode"] = "walking"
    task_value["vehicles"][1]["routing_mode"] = "transit"
    task_value["vehicles"][2]["routing_mode"] = "bicycle"
    common_mrapi.check_cached_link(async_backend_url, task_value)
