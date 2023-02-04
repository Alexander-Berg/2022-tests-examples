import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_xdock_zones_restriction():
    # in this test the location is incompatible with the depot->xdock vehicle by zones
    # which is ok, but solver used to check zones restriction for depot->xdock vehicles
    # This tests ensures that we don't drop anything (status == SOLVED)
    mvrp_checker.solve_and_check(
        tools.get_test_data("xdock_zones_restriction.json"), None,
        solver_arguments={'sa_iterations': 1000},
        expected_status="SOLVED"
    )
