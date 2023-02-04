import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_delivery_to_compatibility1():
    # in this test each delivery location has pickup_from_any attribute
    # all delivery locations are compatible with vehicles
    # pickup locations are mostly incompatible
    # Solver used to ignore compatibility of linked pickups, put everything in 1 car and
    # drop a lot of locations during postprocessing.
    # This tests ensures that we don't drop anything (status == SOLVED)
    mvrp_checker.solve_and_check(
        tools.get_test_data("incompatible_coupled_pickup1.json"), None,
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED"
    )


def test_delivery_to_compatibility2():
    # in this test we have 2 cars (cheap and expensive) and a delivery_to_any location with 2 pickup options
    # (far and near). In terms of cost it's optimal to use the cheap car and the near pickup option.
    # But the cheap car and the near option are incompatible by tags.
    # Solver used to ignore the incompatibility and drop delivery on postprocessing
    # This test ensures that solver doesn't do that anymore.
    mvrp_checker.solve_and_check(
        tools.get_test_data("incompatible_coupled_pickup2.json"), None,
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED"
    )
