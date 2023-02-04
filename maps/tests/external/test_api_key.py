import requests
import json
import os
from yatest.common import source_path

from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    wait_task, TASK_DIRECTORY, fill_dummy_test_options
)

API_KEY = "7f963d7c-5fa8-48b9-8ce7-1e8796e0dc76"


def check_task(backend_url, task_type, task_file):
    file_path = source_path(os.path.join(TASK_DIRECTORY, task_file))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    if task_type == "mvrp":
        fill_dummy_test_options(task_value)
    else:
        task_value["options"]["matrix_router"] = "geodesic"
    response = requests.post(backend_url + "/add/{}?apikey={}".format(task_type, API_KEY), json.dumps(task_value))
    assert response.ok, response.text
    task_id = response.json()['id']
    response = requests.get(backend_url + "/result/" + task_id)
    assert response.ok

    j = wait_task(backend_url, task_id)
    assert j["id"] == task_id
    assert "status" in j
    assert "calculated" in j["status"]
    assert j["message"] == 'Task successfully completed', j["message"]


def test_mvrp_task(async_backend_url):
    check_task(async_backend_url, "mvrp", "10_locs.json")


def test_svrp_task(async_backend_url):
    check_task(async_backend_url, "svrp", "svrp_request.json")
