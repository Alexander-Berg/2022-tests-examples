import requests
import json
import copy
import http.client
import os
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY, TASK_DIRECTORY, wait_task, fill_dummy_test_options
)


def get_task():
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)

    return task_value


def test_accepted(async_backend_url):
    task = get_task()
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.ACCEPTED, response.text
    j = wait_task(async_backend_url, response.json()['id'])
    assert "calculated" in j.get("status", {})


def test_no_time_zone(async_backend_url):
    task = get_task()
    del task['options']['time_zone']
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST
    print(response.text)


def test_wrong_lon(async_backend_url):
    task = get_task()
    task['locations'][0]['point']['lon'] = 999999
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST
    print(response.text)


def test_wrong_lat(async_backend_url):
    task = get_task()
    task['locations'][0]['point']['lat'] = -1000
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST
    print(response.text)


def test_wrong_depot_lat_lon(async_backend_url):
    task = get_task()
    task['depot']['point']['lat'] = 100
    task['depot']['point']['lon'] = 280
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST
    print(response.text)


def test_wrong_time_zone(async_backend_url):
    task = get_task()
    task['options']['time_zone'] = 180
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST
    print(response.text)


def test_shift_penalty_extra_field(async_backend_url):
    task = get_task()
    vehicles = task['vehicles']
    vehicle = copy.copy(vehicles[0])
    vehicle["id"] = len(vehicles)
    vehicle["shifts"] = [{"extra_field": ""}]
    vehicles.append(vehicle)
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST
    print(response.text)


def test_wrong_time_format_depot(async_backend_url):
    wrong_interval = '00:00:00-1.06.00.00'

    task = get_task()
    task['depot']['time_window'] = wrong_interval
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST, response.text
    resp = response.json()
    assert 'error' in resp
    assert wrong_interval in resp['error']['message']


def test_wrong_time_format_location(async_backend_url):
    wrong_interval = '-1.00.00 - 15.00'

    task = get_task()
    task['locations'][0]['time_window'] = wrong_interval
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST, response.text
    resp = response.json()
    assert 'error' in resp
    assert wrong_interval in resp['error']['message']


def test_extra_option_forbidden(async_backend_url):
    task = get_task()
    task['options']['non_existing_option'] = 'ololo'
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST
    print(response.text)


def test_wrong_location_title(async_backend_url):
    task = get_task()
    task['locations'][0]['title'] = 42
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST
    print(response.text)


def test_wrong_location_description(async_backend_url):
    task = get_task()
    task['locations'][0]['description'] = 42
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST
    print(response.text)


def test_wrong_location_phone(async_backend_url):
    task = get_task()
    task['locations'][0]['phone'] = 42
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST

    # 'phone' is not allowed in depot
    del task['locations'][0]['phone']
    task['depot']['phone'] = "+1 234 567 89 00"
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST


def test_wrong_location_comments(async_backend_url):
    task = get_task()
    task['locations'][0]['comments'] = 42
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST

    # 'comments' is not allowed in depot
    del task['locations'][0]['comments']
    task['depot']['comments'] = "Lorem ipsum"
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST


def test_wrong_location_shared_with_company_ids(async_backend_url):
    task = get_task()
    task['locations'][0]['shared_with_company_ids'] = 42
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST

    task['locations'][0]['shared_with_company_ids'] = [1.23]
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST

    task['locations'][0]['shared_with_company_ids'] = [-1]
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST


def test_vehicle_imei(async_backend_url):
    task = get_task()
    imei = 865905023851110
    task["vehicles"][1]["imei"] = imei

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.ACCEPTED, response.text
    task_id = response.json()['id']
    wait_task(async_backend_url, task_id)

    def check_vehicles(vehicles):
        assert len(vehicles) == 3
        assert "imei" not in vehicles[0]
        assert vehicles[1]["imei"] == imei
        assert "imei" not in vehicles[2]

    response = requests.get(async_backend_url + "/result/{}".format(task_id))
    assert response.status_code == 200, response.text
    check_vehicles(response.json()["result"]["vehicles"])

    response = requests.get(async_backend_url + "/log/request/{}".format(task_id))
    assert response.status_code == 200, response.text
    check_vehicles(response.json()["vehicles"])


def test_vehicle_imei_incorrect(async_backend_url):
    task = get_task()
    task["vehicles"][1]["imei"] = "imei"
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST, response.text

    task["vehicles"][1]["imei"] = 123.456
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == http.client.BAD_REQUEST, response.text
