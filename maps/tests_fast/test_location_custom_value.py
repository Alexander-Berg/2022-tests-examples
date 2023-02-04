import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_location_custom_value():
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('location_custom_value.json'),
        solver_arguments={'sa_iterations': 50000},
        expected_status="SOLVED"
    )

    # in this test each location has a `custom_value` field
    # vehicle cost function is "1000 + 10 * distance_km + 5 * total_custom_value ^ 3"
    # `total_custom_value` is in range [11, 29], which means it's the biggest cost component
    # which all means that it's optimal to use all the vehicles and
    # `total_custom_value` should be nearly the same for all routes

    assert len(result['dropped_locations']) == 0
    assert len(result['routes']) == 5

    for i in range(5):
        assert len(result['routes'][i]['route']) == 4  # depot, order, order, depot
        custom_sum = 0
        for j in range(2):
            custom_sum += result['routes'][i]['route'][j + 1]['node']['value']['custom_value']
        # in the ideal solution each route should have `total_custom_value` == 40
        # but we respect the random and leave some error margin
        assert 38 <= custom_sum <= 42
