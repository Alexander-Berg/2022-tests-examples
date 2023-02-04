import requests
import json
import pytest
import os
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import wait_task, API_KEY, TASK_DIRECTORY


@pytest.mark.parametrize("quality_time_limit", [
    ("low", 1),
    ("normal", 5),
])
def test_modes_task(async_backend_url, quality_time_limit):
    quality, time_limit = quality_time_limit
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    test_options = {
        "options": {
            "matrix_router": "geodesic",
            "quality": quality,
            "time_zone": 3,
            "date": "2018-09-01"
        }
    }
    task_value.update(test_options)
    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok
    task_id = response.json()['id']
    j = wait_task(async_backend_url, task_id)
    assert 'calculated' in j['status']
    print(json.dumps(j, indent=4))

    assert "result" in j
    result = j["result"]
    assert "options" in result
    assert result["options"]["solver_time_limit_s"] == time_limit
