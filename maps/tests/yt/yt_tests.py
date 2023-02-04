import json
import pytest
import os
import re
import signal
import sys
import time
import requests
from yatest.common import source_path, build_path, output_path
import yt.wrapper
import yt.yson
from cyson import loads
from yandex.maps.test_utils.common import wait_until
from maps.b2bgeo.mvrp_solver.backend.tests_lib.db_util import db_connection, load_attempt_count
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    TASK_DIRECTORY,
    API_KEY,
    add_task_mvrp,
    fill_dummy_test_options,
    scoped_handler_cb,
    ProxyException
)
from maps.b2bgeo.mvrp_solver.backend.tests_lib.conftest import (
    SOLVER_BACKEND_BINARY_PATH,
    bring_up_backend,
    bring_up_slow_yt_stub
)
from maps.b2bgeo.mvrp_solver.backend.tests_lib.yt_common import (
    run_yt_task, yt_handler_delay, task_is_completed
)


MVRP_ANNEALING = build_path('maps/b2bgeo/mvrp_solver/annealing_mvrp/annealing-mvrp/annealing-mvrp')
os.environ['ANNEALING_BINARY_PATH'] = MVRP_ANNEALING

NUMBER_OF_CLUSTERS = 2  # hardcoded constant in async_backend config


def test_all_jobs_completed(async_backend_url):
    result = run_yt_task(async_backend_url, 1, task_count=3, save_as="task_default")
    assert len(result["response"]["yt_operations"]) == 2


def test_saving_stderr(async_backend_url):
    result = run_yt_task(async_backend_url, 1, task_count=3, save_as="task_default")
    assert len(result["response"]["yt_operations"]) == 2
    yt_proxy = os.environ.get('YT_PROXY')
    yt_client = yt.wrapper.YtClient(yt_proxy)
    assert len(list(yt_client.list("//home/b2bgeo/logs/solver_stderr"))) != 0


def test_complete_job_one_cluster_is_down(async_backend_dead_and_alive_yt_clusters):
    backend_url = async_backend_dead_and_alive_yt_clusters["url"]
    run_yt_task(backend_url, 1, task_count=3, check_all_jobs_completed=False,
                save_as="task_paraller_one_down")


def test_attempt_count_is_one_after_yt_task(postgres_database_connection):
    with bring_up_backend(SOLVER_BACKEND_BINARY_PATH, postgres_database_connection) as (backend_url, _):
        result = run_yt_task(backend_url, 1, task_count=3)
        task_id = result['response']['id']
    with db_connection(postgres_database_connection) as conn:
        assert load_attempt_count(conn, task_id) == 1


def test_computing_num_vehicle_classes(async_backend_url):
    run_yt_task(async_backend_url, 1, task_count=3)
    backend_logs_path = output_path("stderr")
    with open(backend_logs_path, 'r') as backend_logs_f:
        full_file = backend_logs_f.read()
        assert "#vehicle classes=1" in full_file
        assert "Model parameters validation failed with error:" not in full_file
        assert "Model parameters validation failed with unknown error." not in full_file


