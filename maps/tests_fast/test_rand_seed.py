import json
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


DATA = {
    "depot": {
        "id": 10000,
        "point": {"lon": 37.729377, "lat": 55.799087},
        "time_window": "07:00-23:59"
    },
    "options": {
        "rand_seed": 123,
        "time_zone": 3.0
    },
    "vehicles": [{"id": 0}],
    "locations": [
        {
            "id": 2,
            "point": {"lon": 37.722388, "lat": 55.780846},
            "time_window": "10:00-15:00",
        }
    ]
}


def test_rand_seed_output():
    """
    Run solver and check options.rand_seed appears in output options
    """
    route = mvrp_checker.solve_and_check(json.dumps(DATA),
                                         solver_arguments={'sa_iterations': 1000},
                                         fixed_rand_seed=True)
    assert(route["options"]["rand_seed"] == DATA["options"]["rand_seed"])


def test_rand_seed_different_solutions():
    """
    """
    with open(tools.arcadia_path('tests_data/10_locs.json'), 'r') as svrp100:
        buff = svrp100.read()
        task1 = json.loads(buff)
        task2 = json.loads(buff)

    task1["options"]["rand_seed"] = 0
    task2["options"]["rand_seed"] = 42

    route1 = mvrp_checker.solve_and_check(json.dumps(task1),
                                          solver_arguments={'sa_iterations': 1000},
                                          fixed_rand_seed=True)
    route2 = mvrp_checker.solve_and_check(json.dumps(task2),
                                          solver_arguments={'sa_iterations': 1000},
                                          fixed_rand_seed=True)

    assert route1["metrics"]["total_duration_s"] != route2["metrics"]["total_duration_s"]
