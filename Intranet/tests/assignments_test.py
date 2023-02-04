import pytest
import mock

from itertools import permutations
from requests.exceptions import Timeout, ConnectTimeout, ReadTimeout

from staff.oebs.utils import get_oebs_assignments_by_person_logins, _sort_person_assignments


@pytest.mark.parametrize('exc', (Timeout, ConnectTimeout, ReadTimeout))
@mock.patch('staff.oebs.utils.get_tvm_ticket_by_deploy')
def test_get_oebs_assignments_not_fail_on_timeout(get_tvm_ticket_by_deploy_patch, exc):
    with mock.patch(
        'staff.lib.requests.Session.post',
        mock.Mock(side_effect=exc)
    ):
        assert get_oebs_assignments_by_person_logins(['login1', 'login2']) == {}


@mock.patch('staff.oebs.utils.get_tvm_ticket_by_deploy')
def test_get_oebs_assignments_not_fail_on_any_exception(get_tvm_ticket_by_deploy_patch):
    with mock.patch(
        'staff.lib.requests.Session.post',
        mock.Mock(side_effect=Exception)
    ):
        assert get_oebs_assignments_by_person_logins(['login1', 'login2']) == {}


@mock.patch('staff.oebs.utils.get_tvm_ticket_by_deploy')
def test_get_oebs_assignments_not_fail_on_wrong_status_code(get_tvm_ticket_by_deploy_patch):
    with mock.patch(
        'staff.lib.requests.Session.post',
        return_value=mock.Mock(status_code=5030)
    ):
        assert get_oebs_assignments_by_person_logins(['login1', 'login2']) == {}


@mock.patch('staff.oebs.utils.get_tvm_ticket_by_deploy')
def test_get_oebs_assignments_not_fail_on_empty_response(get_tvm_ticket_by_deploy_patch):
    with mock.patch(
        'staff.lib.requests.Session.post',
        return_value=mock.Mock(status_code=200, content='')
    ):
        assert get_oebs_assignments_by_person_logins(['login1', 'login2']) == {}


def test_assignments_sorting():
    assignment_list = [
        {
            'assignmentID': 4,
            'legislationCode': 'UA',
            'primaryFlag': 'N',
            'taxUnitID': 121,
            'taxUnitName': 'ООО Яндекс',
            'orgID': 77651,
            'orgName': 'Группа разработки Стаффа и Фемиды',
            'contractType': 'Основное место работы',
        },
        {
            'assignmentID': 2,
            'legislationCode': 'EN',
            'primaryFlag': 'Y',
            'taxUnitID': 121,
            'taxUnitName': 'ООО Тындекс',
            'orgID': 77652,
            'orgName': 'Группа разработки Стаффа и Фемиды',
            'contractType': 'Основное место работы',
        },
        {
            'assignmentID': 1,
            'legislationCode': 'RU',
            'primaryFlag': 'Y',
            'taxUnitID': 121,
            'taxUnitName': 'ООО Яндекс',
            'orgID': 77655,
            'orgName': 'Группа разработки Стаффа и Фемиды',
            'contractType': 'Основное место работы',
        },
        {
            'assignmentID': 3,
            'legislationCode': 'RU',
            'primaryFlag': 'X',
            'taxUnitID': 121,
            'taxUnitName': 'ООО Яндекс',
            'orgID': 77657,
            'orgName': 'Группа разработки Стаффа и Фемиды',
            'contractType': 'Основное место работы',
        },
    ]
    for assignments in permutations(assignment_list):
        assignments = list(assignments)
        _sort_person_assignments(assignments)
        assert [1, 2, 3, 4] == [a['assignmentID'] for a in assignments]
