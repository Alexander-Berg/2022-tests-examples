import json

import pytest

import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


DEFAULT_SEQUENCE = ['depot', 'early_window', 'late_window', 'depot']


def load_request(penalize_late_service, strip_shifts):
    request = tools.get_test_json('penalize_late_service.json')
    if penalize_late_service is not None:
        request['options']['penalize_late_service'] = penalize_late_service
    if strip_shifts:
        del request['vehicle']['shifts']
    return request


@pytest.mark.parametrize('penalize_late_service', [None, True, False])
@pytest.mark.parametrize('service', [True, False])
def test_locations(penalize_late_service, service):
    request = load_request(penalize_late_service, strip_shifts=True)
    if service:
        request['locations'][1]['service_duration_s'] = 3600
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        kind='svrp')
    if penalize_late_service and service:
        expected_route = ['depot', 'late_window', 'early_window', 'depot']
    else:
        expected_route = DEFAULT_SEQUENCE
    tools.check_svrp_route(response, expected_route)


@pytest.mark.parametrize('penalize_late_service', [None, True, False])
@pytest.mark.parametrize('service', [True, False])
def test_depots(penalize_late_service, service):
    request = load_request(penalize_late_service, strip_shifts=True)
    if service:
        request['depot']['finish_service_duration_s'] = 7200
    if penalize_late_service and service:
        expected_route = ['depot', 'late_window', 'depot']
        expected_status = 'PARTIAL_SOLVED'
    else:
        expected_route = DEFAULT_SEQUENCE
        expected_status = 'SOLVED'
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        kind='svrp',
        expected_status=expected_status)
    tools.check_svrp_route(response, expected_route)


@pytest.mark.parametrize('penalize_late_service', [None, True, False])
@pytest.mark.parametrize('service', [True, False])
def test_shifts(penalize_late_service, service):
    request = load_request(penalize_late_service, strip_shifts=False)
    if service:
        request['vehicle']['shifts'][0]['service_duration_s'] = 1800
    if penalize_late_service and service:
        # 'late_window' location is closer to the depot
        # for this reason we can visit it during the first shift to fit into shift hard window
        expected_route = ['depot', 'late_window', 'depot', 'depot', 'early_window', 'depot']
    else:
        # but generally it is better to visit 'early_window' during the first shift
        # to minimize the window violation penalties
        expected_route = ['depot', 'early_window', 'depot', 'depot', 'late_window', 'depot']
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        kind='svrp')
    tools.check_svrp_route(response, expected_route)
