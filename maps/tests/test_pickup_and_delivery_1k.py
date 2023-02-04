import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_pickup_and_delivery_1k():
    """
    This test includes 1k locations and 50 vehicles, each vehicle has 2 shifts.
    The idea is to checks mutations, which move orders from one vehicle (or shift) to another.
    A special thing about pickup-and-delivery orders is that both (pickup and delivery) orders
    must be in the same vehicle and in the same shift, therefore mutations should move them together,
    or somehow achieve it in the end of optimization process.
    """
    mvrp_checker.solve_and_check(
        open(tools.data_path("data/pickup_and_delivery_1k.json")).read(), None,
        solver_arguments={'sa_iterations': 100000})
