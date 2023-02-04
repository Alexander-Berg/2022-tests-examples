import requests
import json
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY, get_task_value, wait_task
)


def test_initial_routes(async_backend_url):
    task_value = get_task_value('10_locs.json')

    old_resp = requests.post(async_backend_url + '/add/mvrp?apikey={}'.format(API_KEY), json.dumps(task_value))
    assert old_resp.ok

    old_j = wait_task(async_backend_url, old_resp.json()['id'])

    task_value["initial_routes"] = old_j["result"]["routes"]
    task_value["options"]["solver_time_limit_s"] = 0
    new_resp = requests.post(async_backend_url + '/add/mvrp?apikey={}'.format(API_KEY), json.dumps(task_value))
    assert new_resp.ok

    new_j = wait_task(async_backend_url, new_resp.json()['id'])
    assert len(old_j["result"]["routes"]) == len(new_j["result"]["routes"])
    for i in range(len(old_j["result"]["routes"])):
        old_route = old_j["result"]["routes"][i]["route"]
        new_route = new_j["result"]["routes"][i]["route"]
        assert len(old_route) == len(new_route)
        for j in range(len(old_route)):
            assert old_route[j]["node"]["value"]["id"] == new_route[j]["node"]["value"]["id"]
