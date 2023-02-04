import json
import os
import pytest
import requests
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import API_KEY, TASK_DIRECTORY, wait_task


class MultiDict(dict):
    def __init__(self, items):
        self['not_empty'] = 'not_empty'
        self._items = items

    def items(self):
        return self._items


def test_add_options_array(async_backend_url):
    task = {"options": []}
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == requests.codes.bad_request
    j = response.json()
    assert "error" in j
    assert "Request parameters do not meet the requirements" in j["error"]["message"]
    assert len(j["error"]["incident_id"])


def test_type_missing(async_backend_url):
    task = {"options": {"time_zone": 3}}
    response = requests.post(async_backend_url + "/add/?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == requests.codes.bad_request
    j = response.json()
    assert "error" in j
    assert j["error"]["message"] == "Unknown task type ''"
    assert len(j["error"]["incident_id"])


@pytest.mark.parametrize("locale", ["en_US", "ru_RU", "zh_CN"])
def test_value_missing(async_backend_url, locale):
    url = f"{async_backend_url}/add/mvrp?apikey={API_KEY}"
    response_default = requests.post(url, "")
    json_default = response_default.json()

    if locale:
        url += f"&lang={locale}"
    response_locale = requests.post(url, "")
    json_locale = response_locale.json()

    assert response_default.status_code == requests.codes.bad_request, json.dumps(json_default)
    assert response_locale.status_code == requests.codes.bad_request, json.dumps(json_locale)
    assert "error" in json_default
    assert "error" in json_locale
    assert len(json_default["error"]["incident_id"])
    assert len(json_locale["error"]["incident_id"])
    if locale == "ru_RU":
        assert json_default["error"]["message"] != json_locale["error"]["message"]
    else:
        assert json_default["error"]["message"] == json_locale["error"]["message"]


@pytest.mark.parametrize("locale", ["en_US", "ru_RU", "zh_CN"])
def test_invalid_json(async_backend_url, locale):
    url = f"{async_backend_url}/add/mvrp?apikey={API_KEY}"
    response_default = requests.post(url, "{")
    json_default = response_default.json()

    if locale:
        url += f"&lang={locale}"
    response_locale = requests.post(url, "{")
    json_locale = response_locale.json()

    assert response_default.status_code == requests.codes.bad_request, json.dumps(json_default)
    assert response_locale.status_code == requests.codes.bad_request, json.dumps(json_locale)
    assert "error" in json_default
    assert "error" in json_locale
    assert len(json_default["error"]["incident_id"])
    assert len(json_locale["error"]["incident_id"])
    if locale == "ru_RU":
        assert json_default["error"]["message"] != json_locale["error"]["message"]
    else:
        assert json_default["error"]["message"] == json_locale["error"]["message"]


def test_unsupported_task(async_backend_url):
    task = {"options": {"time_zone": 3}}
    response = requests.post(async_backend_url + "/add/unsupported?apikey={}".format(API_KEY), json.dumps(task))
    assert response.status_code == requests.codes.bad_request
    j = response.json()
    assert 'error' in j
    assert "Unknown task type 'unsupported'" == j["error"]["message"]
    assert len(j["error"]["incident_id"])


def test_duplicate_field(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)

    data = json.dumps({
        "options": MultiDict([("date", "2020-01-04"), ("minimize_lateness_risk", False), ("minimize_lateness_risk", False), ("time_zone", 3), ("solver_time_limit_s", 200)]),
        "vehicles": task_value["vehicles"],
        "locations": task_value["locations"],
        "depot": task_value["depot"]
    })
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), data)

    assert response.status_code == requests.codes.bad_request
    assert "Duplicating field `minimize_lateness_risk'" in response.json()['error']['message']


@pytest.mark.parametrize("filename", ['parking_same_point.json',    # Identical locations test
                                      'test_shift_separation.json'  # Empty shifts test
                                      ])
def test_different_files(async_backend_url, filename):
    file_path = source_path(os.path.join(TASK_DIRECTORY, filename))
    with open(file_path, "r") as f:
        task_value = json.load(f)

    task_value["options"] = {
        "quality": "low",
        "time_zone": 3,
        "solver_time_limit_s": 0.1,
    }

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok, response.text

    j = wait_task(async_backend_url, response.json()['id'])
    assert "calculated" in j.get("status", {})
