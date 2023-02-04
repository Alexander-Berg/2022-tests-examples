import unittest
from parameterized import parameterized

from .common import (
    API_ENDPOINT,
    SOLVER_TEST_APIKEY,
    CLOUD_TYPE,
    generate_task,
    make_request,
    wait_task
)

RTC_CLOUD_PARAMS = {
    'timeouts': (100, 200),
}
AWS_CLOUD_PARAMS = {
    'timeouts': (300, 600),
}
TEST_PARAMS = RTC_CLOUD_PARAMS if CLOUD_TYPE == 'rtc' else AWS_CLOUD_PARAMS


class CoreTest(unittest.TestCase):
    @parameterized.expand([(11, 20), TEST_PARAMS['timeouts']])
    def test_handler_core_mt(self, location_count, timeout):
        task_value = generate_task(location_count=location_count, solver_time_limit_s=0.1)
        task_value["vehicles"][0]["routing_mode"] = "walking"
        task_value["vehicles"][1]["routing_mode"] = "transit"
        task_value["vehicles"][2]["routing_mode"] = "bicycle"
        task_value["options"]["matrix_router"] = "main"

        response = make_request("post", f"{API_ENDPOINT}/add/mvrp", json=task_value, params={"apikey": SOLVER_TEST_APIKEY})
        assert response.ok

        task_id = response.json()['id']
        response = make_request("get", f"{API_ENDPOINT}/result/{task_id}")
        assert response.ok

        j = wait_task(task_id, timeout)

        assert j["id"] == task_id
        assert j["message"] == "Task successfully completed"
