import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_compatibility():
    """
    checks vehicle/location compatibility using tag regexps
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("tag_regexps.json"),
        solver_arguments={'sa_iterations': 100000},
        expected_status="SOLVED"
    )
