import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import pytest
import json


@pytest.mark.parametrize('metric', ['dist', 'alpha_nearest', 'dist_position'])
def test_probability_metric(metric):
    data = tools.get_test_json("alpha_nearest.json")

    data['options']['probability_metric'] = metric

    result = mvrp_checker.solve_and_check(json.dumps(data), solver_arguments={'sa_iterations': 10000})

    assert result['options']['probability_metric'] == metric
