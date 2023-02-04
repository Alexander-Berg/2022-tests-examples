import vrp_solver.schema


def test_smoke():
    assert vrp_solver.schema.solver_options_mvrp['properties']['solver_time_limit_s']['default'] == 1 * 60
    assert vrp_solver.schema.solver_options_svrp['properties']['solver_time_limit_s']['default'] == 1
