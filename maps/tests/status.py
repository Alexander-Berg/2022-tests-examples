import os
import requests
import json
import http.client
import datetime
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import API_KEY, TASK_DIRECTORY, wait_task
from yatest.common import source_path


def test_estimate(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task = json.load(f)
    task["options"]["solver_time_limit_s"] = 10
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    j = response.json()
    assert response.status_code == http.client.ACCEPTED
    assert "status" in j
    assert "queued" in j["status"]
    assert "estimate" in j["status"]
    now = datetime.datetime.now().timestamp()
    assert now + 10 < j["status"]["estimate"] < now + 15
    wait_task(async_backend_url, response.json()['id'])