def test_yt_task_killed(postgres_database_connection):
    with bring_up_backend(SOLVER_BACKEND_BINARY_PATH, postgres_database_connection, kv_disk_directory=".") \
            as (backend_url, process):
        print("Solver backend started with pid={}".format(process.pid))

        file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
        with open(file_path, "r") as fin:
            task_value = json.load(fin)
        fill_dummy_test_options(task_value)
        task_value["options"]["task_count"] = 11
        task_value["options"]["solver_time_limit_s"] = 10

        response = requests.post(backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
        assert response.ok
        task_id = response.json()['id']
        response = requests.get(backend_url + "/result/" + task_id)
        assert response.ok

        def task_is_yt_running(resp):
            print('Status code {}'.format(resp.status_code))
            sys.stderr.write('Status code {}\n'.format(resp.status_code))
            sys.stderr.write('   message: {}\n'.format(resp.json()['message']))
            sys.stderr.write('   time passed: {}\n'.format(time.time() - start_time))
            return resp.status_code == 201 and resp.json()["message"] == "Task is running and available for polling"

        start_time = time.time()
        started = wait_until(lambda: task_is_yt_running(requests.get(backend_url + "/result/" + task_id)),
                             check_interval=1, timeout=100)
        assert started

    with bring_up_backend(SOLVER_BACKEND_BINARY_PATH, postgres_database_connection, kv_disk_directory=".") \
            as (backend_url, process):
        print("Solver backend started with pid={}".format(process.pid))

        def task_is_completed(resp):
            print('Status code {}'.format(resp.status_code))
            sys.stderr.write('Status code {}\n'.format(resp.status_code))
            sys.stderr.write('   message: {}\n'.format(resp.json()['message']))
            sys.stderr.write('   time passed: {}\n'.format(time.time() - start_time))
            return resp.status_code == 200

        start_time = time.time()
        completed = wait_until(lambda: task_is_completed(requests.get(backend_url + "/result/" + task_id)),
                               check_interval=1, timeout=450)
        assert completed

        response = requests.get(backend_url + "/result/" + task_id)
        assert response.status_code == 200, response.text
        j = response.json()

        sys.stderr.write(json.dumps(j, indent=4))

        assert j["id"] == task_id
        assert "status" in j
        assert "completed" in j["status"]
        assert j["message"] == 'Task successfully completed', j["message"]
        assert "result" in j
        assert "solver_status" in j["result"]
        assert j["result"]["solver_status"] == "SOLVED"
        assert "_tasks_summary" in j["result"]["metrics"]
        assert len(j["result"]["metrics"]["_tasks_summary"])

    with db_connection(postgres_database_connection) as conn:
        assert load_attempt_count(conn, task_id) == 1


def test_yt_task_multithreaded(async_backend_url):
    result = run_yt_task(async_backend_url, 1, {"thread_count": 2}, task_count=3)
    assert result["response"]["result"]["options"]["thread_count"] == 2

    yt_client = yt.wrapper.YtClient(result["response"]["yt_operations"][0]["cluster"], os.environ.get("YT_AUTH_TOKEN"))
    yt_operation_id = result["response"]["yt_operations"][0]["id"]
    attrs = yt_client.get_operation_attributes(yt_operation_id, fields=["spec"])
    print(yt.yson.dumps(attrs, indent=4), file=sys.stderr, flush=True)
    assert int(attrs["spec"]["mapper"]["cpu_limit"]) == 2


def test_yt_task_creation_timeouted(async_backend_short_timeout_yt_stucked):
    backend_url = async_backend_short_timeout_yt_stucked['url']
    yt_stub = async_backend_short_timeout_yt_stucked['yt_stub']
    create_yt_task_timeout_s = async_backend_short_timeout_yt_stucked['timeout_s']

    def slow_yt_handler(path, request):
        network_delay_s = min(16, create_yt_task_timeout_s * 1.5)
        time.sleep(network_delay_s)

    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), "r") as fin:
        task_value = json.load(fin)
    fill_dummy_test_options(task_value)
    task_value["options"]["solver_time_limit_s"] = 300
    task_value["options"]["thread_count"] = 2

    with scoped_handler_cb(yt_stub, slow_yt_handler):
        result = add_task_mvrp(backend_url, API_KEY, task_value, timeout=100)

    time_elapsed = result['status']['internal_error'] - result['status']['matrix_downloaded']
    sys.stderr.write("result {}".format(result))
    assert time_elapsed < NUMBER_OF_CLUSTERS * (create_yt_task_timeout_s + 3)

    backend_logs_path = output_path("stderr")
    with open(backend_logs_path, 'r') as backend_logs_f:
        assert 'retry timeout exceeded (timeout:' in backend_logs_f.read()


def test_cannot_get_result_from_one_cluster(async_backend_dead_and_alive_yt_clusters):
    backend_url = async_backend_dead_and_alive_yt_clusters['url']
    yt_stub = async_backend_dead_and_alive_yt_clusters['yt_stub']

    def forbidden_result(path, request):
        method = request.method
        data = request.get_data()
        if path != "api/v3/lock" or method != "POST":
            return
        yson = loads(data)
        if "path".encode('ascii') not in yson:
            return
        lock_file = yson["path".encode('ascii')].decode('ascii')
        if lock_file.endswith(".out"):
            raise ProxyException(message="Access denied", status_code=403)

    with scoped_handler_cb(yt_stub, forbidden_result):
        result = run_yt_task(backend_url, 1, task_count=2, check_all_jobs_completed=False)

    assert len(result["response"]["yt_operations"]) == 2
    assert "result" in result["response"]
    assert "metrics" in result["response"]["result"]
    assert "_tasks_summary" in result["response"]["result"]["metrics"]
    assert len(result["response"]["result"]["metrics"]["_tasks_summary"]) == 1


