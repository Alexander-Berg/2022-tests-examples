import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest


def test_max_runs():
    with open(tools.arcadia_path('tests_data/max_runs.json')) as f_in:
        data = f_in.read()

    result = mvrp_checker.solve_and_check(
        data,
        None,
        expected_status='PARTIAL_SOLVED',
        solver_arguments={'sa_iterations': 100000}
    )

    expected_metrics = {
        "assigned_locations_count": 6,
        "dropped_locations_count": 3,
        "total_stops": 6,
        "used_vehicles": 3,
        "number_of_routes": 6,
        "max_vehicle_runs": 3
    }

    tools.check_metrics_are_close(result["metrics"], expected_metrics)

    # a vehicle with id N mustn't have more than N runs (by design)
    for route in result['routes']:
        assert route["run_number"] <= route['vehicle_id']


@pytest.mark.parametrize('visited', [True, False])
@pytest.mark.parametrize('start_at', [True, False])
@pytest.mark.parametrize('finish_at', [True, False])
@pytest.mark.parametrize('visit_depot_at_start', [True, False])
@pytest.mark.parametrize('return_to_depot', [True, False])
@pytest.mark.parametrize('shifts', [0, 1, 2])
def test_max_runs_start_finish_at(visited, start_at, finish_at, visit_depot_at_start, return_to_depot, shifts):
    """
    This test checks that start and finish locations don't break number of runs.
    """
    if not start_at and not visit_depot_at_start:
        # this case is forbidden by solver, the first location is required
        return

    task = tools.get_test_json("max_runs_start_finish_at.json")

    for vehicle in task["vehicles"]:
        if not visited:
            del vehicle["visited_locations"]
        if not start_at:
            del vehicle["start_at"]
        if not finish_at:
            del vehicle["finish_at"]
        if shifts == 0:
            del vehicle["shifts"]
        elif shifts == 1:
            del vehicle["shifts"][1]
        vehicle["visit_depot_at_start"] = visit_depot_at_start
        vehicle["return_to_depot"] = return_to_depot

    result = mvrp_checker.solve_and_check(
        json.dumps(task),
        expected_status='PARTIAL_SOLVED',
        solver_arguments={'sa_iterations': 10000})

    expected_metrics = {
        "assigned_locations_count": 5,
        "dropped_locations_count": 4,
        "number_of_routes": 5,
        "max_vehicle_runs": 3
    }

    tools.check_metrics_are_close(result["metrics"], expected_metrics)

    max_runs = [3, 2]
    runs = [0, 0]
    for route in result['routes']:
        runs[route['vehicle_id']] += 1
    assert runs == max_runs
