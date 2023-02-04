import json
import requests
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import wait_task, API_KEY
from yatest.common import output_path


def check_cached_link(async_backend_url, task_value):
    task_value["options"]["matrix_router"] = "main"
    for idx in range(2):
        response = requests.post(f"{async_backend_url}/add/mvrp?apikey={API_KEY}", json.dumps(task_value))
        assert response.ok

        task_id = response.json()['id']

        j = wait_task(async_backend_url, task_id, 200)
        assert j["id"] == task_id
        assert j["message"] == "Task successfully completed"

    backend_logs_path = output_path("stderr")
    with open(backend_logs_path, 'r') as backend_logs_f:
        logs = backend_logs_f.read()
        assert 'Use cached download link' in logs
        assert 'Failed to download cached matrix' not in logs
