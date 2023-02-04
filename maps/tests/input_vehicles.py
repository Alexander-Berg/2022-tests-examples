import requests
import json
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    wait_task, get_task_value, API_KEY
)


def _create_vehicle(id, routing_mode=None, specs=None):
    vehicle = {"capacity": {"weight_kg": 3}, "id": id, "max_runs": 3}
    if routing_mode is not None:
        vehicle["routing_mode"] = routing_mode
    if specs is not None:
        vehicle["specs"] = specs

    return vehicle


def _check_stats(ms):
    assert ms["total_distances"] > 0
    assert ms["requested_router"] == "geodesic"
    assert ms["used_router"] == "geodesic"
    assert ms["geodesic_distances"] == ms["total_distances"]


def _solve_task(async_backend_url, task_value):
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok, response.text
    j = response.json()
    assert "id" in j
    task_id = j["id"]

    response = requests.get(async_backend_url + "/result/" + task_id)
    assert response.ok, response.text

    j = wait_task(async_backend_url, task_id)
    assert "status" in j
    assert "calculated" in j["status"]
    assert j["message"] == "Task successfully completed", j["message"]

    return j


def _read_default_task():
    return get_task_value("10_locs.json")


def test_routing_modes(async_backend_url):
    task_value = _read_default_task()
    task_value["vehicles"] = [
        _create_vehicle(0, "driving"),
        _create_vehicle(1, "truck"),
        _create_vehicle(2, "transit"),
        _create_vehicle(3, "walking"),
        _create_vehicle(4, "bicycle"),
        _create_vehicle(5),
    ]

    j = _solve_task(async_backend_url, task_value)

    result_vehicles = j["result"]["vehicles"]
    assert len(result_vehicles) == 6

    routing_modes = ["driving", "truck", "transit", "walking", "bicycle"]
    for idx, routing_mode in enumerate(routing_modes):
        assert result_vehicles[idx]["routing_mode"] == routing_mode
    assert "routing_mode" not in result_vehicles[len(routing_modes)]

    matrix_statistics = j["matrix_statistics"]
    assert len(matrix_statistics) == len(routing_modes)

    for routing_mode in routing_modes:
        _check_stats(matrix_statistics[routing_mode])


def test_vehicle_classes(async_backend_url):
    task_value = _read_default_task()
    task_value["vehicles"] = [
        _create_vehicle(0),
        _create_vehicle(1, "truck"),
        _create_vehicle(2, "truck", {"height": 2.0, "width": 2.0}),
        _create_vehicle(3, "truck", {"length": 7.0}),
        _create_vehicle(4, "truck", {"max_weight": 22.0, "height": 3.9}),
        _create_vehicle(5, "truck", {"length": 20.0}),
        _create_vehicle(6, "truck", {"height": 40.0}),
    ]

    j = _solve_task(async_backend_url, task_value)

    result_vehicles = j["result"]["vehicles"]
    assert len(result_vehicles) == 7

    assert "routing_mode" not in result_vehicles[0]
    for idx in range(1, len(task_value["vehicles"])):
        assert result_vehicles[idx]["routing_mode"] == "truck"

    matrix_statistics = j["matrix_statistics"]
    classes = set(matrix_statistics.keys())
    assert len(classes) == 6
    assert sum([cls.startswith('truck') for cls in classes]) == 5
    assert 'driving' in classes

    for vehicle_class in classes:
        _check_stats(matrix_statistics[vehicle_class])


def test_disabled_vehicle_classes(async_backend_url):
    task_value = _read_default_task()
    task_value["vehicles"] = [
        _create_vehicle(1, "truck"),  # truck N1
        _create_vehicle(3, "truck", {"length": 7.0}),  # truck N2
    ]
    task_value["options"]["enable_vehicle_classes"] = False

    j = _solve_task(async_backend_url, task_value)

    result_vehicles = j["result"]["vehicles"]
    assert len(result_vehicles) == 2
    assert result_vehicles[0]["routing_mode"] == "truck"
    assert result_vehicles[1]["routing_mode"] == "truck"

    matrix_statistics = j["matrix_statistics"]
    assert set(matrix_statistics.keys()) == {"truck"}
    _check_stats(matrix_statistics["truck"])


def test_vehicle_specs_are_the_same_in_input_and_output(async_backend_url):
    task_value = _read_default_task()
    task_value["vehicles"] = [
        _create_vehicle(1, "truck", {"max_weight_kg": 1001}),
    ]

    j = _solve_task(async_backend_url, task_value)
    result_vehicles = j["result"]["vehicles"]
    assert len(result_vehicles) == 1
    assert result_vehicles[0]["specs"] == task_value["vehicles"][0]["specs"]
