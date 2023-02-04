import json
import requests

from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import wait_task, API_KEY, generate_task


def test_ok(async_backend_increase_task_count_for_local_tasks_url):
    url = async_backend_increase_task_count_for_local_tasks_url
    task_value = generate_task(location_count=10, solver_time_limit_s=3)
    task_value["options"]["thread_count"] = 2
    task_value["options"]["task_count"] = 5

    response = requests.post(f"{url}/add/mvrp?apikey={API_KEY}", json.dumps(task_value))
    assert response.ok

    task_id = response.json()["id"]
    response = requests.get(f"{url}/result/{task_id}")

    j = wait_task(url, task_id)
    assert len(j["result"]["metrics"]["_tasks_summary"]) == task_value["options"]["task_count"]
