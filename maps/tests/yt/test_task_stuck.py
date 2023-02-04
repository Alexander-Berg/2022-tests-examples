import json
import os
import sys
import requests

from yatest.common import source_path
from yandex.maps.test_utils.common import wait_until

from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    TASK_DIRECTORY,
    API_KEY,
    get_unistat_value,
    scoped_handler_cb,
    fill_dummy_test_options
)
from maps.b2bgeo.mvrp_solver.backend.tests_lib.conftest import (
    SOLVER_BACKEND_BINARY_PATH,
    bring_up_backend,
    bring_up_slow_yt_stub
)
from maps.b2bgeo.mvrp_solver.backend.tests_lib.yt_common import yt_handler_delay, task_is_completed


def test_stuck_task_counter(postgres_database_connection):
    with bring_up_slow_yt_stub() as yt_proxy:
        network_delay_s = 5
        env = {
            "YT_PROXY": yt_proxy['proxy_address'],
            "YC_STUCK_TASKS_HOURS_THRESHOLD": str(network_delay_s / (60 * 60))
        }
        with bring_up_backend(SOLVER_BACKEND_BINARY_PATH, postgres_database_connection, kv_disk_directory=".", env_params=env) as (backend_url, process):
            sys.stderr.write("Solver backend started with pid={process.pid}\n")

            file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
            with open(file_path, "r") as f:
                task_value = json.load(f)
            fill_dummy_test_options(task_value)
            task_value["options"]["solver_time_limit_s"] = 0
            task_value["options"]["task_count"] = 2

            def slow_yt_start(path, request):
                return yt_handler_delay(path, request, start_delay=5)

            def task_yt_running(resp):
                return (resp.status_code == 201 or resp.status_code == 200) and "yt_running" in resp.json()["status"]

            with scoped_handler_cb(yt_proxy["yt_stub"], slow_yt_start):
                response = requests.post(backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
                assert response.ok
                task_id = response.json()['id']
                result_url = f"{backend_url}/result/{task_id}"
                assert wait_until(lambda: task_yt_running(requests.get(result_url)), check_interval=1, timeout=100)

            # check unistat
            response = requests.get(backend_url + "/unistat")
            assert response.ok
            assert response.headers['content-type'] == "application/json"
            assert get_unistat_value(response.json(), "stuck_tasks_axxx") == 1

            assert wait_until(lambda: task_is_completed(requests.get(result_url)), check_interval=1, timeout=100)
