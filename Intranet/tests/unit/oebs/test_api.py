import pytest

from unittest.mock import patch

from intranet.femida.src.oebs.api import OebsAPI, BudgetPositionError


@pytest.mark.parametrize('oebs_answer, result', (
    (
        {'error': 'Персона не найдена', 'full_name': '', 'login': '', 'person_id': ''},
        None,
    ),
    (
        {'error': '', 'full_name': 'Имен Фамильевич', 'login': 'familevich', 'person_id': '12345'},
        'familevich',
    ),
))
def test_get_login_by_document(oebs_answer, result):
    with patch('intranet.femida.src.oebs.api.OebsAPI._post', return_value=oebs_answer):
        login = OebsAPI.get_login_by_document('', '')
    assert login == result


def test_get_login_by_document_failure():
    oebs_bad_answer = {'error': 'Some OEBS error', 'full_name': '', 'login': '', 'person_id': ''}
    with patch('intranet.femida.src.oebs.api.OebsAPI._post', return_value=oebs_bad_answer):
        with pytest.raises(BudgetPositionError):
            OebsAPI.get_login_by_document('', '')
