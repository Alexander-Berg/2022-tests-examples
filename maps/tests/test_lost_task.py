import requests
import json
import os
import signal
import sys
from yatest.common import source_path
from yandex.maps.test_utils.common import wait_until
from maps.b2bgeo.mvrp_solver.backend.tests_lib.db_util import db_connection, load_attempt_count
from maps.b2bgeo.mvrp_solver.backend.tests_lib.conftest import SOLVER_BACKEND_BINARY_PATH, bring_up_backend
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY, TASK_DIRECTORY, fill_dummy_test_options
)


def _check_response_and_message(resp, code, message):
    sys.stderr.write('Status code {}\n'.format(resp.status_code))
    sys.stderr.write('Message: {}\n'.format(resp.json()['message']))
    sys.stderr.write('Status: {}\n'.format(resp.json()['status']))
    return resp.status_code == code and resp.json()["message"] == message


def _task_is_running(resp):
    return _check_response_and_message(resp, 201, "Task is running and available for polling")


def _task_is_completed(resp):
    return _check_response_and_message(resp, 200, "Task successfully completed")


def test_lost_task(postgres_database_connection):
    with bring_up_backend(
        SOLVER_BACKEND_BINARY_PATH, postgres_database_connection, kv_disk_directory=".", exit_code=-9
    ) as (backend_url, process):
        sys.stderr.write("Solver backend started with pid={}\n".format(process.pid))

        file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
        with open(file_path, "r") as f:
            task_value = json.load(f)
        fill_dummy_test_options(task_value)
        task_value["options"]["solver_time_limit_s"] = 10

        response = requests.post(backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
        assert response.ok
        task_id = response.json()['id']
        result_url = "{url}/result/{task_id}".format(url=backend_url, task_id=task_id)
        assert wait_until(lambda: _task_is_running(requests.get(result_url)), check_interval=1, timeout=20)

        process.send_signal(signal.SIGKILL)
        process.wait()

    with bring_up_backend(SOLVER_BACKEND_BINARY_PATH, postgres_database_connection, kv_disk_directory=".") as (backend_url, process):
        sys.stderr.write("Solver backend started with pid={}\n".format(process.pid))
        result_url = "{url}/result/{task_id}".format(url=backend_url, task_id=task_id)
        assert wait_until(lambda: _task_is_running(requests.get(result_url)), check_interval=1, timeout=20)
        assert wait_until(lambda: _task_is_completed(requests.get(result_url)), check_interval=1, timeout=100)

        response = requests.get(result_url)
        assert response.status_code == 200, response.text
        j = response.json()
        assert 'status' in j
        assert "completed" in j["status"]
        assert j["message"] == 'Task successfully completed', j["message"]

        # check unistat
        response = requests.get(backend_url + "/unistat")
        assert response.ok
        assert response.headers['content-type'] == "application/json"

        unistat = response.json()
        unistat = {x[0]: x[1] for x in unistat}
        assert unistat['type_svrp_lost_summ'] == 0
        assert unistat['type_mvrp_lost_summ'] == 0
        assert unistat['inactive_task_restarted_summ'] == 1

    with db_connection(postgres_database_connection) as conn:
        assert load_attempt_count(conn, task_id) == 2
