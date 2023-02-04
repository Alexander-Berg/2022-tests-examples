import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_drop_off_in_planned_route_only():
    """
    This test checks a request where a drop_off location is
    used in a planned_route, but is not referenced by any
    pickup order.
    """

    mvrp_checker.solve_and_check(
        tools.get_test_data("drop_off_in_planned_route_only.json"), None,
        solver_arguments={"sa_iterations": 1})
