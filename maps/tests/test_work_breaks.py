import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import _solver as solver
import json
import pytest
import copy

DURATION_THRESHOLD_S = 0.001

PlannedRouteWithShifts = {
    "locations": [
        {"id": "8", "shift_id": "day 2"},
        {"id": "4", "shift_id": "day 2"},
        {"id": "6", "shift_id": "day 2"},
        {"id": "3", "shift_id": "day 2"},
        {"id": "2", "shift_id": "day 2"},
        {"id": "7", "shift_id": "day 3"},
        {"id": "1", "shift_id": "day 3"},
        {"id": "0", "shift_id": "day 3"},
        {"id": "5", "shift_id": "day 3"}
    ],
    "work_breaks": [
        {"shift_id": "day 2", "work_duration_s": 15360},
        {"shift_id": "day 2", "work_duration_s": 14820},
        {"shift_id": "day 2", "work_duration_s": 14940},
        {"shift_id": "day 2", "work_duration_s": 16200},
        {"shift_id": "day 3", "work_duration_s": 15720},
        {"shift_id": "day 3", "work_duration_s": 15960}
    ]
}

PlannedRouteNoShifts = {
    "locations": [
        {"id": "8"},
        {"id": "4"},
        {"id": "6"},
        {"id": "3"},
        {"id": "2"},
        {"id": "depot"},
        {"id": "7"},
        {"id": "1"},
        {"id": "0"},
        {"id": "5"}
    ],
    "work_breaks": [
        {"work_duration_s": 15360},
        {"work_duration_s": 14820},
        {"work_duration_s": 14940},
        {"work_duration_s": 16200},
        {"work_duration_s": 15720},
        {"work_duration_s": 15960}
    ]
}


def get_planned_route(shifts, work_duration_s=None):
    planned_route = PlannedRouteWithShifts if shifts else PlannedRouteNoShifts
    if work_duration_s is None:
        return planned_route

    planned_route = copy.deepcopy(planned_route)
    for work_break in planned_route["work_breaks"]:
        work_break["work_duration_s"] = work_duration_s
    return planned_route


def get_request(shifts):
    request = tools.get_test_json('work_breaks.json')

    assert len(request['vehicles']) == 1
    vehicle = request['vehicles'][0]

    if not shifts:
        del vehicle['shifts']

    return request


@pytest.mark.parametrize('shifts', [False, True])
def test_planned_work_breaks(shifts):
    request = get_request(shifts)

    vehicle = request['vehicles'][0]
    planned_route = vehicle["planned_route"] = get_planned_route(shifts)

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 0},
        expected_metrics={
            "assigned_locations_count": 9,
            "total_stops": 9,
            "dropped_locations_count": 0,
            "total_unfeasibility_count": 0,
            "failed_max_work_duration_count": 0,
            "failed_min_work_duration_count": 0,
            "failed_work_duration_count": 0,
            "failed_work_duration_count_penalty": 0,
            "failed_work_duration_penalty": 0,
            "failed_work_duration_s": 0,
            "max_vehicle_runs": 2,
            "number_of_routes": 2,
            "total_failed_time_window_count": 0,
            "total_failed_time_window_duration_s": 0,
            "total_failed_time_window_penalty": 0,
            "total_work_breaks": 6,
            "used_vehicles": 1
        }
    )

    work_durations = []
    for route in response['routes']:
        for item in route['route']:
            node = item['node']
            if node['type'] == 'break':
                work_durations.append(node['value']['work_duration_s'])

    planned_breaks = planned_route['work_breaks']
    assert len(planned_breaks) == len(work_durations)
    for work_break, work_duration_s in zip(planned_breaks, work_durations):
        assert abs(work_break["work_duration_s"] - work_duration_s) < DURATION_THRESHOLD_S


@pytest.mark.skip(reason="test is brocken")
@pytest.mark.parametrize('shifts', [False, True])
@pytest.mark.parametrize('planned_work_duration_h', [None, 0, 1, 10])
def test_work_breaks(shifts, planned_work_duration_h):
    """
    This test checks that work breaks are used by solver correctly
    when shifts are defined and when not defined.
    Here planned work duration is always non-optimal or not defined,
    so it shouldn't be accepted and we should get a response of the
    same quality as without planned work breaks.
    """

    request = get_request(shifts)

    if planned_work_duration_h is not None:
        request["vehicles"][0]["planned_route"] = get_planned_route(shifts, 3600 * planned_work_duration_h)

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 2000000},
        expected_metrics={
            "assigned_locations_count": 9,
            "total_stops": 9,
            "number_of_routes": 2,
            "max_vehicle_runs": 2,
            "total_failed_time_window_count": 0,
            "failed_work_duration_count": 0,
            "failed_work_duration_count_penalty": 0,
            "failed_work_duration_penalty": 0,
            "failed_work_duration_s": 0
        })

    expectedBreakCount = [6, 7, 8]
    totalCostWithPenaltyUpperBound = 10000

    assert response["metrics"]["total_work_breaks"] in expectedBreakCount
    assert response["metrics"]["total_cost_with_penalty"] <= totalCostWithPenaltyUpperBound


