import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def solve_and_check_routes_number(data_path,
                                  travel_time_multiplier,
                                  expected_count,
                                  iterations):
    task = json.loads(open(tools.arcadia_path(data_path)).read())
    if travel_time_multiplier is not None:
        for v in task["vehicles"]:
            v["travel_time_multiplier"] = travel_time_multiplier
    result = mvrp_checker.solve_and_check(json.dumps(task),
                                          None,
                                          duration_callback=mvrp_checker.haversine_20_duration,
                                          solver_arguments={
                                              'sa_iterations': iterations,
                                              'sa_temperature': 50})
    vehicle_cnt = len(result['routes'])
    if vehicle_cnt != expected_count:
        print(json.dumps(result, indent=4))
    assert vehicle_cnt == expected_count, \
        "routes expected: %d, got: %d" % (expected_count, vehicle_cnt)


def test_missing_duration_adjustment():
    """
    Test case when duration_adjustment is ommited
    """
    solve_and_check_routes_number("tests_data/vehicle_duration_adjustment.json",
                                  None, 1, 100000)


def test_default_duration_adjustment():
    """
    Test case when duration_adjustment specified with default value
    """
    solve_and_check_routes_number("tests_data/vehicle_duration_adjustment.json",
                                  1, 1, 100000)


def test_trucks():
    """
    Test case when duration_adjustment specified as for tracks that 70 percent
    slower then a regular car.
    In this case one track cannot deliver to all locations in time so
    solver should involve extra vehicle.
    """
    solve_and_check_routes_number("tests_data/vehicle_duration_adjustment.json",
                                  1.4, 2, 100000)
