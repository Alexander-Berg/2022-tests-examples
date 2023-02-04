import json
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.try_change_vehicles as try_change_vehicles


def test_vehicle_shift_selection():
    request = open(tools.data_path('data/vehicle_shift_selection_request.json')).read()
    distances = open(tools.data_path('data/vehicle_shift_selection_distances.json')).read()
    response = mvrp_checker.solve_and_check(
        request,
        distances,
        solver_arguments={'sa_iterations': 30000000}
    )
    cheaper_vehicles_found = try_change_vehicles.try_change_vehicles(json.loads(request), response)
    assert cheaper_vehicles_found == 0, ("%d cheaper vehicle(s) can be used" % cheaper_vehicles_found)
