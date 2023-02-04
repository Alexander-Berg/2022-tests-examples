import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_transport_work_cost():
    """
    Without transport work cost it is more favorable to visit location 1 first, because otherwise
    time window is violated. With transport work cost, cost increase is bigger than lateness penalty,
    and location 0 is visited first.
    """
    data = tools.get_test_json("test_transport_work_cost.json")
    response = mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 10000})

    assert response["metrics"]["total_late_count"] == 1

    location_ids = []
    for route_info in response['routes']:
        for location_info in route_info["route"]:
            if location_info["node"]["type"] != "depot":
                location_ids.append(location_info["node"]["value"]["id"])
    assert location_ids == [0, 1]