def test_planned_phantom_breaks():
    """
    This test previously caused the solver to crash.
    """
    with pytest.raises(
            solver.SolverLocalizedException):
        mvrp_checker.solve_and_check(json.dumps(tools.get_test_json('planned_route_segfault.json')))


@pytest.mark.parametrize('shifts', [False, True])
def test_work_breaks_from_start(shifts):
    """
    This test checks that `work_time_range_from_start` works.
    """
    request = tools.get_test_json('work_breaks_from_start.json')

    if not shifts:
        vehicle = request['vehicles'][0]
        del vehicle['shifts']

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
    )

    route_start = 12 * 3600
    work_breaks_starts = [300, 1200, 2100]
    work_breaks_durations = [300, 600, 900]
    pos = 0

    route = response['routes'][0]['route']
    for node in route:
        if node['node']['type'] == 'break':
            assert pos < 3
            assert node['arrival_time_s'] == route_start + work_breaks_starts[pos]
            assert node['departure_time_s'] == route_start + work_breaks_starts[pos] + work_breaks_durations[pos]
            pos += 1


@pytest.mark.parametrize('shifts', [False, True])
def test_work_breaks_continuous(shifts):
    """
    This test checks that `continuous_travel_time_range` works.
    """
    request = tools.get_test_json('work_breaks_continuous.json')

    if not shifts:
        vehicle = request['vehicles'][0]
        del vehicle['shifts']

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 200000},
        expected_metrics={'total_work_breaks': 6}
    )

    route = response['routes'][0]['route']
    for node in route:
        if node['node']['type'] == 'break':
            assert node['transit_duration_s'] == 3 * 3600


@pytest.mark.parametrize('shifts', [False, True])
def test_work_breaks_before_after(shifts):
    """
    This test checks that work breaks with fields `before_first_location` and `after_last_location`
    are added correctly.
    """
    request = tools.get_test_json('work_breaks_before_after.json')

    if not shifts:
        vehicle = request['vehicles'][0]
        del vehicle['shifts']

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
    )

    route = response['routes'][0]['route']
    assert route[1]['node']['type'] == 'break'
    assert route[-2]['node']['type'] == 'break'


@pytest.mark.parametrize('shifts', [False, True])
def test_work_breaks_merge(shifts):
    """
    This test checks that work breaks with fields `before_first_location` and `after_last_location`
    are merged correctly.
    """
    request = tools.get_test_json('work_breaks_merge.json')

    if not shifts:
        vehicle = request['vehicles'][0]
        del vehicle['shifts']

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
    )

    route = response['routes'][0]['route']

    assert route[1]['node']['type'] == 'break'
    assert route[2]['node']['type'] == 'break'
    assert route[1]['arrival_time_s'] + route[1]['waiting_duration_s'] == route[2]['arrival_time_s']
    assert route[1]['departure_time_s'] == route[2]['departure_time_s']
    assert route[2]['departure_time_s'] - route[2]['arrival_time_s'] == 1800

    assert route[-3]['node']['type'] == 'break'
    assert route[-2]['node']['type'] == 'break'
    assert route[-3]['arrival_time_s'] + route[-3]['waiting_duration_s'] == route[-2]['arrival_time_s']
    assert route[-3]['departure_time_s'] == route[-2]['departure_time_s']
    assert route[-2]['departure_time_s'] - route[-2]['arrival_time_s'] == 1800


