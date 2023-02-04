import re
import requests

from maps.b2bgeo.mvrp_solver.backend.async_backend.tests_lib.cp_util import (
    post_cp_task, wait_cp_task, get_cp_task_result
)

from yatest.common import output_path


def test_on_yt(async_backend_small_task_count_increase_for_local_tasks_url):
    url = async_backend_small_task_count_increase_for_local_tasks_url
    j = post_cp_task(url, task_count=3)
    task_id = j['id']

    get_cp_task_result(url, task_id, expected_status_code=requests.codes.not_found)

    j = wait_cp_task(url, task_id)
    assert 'routes' in j

    backend_logs_path = output_path("stderr")
    with open(backend_logs_path, 'r') as backend_logs_f:
        match = re.search(
            rf'tid={task_id}> <pid=(\d+)> info: Worker: yt', backend_logs_f.read())
        assert match is not None
