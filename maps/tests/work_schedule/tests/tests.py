from maps.wikimap.stat.tasks_payment.dictionaries.work_schedule.lib.work_schedule import (
    _SCHEDULE_STD,
    _SCHEDULE_SHIFTED,
    _SCHEDULE_UNKNOWN,
    _get_schedule_type,
    get_login_to_schedule_list,
)

from datetime import date
import json
import pytest


def test_should_get_schedule_type():
    assert _get_schedule_type(
        'login',
        json.loads('{"isNonStdSchedule": {"name": "Y"}, "login": "login", "test-field": "test-value"}')
    ) == _SCHEDULE_SHIFTED

    assert _get_schedule_type(
        'login',
        json.loads('{"isNonStdSchedule": {"name": "N"}, "login": "login", "test-field": "test-value"}')
    ) == _SCHEDULE_STD

    assert _get_schedule_type(
        'login',
        json.loads('{"login": "login", "test-field": "test-value"}')
    ) == _SCHEDULE_UNKNOWN


def test_should_fail_getting_unsupported_schedule_type():
    with pytest.raises(RuntimeError, match='Unsupported isNonStdSchedule value "wrong-value"'):
        _get_schedule_type(
            'login',
            json.loads('{"isNonStdSchedule": {"name": "wrong-value"}, "login": "login"}')
        )


def test_should_fail_getting_schedule_type_with_wrong_login():
    with pytest.raises(AssertionError, match='No "login" field in assignment'):
        _get_schedule_type(
            'login',
            json.loads('{"isNonStdSchedule": {"name": "Y"}}')
        )

    with pytest.raises(AssertionError, match='Unexpected login "other-login" in assignment for "login".'):
        _get_schedule_type(
            'login',
            json.loads('{"isNonStdSchedule": {"name": "Y"}, "login": "other-login"}')
        )


def setup_primary_assignments_mock(monkeypatch, exp_env, exp_client_id, exp_secret, exp_date, schedule_flag):
    def primary_assignments_mock(got_env, got_client_id, got_secret, got_logins, got_date):
        assert got_env == exp_env
        assert got_client_id == exp_client_id
        assert got_secret == exp_secret
        assert got_date.isoformat() == exp_date
        assert len(got_logins) == 1
        return json.loads(
            '{'
            '  "logins": ['
            '    {'
            '      "login": "' + got_logins[0] + '",'
            '      "isNonStdSchedule": {"name": "' + schedule_flag + '"}'
            '    }'
            '  ]'
            '}'
        )

    monkeypatch.setattr(
        'maps.wikimap.stat.tasks_payment.dictionaries.work_schedule.lib.work_schedule.oebs_api.primary_assignments',
        primary_assignments_mock
    )


def setup_empty_primary_assignments_mock(monkeypatch):
    monkeypatch.setattr(
        'maps.wikimap.stat.tasks_payment.dictionaries.work_schedule.lib.work_schedule.oebs_api.primary_assignments',
        lambda env, client_id, secret, logins, date: json.loads('{}')
    )


def test_should_get_login_to_schedule_list(monkeypatch):
    setup_primary_assignments_mock(monkeypatch, 'env', 'client_id', 'tvm_secret', '2022-01-25', 'Y')
    result = get_login_to_schedule_list('env', 'client_id', 'tvm_secret', ['login 1', 'login 2'], date(2022, 1, 25))
    assert result == [
        {'staff_login': 'login 1', 'schedule': 'shifted'}, {'staff_login': 'login 2', 'schedule': 'shifted'}
    ]

    setup_primary_assignments_mock(monkeypatch, 'env', 'client_id', 'tvm_secret', '2022-01-25', 'N')
    result = get_login_to_schedule_list('env', 'client_id', 'tvm_secret', ['login 3'], date(2022, 1, 25))
    assert result == [
        {'staff_login': 'login 3', 'schedule': 'std'}
    ]

    setup_empty_primary_assignments_mock(monkeypatch)
    result = get_login_to_schedule_list('env', 'client_id', 'tvm_secret', ['login 4'], date(2022, 1, 25))
    assert result == [
        {'staff_login': 'login 4', 'schedule': 'unknown'}
    ]
