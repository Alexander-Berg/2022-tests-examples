import requests
import json
import uuid
import http.client
import os
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import BANNED_API_KEY, TASK_DIRECTORY, fill_dummy_test_options


INVALID_API_KEY = str(uuid.uuid4())


def load_task(filename):
    file_path = source_path(os.path.join(TASK_DIRECTORY, filename))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)

    return task_value


def test_mvrp_invalid(async_backend_url):
    task_value = load_task('10_locs.json')

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(INVALID_API_KEY), json.dumps(task_value))
    assert response.status_code == http.client.FORBIDDEN
    assert "error" in response.text and "An invalid API key was passed" in response.text


def test_mvrp_banned(async_backend_url):
    task_value = load_task('10_locs.json')

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(BANNED_API_KEY), json.dumps(task_value))
    assert response.status_code == http.client.PAYMENT_REQUIRED
    assert "error" in response.text and "Your API key is blocked." in response.text


def test_svrp_invalid(async_backend_url):
    task_value = load_task('svrp_request.json')

    response = requests.post(async_backend_url + "/add/svrp?apikey={}".format(INVALID_API_KEY), json.dumps(task_value))
    assert response.status_code == http.client.FORBIDDEN
    assert "error" in response.text and "An invalid API key was passed" in response.text


def test_svrp_banned(async_backend_url):
    task_value = load_task('svrp_request.json')

    response = requests.post(async_backend_url + "/add/svrp?apikey={}".format(BANNED_API_KEY), json.dumps(task_value))
    assert response.status_code == http.client.PAYMENT_REQUIRED
    assert "error" in response.text and "Your API key is blocked." in response.text
