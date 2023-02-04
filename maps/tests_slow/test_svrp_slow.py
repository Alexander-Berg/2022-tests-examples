import sys
sys.path.append("tests_lib")  # relative path to tools module
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import pytest


def test_svrp1000():
    """
    1000 single VRP tasks from real courier business
    """
    tools.check_solver(tools.data_path('tests/svrp1000'),
                       tools.arcadia_path('tests_data/svrp1000_metrics.json'),
                       1000000,
                       solver_runs=1,
                       approx_compare=True)


@pytest.mark.skip(reason="too slow")
def test_svrp2500():
    """
    2500 single VRP tasks from real courier business
    """
    tools.check_solver(tools.data_path('tests/svrp2500'),
                       tools.arcadia_path('tests_data/svrp2500_metrics.json'),
                       1000000,
                       solver_runs=1,
                       approx_compare=True)


def main():
    args = tools.parse_args()
    tools.check_solver(args.tests, args.metrics, args.iterations, args.update)


if __name__ == "__main__":
    main()
