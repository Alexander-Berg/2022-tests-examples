import json
import requests
import os
import pytest
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import wait_task, API_KEY, TASK_DIRECTORY
from yatest.common import source_path


def check_too_many_fallbacks(async_backend_url, explicit_routing_modes):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    test_options = {"options": {
        "time_zone": 3,
        "date": "2018-09-01",
        "quality": "low",
        "routing_mode": "driving",
        "matrix_router": "main"
    }}
    task_value.update(test_options)
    task_value["locations"] = task_value["locations"][0:3]
    task_value["locations"][1]["point"] = {"lat": 1.0, "lon": 1.0}
    task_value["locations"][2]["point"] = {"lat": 1.0, "lon": 179.0}
    for i, routing_mode in enumerate(explicit_routing_modes):
        task_value["vehicles"][i]["routing_mode"] = routing_mode

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok

    task_id = response.json()['id']
    response = requests.get(async_backend_url + "/result/" + task_id)
    assert response.ok

    j = wait_task(async_backend_url, task_id, timeout=200)
    assert j["id"] == task_id
    assert j["message"] == "Task failed"
    assert "error" in j
    assert "message" in j["error"]
    assert "Some of the failed location pairs" in j["error"]["message"]


@pytest.mark.parametrize(
    "explicit_routing_modes",
    [
        [],
        ['transit', 'transit', 'transit'],
        ['transit', 'driving', 'walking'],
        ['transit', 'truck', 'walking'],
    ]
)
def test_too_many_fallbacks_api(async_backend_url, explicit_routing_modes):
    check_too_many_fallbacks(async_backend_url, explicit_routing_modes)


def check_allow_fallbacks_for_walking_mode(async_backend_url):
    # 80% of distances are greater than 10km.
    task = {"vehicles": [], "locations": [], "depots": []}
    for i, routing_mode in enumerate(["walking", "driving"]):
        task["vehicles"].append({
            "id": i,
            "routing_mode": routing_mode
        })
    for i, point in enumerate([{"lat": 55.61, "lon": 37.76}, {"lat": 55.83, "lon": 37.42}, {"lat": 55.72, "lon": 37.55}]):
        task["locations"].append({
            "id": i,
            "point": point,
            "time_window": "07:00-23:00"
        })
    for i, point in enumerate([{"lat": 55.71, "lon": 37.86}, {"lat": 55.89, "lon": 37.49}]):
        task["depots"].append({
            "id": len(task["locations"]) + i,
            "point": point,
            "time_window": "07:00-23:00"
        })
    task["options"] = {
        "time_zone": 3,
        "quality": "low"
    }

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.ok

    task_id = response.json()['id']
    response = requests.get(async_backend_url + "/result/" + task_id)
    assert response.ok

    j = wait_task(async_backend_url, task_id)
    assert j["id"] == task_id
    assert j["message"] == "Task successfully completed"

    # Make locations unreachable. Task fails because of too many fallbacks in matrix for driving mode.
    for idx, location in enumerate(task["locations"]):
        location["point"] = {"lat": 60.7669, "lon": 31.5156 + idx / 10000}

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.ok

    task_id = response.json()['id']
    response = requests.get(async_backend_url + "/result/" + task_id)
    assert response.ok

    j = wait_task(async_backend_url, task_id)
    assert j["id"] == task_id
    assert j["message"] == "Task failed"

    # Make routing_mode for all vehicles equal to 'walking'. Task does not fail.
    for vehicle in task["vehicles"]:
        vehicle["routing_mode"] = "walking"

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.ok

    task_id = response.json()['id']
    response = requests.get(async_backend_url + "/result/" + task_id)
    assert response.ok

    j = wait_task(async_backend_url, task_id)
    assert j["id"] == task_id
    assert j["message"] == "Task successfully completed"
    assert j["matrix_statistics"]["walking"]["geodesic_distances"] == 14


def test_allow_fallbacks_for_walking_mode_api(async_backend_url):
    check_allow_fallbacks_for_walking_mode(async_backend_url)
