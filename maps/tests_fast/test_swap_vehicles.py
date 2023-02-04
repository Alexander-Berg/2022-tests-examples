import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools

import json


def test_swap_vehicles():
    """
    Longer route (in the west) should be taken by the second vehicle,
    because it has longer shift. However, there was a chance (probably about 50%)
    that solver would stack in local minimum with vehicles taking wrong routes,
    and it would not be possible to swap them in small steps,
    because locs in different routes will have incompatible load types.
    Check that mutation SWAP_VEHICLES solves this problem
    """
    request = tools.get_test_json("swap_vehicles.json")

    for rand_seed in range(10):
        request["options"]["rand_seed"] = rand_seed
        response = mvrp_checker.solve_and_check(
            json.dumps(request),
            solver_arguments={'sa_iterations': 10000},
            fixed_rand_seed=True)

        assert response["metrics"]["total_late_duration_s"] == 0
        assert response["metrics"]["total_early_duration_s"] == 0

        expected_vehicles = {
            "852244f3-5afb-0a5a-3ade-9337497808a3": "А615МО 154",
            "dce74adc-083e-2449-ea4f-d9a5b678fcb1": "К922КГ54"
        }
        for route in response["routes"]:
            for loc in route["route"]:
                if loc["node"]["value"]["id"] in expected_vehicles:
                    assert route["vehicle_id"] == expected_vehicles[loc["node"]["value"]["id"]]


def test_swap_vehicles_multiple_shifts_and_runs():
    """
    Test that SWAP_VEHICLES mutation works correctly when there are
    multiple shifts and multiple runs in shifts
    """
    request = tools.get_test_json("swap_vehicles.json")
    for vehicle in request["vehicles"]:
        vehicle.pop("max_runs")
        vehicle["shifts"] = [
            {
                "hard_window": False,
                "id": "1",
                "max_duration_s": 68400,
                "time_window": "10:00:00-1.00:00:00",
                "max_runs": 3
            },
            {
                "hard_window": False,
                "id": "2",
                "max_duration_s": 68400,
                "time_window": "1.00:00:00-3.00:00:00",
                "max_runs": 3
            }
        ]

    mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000})
