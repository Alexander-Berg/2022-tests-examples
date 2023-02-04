import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def check_drops(task_path, drop_ids, drop_reasons=None):
    route = mvrp_checker.solve_and_check(
        open(tools.arcadia_path(task_path)).read(),
        solver_arguments={'sa_iterations': 51000},
        expected_status="PARTIAL_SOLVED")

    drops = route.get('dropped_locations', [])
    assert len(drops) == len(drop_ids)
    assert route['metrics']['dropped_locations_count'] == len(drop_ids)

    ids = set(loc["id"] for loc in drops)
    assert ids == set(drop_ids)

    for loc in drops:
        assert loc.get('drop_reason', "").strip() != ""
        assert "[{" not in loc.get('drop_reason', "").strip()

    if drop_reasons is not None:
        for loc in drops:
            assert drop_reasons[loc["id"]] == loc.get('drop_reason', "")


def test_hard_window_drop():
    """
        A delivery order id 101 has a narrow hard time window and solver cannot create
        a route to be on time there, so it must be dropped. Moreover an order 100 is
        a corresponding pickup for that delivery, so it must be dropped too.
    """
    check_drops('tests_data/hard_window_drop.json', [100, 101])


def test_drops_and_reasons():
    check_drops('tests_data/test_drops.json', [1, 3, 4, 5, 6, 7, 8, 9])


def test_drop_reasons_descriptions():
    check_drops('tests_data/drop_reasons.json', [0, 1, 2, 3], {
        0: "\nDrop reasons (for different vehicles):\n\t - Vehicle overload: 1 vehicles\n\t - Required tags missing: 1 vehicles",
        1: "\nDrop reasons (for different vehicles):\n\t - Not enough time to visit location (hard time window failed): 1 vehicles\n\t - Incompatible tags: 1 vehicles",
        2: "\nDrop reasons (for different vehicles):\n\t - Incompatible load types: 1 vehicles\n\t - Location is too far (route cost increase is higher than drop penalty): 1 vehicles",
        3: "\nDrop reasons (for different vehicles):\n\t - Not enough time to visit location (lateness penalty increase is higher than drop penalty): 2 vehicles",
    })


def test_max_drop_penalty_percentage():
    """
    It is not possible to visit all locations withing vehicle shifts, and drop penalties are low,
    so most of the locations are dropped. Adding limit for max_drop_penalty_percentage,
    allows to decrease number of dropped locations
    """

    request = tools.get_test_json("test_max_drop_penalty_percentage.json")
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="PARTIAL_SOLVED")
    assert response["metrics"]["dropped_locations_count"] > 5
    assert tools.is_abs_close(response["metrics"]["drop_penalty_percentage"],
                              response["metrics"]["dropped_locations_count"] / 14 * 100, 1e-3)

    request["options"]["max_drop_penalty_percentage"] = 40.0
    request["options"]["penalty"] = {
        "drop_penalty_percentage": {
            "fixed": 100000,
            "per_percent": 5000
        }
    }
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="PARTIAL_SOLVED")
    assert response["metrics"]["dropped_locations_count"] <= 5
    assert tools.is_abs_close(response["metrics"]["drop_penalty_percentage"],
                              response["metrics"]["dropped_locations_count"] / 14 * 100, 1e-3)
