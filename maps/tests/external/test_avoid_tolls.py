import requests
import json
import os
import sys
import pytest
from yatest.common import source_path
from yandex.maps.test_utils.common import wait_until
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import TASK_DIRECTORY, API_KEY


def _check_response_and_message(resp, code, message):
    sys.stderr.write('Status code {}\n'.format(resp.status_code))
    sys.stderr.write('Message: {}\n'.format(resp.json()['message']))
    sys.stderr.write('Status: {}\n'.format(resp.json()['status']))
    return resp.status_code == code and resp.json()["message"] == message


def _task_is_running(resp):
    return _check_response_and_message(resp, 201, "Task started and available for polling")


def _task_is_completed(resp):
    return _check_response_and_message(resp, 200, "Task successfully completed")


def check_avoid_tolls(backend_url, router, routing_mode, vrp):
    file_path = source_path(os.path.join(TASK_DIRECTORY, 'svrp_avoid_tolls.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    if vrp == "mvrp":
        task_value["vehicles"] = [task_value["vehicle"]]
        del task_value["vehicle"]
    task_value["options"]["routing_mode"] = routing_mode
    task_value["options"]["matrix_router"] = router
    results = []

    for avoid_tolls in [True, False]:
        task_value["options"]["avoid_tolls"] = avoid_tolls
        response = requests.post(backend_url + "/add/{}?apikey={}".format(vrp, API_KEY), json.dumps(task_value))

        assert response.status_code == 202, response.text
        task_id = response.json()['id']

        result_url = "{url}/result/{task_id}".format(url=backend_url, task_id=task_id)
        assert wait_until(lambda: _task_is_running(requests.get(result_url)), check_interval=1, timeout=20)
        assert wait_until(lambda: _task_is_completed(requests.get(result_url)), check_interval=1, timeout=120)
        response = requests.get(result_url)
        assert response.status_code == 200, response.text
        j = response.json()
        assert 'status' in j
        assert "completed" in j["status"]
        assert j["message"] == 'Task successfully completed', j["message"]
        results.append(requests.get(result_url))

    assert results[0].json()['result']['metrics']['total_duration_s'] > results[1].json()['result']['metrics']['total_duration_s']
    assert results[0].json()['result']['metrics']['total_transit_distance_m'] > results[1].json()['result']['metrics']['total_transit_distance_m']


@pytest.mark.parametrize("router", ["main"])
@pytest.mark.parametrize("routing_mode", ["driving", "truck"])
@pytest.mark.parametrize("vrp", ["mvrp", "svrp"])
def test_avoid_tolls_mrapi(async_backend_url, router, routing_mode, vrp):
    check_avoid_tolls(async_backend_url, router, routing_mode, vrp)
