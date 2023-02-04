import requests
import json
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY, post_task_and_check_error_message, get_task_value, wait_task
)


def test_time_windows_both_fields(async_backend_url):
    task_value = get_task_value('10_locs.json')

    task_value["locations"][1]["time_windows"] = [
        {
            "time_window": "2018-09-01T07:00:00+03/2018-09-01T10:00:00+03"
        }
    ]

    post_task_and_check_error_message(async_backend_url, task_value, "delivery id 1: fields `time_window` and `time_windows` are mutually exclusive, only one of them should be specified.")


def test_time_windows_invalid_work_time(async_backend_url):
    task_value = get_task_value('10_locs.json')

    task_value["vehicles"][0]["rest_schedule"] = {}
    task_value["vehicles"][0]["rest_schedule"]["breaks"] = [
        {
            "rest_duration_s": 5400,
            "work_time_range_till_rest": "6:00"
        }
    ]

    post_task_and_check_error_message(async_backend_url, task_value, "Invalid work duration range format: 6:00.")


def test_time_windows_overlap(async_backend_url):
    task_value = get_task_value('10_locs.json')

    del task_value["locations"][1]["time_window"]
    task_value["locations"][1]["time_windows"] = [
        {
            "time_window": "18:00-20:00"
        },
        {
            "time_window": "2018-09-01T07:00:00+03/2018-09-01T10:00:00+03"
        },
        {
            "time_window": "2018-09-01T08:00:00+03/2018-09-01T09:00:00+03"
        }
    ]

    post_task_and_check_error_message(
        async_backend_url, task_value,
        "delivery id 1: time windows 2018-09-01T07:00:00+03/2018-09-01T10:00:00+03 and 2018-09-01T08:00:00+03/2018-09-01T09:00:00+03 overlap.")


def test_depot_time_window_with_throughput(async_backend_url):
    task_value = get_task_value('10_locs.json')

    task_value["depot"]["time_window"] = "2018-09-01T09:00:00.000+03:00/2018-09-01T18:00:00.000+03:00"
    task_value["depot"]["throughput"] = {
        "vehicle_count": [
            {
                "value": 2,
                "time_window": "09:00-18:00"
            }
        ]
    }
    resp = requests.post(async_backend_url + '/add/mvrp?apikey={}'.format(API_KEY), json.dumps(task_value))
    assert resp.ok, resp.text


def test_time_windows_ok(async_backend_url):
    test_cases = [
        {
            "time_windows": ["18:00-20:00", "2020-05-06T07:00:00+03/2020-05-06T10:00:00+03"],
            "expected_time_windows": ["18:00:00-20:00:00", "613.07:00:00-613.10:00:00"],
            "expected_used_time_window": "18:00:00-20:00:00"
        },
        {
            "time_windows": ["2020-05-06T07:00:00+03/2020-05-06T10:00:00+03", "15:00-20:00", "05:00-06:00"],
            "expected_time_windows": ["05:00:00-06:00:00", "15:00:00-20:00:00", "613.07:00:00-613.10:00:00"],
            "expected_used_time_window": "15:00:00-20:00:00"
        },
        {
            "time_windows": ["23:00-23:20", "05:00-06:00", "2020-05-06T07:00:00+03/2020-05-06T10:00:00+03", "17:00-20:00"],
            "expected_time_windows": ["05:00:00-06:00:00", "17:00:00-20:00:00", "23:00:00-23:20:00", "613.07:00:00-613.10:00:00"],
            "expected_used_time_window": "17:00:00-20:00:00"
        }
    ]

    for data in test_cases:
        task_value = get_task_value('10_locs.json')

        # 6 seconds instead of 1 is to decrease chance of dropping locations when sanitizers are used
        task_value["options"]["solver_time_limit_s"] = 6

        del task_value["locations"][0]["time_window"]
        task_value["locations"][0]["time_windows"] = [{"time_window": x} for x in data["time_windows"]]

        resp = requests.post(async_backend_url + '/add/mvrp?apikey={}'.format(API_KEY), json.dumps(task_value))
        assert resp.ok, resp.text

        j = wait_task(async_backend_url, resp.json()['id'])
        assert j["message"] == 'Task successfully completed', j["message"]

        for route in j["result"]["routes"]:
            for node in route["route"]:
                assert "used_time_window" in node["node"]
                if "time_windows" in node["node"]["value"]:
                    assert "time_windows" in task_value["locations"][node["node"]["value"]["id"]]
                    assert "time_window" not in task_value["locations"][node["node"]["value"]["id"]]
                    if node["node"]["value"]["id"] == 0:
                        assert node["node"]["used_time_window"] == data["expected_used_time_window"]
                        assert node["node"]["value"]["time_windows"] == [{"time_window": x} for x in data["expected_time_windows"]]
                else:
                    assert "time_windows" not in task_value["locations"][node["node"]["value"]["id"]]
                    assert "time_window" in task_value["locations"][node["node"]["value"]["id"]]
                    assert node["node"]["used_time_window"] == node["node"]["value"]["time_window"]
