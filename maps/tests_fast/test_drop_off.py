import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools

import json


def test_drop_off():
    mvrp_checker.solve_and_check(
        tools.get_test_data("drop_off.json"), None,
        solver_arguments={'sa_iterations': 10000})


def test_drop_off_or_depot():
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("drop_off_or_depot.json"), None,
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')
    assert len(r["routes"]) == 1
    route = r["routes"][0]
    # Достаточно одного дропофа в маршруте, второй пикап можно отвезти напрямую в депо
    assert len([item for item in route["route"] if item["node"]["value"].get("type") == 'drop_off']) == 1


def test_drop_off_in_visited():
    data = tools.get_test_json("drop_off.json")
    data["vehicles"][0]["visited_locations"] = [{"id": 2, "time": "10:00"}]
    mvrp_checker.solve_and_check(
        json.dumps(data), None,
        solver_arguments={'sa_iterations': 10000})


def test_drop_off_no_depot():
    """
    A corner case, when there is no depot in a route
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("drop_off_no_depot.json"), None,
        solver_arguments={'sa_iterations': 10000})


def test_drop_off_planned_route():
    """
    drop_off location are specified in a planned route
    """
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("drop_off_planned_route.json"), None,
        solver_arguments={'sa_iterations': 1})
    assert len(r["routes"]) == 1
    route = r["routes"][0]
    ids = [item["node"]["value"]["id"] for item in route["route"]]
    assert ids == ['0', '5', '4', '1', '2', '6', '7', '0']


def test_drop_off_tags():
    """
    tag compatibility for drop_off locations
    """
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("drop_off_tags.json"), None,
        solver_arguments={'sa_iterations': 10000})

    for route in r["routes"]:
        tag = "TYPE_A" if route["vehicle_id"] == "1" else "TYPE_B"
        for item in route["route"]:
            if item["node"]["value"].get("type", "") == "drop_off":
                assert "required_tags" in item["node"]["value"]
                assert item["node"]["value"]["required_tags"][0] == tag


def test_unused_drop_offs():
    """
    Test that there are no drop offs in final solution, which are not used by any pickup
    """
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("unused_drop_offs.json"), None,
        solver_arguments={'sa_iterations': 10000},
    )

    pickupsWithDeliveryToAny = {}
    for route in r["routes"]:
        for item in route["route"]:
            loc_id = item["node"]["value"]["id"]
            type = item["node"]["value"].get("type", "")
            if type == "drop_off":
                drop_off_pickups = []
                for pickup_id, delivery_to_any in pickupsWithDeliveryToAny.items():
                    if loc_id in delivery_to_any:
                        drop_off_pickups.append(pickup_id)
                assert len(drop_off_pickups) > 0
                for pickup in drop_off_pickups:
                    pickupsWithDeliveryToAny.pop(pickup)
            elif type == "pickup" and "delivery_to_any" in item["node"]["value"]:
                pickupsWithDeliveryToAny[loc_id] = item["node"]["value"]["delivery_to_any"]


def test_drop_off_with_load_types():
    """
    Test checks feasibility of generated solution
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("dropoff_load_types.json"), None,
        solver_arguments={'sa_iterations': 10000},
        expected_status='PARTIAL_SOLVED',
    )


def test_drop_off_no_return_to_depot():
    """
    Test that when vehicle is not returning to depot, drop offs are
    not removed by removeUnnecessaryDropOffs function
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("drop_off_no_return_to_depot.json"), None,
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED',
    )
