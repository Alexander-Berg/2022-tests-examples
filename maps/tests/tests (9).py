from maps.wikimap.stat.tasks_payment.dictionaries.holidays.lib.holidays import (
    _get_records,
    get_holidays,
)

from datetime import date
import json
import pytest


def test_should_get_records():
    assert _get_records(
        json.loads(
            '['
            '  {"login": "login 1", "holiday": []},'
            '  {"login": "login 2", "holiday": ["2022-01-24"]},'
            '  {"login": "login 3", "holiday": ["2022-01-25"]},'
            '  {"login": "login 4", "holiday": ["2022-01-26"]},'
            '  {"login": "login 5", "holiday": ["2022-01-24", "2022-01-25"]},'
            '  {"login": "login 6", "holiday": ["2022-01-25", "2022-01-26"]},'
            '  {"login": "login 7", "holiday": ["2022-01-24", "2022-01-26"]},'
            '  {"login": "login 8", "holiday": ["2022-01-24", "2022-01-25", "2022-01-26"]}'
            ']'
        ),
        '2022-01-25'
    ) == [
        {'staff_login': 'login 3', 'date': '2022-01-25'},
        {'staff_login': 'login 5', 'date': '2022-01-25'},
        {'staff_login': 'login 6', 'date': '2022-01-25'},
        {'staff_login': 'login 8', 'date': '2022-01-25'},
    ]

    assert _get_records(json.loads('[]'), '2022-01-25') == []


def setup_oebs_api_holiday_mock(monkeypatch, exp_env, exp_client_id, exp_secret, exp_logins, exp_date_from, exp_date_to, response):
    def holiday_mock(got_env, got_client_id, got_secret, got_logins, got_date_from, got_date_to):
        assert got_env == exp_env
        assert got_client_id == exp_client_id
        assert got_secret == exp_secret
        assert got_logins == exp_logins
        assert got_date_from.isoformat() == exp_date_from
        assert got_date_to.isoformat() == exp_date_to
        return json.loads(response)

    monkeypatch.setattr(
        'maps.wikimap.stat.tasks_payment.dictionaries.holidays.lib.holidays.oebs_api.holiday',
        holiday_mock
    )


def test_should_fail_get_holidays_on_wrong_response(monkeypatch):
    setup_oebs_api_holiday_mock(
        monkeypatch,
        'env', 'client_id', 'tvm_secret', ['login'], '2022-01-24', '2022-01-25',
        '{"test_field": 42}'
    )

    with pytest.raises(AssertionError, match='Cannot retrieve information about holidays from OEBS.'):
        get_holidays('env', 'client_id', 'tvm_secret', ['login'], date(2022, 1, 25))


def test_should_get_holidays(monkeypatch):
    setup_oebs_api_holiday_mock(
        monkeypatch,
        'env', 'client_id', 'tvm_secret', ['Olga', 'Tatyana', 'Natalya'], '2022-01-24', '2022-01-25',
        '{'
        '  "logins": ['
        '    {"login": "Olga",    "holiday": [],             "total": 0},'
        '    {"login": "Tatyana", "holiday": ["2022-01-25"], "total": 1},'
        '    {"login": "Natalya", "holiday": [],             "total": 0}'
        '  ],'
        '  "total_all": 1'
        '}'
    )

    result = get_holidays('env', 'client_id', 'tvm_secret', ['Olga', 'Tatyana', 'Natalya'], date(2022, 1, 25))
    assert result == [
        {'staff_login': 'Tatyana', 'date': '2022-01-25'}
    ]
