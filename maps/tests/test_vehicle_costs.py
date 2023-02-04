import json

import pytest

import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def solve_and_check(fname, iterations=100000, temperature=None):
    solver_arguments = {'sa_iterations': iterations}
    if temperature is not None:
        solver_arguments['sa_temperature'] = temperature
    return mvrp_checker.solve_and_check(
        tools.get_test_data(fname),
        solver_arguments=solver_arguments)


def check_used_vehicles(result, vehicle_ids):
    vehicle_ids = sorted(vehicle_ids)
    ids = sorted(route["vehicle_id"] for route in result['routes'])
    assert ids == vehicle_ids, "Wrong vehicle(s) selected for route: %s, expected: %s" % (ids, vehicle_ids)


def solve_and_check_used_vehicles(fpath, vehicle_ids, iterations=100000, temperature=None):
    result = solve_and_check(fpath, iterations, temperature)
    check_used_vehicles(result, vehicle_ids)


def test_vehicle_cost_fixed():
    solve_and_check_used_vehicles('vehicle_cost_fixed.json', [7])


def test_vehicle_cost_per_hour():
    solve_and_check_used_vehicles('vehicle_cost_per_hour.json', [10])


def test_vehicle_cost_per_loc():
    solve_and_check_used_vehicles('vehicle_cost_per_loc.json', [7])


def test_vehicle_cost_per_km():
    solve_and_check_used_vehicles('vehicle_cost_per_km.json', [1])


def test_vehicle_priority():
    solve_and_check_used_vehicles('vehicle_priority.json', [10])


def test_vehicle_cost_per_run():
    solve_and_check_used_vehicles('vehicle_cost_per_run.json', [7, 7, 7, 7, 7], 500000)


@pytest.mark.parametrize('hard_window', [False, True])
def test_flexible_start_time_and_vehicle_cost_per_hour0(hard_window):
    request = json.loads(tools.get_test_data('vehicle_cost_per_hour0.json'))
    for vehicle in request['vehicles']:
        vehicle['shifts'][0]['hard_window'] = hard_window
    result = mvrp_checker.solve_and_check(
        json.dumps(request), solver_arguments={'sa_iterations': 100000})
    for route in result['routes']:
        shift_node = route['shift']["start"]
        assert shift_node["waiting_duration_s"] < 1e-3, \
            "Waiting before the first shift should not occur when flexible_start_time is used."
        first_node = route['route'][0]
        assert first_node["waiting_duration_s"] < 1e-3, \
            "Waiting before the depot should not occur when flexible_start_time is used."
