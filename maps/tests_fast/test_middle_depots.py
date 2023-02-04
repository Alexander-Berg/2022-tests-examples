import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools

import pytest
import json


def test_middle_depots():
    response = mvrp_checker.solve_and_check(
        tools.get_test_data('middle_depot.json'),
        None,
        solver_arguments={'sa_iterations': 10000})

    assert response["metrics"]["dropped_locations_count"] == 0

    route = response["routes"][0]["route"]
    depot_ids = set([loc["node"]["value"]["id"] for loc in route if loc["node"]["type"] == "depot"])
    assert len(depot_ids) == 2


@pytest.mark.skip()
@pytest.mark.parametrize('pickups', [True, False])
def test_middle_depots_overload(pickups):
    request = tools.get_test_json('middle_depot_overload.json')
    if not pickups:
        for loc in request["locations"]:
            loc["type"] = "delivery"

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        solver_arguments={'sa_iterations': 10000},
        expected_status="PARTIAL_SOLVED"
    )

    assert response["metrics"]["dropped_locations_count"] == 2

    route = response["routes"][0]["route"]
    num_orders = 0
    depot_ids = set()
    for i, loc in enumerate(route):
        if loc["node"]["type"] == "location":
            num_orders += 1
        elif i > 0:
            assert num_orders == 2
            num_orders = 0
            depot_ids.add(loc["node"]["value"]["id"])
    assert len(depot_ids) == 2


def test_middle_depots_planned_route():
    request = tools.get_test_json("middle_depot.json")
    request["vehicles"][0]["planned_route"] = {
        "locations": [
            {"id": 6},
            {"id": 4},
            {"id": 5},
            {"id": 1, "is_middle_depot": True},
            {"id": 3}
        ]
    }

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        solver_arguments={'sa_iterations': 0}
    )

    route = response["routes"][0]["route"]
    loc_ids = [loc["node"]["value"]["id"] for loc in route]
    assert loc_ids == [2, 0, 6, 4, 5, 1, 3, 0, 2]
