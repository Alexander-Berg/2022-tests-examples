import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest


"""
test vehicle shift duration with various parameters
"""


@pytest.mark.parametrize("return_to_depot,flexible_start_time", [
    (True, True),
    (True, False),
    (False, True),
    (False, False),
])
def test_shift_duration(return_to_depot, flexible_start_time):
    data = tools.get_test_json("test_shift_duration.json")
    data['depot']['flexible_start_time'] = flexible_start_time
    for vehicle in data["vehicles"]:
        vehicle["return_to_depot"] = return_to_depot
    r = mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 10000})
    # In fact, all checks are in mvrp_checker,
    # but just in case let's leave them here too:
    assert r['metrics']['overtime_shifts_count'] == 1
    assert r['metrics']['overtime_duration_s'] >= 418
