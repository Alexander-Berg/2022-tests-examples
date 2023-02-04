from http import HTTPStatus
import json
import os
import requests
import time

from yatest.common import source_path


ZONE_TASK_DIRECTORY = "maps/b2bgeo/mvrp_solver/backend/async_backend/tests/zones/data/"


def get_all_zones():
    file_path = source_path(os.path.join(ZONE_TASK_DIRECTORY + "zones_list.json"))
    with open(file_path, "r") as f:
        return json.load(f)


def get_all_objects():
    file_path = source_path(os.path.join(ZONE_TASK_DIRECTORY + "objects_list.json"))
    with open(file_path, "r") as f:
        return json.load(f)


def get_all_locations():
    locations = get_all_objects()
    for location in locations:
        location["point"] = {}
        location["point"]["lon"] = location["geometry"]["coordinates"][0]
        location["point"]["lat"] = location["geometry"]["coordinates"][1]
        location.pop("geometry")
        location["time_window"] = "10:00-20:00"
    return locations


def get_point_inside_all_zones():
    return {
        "lon": 37.85,
        "lat": 55.75
    }


def _get_zone_by_id(id):
    all_zones = get_all_zones()
    for zone in all_zones:
        if zone["id"] == id:
            return zone
    raise Exception(f"No zone with id '{id}' found")


def get_between_big_and_small_squares_zone():
    big_square_zone = _get_zone_by_id("big square")
    small_square_zone = _get_zone_by_id("small square")

    between_big_and_small_squares_zone = big_square_zone
    between_big_and_small_squares_zone["id"] = "between big and small square"
    between_big_and_small_squares_zone["geometry"]["coordinates"].append(small_square_zone["geometry"]["coordinates"][0])

    return between_big_and_small_squares_zone


def get_between_small_and_tiny_squares_zone():
    small_square_zone = _get_zone_by_id("small square")
    tiny_square_zone = _get_zone_by_id("tiny square")

    between_small_and_tiny_squares_zone = small_square_zone
    between_small_and_tiny_squares_zone["id"] = "between small and tiny square"
    between_small_and_tiny_squares_zone["geometry"]["coordinates"].append(tiny_square_zone["geometry"]["coordinates"][0])

    return between_small_and_tiny_squares_zone


def get_big_square_zone():
    return _get_zone_by_id("big square")


def get_task_value():
    task_value = {}
    task_value["zones"] = get_all_zones()
    task_value["objects"] = get_all_objects()

    return task_value


def get_task_result():
    file_path = source_path(os.path.join(ZONE_TASK_DIRECTORY + "result.json"))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    return task_value


def wait_task_completed(async_backend_url, task_id):
    timeout = 5
    start_time = time.time()
    while True:
        result_url = f"{async_backend_url}/zone_detect/{task_id}/result"
        response = requests.get(result_url)
        if response.status_code == HTTPStatus.OK:
            j = response.json()
            assert "id" in j
            return j
        if start_time + timeout < time.time():
            raise Exception(f"Task {task_id} was not completed in {timeout} seconds.")
        time.sleep(int(response.headers["Retry-After"]))


def start_task(async_backend_url, task_value):
    response = requests.post(async_backend_url + "/zone_detect", json.dumps(task_value))
    assert response.status_code == HTTPStatus.ACCEPTED
    return response.json()['id']