def test_slow_get_result_from_one_cluster(async_backend_dead_and_alive_yt_clusters):
    backend_url = async_backend_dead_and_alive_yt_clusters['url']
    yt_stub = async_backend_dead_and_alive_yt_clusters['yt_stub']

    def slow_result(path, request):
        return yt_handler_delay(path, request, result_delay=100)

    with scoped_handler_cb(yt_stub, slow_result):
        result = run_yt_task(backend_url, 1, task_count=2, check_all_jobs_completed=False)

    assert len(result["response"]["yt_operations"]) == 2
    assert "result" in result["response"]
    assert "metrics" in result["response"]["result"]
    assert "_tasks_summary" in result["response"]["result"]["metrics"]
    assert len(result["response"]["result"]["metrics"]["_tasks_summary"]) == 1


def test_softtimelimit(async_backend_dead_and_alive_yt_clusters_long_timeout):
    backend_url = async_backend_dead_and_alive_yt_clusters_long_timeout['url']
    yt_stub = async_backend_dead_and_alive_yt_clusters_long_timeout['yt_stub']

    def slow_yt_start(path, request):
        return yt_handler_delay(path, request, start_delay=30, result_delay=100)

    solver_time_limit_s = 1
    with scoped_handler_cb(yt_stub, slow_yt_start):
        result = run_yt_task(backend_url, solver_time_limit_s, task_count=2, check_all_jobs_completed=False)
    assert len(result["response"]["yt_operations"]) == 2
    assert "result" in result["response"]
    assert "metrics" in result["response"]["result"]
    assert "_tasks_summary" in result["response"]["result"]["metrics"]
    assert len(result["response"]["result"]["metrics"]["_tasks_summary"]) == 1


def test_cannot_abort_operation_from_one_cluster(async_backend_dead_and_alive_yt_clusters):
    backend_url = async_backend_dead_and_alive_yt_clusters['url']
    yt_stub = async_backend_dead_and_alive_yt_clusters['yt_stub']

    def abort_unresponsive(path, request):
        method = request.method
        data = request.get_data()
        if path == "api/v3/abort_tx" and method == "POST":
            raise ProxyException(message="Service unavailable", status_code=503)

        if path != "api/v3/lock" or method != "POST":
            return
        yson = loads(data)
        if "path".encode('ascii') not in yson:
            return
        lock_file = yson["path".encode('ascii')].decode('ascii')
        if lock_file.endswith(".out"):
            raise ProxyException(message="Access denied", status_code=403)

    with scoped_handler_cb(yt_stub, abort_unresponsive):
        result = run_yt_task(backend_url, 1, task_count=2, check_all_jobs_completed=False)

    assert len(result["response"]["yt_operations"]) == 2
    assert "result" in result["response"]
    assert "metrics" in result["response"]["result"]
    assert "_tasks_summary" in result["response"]["result"]["metrics"]
    assert len(result["response"]["result"]["metrics"]["_tasks_summary"]) == 1


def test_get_stderr(async_backend_url):
    response = run_yt_task(async_backend_url, 1, task_count=3)
    task_id = response['response']['id']
    response = requests.get(async_backend_url + "/log/stderr/" + task_id)
    assert response.ok
    r = response.json()
    print("\n\n", response.json(), "\n\n", file=sys.stderr, flush=True)
    assert len(r) > 0
    assert "localhost" in r[0]["cluster"]


def _task_has_1_yt_operation(resp):
    sys.stderr.write('Status code {}\n'.format(resp.status_code))
    j = resp.json()
    sys.stderr.write(f'Message: {j["message"]}\n')
    sys.stderr.write(f'Status: {j["status"]}\n')
    sys.stderr.write(f'yt_operations: {j["yt_operations"]}\n')
    return resp.status_code == 201 and len(j["yt_operations"]) == 1


