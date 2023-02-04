import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_custom_cost():
    request = tools.get_test_json("custom_cost.json")
    response1 = mvrp_checker.solve_and_check(
        json.dumps(request), None, solver_arguments={'sa_iterations': 10000})

    request["vehicles"][0]["cost"] = (
        "(150 * duration_h + 10 * distance_km + 100 * locations + 3000) + "
        "(120 * trailer_duration_h + 12 * trailer_distance_km + 2000 * trailer_used)"
    )
    response2 = mvrp_checker.solve_and_check(
        json.dumps(request), None, solver_arguments={'sa_iterations': 10000})

    for key in response1["metrics"]:
        if key not in [
            "operations_per_second",
            "total_duration_cost",
            "total_fixed_cost",
            "total_locations_cost",
            "total_runs_cost",
            "total_transit_distance_cost",
            "total_custom_cost",
        ]:
            assert tools.is_abs_close(response1["metrics"][key], response2["metrics"][key], 1e-6)


def test_payout():
    # Решаем задачу, в которой по формулам выплата и стоимость отличаются ровно на 10000
    request = tools.get_test_json("custom_cost.json")

    cost_string = (
        "(150 * duration_h + 10 * distance_km + 100 * locations + 3000) + "
        "(120 * trailer_duration_h + 12 * trailer_distance_km + 2000 * trailer_used)"
    )
    payout_string = cost_string + " + 10000"

    request["vehicles"][0]["cost"] = cost_string
    request["vehicles"][0]["payout"] = payout_string

    response = mvrp_checker.solve_and_check(
        json.dumps(request), None, solver_arguments={'sa_iterations': 10000})

    assert tools.is_abs_close(response["metrics"]["total_custom_cost"] + 10000, response["metrics"]["total_payout"], 1e-6)
