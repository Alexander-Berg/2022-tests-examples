import os
import unittest
import logging
import json
from .common import (
    requests_get_with_retry,
    requests_post_with_retry,
    start_task,
    get_task_result,
    API_ENDPOINT,
    VRP_SOLVER_URL,
    SOLVER_TEST_APIKEY,
    ADD_TASK_QUERY_PATTERN)


MOSCOW_LOCATIONS = [
    {"lat": 55.756567, "lon": 37.602243},
    {"lat": 55.754776, "lon": 37.611298},
    {"lat": 55.756754, "lon": 37.605369},
]


def prepare_vehicle(task_type):
    vehicle = {
        "id": 0,
        "capacity": {
            "limits": {
                "volume_perc": 90,
                "weight_perc": 100
            },
            "volume": {
                "width_m": 5,
                "depth_m": 5,
                "height_m": 5
            },
            "weight_kg": 100
        },
        "ref": "Vehicle_1",
    }
    if task_type == "svrp":
        return {"vehicle": vehicle}
    return {"vehicles": [vehicle]}


def prepare_task(task_type, routing_mode, task_count):
    time_window = "03:00:00-23:59:59"
    result = {
        "depot": {
            "id": 0,
            "point": MOSCOW_LOCATIONS[0],
            "time_window": time_window,
            "service_duration_s": 0
        },
        "locations": [
            {
                "id": 1,
                "point": MOSCOW_LOCATIONS[1],
                "time_window": time_window,
                "service_duration_s": 0,
                "hard_window": False,
                "shipment_size": {
                    "volume": {
                        "width_m": 0,
                        "depth_m": 0,
                        "height_m": 0
                    },
                    "weight_kg": 1
                }
            }
        ],
        "options": {
            "date": "2018-07-13",
            "time_zone": 3,
            "minimize": "combined",
            "default_speed_km_h": 20,
            "solver_time_limit_s": 1,
            "routing_mode": routing_mode,
            "task_count": task_count,
            "thread_count": 1
        },
    }
    result.update(prepare_vehicle(task_type))
    return result


def check_task(task_type, routing_mode, task_count):
    logging.info("Task type: {}, routing mode: {}, task count: {}".format(
        task_type, routing_mode, task_count))
    task = prepare_task(task_type, routing_mode, task_count)
    add_task_query = ADD_TASK_QUERY_PATTERN.format(task_type)

    task_id = start_task(API_ENDPOINT + add_task_query, task)
    result = get_task_result(API_ENDPOINT + "/result/" + task_id, task_id)
    if "result" not in result:
        logging.error("Error:" + json.dumps(result, indent=4))
    assert result["result"]["solver_status"] == "SOLVED"
    resp = requests_get_with_retry(API_ENDPOINT + "/log/request/" + task_id)
    assert resp.ok, resp.text
    resp = requests_get_with_retry(API_ENDPOINT + "/log/response/" + task_id)
    assert resp.ok, resp.text


def sync_solve(task):
    headers = {'Content-type': 'application/json'}
    params = {'origin': 'ya_courier', 'apikey': SOLVER_TEST_APIKEY}
    result = requests_post_with_retry(VRP_SOLVER_URL + "/solve", json=task, headers=headers, params=params)
    assert result.ok, result.text
    return result.json()


class SmokeTest(unittest.TestCase):
    def test_apikeys(self):
        token = os.environ.get("SOLVER_AUTH_TOKEN")
        assert token
        logging.info("Check stat/apikeys_day")
        headers = {}
        headers["Authorization"] = "OAuth {}".format(token)
        resp = requests_get_with_retry(
            f"{API_ENDPOINT}/stat/apikeys",
            params={"time_interval": "2020-10-01T01:00:00Z/2020-10-01T02:30:00Z"},
            headers=headers)
        assert resp.ok, resp.text

    def test_svrp(self):
        check_task("svrp", "driving", 1)

    def test_mvrp(self):
        check_task("mvrp", "driving", 1)

    def test_mvrp_yt(self):
        check_task("mvrp", "driving", 2)

    def test_mvrp_truck(self):
        check_task("mvrp", "truck", 1)

    def test_mvrp_walking(self):
        check_task("mvrp", "walking", 1)

    def test_mvrp_transit(self):
        check_task("mvrp", "transit", 1)

    def test_mvrp_truck_yt(self):
        check_task("mvrp", "truck", 2)

    def test_mvrp_walking_yt(self):
        check_task("mvrp", "walking", 2)

    def test_mvrp_transit_yt(self):
        check_task("mvrp", "transit", 2)

    def test_sync_backend(self):
        j = sync_solve(prepare_task("svrp", "driving", 1))
        if "result" not in j:
            logging.error("Error:" + json.dumps(j, indent=4))
        assert j["result"]["solver_status"] == "SOLVED"

    def test_haversine_fallback(self):
        task = prepare_task("svrp", "driving", 1)
        unreachable_location = {"lat": 61.698653, "lon": 99.505405}
        task["locations"] = [
            {
                "id": counter + 1,
                "point": location,
                "time_window": "03:00:00-23:59:59",
                "service_duration_s": 0,
                "hard_window": False,
                "shipment_size": {
                    "volume": {
                        "width_m": 0,
                        "depth_m": 0,
                        "height_m": 0
                    },
                    "weight_kg": 1
                }
            }
            for counter, location in enumerate(MOSCOW_LOCATIONS[1:] + [unreachable_location])
        ]

        j = sync_solve(task)
        if "result" not in j:
            logging.error("Error:" + json.dumps(j, indent=4))
            assert False

        self.assertEqual(j["result"]["solver_status"], "PARTIAL_SOLVED")

        self.assertEqual(len(j["result"]["dropped_locations"]), 1)
        self.assertEqual(j["result"]["dropped_locations"][0]['id'], len(task["locations"]))

        self.assertEqual(len(j["matrix_statistics"]), 1)
        self.assertEqual(j["matrix_statistics"]['driving']["requested_router"], "auto")
        self.assertEqual(j["matrix_statistics"]['driving']["used_router"], "main")
        location_count = len(task["locations"]) + 1
        timestamp_count = 13
        self.assertEqual(j["matrix_statistics"]['driving']["total_distances"], location_count * location_count * timestamp_count)
        self.assertEqual(j["matrix_statistics"]['driving']["geodesic_distances"], (2 * location_count - 1) * timestamp_count)
