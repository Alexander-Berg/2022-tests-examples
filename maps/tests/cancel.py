import requests
import json
import os
from yatest.common import source_path
from yandex.maps.test_utils.common import wait_until
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY, TASK_DIRECTORY, fill_dummy_test_options
)


def test_cancel(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok
    task_id = response.json()['id']
    result_url = "{url}/result/{task_id}".format(url=async_backend_url, task_id=task_id)
    cancel_url = "{url}/cancel/{task_id}".format(url=async_backend_url, task_id=task_id)
    response = requests.get(result_url)
    assert response.ok

    wait_until(lambda: requests.get(result_url).status_code == 201)

    response = requests.get(cancel_url)
    assert response.status_code == requests.codes.not_implemented
    assert "id" in response.text
