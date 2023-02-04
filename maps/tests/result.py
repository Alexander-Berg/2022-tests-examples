import requests
import json
import os
import http.client
from collections import namedtuple
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    wait_task,
    API_KEY,
    TASK_DIRECTORY,
    check_unistat,
    fill_dummy_test_options
)
from maps.b2bgeo.mvrp_solver.backend.tests_lib.conftest import SOLVER_BACKEND_BINARY_PATH, bring_up_backend
from sqlalchemy.orm import sessionmaker
from sqlalchemy import create_engine


def test_non_existing_task(async_backend_url):
    response = requests.get(async_backend_url + "/result/not_existing_task")
    assert response.status_code == 410
    assert response.json() == {'error': {'message': 'No task with this ID (task_id) was found.'}}


def test_existing_task(async_backend_url_clear):
    backend_url = async_backend_url_clear
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)
    response = requests.post(backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok
    task_id = response.json()['id']
    response = requests.get(backend_url + "/result/" + task_id)
    assert response.ok

    j = wait_task(backend_url, task_id)
    assert j["id"] == task_id
    assert "status" in j
    assert "calculated" in j["status"]
    assert j["message"] == 'Task successfully completed', j["message"]
    assert "result" in j
    assert j["result"]["metrics"]["total_probable_penalty"] == 0
    assert not j["result"]["options"]["minimize_lateness_risk"]

    assert "matrix_statistics" in j
    matrix_statistics = j["matrix_statistics"]
    assert len(matrix_statistics) == 1
    ms = matrix_statistics['driving']
    assert ms["total_distances"] > 0
    assert ms["requested_router"] == "geodesic"
    assert ms["used_router"] == "geodesic"
    assert ms["geodesic_distances"] == ms["total_distances"]

    check_unistat(backend_url, {"haversine_matrix_requests_summ": 1})


def log_request_response_mvrp(backend_url, task_type, task_file):
    file_path = source_path(os.path.join(TASK_DIRECTORY, task_file))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)
    response = requests.post(backend_url + "/add/{}?apikey={}".format(task_type, API_KEY), json.dumps(task_value))
    assert response.ok
    task_id = response.json()['id']
    response = requests.get(backend_url + "/result/" + task_id)
    assert response.ok
    j = wait_task(backend_url, task_id)

    response = requests.get(backend_url + "/log/request/{}".format(task_id))
    assert response.ok
    assert "options" in response.json()
    assert response.json() == task_value

    response = requests.get(backend_url + "/log/response/{}".format(task_id))
    assert response.ok
    assert j == response.json()

    response = requests.get(backend_url + "/log/stderr/{}".format(task_id))
    assert response.ok


def test_non_existing_stderr(async_backend_url):
    response = requests.get(async_backend_url + "/log/stderr/xxx")
    assert response.status_code == http.client.GONE


def test_log_request_response_mvrp(async_backend_url):
    log_request_response_mvrp(async_backend_url, "mvrp", "10_locs.json")


def test_log_request_response_svrp(async_backend_url):
    log_request_response_mvrp(async_backend_url, "svrp", "svrp_request.json")


def test_requeued_task(postgres_database_connection):
    with bring_up_backend(SOLVER_BACKEND_BINARY_PATH, postgres_database_connection) as (backend_url, process):
        print("Solver backend started with pid={}".format(process.pid))

        file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
        with open(file_path, "r") as f:
            task_value = json.load(f)
        fill_dummy_test_options(task_value)
        response = requests.post(backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
        assert response.ok
        task_id = response.json()['id']
        response = requests.get(backend_url + "/result/" + task_id)
        assert response.ok

        j = wait_task(backend_url, task_id)
        assert j["id"] == task_id
        assert "status" in j
        assert "calculated" in j["status"]
        assert 'yt_ready' not in j['status']
        queued_old_ts = j['status']['queued']

        connection_string = 'postgresql+psycopg2://{user}:{password}@{host}:{port}/{database}'\
                            .format(**postgres_database_connection)
        pg_engine = create_engine(connection_string)
        session_factory = sessionmaker(bind=pg_engine)
        session = session_factory()

        task_id_internal = query(session, "SELECT DISTINCT id FROM tasks.tasks WHERE uuid = '{}'".format(task_id))[0].id
        logs = query(session, "SELECT * FROM tasks.task_statuses WHERE task_id = {} ORDER BY timestamp LIMIT 1"
                              .format(task_id_internal))
        query(session,
              """
              INSERT INTO tasks.task_statuses (task_id, status, timestamp, host_id)
               VALUES ({task_id}, '{status}', now(), {host_id});
              """.format(task_id=task_id_internal, status="yt_ready", host_id=logs[0].host_id))
        session.commit()
        response = requests.get(backend_url + "/result/" + task_id)
        assert response.ok
        j = response.json()
        assert 'yt_ready' in j['status']
        assert 'queued' in j['status']
        assert 'calculated' not in j['status']

        query(session,
              """
              INSERT INTO tasks.task_statuses (task_id, status, timestamp, host_id)
               VALUES ({task_id}, '{status}', now(), {host_id});
              """.format(task_id=task_id_internal, status="queued", host_id=logs[0].host_id))
        session.commit()
        response = requests.get(backend_url + "/result/" + task_id)
        assert response.ok
        j = response.json()
        assert 'queued' in j['status']
        assert 1 == len(j['status'])
        assert j['status']['queued'] > queued_old_ts


def query(session, *args, **kwargs):
    exec_result = session.execute(*args, **kwargs)
    if exec_result.returns_rows:
        Record = namedtuple('Record', exec_result.keys())
        return [Record(*r) for r in exec_result.fetchall()]
