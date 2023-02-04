import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools

import json
import pytest


def run_test(data, expected_stop_count):
    route = mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 100000}
    )
    expected_metrics = {"total_stops": expected_stop_count}
    tools.check_metrics_are_close(route["metrics"], expected_metrics)


@pytest.mark.parametrize('return_to_depot', [True, False])
@pytest.mark.parametrize('use_shifts', [True, False])
@pytest.mark.parametrize('finish_at', [True, False])
def test_stop_count(return_to_depot, use_shifts, finish_at):
    request = tools.get_test_json('stop_count.json')
    vehicle = request["vehicles"][0]
    if not use_shifts:
        del vehicle["shifts"]
    vehicle["return_to_depot"] = return_to_depot
    if not finish_at:
        del vehicle["finish_at"]
    run_test(request, 4)
