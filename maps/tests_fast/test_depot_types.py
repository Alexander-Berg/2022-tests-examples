import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools

import pytest
import json


def get_depot_ids(response):
    depot_ids = []
    for route in response["routes"]:
        for loc in route["route"]:
            if loc["node"]["type"] == "depot":
                depot_ids.append(loc["node"]["value"]["id"])
    return depot_ids


def remove_shifts(request):
    for vehicle in request["vehicles"]:
        vehicle["max_runs"] = len(vehicle["shifts"])
        vehicle.pop("shifts")
        if "visited_locations" in vehicle:
            for loc in vehicle["visited_locations"]:
                loc.pop("shift_id")
            for loc in vehicle["planned_route"]["locations"]:
                loc.pop("shift_id")


@pytest.mark.parametrize('shifts', [True, False])
def test_depot_types(shifts):
    """
    Test request with different depot types.
    Depot 1 should be used as the first depot and depot 2 as the last,
    because they are in the same locations as orders.
    Between the runs only depot 1 can be used, and a middle depot 3 should
    be added in one of the runs, to take all the orders
    """
    request = tools.get_test_json('depot_types.json')

    if not shifts:
        remove_shifts(request)

    response = mvrp_checker.solve_and_check(
        json.dumps(request), None,
        solver_arguments={'sa_iterations': 100000})

    assert response["metrics"]["number_of_routes"] == 2

    depot_ids = get_depot_ids(response)
    assert depot_ids == [1, 3, 0, 0, 2] or depot_ids == [1, 0, 0, 3, 2]


@pytest.mark.parametrize('shifts', [True, False])
def test_depot_types_planned_visited(shifts):
    """
    Check that depot ids are correctly extracted from planned_route and visited_locations
    """
    request = tools.get_test_json('depot_types_planned_visited.json')

    if not shifts:
        remove_shifts(request)

    response = mvrp_checker.solve_and_check(
        json.dumps(request), None,
        solver_arguments={'sa_iterations': 10000})

    assert response["metrics"]["number_of_routes"] == 3

    depot_ids = get_depot_ids(response)
    assert depot_ids[:5] == [1, 3, 3, 4, 4]
