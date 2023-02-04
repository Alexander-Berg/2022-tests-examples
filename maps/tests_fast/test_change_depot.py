import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest


def check_depot_ids(response, expected_depot_ids):
    for route in response["routes"]:
        expected_depot_id = expected_depot_ids[route["vehicle_id"]]
        for loc in route["route"]:
            if loc["node"]["type"] == "depot":
                assert loc["node"]["value"]["id"] == expected_depot_id
            elif "depot_id" in loc["node"]["value"]:
                loc_depot_id = loc["node"]["value"]["depot_id"]
                if isinstance(loc_depot_id, list):
                    assert expected_depot_id in loc_depot_id
                else:
                    assert expected_depot_id == loc_depot_id

        if "shift" in route:
            if "start" in route["shift"]:
                assert route["shift"]["start"]["node"]["value"]["id"] == expected_depot_id
            if "end" in route["shift"]:
                assert route["shift"]["end"]["node"]["value"]["id"] == expected_depot_id


@pytest.mark.parametrize("with_shifts", [True, False])
def test_change_depot(with_shifts):
    """
    Test that solver replaces default vehicle depot with a closer one
    """
    task = tools.get_test_json("change_depot.json")
    if not with_shifts:
        task["vehicles"][0].pop("shifts")

    response = mvrp_checker.solve_and_check(
        json.dumps(task), None, solver_arguments={'sa_iterations': 100000})
    assert len(response["routes"]) == 2
    check_depot_ids(response, {
        1: 1
    })


def test_change_depot_locations_depot_id():
    """
    Test that CHANGE_DEPOT mutation takes locations depot_id fields into account.
    Here the only way to satisfy all constraints is to assign
    depot 1 to the first vehicle and depot 2 to the second
    """
    task = tools.get_test_json("change_depot_locations_depot_id.json")
    response = mvrp_checker.solve_and_check(
        json.dumps(task), None, solver_arguments={'sa_iterations': 100000})
    assert len(response["routes"]) == 2
    check_depot_ids(response, {
        1: 1,
        2: 2
    })


def test_change_depot_planned_route():
    """
    Test that if there is a depot in planned_route, it will be considered vehicle's
    initial depot (instead of the first depot, used by default)
    """
    task = tools.get_test_json("change_depot_planned_route.json")
    response = mvrp_checker.solve_and_check(
        json.dumps(task), None,
        solver_arguments={'sa_iterations': 0},
        expected_status="PARTIAL_SOLVED")
    assert len(response["routes"]) == 2
    check_depot_ids(response, {
        1: 1
    })
