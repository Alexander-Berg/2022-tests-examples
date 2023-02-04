import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import json
import datetime
import copy
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


"""
checks vehicle start time, which depends on location time windows,
used shifts, flexible start time option, visited locations.
"""


HOUR_S = 3600

DATA = {
    "depot": {
        "id": 1,
        "point": {"lon": 37.729377, "lat": 55.799087},
        "time_window": "07:00-23:00",
        "service_duration_s": 1200
    },
    "vehicles": [
        {
            "id": 1,
            "cost": {
                "hour": 500,
            },
            "shifts": [
                {
                    "id": "morning",
                    "time_window": "08:00-16:00",
                    "hard_window": True,
                    "service_duration_s": 1800
                },
                {
                    "id": "evening",
                    "time_window": "16:00-22:00",
                    "hard_window": False,
                    "service_duration_s": 1800
                }
            ]
        }
    ],
    "options": {
        "time_zone": 3
    },
    "locations": [
        {
            "id": 2,
            "point": {"lon": 37.708392, "lat": 55.781803},
            "time_window": "12:00-13:00"
        }
    ]
}

LOC2 = {
    "id": 3,
    "point": {"lon": 37.729377, "lat": 55.799087},
    "time_window": "06:00-22:00"
}


def solve(data, iterations=10000):
    return mvrp_checker.solve_and_check(data, solver_arguments={'sa_iterations': iterations})


def solve_and_get_start_time(data, iterations=10000):
    result = solve(json.dumps(data), iterations)
    route = result['routes'][0]
    first_loc = route['shift']['start'] if 'shift' in route else route['route'][0]
    return first_loc['arrival_time_s']


def sec2str(seconds):
    return datetime.timedelta(seconds=seconds)


def run_and_check_range(data, min_start_time, max_start_time):
    start_time = solve_and_get_start_time(data, iterations=100000)
    assert start_time >= min_start_time, \
        "Invalid start time: {}, should be not less than {}".format(
            sec2str(start_time), sec2str(min_start_time))
    assert start_time <= max_start_time, \
        "Invalid start time: {}, should be not greater than {}".format(
            sec2str(start_time), sec2str(max_start_time))


def run_and_check_start_time(data, expected_start_time):
    start_time = solve_and_get_start_time(data)
    assert start_time == expected_start_time, \
        "Invalid start time: {}, expected: {}".format(
            sec2str(start_time), sec2str(expected_start_time))


def test_start_time_shift1():
    run_and_check_start_time(DATA, 8 * HOUR_S)


def test_start_time_shift2():
    data = copy.deepcopy(DATA)
    data['locations'][0]['time_window'] = "17:00-18:00"
    run_and_check_start_time(data, 16 * HOUR_S)


def test_start_time_depot():
    data = copy.deepcopy(DATA)
    del data['vehicles'][0]['shifts']
    run_and_check_start_time(data, 7 * HOUR_S)


def test_start_time_visited_locations_shifts():
    data = copy.deepcopy(DATA)
    data['locations'].append(LOC2)
    data['vehicles'][0]['visited_locations'] = [
        {'id': LOC2['id'], "time": "10:00", "shift_id": "evening"}
    ]
    run_and_check_start_time(data, 10 * HOUR_S)


def test_start_time_visited_locations():
    data = copy.deepcopy(DATA)
    data['locations'].append(LOC2)
    data['vehicles'][0]['visited_locations'] = [
        {'id': LOC2['id'], "time": "06:00"}
    ]
    del data['vehicles'][0]['shifts']
    run_and_check_start_time(data, 6 * HOUR_S)


def test_start_time_visited_depot():
    data = copy.deepcopy(DATA)
    data['vehicles'][0]['visited_locations'] = [
        {'id': DATA['depot']['id'], "time": "07:00"}
    ]
    del data['vehicles'][0]['shifts']
    run_and_check_start_time(data, 7 * HOUR_S - data['depot']['service_duration_s'])


def test_flexible_start_time_shift1():
    data = copy.deepcopy(DATA)
    data['depot']['flexible_start_time'] = True
    run_and_check_range(data, 11 * HOUR_S, 13 * HOUR_S)


def test_flexible_start_time_shift2():
    data = copy.deepcopy(DATA)
    data['depot']['flexible_start_time'] = True
    data['locations'][0]['time_window'] = "17:00-18:00"
    run_and_check_range(data, 16 * HOUR_S, 18 * HOUR_S)


def test_flexible_start_time_depot():
    data = copy.deepcopy(DATA)
    data['depot']['flexible_start_time'] = True
    del data['vehicles'][0]['shifts']
    run_and_check_range(data, 11 * HOUR_S, 13 * HOUR_S)


def test_flexible_start_time():
    with open(tools.arcadia_path("tests_data/flexible_start_time.json")) as f_in:
        result = solve(f_in.read(), 300000)
        assert result['metrics']['total_waiting_duration_s'] < 1e-3, "Waitings must be eliminated in this task by flexible start time."


def test_flexible_start_time_and_shift():
    mvrp_checker.solve_and_check(
        json.dumps(tools.get_test_json('flexible_start_time_and_shift.json')),
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')


def test_non_negative_start_time():
    mvrp_checker.solve_and_check(
        json.dumps(tools.get_test_json('negative_start.json')),
        solver_arguments={'sa_iterations': 10000},
        expected_status='PARTIAL_SOLVED')
