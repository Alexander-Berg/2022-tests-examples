import requests
import json
import os
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    wait_task, fill_dummy_test_options, API_KEY, TASK_DIRECTORY,
    post_task_and_check_async_error_message,
    post_task_and_check_error_message,
)


def test_int_ids(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)

    task_value["depot"]["id"] = 1234
    for idx, location in enumerate(task_value["locations"]):
        location["id"] = idx + 1

    task_value["locations"][1]["type"] = "pickup"
    task_value["locations"][1]["delivery_to"] = 5

    task_value["vehicles"][0]["visited_locations"] = [
        {
            "id": 4
        },
        {
            "id": 5
        }
    ]

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok, response.text
    j = response.json()
    assert 'id' in j
    task_id = j['id']

    response = requests.get(async_backend_url + "/result/" + task_id)
    assert response.ok, response.text

    j = wait_task(async_backend_url, task_id)
    assert "status" in j
    assert "calculated" in j["status"]
    assert j["message"] == 'Task successfully completed', j["message"]


def test_string_ids(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)

    task_value["depot"]["id"] = "depot_id"
    for idx, location in enumerate(task_value["locations"]):
        location["id"] = "location_" + str(idx + 1)

    task_value["locations"][1]["type"] = "pickup"
    task_value["locations"][1]["delivery_to"] = "location_5"

    task_value["vehicles"][0]["visited_locations"] = [
        {
            "id": "location_4"
        },
        {
            "id": "location_5"
        }
    ]

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok, response.text
    j = response.json()
    assert 'id' in j
    task_id = j['id']

    response = requests.get(async_backend_url + "/result/" + task_id)
    assert response.ok, response.text

    j = wait_task(async_backend_url, task_id)
    assert "status" in j
    assert "calculated" in j["status"]
    assert j["message"] == 'Task successfully completed', j["message"]


def test_int_and_string_ids(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)

    task_value["depot"]["id"] = 1234
    for idx, location in enumerate(task_value["locations"][:5]):
        location["id"] = "location_" + str(idx + 1)
    for idx, location in enumerate(task_value["locations"][5:], 5):
        location["id"] = idx + 1

    task_value["locations"][1]["type"] = "pickup"
    task_value["locations"][1]["delivery_to"] = "location_5"

    task_value["vehicles"][0]["visited_locations"] = [
        {
            "id": "location_4"
        },
        {
            "id": 6
        }
    ]

    post_task_and_check_async_error_message(
        async_backend_url, task_value,
        "All assigned location/depot IDs should be either integers or strings. Found integer ID 1234, string ID 'location_1'.")


def test_int_ids_and_string_delivery_to(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)

    task_value["depot"]["id"] = 1234
    for idx, location in enumerate(task_value["locations"]):
        location["id"] = idx + 1

    task_value["locations"][1]["type"] = "pickup"
    task_value["locations"][1]["delivery_to"] = "5"

    post_task_and_check_async_error_message(
        async_backend_url, task_value,
        "All assigned location/depot IDs should be either integers or strings. Found integer ID 1234, string ID '5'.")


def test_locations_with_the_same_id(async_backend_url):
    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), 'r') as f:
        task = json.load(f)
    fill_dummy_test_options(task)

    task["locations"][5]["id"] = 0
    task["locations"][8]["id"] = 0

    post_task_and_check_error_message(async_backend_url, task, "At least two locations or depots have the same id: 0")
