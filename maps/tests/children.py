import http.client
import json
import os
import requests
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY,
    TASK_DIRECTORY,
    fill_dummy_test_options,
    wait_task
)

FAKE_APIKEY = '6e8d1df6-2bd2-49b5-8562-0210fe13a1fc'


def test_children(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok
    parent_task_id = response.json()['id']
    j = wait_task(async_backend_url, parent_task_id)
    assert "calculated" in j.get("status", {})

    response = requests.post(async_backend_url + f"/add/mvrp?apikey={API_KEY}&parent_task_id={parent_task_id}", json.dumps(task_value))
    assert response.ok
    child_task_id = response.json()['id']
    j = wait_task(async_backend_url, child_task_id)
    assert "calculated" in j.get("status", {})

    response = requests.get(async_backend_url + f"/children?apikey={API_KEY}&parent_task_id={parent_task_id}")
    assert response.ok
    assert child_task_id in [item['task_id'] for item in response.json()]


def test_add_apikey_mismatch(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok
    parent_task_id = response.json()['id']
    j = wait_task(async_backend_url, parent_task_id)
    assert "calculated" in j.get("status", {})

    response = requests.post(async_backend_url + f"/add/mvrp?apikey={FAKE_APIKEY}&parent_task_id={parent_task_id}", json.dumps(task_value))
    assert response.status_code == http.client.BAD_REQUEST
    assert "Apikey mismatch" in response.json()['error']['message']


def test_children_apikey_mismatch(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok
    parent_task_id = response.json()['id']
    j = wait_task(async_backend_url, parent_task_id)
    assert "calculated" in j.get("status", {})

    response = requests.post(async_backend_url + f"/add/mvrp?apikey={API_KEY}&parent_task_id={parent_task_id}", json.dumps(task_value))
    assert response.ok
    child_task_id = response.json()['id']
    j = wait_task(async_backend_url, child_task_id)
    assert "calculated" in j.get("status", {})

    response = requests.get(async_backend_url + f"/children?apikey={FAKE_APIKEY}&parent_task_id={parent_task_id}")
    assert response.status_code == http.client.BAD_REQUEST
    assert "Apikey mismatch" in response.json()['error']['message']


def test_add_non_existing_parent(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)
    response = requests.post(async_backend_url + f"/add/mvrp?apikey={API_KEY}&parent_task_id=FAKE_PARENT_ID", json.dumps(task_value))
    assert response.status_code == http.client.UNPROCESSABLE_ENTITY
    assert "Task id FAKE_PARENT_ID doesn't exist" in response.json()['error']['message']


def test_children_non_existing_parent(async_backend_url):
    response = requests.get(async_backend_url + f"/children?apikey={API_KEY}&parent_task_id=FAKE_PARENT_ID_2")
    assert response.status_code == http.client.UNPROCESSABLE_ENTITY
    assert "Task id FAKE_PARENT_ID_2 doesn't exist" in response.json()['error']['message']
