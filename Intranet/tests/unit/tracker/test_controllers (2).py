import pytest

from ok.tracker.controllers import IssueController


ISSUE_KEY = 'TEST-1'


@pytest.mark.parametrize('text', (
    'ok', 'okey', 'okay', 'ook', 'ок', 'оок', 'окей', 'окай', 'окс', 'оки',
    'подтверждаю', 'да', 'согласовано', 'подтверждаю, спасибо', 'да, спасибо', 'спасибо да',
    'спасибо. подтверждаю', 'ок. спасибо.', 'да, спасибо, подтверждаю!', 'да,ок', 'да-да'
))
def test_is_ok(text):
    assert IssueController(ISSUE_KEY).is_ok(text)


@pytest.mark.parametrize('text', (
    '', 'спасибо', 'спасибо!', '-', 'не ок', 'не подтверждаю', 'я',
))
def test_is_not_ok(text):
    assert not IssueController(ISSUE_KEY).is_ok(text)