@pytest.mark.skip(reason="test is brocken")
@pytest.mark.parametrize('shifts', [False, True])
def test_work_breaks_not_merge(shifts):
    """
    Same as previous test, but we can't merge breaks.
    """
    request = tools.get_test_json('work_breaks_merge.json')

    vehicle = request['vehicles'][0]
    if not shifts:
        del vehicle['shifts']

    breaks = vehicle['rest_schedule']['breaks']
    breaks[0]['type'] = 'b'
    breaks[1]['type'] = 'a'

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
    )

    route = response['routes'][0]['route']

    assert route[1]['node']['type'] == 'break'
    assert route[2]['node']['type'] == 'break'
    assert route[1]['departure_time_s'] <= route[2]['arrival_time_s']
    assert route[2]['departure_time_s'] - route[2]['arrival_time_s'] == 1800

    assert route[-3]['node']['type'] == 'break'
    assert route[-2]['node']['type'] == 'break'
    assert route[-3]['departure_time_s'] <= route[-2]['arrival_time_s']
    assert route[-3]['departure_time_s'] - route[-3]['arrival_time_s'] == 1800


@pytest.mark.parametrize('shifts', [False, True])
def test_work_breaks_before_2(shifts):
    """
    2 breaks before first location that should be merged.
    """
    request = tools.get_test_json('work_breaks_before_2.json')

    if not shifts:
        vehicle = request['vehicles'][0]
        del vehicle['shifts']

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
    )

    route = response['routes'][0]['route']

    assert route[1]['node']['type'] == 'break'
    assert route[2]['node']['type'] == 'break'
    assert route[1]['arrival_time_s'] + route[1]['waiting_duration_s'] == route[2]['arrival_time_s']
    assert route[1]['departure_time_s'] == route[2]['departure_time_s']
    assert route[2]['departure_time_s'] - route[2]['arrival_time_s'] == 3600


@pytest.mark.skip(reason="test is brocken")
def test_work_breaks_not_fit():
    """
    There are 2 small breaks `a` and `b` before first location.
    If we merge a big work break with `a`, `b` should move after first location.
    """
    response = mvrp_checker.solve_and_check(
        tools.get_test_data('work_breaks_not_fit.json'),
        solver_arguments={'sa_iterations': 100},
    )

    route = response['routes'][0]['route']

    assert route[1]['node']['type'] == 'break'
    assert route[2]['node']['type'] == 'break'
    assert route[1]['arrival_time_s'] + route[1]['waiting_duration_s'] == route[2]['arrival_time_s']
    assert route[1]['departure_time_s'] == route[2]['departure_time_s']
    assert route[2]['departure_time_s'] - route[2]['arrival_time_s'] == 28800

    assert route[-2]['node']['type'] == 'break'


def test_work_breaks_route_duration():
    mvrp_checker.solve_and_check(
        tools.get_test_data('work_breaks_route_duration.json'),
        solver_arguments={'sa_iterations': 100},
        expected_metrics={'total_work_breaks': 2}
    )


def test_work_breaks_template():
    mvrp_checker.solve_and_check(
        tools.get_test_data('work_breaks_template.json'),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={'total_work_breaks': 2}
    )


def test_work_breaks_drops():
    mvrp_checker.solve_and_check(
        tools.get_test_data('work_breaks_drops.json'),
        solver_arguments={'sa_iterations': 100000},
        expected_metrics={
            "failed_max_work_duration_count": 1,
            "failed_min_work_duration_count": 0,
            "failed_work_duration_count": 1,
            "failed_work_duration_count_penalty": 1000,
        }
    )


@pytest.mark.skip(reason="test is broken")
def test_work_break_chains():
    mvrp_checker.solve_and_check(
        tools.get_test_data('work_break_chains.json'),
        solver_arguments={'sa_iterations': 100000},
        expected_metrics={
            "total_late_count": 0,
            "total_work_breaks": 6
        }
    )


@pytest.mark.skip(reason="test is broken")
def test_work_breaks_repeatable():
    mvrp_checker.solve_and_check(
        tools.get_test_data('work_breaks_repeatable.json'),
        solver_arguments={'sa_iterations': 100000},
        expected_metrics={
            "failed_work_duration_count": 0,
            "total_late_count": 0,
            "total_work_breaks": 4
        }
    )


def test_work_breaks_travel():
    response = mvrp_checker.solve_and_check(
        tools.get_test_data('work_breaks_travel.json'),
        solver_arguments={'sa_iterations': 100000},
        expected_metrics={
            "failed_work_duration_count": 0,
            "total_late_count": 0,
            "total_work_breaks": 3
        }
    )

    route = response['routes'][0]['route']
    travel_time = 0
    for node in route:
        travel_time += node['transit_duration_s']
        if node['node']['type'] == 'break':
            assert abs(travel_time - 300) < DURATION_THRESHOLD_S
            travel_time = 0
