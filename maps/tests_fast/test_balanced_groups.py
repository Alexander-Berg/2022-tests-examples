import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import collections


"""
Checks balanced route groups.

Test data:
 - 9 locations;
 - 3 identical vehicles with almost zero fixed cost;
 - each vehicle has 3 shifts with hard windows;
 - each shift corresponds to a balanced group: morning=>bgroup1, day=>bgroup2, evening=>bgroup3;
 - penalties for imbalanced routes are high.
"""


def test_balanced_groups():
    data = tools.get_test_data("balanced_groups.json")
    result = mvrp_checker.solve_and_check(data, solver_arguments={'sa_iterations': 100000})

    assert result['metrics']['used_vehicles'] == 3
    assert result['metrics']['number_of_routes'] == 9
    assert result['metrics']['balanced_group_penalty'] < 36000

    groups = collections.defaultdict(list)
    for route in result['routes']:
        groups[route['shift']['balanced_group_id']].append(len(route['route']))

    for counts in groups.values():
        assert max(counts) == min(counts)
