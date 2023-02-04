import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json

"""
checks vehicle shifts with return_to_depot=true/false
"""


def run_test(return_to_depot, num_vehicles=None, max_runs=10, expected_status='SOLVED'):
    data = tools.get_test_json("test_shifts.json")
    if num_vehicles is not None:
        data["vehicles"] = data['vehicles'][:num_vehicles]
    for vehicle in data["vehicles"]:
        vehicle["return_to_depot"] = return_to_depot
        vehicle["max_runs"] = max_runs
    mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 10000},
        expected_status=expected_status)


def test_with_return_to_depot():
    run_test(return_to_depot=True)


def test_with_return_to_depot_1vehicle_1run():
    run_test(return_to_depot=True, num_vehicles=1, max_runs=1, expected_status='PARTIAL_SOLVED')


def test_with_return_to_depot_1vehicle():
    run_test(return_to_depot=True, num_vehicles=1)


def test_with_no_return_to_depot():
    run_test(return_to_depot=False)


def test_with_no_return_to_depot_1vehicle_1run():
    run_test(return_to_depot=False, num_vehicles=1, max_runs=1, expected_status='PARTIAL_SOLVED')


def test_with_no_return_to_depot_1vehicle():
    run_test(return_to_depot=False, num_vehicles=1)


def test_empty_shifts():
    """
    This test was created to check metrics of invididual routes when vehicles have unused shifts
    """
    data = tools.get_test_json("test_empty_shifts.json")
    mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 10000})


def test_shift_selection():
    """
    Check that correct shift is chosen when number of locations in one shift is greater than MAX_MOVE_NODE_COUNT. BBGEO-3376
    """
    data = tools.get_test_json("test_shift_selection.json")
    response = mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 100000})
    assert response['metrics']['total_penalty'] + response['metrics']['failed_time_window_shifts_count'] == 0
    assert response['routes'][0]['shift']['id'] == 'correct_shift'


def test_shift_separation():
    """
    Check automatic shift separation in planned route when shift ids are not specified
    """
    data = tools.get_test_json("test_shift_separation.json")
    response = mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 0})

    shift_locations = {"shift1": [], "shift2": []}
    for route_info in response['routes']:
        shift_id = route_info["shift"]["id"]
        for location_info in route_info["route"]:
            if location_info["node"]["type"] != "depot":
                shift_locations[shift_id].append(location_info["node"]["value"]["id"])
    assert shift_locations["shift1"] == [9, 6, 14, 2, 12, 13, 7, 10, 1]
    assert shift_locations["shift2"] == [5, 8, 3, 4, 11]
