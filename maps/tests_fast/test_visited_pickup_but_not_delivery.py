import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_visited_pickup_but_not_delivery():
    """
    Test of a case when a pickup order is in visited locations, but
    the corresponding delivery order is not. There was an exception related to
    REMOVE_NODE mutation, which removed delivery location from a route together with
    visited (fixed) pickup location. See https://st.yandex-team.ru/BBGEO-2150.
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data('visited_pickup_but_not_delivery.json'),
        solver_arguments={'sa_iterations': 100000, 'sa_temperature': 5e6})