@pytest.mark.parametrize('graceful', [True, False])
def test_shutdown_yt_task_after_1_yt_operation_created(postgres_database_connection, graceful):
    with bring_up_slow_yt_stub() as yt_proxy:
        timeout = 150
        clusters = ', '.join([yt_proxy['proxy_address'], os.environ['YT_PROXY']])
        env = {"YT_CALL_TIMEOUT_S": f"createTask:{timeout}", "YC_YT_CLUSTERS": clusters}
        print(f"yt clusters: {os.environ['YT_PROXY']}, slow: {yt_proxy['proxy_address']}", file=sys.stderr, flush=True)

        # 1. Start task, wait for 1 yt operation
        exit_code = 0 if graceful else -9
        with bring_up_backend(
            SOLVER_BACKEND_BINARY_PATH,
            postgres_database_connection,
            kv_disk_directory=".",
            env_params=env,
            exit_code=exit_code,
        ) as (backend_url, process):
            sys.stderr.write("Solver backend started with pid={process.pid}\n")
            file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
            with open(file_path, "r") as f:
                task_value = json.load(f)
            fill_dummy_test_options(task_value)
            task_value["options"]["solver_time_limit_s"] = 20
            task_value["options"]["task_count"] = 2

            def slow_yt_start(path, request):
                return yt_handler_delay(path, request, start_delay=timeout)

            with scoped_handler_cb(yt_proxy["yt_stub"], slow_yt_start):
                response = requests.post(backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
                assert response.ok
                task_id = response.json()['id']
                result_url = f"{backend_url}/result/{task_id}"
                assert wait_until(lambda: _task_has_1_yt_operation(requests.get(result_url)), check_interval=1, timeout=20)

            if not graceful:
                process.send_signal(signal.SIGKILL)
                process.wait()

        # 2. backend is down - task is postponed/lost

        # 3. restart task on another backend
        with bring_up_backend(SOLVER_BACKEND_BINARY_PATH, postgres_database_connection, kv_disk_directory=".", env_params=env) as (backend_url, process):
            sys.stderr.write("Solver backend started with pid={process.pid)}\n")
            result_url = "{url}/result/{task_id}".format(url=backend_url, task_id=task_id)
            # assert wait_until(lambda: _task_has_1_yt_operation(requests.get(result_url)), check_interval=1, timeout=20)
            assert wait_until(lambda: task_is_completed(requests.get(result_url)), check_interval=1, timeout=100)

            response = requests.get(result_url)
            assert response.status_code == 200, response.text
            j = response.json()
            assert 'status' in j
            assert "completed" in j["status"]
            assert j["message"] == 'Task successfully completed', j["message"]
            # "1 yt operation before restart" + "2 new yt operation after restart" = 3
            assert len(j["yt_operations"]) == 3

            # check unistat to confirm that task was lost
            response = requests.get(backend_url + "/unistat")
            assert response.ok
            assert response.headers['content-type'] == "application/json"

            unistat = response.json()
            unistat = {x[0]: x[1] for x in unistat}
            assert unistat['type_svrp_lost_summ'] == 0
            assert unistat['type_mvrp_lost_summ'] == 0 if graceful else 1
            assert unistat['inactive_task_restarted_summ'] == 0 if graceful else 1

    with db_connection(postgres_database_connection) as conn:
        assert load_attempt_count(conn, task_id) == 1 if graceful else 2


def test_default_yt_call_timeout(async_backend_default_timeouts):
    backend_url = async_backend_default_timeouts['url']
    yt_stub = async_backend_default_timeouts['yt_stub']

    def fail_get_operation_call(path, request):
        if path == "api/v3/get_operation":
            time.sleep(2)
            raise ProxyException()

    with scoped_handler_cb(yt_stub, fail_get_operation_call):
        result = run_yt_task(backend_url, 1, task_count=2, check_all_jobs_completed=False)

    backend_logs_path = output_path("stderr")
    with open(backend_logs_path, 'r') as backend_logs_f:
        assert re.search(r'/api/v3/get_operation .{,70}retry timeout exceeded \(timeout:', backend_logs_f.read())
    with open(backend_logs_path, 'r') as backend_logs_f:
        used_timeout = re.search(r'/api/v3/get_operation .{,70}retry timeout exceeded \(timeout: (\d+\.\d+)', backend_logs_f.read()).group(1)
    assert 9 < float(used_timeout) < 11

    assert len(result["response"]["yt_operations"]) == 2
    assert "result" in result["response"]
    assert "metrics" in result["response"]["result"]
    assert "_tasks_summary" in result["response"]["result"]["metrics"]
    assert len(result["response"]["result"]["metrics"]["_tasks_summary"]) == 1


def test_custom_yt_call_timeout(async_backend_small_collect_result_timeout):
    backend_url = async_backend_small_collect_result_timeout['url']
    yt_stub = async_backend_small_collect_result_timeout['yt_stub']

    def slow_call(path, request):
        time.sleep(1)

    with scoped_handler_cb(yt_stub, slow_call):
        result = run_yt_task(backend_url, 1, task_count=2, check_all_jobs_completed=False)

    backend_logs_path = output_path("stderr")
    with open(backend_logs_path, 'r') as backend_logs_f:
        assert "retry timeout exceeded (timeout:" in backend_logs_f.read()
    with open(backend_logs_path, 'r') as backend_logs_f:
        collect_results_timeout = re.findall(r'collectResults:.{,100} duration: (\d+\.\d+)', backend_logs_f.read())
    flag = False
    for duration in collect_results_timeout:
        if float(duration) > 2:
            flag = True
    assert flag, "collectResults was executed < 2s, but had to exceed 2s timeout"

    assert len(result["response"]["yt_operations"]) == 2
    assert "result" in result["response"]
    assert "metrics" in result["response"]["result"]
    assert "_tasks_summary" in result["response"]["result"]["metrics"]
    assert len(result["response"]["result"]["metrics"]["_tasks_summary"]) == 1


def test_local_task_limit_exceeded(async_backend_small_task_count_increase_for_local_tasks_url):
    url = async_backend_small_task_count_increase_for_local_tasks_url
    result = run_yt_task(url, task_count=3, solver_time_limit_s=0.1)
    assert result["response"]["yt_operations"] != []


def test_task_abort(async_backend_custom_handlers):
    backend_url = async_backend_custom_handlers['url']
    yt_stubs = async_backend_custom_handlers['yt_stubs']

    def fail_get_result(path, request):
        method = request.method
        data = request.get_data()
        if path != "api/v3/lock" or method != "POST":
            return
        yson = loads(data)
        if "path".encode('ascii') not in yson:
            return
        lock_file = yson["path".encode('ascii')].decode('ascii')
        if lock_file.endswith(".out"):
            raise ProxyException(message="Access denied", status_code=403)

    def fail_get_state_initially(path, request):
        if path == "api/v3/get_operation":
            if time.time() - fail_get_state_initially.start < 130:  # softlimit is about 121s
                raise ProxyException(message="Access denied", status_code=403)

    fail_get_state_initially.start = time.time()

    with scoped_handler_cb(yt_stubs[0]['yt_stub'], fail_get_result):
        with scoped_handler_cb(yt_stubs[1]['yt_stub'], fail_get_state_initially):
            result = run_yt_task(backend_url, 1, task_count=2, check_all_jobs_completed=False)

    assert len(result["response"]["yt_operations"]) == 2
    assert "result" in result["response"]
    assert "metrics" in result["response"]["result"]
    assert "_tasks_summary" in result["response"]["result"]["metrics"]
    assert len(result["response"]["result"]["metrics"]["_tasks_summary"]) == 1

    backend_logs_path = output_path("stderr")
    with open(backend_logs_path, 'r') as backend_logs_f:
        assert 'Can not get result from completed YT operations, but there are some running ones still.' in backend_logs_f.read()


def test_yt_task_internal_error_on_hard_limit(async_backend_custom_handlers):
    backend_url = async_backend_custom_handlers['url']
    yt_stubs = async_backend_custom_handlers['yt_stubs']

    def fail_get_state(path, request):
        if path == "api/v3/get_operation":
            raise ProxyException(message="Access denied", status_code=403)

    with scoped_handler_cb(yt_stubs[0]['yt_stub'], fail_get_state):
        with scoped_handler_cb(yt_stubs[1]['yt_stub'], fail_get_state):
            result = run_yt_task(backend_url, 1, task_count=2, check_success=False)

    assert result['response']['message'] == 'Error executing task.'


def test_zone_detection_works_on_yt(async_backend_url_task_sizes):
    result = run_yt_task(
        async_backend_url_task_sizes, 1, add_zones=True,
        override_test_options={"quality": "low", "thread_count": 2})
    assert len(result["response"]["yt_operations"]) == 2
    assert len(result["response"]["result"]["routes"]) == 2


def test_duplicate_zones(async_backend_url_task_sizes):
    result = run_yt_task(
        async_backend_url_task_sizes, 1, add_zones=True,
        override_test_options={"quality": "low", "thread_count": 2}, duplicate_zones=True)
    assert len(result["response"]["yt_operations"]) == 2
