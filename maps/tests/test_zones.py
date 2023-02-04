import http.client
import json
import pytest
import re
import requests
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY, post_task_and_check_error_message, get_task_value, wait_task
)
from maps.b2bgeo.identity.libs.pytest_lib.identity_jwt import user_auth_headers
from yatest.common import output_path

from zones.util import (
    get_all_zones, get_all_locations, get_point_inside_all_zones, get_big_square_zone,
    get_between_big_and_small_squares_zone, get_between_small_and_tiny_squares_zone,
)

from maps.b2bgeo.test_lib.reference_book_values import (
    INCOMPATIBLE_ZONES,
    COMPANY_ZONES,
    PUBLIC_ZONES,
)


def _check_matrix_download_started(started, task_id):
    backend_logs_path = output_path("stderr")
    with open(backend_logs_path, 'r') as backend_logs_f:
        match = re.search(
            rf'tid={task_id}> <pid=(\d+)> info: Starting matrix download for the task', backend_logs_f.read())
        assert (match is not None) == started


def test_schema_validation(async_backend_url):
    task_value = get_task_value('10_locs.json')

    task_value["zones"] = get_all_zones()
    task_value["zones"][0]["geometry"]["coordinates"] = {}

    post_task_and_check_error_message(async_backend_url, task_value, "Request parameters do not meet the requirements.")


def test_invalid_id(async_backend_url):
    task_value = get_task_value('10_locs.json')

    task_value["zones"] = get_all_zones()
    task_value["zones"][0]["id"] = "Sepa, rated"

    post_task_and_check_error_message(async_backend_url, task_value,
                                      "At /zones/0: Id 'Sepa, rated' contains forbidden character ','")


@pytest.mark.parametrize(
    ("polygon", "msg"),
    [
        (
            [[[1, 2], [2, 2], [1, 3], [2, 3], [1, 2]]],
            "At /zones/0 in 'big square': Geometry validation failed: Ring Self-intersection at coordinate: 1.5 2.5"
        ),
        (
            [],
            "At /zones/0 in 'big square': Polygon should have at least one ring"
        ),
        (
            [[[0, 0], [1, 1], [0, 0]]],  # Need to test for exceptions that get thrown by GEOS through geolib
            "At /zones/0 in 'big square': IllegalArgumentException: Invalid number of points in LinearRing"
        )
    ]
)
def test_invalid_polygon(async_backend_url, polygon, msg):
    task_value = get_task_value('10_locs.json')

    task_value["zones"] = get_all_zones()
    task_value["zones"][0]["geometry"]["coordinates"] = polygon

    post_task_and_check_error_message(async_backend_url, task_value, msg)


def test_undefined_incompatible_zones(async_backend_url):
    task_value = get_task_value('10_locs.json')

    task_value["zones"] = get_all_zones()
    task_value["options"]["incompatible_zones"] = [["first undefined zone", "second undefined zone"]]

    resp = requests.post(f"{async_backend_url}/add/mvrp?apikey={API_KEY}", json.dumps(task_value))
    assert resp.ok, resp.text

    j = wait_task(async_backend_url, resp.json()['id'], status=http.client.BAD_REQUEST)
    assert j["message"] == "Error executing task.", j["message"]
    assert j["error"]["message"] == "Zone 'first undefined zone' is not defined.", j["error"]["message"]

    _check_matrix_download_started(False, j['id'])


@pytest.mark.parametrize("vehicle_parameter", ["allowed_zones", "forbidden_zones"])
def test_undefined_vehicle_zones(async_backend_url, vehicle_parameter):
    task_value = get_task_value('10_locs.json')

    task_value["zones"] = get_all_zones()
    task_value["vehicles"][0][vehicle_parameter] = ["undefined zone"]

    resp = requests.post(f"{async_backend_url}/add/mvrp?apikey={API_KEY}", json.dumps(task_value))
    assert resp.ok, resp.text

    j = wait_task(async_backend_url, resp.json()['id'], status=http.client.BAD_REQUEST)
    assert j["message"] == "Error executing task.", j["message"]
    assert j["error"]["message"] == "Zone 'undefined zone' is not defined.", j["error"]["message"]

    _check_matrix_download_started(False, j['id'])


def test_duplicate_zones(async_backend_url):
    task_value = get_task_value('10_locs.json')

    task_value["zones"] = get_all_zones()
    task_value["zones"].append(task_value["zones"][0])

    resp = requests.post(f"{async_backend_url}/add/mvrp?apikey={API_KEY}", json.dumps(task_value))
    assert resp.ok, resp.text

    j = wait_task(async_backend_url, resp.json()['id'], status=http.client.BAD_REQUEST)
    assert j["message"] == "Error executing task.", j["message"]
    assert j["error"]["message"] == "Zone 'big square' is defined multiple times.", j["error"]["message"]

    _check_matrix_download_started(False, j['id'])


@pytest.mark.skip(reason="incompatible zones in the same location are not allowed")
def test_ok(async_backend_url):
    task_value = get_task_value("10_locs.json")

    task_value["zones"] = [*get_all_zones(), get_between_big_and_small_squares_zone()]
    task_value["options"]["incompatible_zones"] = [["between big and small square", "small square"]]

    resp = requests.post(f"{async_backend_url}/add/mvrp?apikey={API_KEY}", json.dumps(task_value))
    assert resp.ok, resp.text

    j = wait_task(async_backend_url, resp.json()['id'])
    assert j["message"] == 'Task successfully completed', j["message"]
    _check_matrix_download_started(True, j['id'])  # just check assertion is correct


