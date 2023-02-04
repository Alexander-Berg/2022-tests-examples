import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import pytest


@pytest.mark.skip(reason="не уверен должен ли он работать")
def test_overload_chains_optimization():
    """
    Here we check that client svrp task works as expected.
    There is a big order with weight greater than a vehicle capacity.
    And it can't be dropped since it's assigned to the vehicle by planned_route.
    It should be in the beginning of a route because it has an early short time
    window which is strict (hard).
    """

    result = mvrp_checker.solve_and_check(
        tools.get_test_data("overload_chains_optimization.json"), None,
        solver_arguments={'sa_iterations': 100000},
        expected_status="UNFEASIBLE")

    expected_metrics = {
        "assigned_locations_count": 6,
        "total_unfeasibility_count": 5,
        "total_unfeasibility_penalty": 0,
        "total_penalty": 767914,
        "total_stops": 4,
        "total_transit_distance_m": 382390,
        "number_of_routes": 1
    }

    tools.check_metrics_are_close(result["metrics"], expected_metrics)

    routes = result['routes']
    assert len(routes) == 1, "1 route expected"
    route = routes[0]

    expected_unfeasible_reasons = ["OVERLOAD_WEIGHT", "REQUIRED_TAGS_VIOLATION"]
    for idx, item in enumerate(route["route"]):
        loc = item["node"]["value"]
        if idx in [1, 2]:
            assert loc["id"] in ['3146803', '3146800'], \
                "Locations id 3146800 and id 3146803 must be in the beginning of the route due to an early hard time window."
        for reason in loc.get('unfeasible_reasons', []):
            assert reason["type"] in expected_unfeasible_reasons


def test_overload_drop():
    """
    Тест проверяет, что при перегрузе в planned_route солвер не добавляет лишние заказы в заезд
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data("overload_planned_drop.json"), None,
        solver_arguments={'sa_iterations': 10000},
        expected_status="UNFEASIBLE")

    expected_metrics = {
        "assigned_locations_count": 1
    }

    tools.check_metrics_are_close(result["metrics"], expected_metrics)


def test_overload_hacks():
    """
    Some rollbacking scenarios that were previously broken
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("overload_hacks.json"), None,
        solver_arguments={'sa_iterations': 1},
        expected_status="UNFEASIBLE")
