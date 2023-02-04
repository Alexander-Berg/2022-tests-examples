import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import fnmatch


DefaultPenalty = {
    "drop": 1000000,
    "out_of_time": {
        "fixed": 1000,
        "minute": 17
    }
}


def test_smoke():
    """
    trivial test in 10 locations

    Solver deviation results (30 runs):
    log2_iterations  cost_best  cost_avr  cost_2std(%)  cost_99err(%)
    100000.000       4017.726   4028.798  1.100         1.510
    """
    route = mvrp_checker.solve_and_check(
        tools.get_test_data('10_locs.json'),
        tools.get_test_data('10_locs_distances.json'),
        solver_arguments={'sa_iterations': 100000, 'sa_temperature': 5000},
        runs_count=10)

    expected_metrics = {
        "dropped_locations_count": 0.0,
        "early_locations_count": 0.0,
        "early_shifts_count": 0.0,
        "failed_time_window_locations_count": 0.0,
        "failed_time_window_shifts_count": 0.0,
        "late_locations_count": 0.0,
        "late_shifts_count": 0.0,
        "lateness_risk_locations_count": 0.0,
        "number_of_routes": 2.16,
        "objective_minimum": 234398.285,
        "optimization_steps": 100000.0,
        "total_cost": 4028.798,
        "total_cost_with_penalty": 4028.798,
        "total_depot_penalty": 0.0,
        "total_duration_s": 29331.0,
        "total_early_duration_s": 0.0,
        "total_failed_time_window_duration_s": 0.0,
        "total_guaranteed_penalty": 0.0,
        "total_late_duration_s": 0.0,
        "total_lateness_risk_probability": 0.0,
        "total_penalty": 0.0,
        "total_probable_penalty": 0.0,
        "total_transit_distance_m": 26756.04,
        "total_transit_duration_s": 4005.84,
        "total_waiting_duration_s": 25325.16,
        "used_vehicles": 1.0
    }

    rel_accuracies = {
        "number_of_routes": 0.1732,
        "total_transit_distance_m": 0.10375,
        "total_transit_duration_s": 0.0818
    }

    tools.check_metrics_are_close(route["metrics"], expected_metrics, rel_accuracies)


def test_time_limit_0():
    """
    tests that with solver_time_limit_s == 0
    solver does no mutations (i.e. 0 iterations are expected).
    """

    with open(tools.arcadia_path('tests_data/time_limit_0.json')) as f_in:
        data = json.load(f_in)

    result = mvrp_checker.solve_and_check(json.dumps(data))

    assert result["metrics"]['optimization_steps'] == 0

    # 2 runs are specified in planned_route
    assert len(result['routes']) == 2

    # start_time: "08:08" is specified in planned_route
    assert result['routes'][0]['route'][0]['arrival_time_s'] == 8*3600 + 8*60


def test_smoke_traffic():
    """
    smoke test with time dependant distance matrix
    """
    route = mvrp_checker.solve_and_check(
        tools.get_test_data('test_traffic.json'),
        tools.get_test_data('test_traffic_distances.json'),
        solver_arguments={'sa_iterations': 10000, 'sa_temperature': 500},
        duration_callback=mvrp_checker.traffic_duration)

    expected_metrics = {
        "dropped_locations_count": 0,
        "early_locations_count": 0,
        "failed_time_window_locations_count": 0,
        "late_locations_count": 0,
        "objective_minimum": 166565.55952638388,
        "optimization_steps": 10000,
        "total_cost": 3001.727503926595,
        "total_cost_with_penalty": 3001.727503926595,
        "total_duration_s": 48.284271240234375,
        "total_early_duration_s": 0,
        "total_failed_time_window_duration_s": 0,
        "total_late_duration_s": 0,
        "total_transit_distance_m": 48.284271240234375,
        "total_transit_duration_s": 48.284271240234375,
        "total_waiting_duration_s": 0,
        "used_vehicles": 1
    }
    tools.check_metrics_are_close(route["metrics"], expected_metrics)


def test_smoke_no_traffic():
    """
    smoke test with time dependant distance matrix
    """
    route = mvrp_checker.solve_and_check(
        tools.get_test_data('test_traffic.json'),
        tools.get_test_data('test_traffic_distances_no_traffic.json'),
        solver_arguments={'sa_iterations': 10000, 'sa_temperature': 500},
        duration_callback=mvrp_checker.traffic_duration)

    expected_metrics = {
        "dropped_locations_count": 0,
        "early_locations_count": 0,
        "failed_time_window_locations_count": 0,
        "late_locations_count": 0,
        "objective_minimum": 166550.73988562822,
        "optimization_steps": 10000,
        "total_cost": 3001.4311111111115,
        "total_cost_with_penalty": 3001.4311111111115,
        "total_duration_s": 40,
        "total_early_duration_s": 0,
        "total_failed_time_window_duration_s": 0,
        "total_late_duration_s": 0,
        "total_penalty": 0,
        "total_transit_distance_m": 40,
        "total_transit_duration_s": 40,
        "total_waiting_duration_s": 0,
        "used_vehicles": 1
    }
    tools.check_metrics_are_close(route["metrics"], expected_metrics)


