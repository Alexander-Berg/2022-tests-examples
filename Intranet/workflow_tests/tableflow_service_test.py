import pytest

from staff.budget_position.workflow_service.entities import BonusSchemeIdRequest
from staff.budget_position.workflow_service.gateways import TableflowService


@pytest.mark.django_db
def test_tableflow_service_returns_review_id_on_correct_response(company):
    # given
    service = TableflowService()

    def returns_bonus_scheme_id_for_yandex(_, params):
        results = []
        for request in params['requests']:
            if request['department_url'] == company.yandex.url:
                results.append({'bonus_id': 100500})
            else:
                results.append(None)

        return {'results': results}

    service._post = returns_bonus_scheme_id_for_yandex

    # when
    result = service.bonus_scheme_id([
        BonusSchemeIdRequest(department_id=company.dep11.id, occupation_id='SOMEOCCUPATION', grade_level=16),
    ])

    # then
    assert len(result) == 1
    assert result[0] == 100500


@pytest.mark.django_db
def test_tableflow_service_returns_none_on_correct_response(company):
    # given
    service = TableflowService()

    def returns_bonus_scheme_id_for_yandex(_, params):
        return {}

    service._post = returns_bonus_scheme_id_for_yandex

    # when
    result = service.bonus_scheme_id([
        BonusSchemeIdRequest(department_id=company.dep11.id, occupation_id='SOMEOCCUPATION', grade_level=16),
    ])

    # then
    assert len(result) == 1
    assert result[0] is None


def _returns_department_chain(*args, **kwargs):
    return ['yandex', 'yandex_main_searchadv']


def test_tableflow_service_doesnt_make_requests_on_empty_input():
    # given
    service = TableflowService()

    # when
    result = service.review_scheme_id([])

    # then
    assert not result


def test_parse_results_can_work_without_priority_field():
    # given
    service = TableflowService()
    result_field = 'test_field'
    requests_count = 2
    response = {
        'results': [
            {result_field: 1},
            {result_field: 1},
            {result_field: 2},
            {result_field: 3},
        ]
    }
    response_to_request_idx_map = [0, 0, 1, 1]

    # when
    result = service._parse_response(response, result_field, response_to_request_idx_map, requests_count)

    # then
    assert result == [1, 3]


def test_parse_results_taking_into_account_priority_field():
    # given
    service = TableflowService()
    result_field = 'test_field'
    requests_count = 2
    response = {
        'results': [
            {result_field: 1, 'priority': 0},
            {result_field: 5, 'priority': 20},
            {result_field: 1, 'priority': 0},
            {result_field: 2, 'priority': 10},
            {result_field: 3, 'priority': 0},
        ]
    }
    response_to_request_idx_map = [0, 0, 0, 1, 1]

    # when
    result = service._parse_response(response, result_field, response_to_request_idx_map, requests_count)

    # then
    assert result == [5, 2]
