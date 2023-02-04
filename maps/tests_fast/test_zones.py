import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import pytest
import json


@pytest.mark.parametrize("planned", [True, False])
def test_zones(planned):
    """
    check that locations are dropped or marked unfeasible
    according to allowed_zones and forbidden_zones
    """
    request = tools.get_test_json('zones.json')
    if not planned:
        request["vehicles"][0].pop("planned_route")

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 1000},
        expected_status="UNFEASIBLE" if planned else "PARTIAL_SOLVED")

    if planned:
        route = response["routes"][0]
        for loc in route["route"]:
            if loc["node"]["value"]["id"] == 2:
                assert loc["node"]["value"]["unfeasible_reasons"] == [
                    {
                        "tags": [
                            "zone3"
                        ],
                        "text": "Order is not compatible with the vehicle because it belongs to the following forbidden zones: \"zone3\"",
                        "type": "FORBIDDEN_ZONES_VIOLATION"
                    }
                ]
            elif loc["node"]["value"]["id"] == 3:
                unfeasible_reasons = loc["node"]["value"]["unfeasible_reasons"]
                tags, reasons = zip(*[
                    (frozenset(reason["tags"]), reason["text"][:reason["text"].find(":")])
                    for reason in unfeasible_reasons
                ])
                assert set(tags) == set([
                    frozenset(["zone1", "zone2"]),
                    frozenset(["zone4"])
                ])
                assert set(reasons) == set([
                    "Order is not compatible with the vehicle because it does not belong to any of the following allowed zones",
                    "Order is not compatible with the vehicle because it belongs to the following forbidden zones"
                ])
            else:
                assert "unfeasible_reasons" not in loc["node"]["value"]
    else:
        assert len(response["dropped_locations"]) == 2
        drop_reasons = {
            2: "No compatible vehicles: \nvehicle with id 1: location is in the forbidden zone",
            3: ("No compatible vehicles: \nvehicle with id 1: does not allowed to visit any of location zones,"
                " vehicle with id 1: location is in the forbidden zone")
        }
        for loc in response["dropped_locations"]:
            assert loc["id"] in drop_reasons
            assert loc["drop_reason"] == drop_reasons[loc["id"]]


@pytest.mark.parametrize("explicit", [True, False])
def test_whole_world(explicit):
    request = tools.get_test_json('zones.json')
    vehicle = request["vehicles"][0]
    vehicle.pop("planned_route")
    vehicle["forbidden_zones"] = ["zone4"]
    if explicit:
        vehicle["allowed_zones"] = ["whole_world"]
    else:
        vehicle.pop("allowed_zones")

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 1000},
        expected_status="PARTIAL_SOLVED")

    assert len(response["dropped_locations"]) == 1
    assert response["dropped_locations"][0]["id"] == 3
    assert response["dropped_locations"][0]["drop_reason"] == \
           "No compatible vehicles: \nvehicle with id 1: location is in the forbidden zone"


@pytest.mark.parametrize('vehicles_count', [1, 2])
@pytest.mark.parametrize('max_runs', [1, 2])
@pytest.mark.parametrize('planned', [True, False])
def test_incompatible_zones(vehicles_count, max_runs, planned):
    if (vehicles_count == 2 and (planned or max_runs > 1)):
        return

    request = tools.get_test_json('incompatible_zones.json')

    if vehicles_count == 2 or max_runs == 2:
        expected_status = "SOLVED"
    elif not planned:
        expected_status = "PARTIAL_SOLVED"
    else:
        expected_status = "UNFEASIBLE"

    if vehicles_count == 1:
        request["vehicles"] = request["vehicles"][:1]
    if not planned:
        request["vehicles"][0].pop("planned_route")
        request["vehicles"][0].pop("shifts")
    request["vehicles"][0]["max_runs"] = max_runs

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 2000},
        expected_status=expected_status)

    assert len(response["routes"]) == max(vehicles_count, max_runs)
    if vehicles_count == 2 or max_runs == 2:
        loc_routes = {}
        for i, route in enumerate(response["routes"]):
            for loc in route["route"]:
                if loc["node"]["type"] == "location":
                    loc_routes[loc["node"]["value"]["id"]] = i

        assert loc_routes[1] != loc_routes[2]
        assert loc_routes[1] != loc_routes[3]
    elif planned:
        for loc in response["routes"][0]["route"]:
            if loc["node"]["type"] == "location":
                assert "unfeasible_reasons" in loc["node"]["value"]
                assert loc["node"]["value"]["unfeasible_reasons"][0]["type"] == "INCOMPATIBLE_ZONES_VIOLATION"
    else:
        assert len(response["dropped_locations"]) == 1
        assert response["dropped_locations"][0]["id"] == 1
        assert response["dropped_locations"][0]["drop_reason"] == "\nDrop reasons (for different vehicles):\n\t - Incompatible zones: 1 vehicles"


def test_ignore_zones():
    """
    test that allowed_zones, forbidden_zones and incompatible_zones
    are ignored, when ignore_zones option used
    """
    request = tools.get_test_json("ignore_zones.json")
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 1000},
        expected_status="SOLVED")
    assert response["metrics"]["assigned_locations_count"] == 2
