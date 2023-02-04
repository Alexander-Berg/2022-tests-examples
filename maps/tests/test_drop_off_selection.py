import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker

import gen_drop_off_selection_test
import json


def test_drop_off_selection():
    """
    In this test all order locations and a single drop-off location are generated
    in a rectangle area, then 49 additional drop-off locations are generated outside
    this rectangle scaled by x2, so the closest drop-off is always the first one -
    its ref is "drop_off_0". No other drop-off locations should be selected by solver.
    """

    request = gen_drop_off_selection_test.gen_request(
        delivery_count=10,
        pickup_count=10,
        pickup_and_delivery_count=10,
        delivery_to_any_count=30,
        drop_off_count=300,
        vehicles_count=1
    )

    response = mvrp_checker.solve_and_check(
        json.dumps(request), None,
        solver_arguments={"sa_iterations": 500000})

    for route in response["routes"]:
        for item in route["route"]:
            node = item["node"]
            loc = node["value"]
            if node["type"] == "location" and loc["type"] == "drop_off":
                assert loc["ref"] == "drop_off_0", "Not the closest drop-off location was selected!"
