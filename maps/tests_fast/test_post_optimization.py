import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest


@pytest.mark.parametrize('post_optimization', [True, False])
def test_post_optimization(post_optimization):
    """
    Without post optimization delivery location 2 is visited last,
    because it is more central, and that allows to decrease global proximity
    Test that post optimization fixes this problem
    """

    request = tools.get_test_json("post_optimization.json")
    request["options"]["post_optimization"] = post_optimization

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000})

    route = response["routes"][0]["route"]
    last_delivery_id = route[-2]["node"]["value"]["id"]

    assert (last_delivery_id in [1, 3]) if post_optimization else (last_delivery_id == 2)
