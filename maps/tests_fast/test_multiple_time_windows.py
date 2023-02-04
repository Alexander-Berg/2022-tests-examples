import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker

import copy
import json

depot = {
    "id": "depot_id",
    "point": {
        "lon": 37.729377,
        "lat": 55.799087
    },
    "time_window": "07:00-23:59"
}

data = {
    "depot": depot,
    "options": {
        "time_zone": 3,
        "solver_time_limit_s": 0
    },
    "locations": [
        {
            "id": "loc1",
            "point": {
                "lon": 37.722388,
                "lat": 55.780846
            },
            "time_windows": [
                {
                    "time_window": "13:31-13:31"
                }
            ],
            "hard_window": True
        },
        {
            "id": "loc2",
            "point": {
                "lon": 37.732365,
                "lat": 55.780824
            },
            "time_windows": [
                {
                    "time_window": "11:00-12:00"
                },
                {
                    "time_window": "15:00-16:00"
                }
            ]
        },
        {
            "id": "loc3",
            "point": {
                "lon": 37.732390,
                "lat": 55.780875
            },
            "time_windows": [
                {
                    "time_window": "12:00-13:00"
                },
                {
                    "time_window": "17:00-18:00"
                }
            ]
        }
    ],
    "vehicles": [
        {
            "id": 0,
            "planned_route": {
                "locations": [
                    {
                        "id": "loc1"
                    },
                    {
                        "id": "loc2",
                        "wait_if_early": False
                    },
                    {
                        "id": "loc3",
                        "wait_if_early": False
                    }
                ]
            }
        }
    ]
}


def test_multiple_time_windows():
    """
    checks for used_time_window for task with penalties
    """
    result = mvrp_checker.solve_and_check(json.dumps(data))

    assert len(result["routes"]) == 1

    route = result["routes"][0]["route"]
    assert len(route) == 5
    assert route[1]["node"]["used_time_window"] == "13:31:00-13:31:00"
    assert route[2]["node"]["used_time_window"] == "15:00:00-16:00:00"
    assert route[3]["node"]["used_time_window"] == "12:00:00-13:00:00"


data_with_work_breaks = {
    "depot": depot,
    "options": {
        "time_zone": 3,
        "solver_time_limit_s": 0
    },
    "locations": [
        {
            "id": "loc1",
            "point": {
                "lon": 37.722388,
                "lat": 55.780846
            },
            "time_windows": [
                {
                    "time_window": "13:30-13:31"
                }
            ],
            "hard_window": True
        },
        {
            "id": "loc2",
            "point": {
                "lon": 37.732365,
                "lat": 55.780824
            },
            "time_windows": [
                {
                    "time_window": "13:30-14:00"
                },
                {
                    "time_window": "14:00-14:30"
                }
            ]
        },
        {
            "id": "loc3",
            "point": {
                "lon": 37.732390,
                "lat": 55.780875
            },
            "time_windows": [
                {
                    "time_window": "12:00-13:00"
                },
                {
                    "time_window": "17:00-18:00"
                }
            ]
        }
    ],
    "vehicles": [
        {
            "id": 0,
            "planned_route": {
                "locations": [
                    {
                        "id": "loc1",
                    },
                    {
                        "id": "loc2",
                        "wait_if_early": False,
                    },
                    {
                        "id": "loc3",
                        "wait_if_early": False,
                    }
                ],
                "work_breaks": [
                    {
                        "work_duration_s": 6 * 60 * 60 + 30 * 60 + 1  # 06:30:01
                    }
                ]
            },
            "rest_schedule": {
                "breaks": [
                    {
                        "rest_duration_s": 60 * 30,
                        "work_time_range_till_rest": "06:30:01-06:30:01"  # 07:00:00 - 13:30:01
                    }
                ]
            }
        }
    ]
}


def test_multiple_time_windows_with_work_breaks():
    """
    checks for used_time_window for loc2 and work break before it
    """
    result = mvrp_checker.solve_and_check(json.dumps(data_with_work_breaks))

    assert len(result["routes"]) == 1

    route = result["routes"][0]["route"]
    assert len(route) == 6

    assert route[1]["node"]["used_time_window"] == "13:30:00-13:31:00"

    assert route[2]["node"]["type"] == "break"
    assert route[2]["node"]["value"]["work_duration_s"] == 6 * 60 * 60 + 30 * 60 + 1  # 06:30:01
    assert route[2]["node"]["value"]["rest_duration_s"] == 60 * 30

    # pick "14:00-14:30" time window, not "13:30-14:00"
    assert route[3]["node"]["used_time_window"] == "14:00:00-14:30:00"

    assert route[4]["node"]["used_time_window"] == "12:00:00-13:00:00"


def test_skipping_time_windows_due_to_work_break():
    """
    checks for skipping all loc2's time windows except last due to work break
    """
    task = copy.deepcopy(data_with_work_breaks)
    task["vehicles"][0]["rest_schedule"]["breaks"][0]["rest_duration_s"] = 4 * 60 * 60
    task["locations"][1]["time_windows"] = [
        {
            "time_window": "13:30-14:00"
        },
        {
            "time_window": "14:00-15:00"
        },
        {
            "time_window": "15:30-16:00"
        },
        {
            "time_window": "17:30-18:00"
        }
    ]

    result = mvrp_checker.solve_and_check(json.dumps(task))

    assert len(result["routes"]) == 1

    route = result["routes"][0]["route"]
    assert len(route) == 6

    assert route[1]["node"]["used_time_window"] == "13:30:00-13:31:00"

    assert route[2]["node"]["type"] == "break"
    assert route[2]["node"]["value"]["work_duration_s"] == 6 * 60 * 60 + 30 * 60 + 1  # 06:30:01
    assert route[2]["node"]["value"]["rest_duration_s"] == 4 * 60 * 60

    assert route[3]["node"]["used_time_window"] == "17:30:00-18:00:00"

    assert route[4]["node"]["used_time_window"] == "17:00:00-18:00:00"
