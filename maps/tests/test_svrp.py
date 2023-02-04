import sys
sys.path.append("tests_lib")  # relative path to tools module
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_svrp100():
    """
    100 single VRP tasks from real courier business
    """
    tools.check_solver(
        tools.data_path('tests/svrp100'),
        tools.arcadia_path('tests_data/svrp100_metrics.json'),
        1000000,
        solver_runs=1,
        approx_compare=True)


def main():
    args = tools.parse_args()
    tools.check_solver(args.tests, args.metrics, args.iterations, args.update, args.solver_runs)


if __name__ == "__main__":
    main()
