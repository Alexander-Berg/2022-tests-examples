import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import pytest
import json
import copy


def check_sequence_order(routes, visited_locations=set()):
    for route in routes:
        prev_sequence_order = 0
        for loc in route["route"]:
            value = loc["node"]["value"]
            if "sequence_order" in value:
                if value["sequence_order"] < prev_sequence_order:
                    assert value["id"] in visited_locations
                    assert "unfeasible_reasons" in value
                    assert value["unfeasible_reasons"][0]["text"] == \
                        "Location has lower sequence_order value than some previous location in the route."
                prev_sequence_order = loc["node"]["value"]["sequence_order"]


@pytest.mark.parametrize('vehicles_count', [1, 2])
@pytest.mark.parametrize('max_runs', [1, 2])
def test_sequence_order(vehicles_count, max_runs):
    """
    Check that locations are visited in non-decreasing order of sequence_order
    """
    if vehicles_count == 2 and max_runs == 2:
        return
    request = tools.get_test_json('sequence_order.json')
    request["vehicles"][0]["max_runs"] = max_runs
    for shift in request["vehicles"][0]["shifts"]:
        shift["maximal_stops"] = 10 // max_runs
    if vehicles_count == 2:
        request["vehicles"].append(copy.deepcopy(request["vehicles"][0]))
        request["vehicles"][1]["id"] = 2

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")
    check_sequence_order(response["routes"])


def test_lifo_sequence_order():
    """
    Check that sequence_order works with in_lifo_order
    """
    request = tools.get_test_json('lifo.json')
    request["locations"][0]["sequence_order"] = 0
    request["locations"][0]["sequence_order"] = 1
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")
    check_sequence_order(response["routes"])


def test_delivery_to_sequence_order():
    """
    Check that sequence_order works with delivery_to
    """
    request = tools.get_test_json('delivery_to_sequence_order.json')
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")
    check_sequence_order(response["routes"])


def test_visited_locations_sequence_order():
    """
    Check that sequence_order works with visited_locations and, in particular,
    locations which have lower sequence_order than all visited locations are dropped
    """
    request = tools.get_test_json('sequence_order.json')
    request["vehicles"][0]["visited_locations"] = [
        {
            "id": 2
        },
        {
            "id": 0
        }
    ]
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="UNFEASIBLE")
    assert response["metrics"]["dropped_locations_count"] == 1
    assert response["dropped_locations"][0]["drop_reason"] == \
        "No compatible vehicles: \nvehicle with id 1: location has lower sequence_order than visited location"
    check_sequence_order(response["routes"], set([0, 2]))


def test_planned_route_sequence_order():
    """
    Check that sequence_order works with planned_route and, in particular, reorders locations
    in planned_route according to their sequence_order values (if optimization is applied at all)
    """
    request = tools.get_test_json('sequence_order.json')
    request["vehicles"][0]["planned_route"] = {
        "locations": [
            {
                "id": 7
            },
            {
                "id": 5
            },
            {
                "id": 2
            },
            {
                "id": 0
            }
        ]
    }
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 1},
        expected_status="PARTIAL_SOLVED")
    check_sequence_order(response["routes"])


def test_location_groups_sequence_order():
    """
    Check that sequence_order works with solid and dependent location groups
    """
    request = tools.get_test_json('sequence_order.json')
    request["options"]["location_groups"] = [
        {
            "location_ids": [0, 2, 5],
            "dependent": True
        },
        {
            "location_ids": [3, 4],
            "dependent": True,
            "solid": True
        }
    ]
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")
    check_sequence_order(response["routes"])
    route = response["routes"][0]["route"]
    for i in range(len(route)):
        value = route[i]["node"]["value"]
        if value["id"] in [3, 4]:
            assert i + 1 < len(route)
            assert route[i + 1]["node"]["value"]["id"] in [3, 4]
            break
