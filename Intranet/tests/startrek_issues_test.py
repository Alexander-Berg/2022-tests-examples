import pytest

from ids.exceptions import EmptyIteratorError
from mock import MagicMock

from staff.lib.startrek import issues


@pytest.fixture
def get_one(monkeypatch):
    mock_get_one = MagicMock()
    monkeypatch.setattr(issues.startrek_issues_repository, 'get_one', mock_get_one)

    return mock_get_one


def test_get_issue_simplest_case(get_one):
    get_one.return_value = 42
    assert 42 == issues.get_issue('test_key')


def test_get_issue_with_retry(get_one):
    get_one.side_effect = (EmptyIteratorError, 42)

    assert 42 == issues.get_issue('test_key')


@pytest.mark.parametrize(
    'key, expected_queue',
    [
        ('', None),
        ('A-', None),
        ('A1', None),
        ('a-1', None),
        ('A-1', 'A'),
        ('AB-12', 'AB'),
    ],
)
def test_get_issue_queue(key, expected_queue):
    assert issues.get_issue_queue(key) == expected_queue