def test_zones_logic(async_backend_url):
    task_value = get_task_value('10_locs.json')

    task_value["zones"] = [
        get_between_big_and_small_squares_zone(),
        get_between_small_and_tiny_squares_zone(),
        get_big_square_zone(),
    ]

    task_value["locations"] = get_all_locations()
    task_value["depot"]["id"] = "depot"
    task_value["depot"]["point"] = get_point_inside_all_zones()

    task_value["vehicles"][0]["forbidden_zones"] = [task_value["zones"][0]["id"]]
    task_value["vehicles"][0]["allowed_zones"] = [task_value["zones"][2]["id"]]

    task_value["vehicles"][1]["forbidden_zones"] = [task_value["zones"][1]["id"]]
    task_value["vehicles"][1]["allowed_zones"] = [task_value["zones"][2]["id"]]

    task_value["vehicles"][2]["forbidden_zones"] = [task_value["zones"][0]["id"], task_value["zones"][1]["id"]]

    resp = requests.post(f"{async_backend_url}/add/mvrp?apikey={API_KEY}", json.dumps(task_value))
    assert resp.ok, resp.text

    j = wait_task(async_backend_url, resp.json()['id'])
    assert j["message"] == 'Task successfully completed', j["message"]

    assert len(j["result"]["routes"]) == 3

    assert j["result"]["routes"][0]["route"][0]["node"]["value"]["zones"] == ["big square"]
    assert j["result"]["routes"][0]["route"][1]["node"]["value"]["zones"] == ["between small and tiny square", "big square"]
    assert j["result"]["routes"][1]["route"][1]["node"]["value"]["zones"] == ["between big and small square", "big square"]
    assert "zones" not in j["result"]["routes"][2]["route"][1]["node"]["value"]


def test_defined_zones_locations(async_backend_url):
    task_value = get_task_value('10_locs.json')
    task_value["locations"][0]["zones"] = ["some zone"]

    resp = requests.post(f"{async_backend_url}/add/mvrp?apikey={API_KEY}", json.dumps(task_value))
    assert resp.ok, resp.text

    j = wait_task(async_backend_url, resp.json()['id'])
    assert j["message"] == 'Task successfully completed', j["message"]


def test_incompatible_zones_with_reference_book(async_backend_with_jwt_and_mocked_reference_book):
    url = async_backend_with_jwt_and_mocked_reference_book
    task_value = get_task_value('10_locs.json')

    create_response = requests.post(f'{url}/add/mvrp', data=json.dumps(task_value), headers=user_auth_headers())
    assert create_response.ok, create_response.text
    task_id = create_response.json()['id']
    task_response = requests.get(f'{url}/log/request/{task_id}')
    j = task_response.json()
    assert 'options' in j
    assert 'incompatible_zones' in j['options']
    assert j["options"]["incompatible_zones"] == INCOMPATIBLE_ZONES[1]
    assert 'zones' in j
    assert j['zones'] == [{'id': x['number'], 'geometry': x['polygon']} for x in COMPANY_ZONES[1]]


def test_vehicle_zones_with_reference_book(async_backend_with_jwt_and_mocked_reference_book):
    url = async_backend_with_jwt_and_mocked_reference_book
    zones = COMPANY_ZONES[1][0:4] + PUBLIC_ZONES[0:2]

    task_value = get_task_value('10_locs.json')
    task_value['vehicles'][0]['forbidden_zones'] = [zones[0]['number'], zones[4]['number']]
    task_value['vehicles'][0]['allowed_zones'] = [zones[1]['number']]
    task_value['vehicles'][1]['forbidden_zones'] = [zones[2]['number']]
    task_value['vehicles'][1]['allowed_zones'] = [zones[3]['number'], zones[5]['number']]

    create_response = requests.post(f'{url}/add/mvrp', data=json.dumps(task_value), headers=user_auth_headers())
    assert create_response.ok, create_response.text
    task_id = create_response.json()['id']
    task_response = requests.get(f'{url}/log/request/{task_id}')
    j = task_response.json()
    assert 'zones' in j
    assert j['zones'] == [{'id': x['number'], 'geometry': x['polygon']} for x in zones]


def test_ignore_zones_with_reference_book(async_backend_with_jwt_and_mocked_reference_book):
    url = async_backend_with_jwt_and_mocked_reference_book
    zones = COMPANY_ZONES[1][0:4] + PUBLIC_ZONES[0:2]

    task_value = get_task_value('10_locs.json')
    task_value['options']['ignore_zones'] = True
    task_value['vehicles'][0]['forbidden_zones'] = [zones[0]['number'], zones[4]['number']]
    task_value['vehicles'][0]['allowed_zones'] = [zones[1]['number']]
    task_value['vehicles'][1]['forbidden_zones'] = [zones[2]['number']]
    task_value['vehicles'][1]['allowed_zones'] = [zones[3]['number'], zones[5]['number']]

    create_response = requests.post(f'{url}/add/mvrp', data=json.dumps(task_value), headers=user_auth_headers())
    assert create_response.ok, create_response.text
    task_id = create_response.json()['id']
    task_response = requests.get(f'{url}/log/request/{task_id}')
    j = task_response.json()
    assert 'zones' not in j