def get_oot_duration(node):
    return node.get('failed_time_window', {}).get('duration_s', 0)


def check_penalties_are_equal(a, b):
    assert ("drop" in a) == ("drop" in b)
    if "drop" in a:
        assert a["drop"] == b["drop"]
    assert a["out_of_time"]["fixed"] == b["out_of_time"]["fixed"]
    assert a["out_of_time"]["minute"] == b["out_of_time"]["minute"]


def test_svrp_location_penalties_and_multiorders():
    """
    checks single vehicle routing case with penalties and multi-orders
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('svrp_request.json'), None,
        solver_arguments={'sa_iterations': 1500000},
        kind='svrp', soft_windows=True, expected_status="PARTIAL_SOLVED")

    drops = result["dropped_locations"]
    assert len(drops) == 1
    drop = drops[0]
    assert str(drop["id"]) == "527149"
    check_penalties_are_equal(
        drop["penalty"], {"drop": 1, "out_of_time": DefaultPenalty["out_of_time"]}
    )

    penalties = {
        208: {"out_of_time": {"fixed": 900, "minute": 20}},
        527007: {"drop": DefaultPenalty["drop"], "out_of_time": {"fixed": 15, "minute": 2}},
        527262: {"drop": DefaultPenalty["drop"], "out_of_time": {"fixed": 30, "minute": 1}},
    }

    for route in result["routes"]:
        for item in route["route"]:
            loc = item["node"]["value"]
            penalty = penalties.get(loc["id"])
            if penalty is not None:
                check_penalties_are_equal(loc["penalty"], penalty)

    expected_metrics = {
        "dropped_locations_count": 1,
        "early_locations_count": 0,
        "failed_time_window_locations_count": 6,
        "late_locations_count": 6,
        "total_cost": 4938.5,
        "total_cost_with_penalty": 13557.5,
        "total_failed_time_window_duration_s": 36482,
        "total_penalty": 8619.03,
        "used_vehicles": 1
    }

    tools.check_metrics_are_close(result["metrics"], expected_metrics)


def check_time_window_format(result, pattern):
    assert len(result['routes'])
    for route_info in result['routes']:
        assert len(route_info['route'])
        for route in route_info['route']:
            time_window = route['node']['used_time_window']
            assert fnmatch.fnmatch(time_window, pattern), \
                "time_window '%s' doesn't match pattern '%s'" % (time_window, pattern)


def test_time_window_format():
    """
    test different formats of output time_window
    """
    with open(tools.arcadia_path('tests_data/10_locs.json'), 'r') as f:
        task = json.loads(f.read())

    patterns = ['??:??:??', '????-??-??T??:??:??[+-]??:??']
    local_pattern = patterns[0] + '-' + patterns[0]
    iso8601_pattern = patterns[1] + '/' + patterns[1]

    for absolute_time in [False, True]:
        if absolute_time:
            task["options"]["absolute_time"] = True
        route = mvrp_checker.solve_and_check(
            json.dumps(task),
            tools.get_test_data('10_locs_distances.json'),
            solver_arguments={'sa_iterations': 50000, 'sa_temperature': 5000})

        check_time_window_format(route, iso8601_pattern if absolute_time else local_pattern)


def test_location_ids():
    """
    checks that location ids are handled correctly
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('location_ids.json'),
        None, solver_arguments={'sa_iterations': 10000})

    assert len(result['routes']) == 1
    route = result['routes'][0]["route"]

    expectedIds = set([
        2080000000000000000,
        527982,
        999999999999,
        2222222222222222222,
        3333333333333333333,
        4444444444444444444,
        5555555555555555555,
        2**63 - 1,
        -2**63,
    ])

    locationIds = set(item["node"]["value"]["id"] for item in route)
    assert locationIds == expectedIds


def test_single_fixed_location_route():
    """
    The route should include the only location without depots.
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('single_fixed_location_route.json'),
        None, solver_arguments={'sa_iterations': 100})
    assert len(result['routes']) == 1
    route = result['routes'][0]['route']
    assert len(route) == 1
    assert route[0]['node']['value']['id'] == 1
