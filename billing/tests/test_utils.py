from unittest.mock import patch, mock_open, call, MagicMock

from billing.datalens.utils.reports import (
    get_report_dir,
    get_report_meta_path,
    get_report_json_path,
    get_report_json,
    save_report_json,
    get_meta_json,
    save_meta_json,
    ensure_report_dir_exists,
)

from billing.datalens.utils.datalens import (
    HEADERS,
    DASH_API,
    CHART_API,
    get_report_from_datalens,
    get_chart_from_datalens,
    save_chart_to_file,
    get_report_datalens_path,
    get_chart_datalens_path,
)

#
# reports
#


def test_get_report_dir():
    assert get_report_dir("some-report") == "./reports/some-report"


def test_get_report_dir_with_changed_env(monkeypatch):
    monkeypatch.setenv("BILLING_CHART_REPORTS_DIR", "./some-new-place")
    assert get_report_dir("some-report") == "./some-new-place/some-report"


def test_get_report_json_path():
    assert get_report_json_path("some-report", "main-report") == "./reports/some-report/main-report.json"


def test_get_report_json_path_with_changed_env(monkeypatch):
    monkeypatch.setenv("BILLING_CHART_REPORTS_DIR", "./some-new-place")
    assert get_report_json_path("some-report", "main-report") == "./some-new-place/some-report/main-report.json"


def test_get_report_json_first_time():
    with patch('builtins.open', mock_open()) as m:
        m.side_effect = IOError()
        assert get_report_json("test-reports", "second") == dict(data=0)
        m.assert_called_once_with('./reports/test-reports/second.json')


def test_get_report_json_existing_file():
    with patch('builtins.open', mock_open(read_data='{"id": 1, "created": "today"}')) as m:
        assert get_report_json("test-reports", "second") == dict(id=1, created="today")
        m.assert_called_once_with('./reports/test-reports/second.json')


def test_get_report_meta_path():
    assert get_report_meta_path("some-report") == "./reports/some-report/meta.json"


def test_get_report_meta_path_with_changed_env(monkeypatch):
    monkeypatch.setenv("BILLING_CHART_REPORTS_DIR", "./some-new-place")
    assert get_report_meta_path("some-report") == "./some-new-place/some-report/meta.json"


def test_save_report_json():
    with patch('builtins.open', mock_open()) as m:
        expected = './reports/test-reports/report.json'
        assert save_report_json("test-reports", dict(id=2, created="tomorrow")) == expected
        m.assert_called_once_with(expected, 'w')
        handle = m()
        handle.write.assert_called()
        calls = [
            call('{'),
            call('\n    '),
            call('"created"'),
            call(': '),
            call('"tomorrow"'),
            call(',\n    '),
            call('"id"'),
            call(': '),
            call('2'),
            call('\n'),
            call('}'),
        ]
        assert handle.write.mock_calls == calls


def test_save_report_json_with_custom_name():
    with patch('builtins.open', mock_open()) as m:
        expected = './reports/test-reports/xxx.json'
        assert save_report_json("test-reports", dict(id=2, created="tomorrow"), 'xxx') == expected
        m.assert_called_once_with(expected, 'w')
        handle = m()
        handle.write.assert_called()
        calls = [
            call('{'),
            call('\n    '),
            call('"created"'),
            call(': '),
            call('"tomorrow"'),
            call(',\n    '),
            call('"id"'),
            call(': '),
            call('2'),
            call('\n'),
            call('}'),
        ]
        assert handle.write.mock_calls == calls


def test_get_meta_json_first_time():
    with patch('builtins.open', mock_open()) as m:
        m.side_effect = IOError()
        assert get_meta_json("test-reports") == dict(dash=dict(dev=0, test=0, prod=0), charts={})
        m.assert_called_once_with('./reports/test-reports/meta.json')


def test_get_meta_json_existing_file():
    with patch('builtins.open', mock_open(read_data='{"id": 1, "created": "today"}')) as m:
        assert get_meta_json("test-reports") == dict(id=1, created="today")
        m.assert_called_once_with('./reports/test-reports/meta.json')


def test_save_meta_json():
    with patch('builtins.open', mock_open()) as m:
        expected = './reports/test-reports/meta.json'
        assert save_meta_json("test-reports", dict(id=1, created="today")) == expected
        m.assert_called_once_with(expected, 'w')
        handle = m()
        handle.write.assert_called()
        calls = [
            call('{'),
            call('\n    '),
            call('"id"'),
            call(': '),
            call('1'),
            call(',\n    '),
            call('"created"'),
            call(': '),
            call('"today"'),
            call('\n'),
            call('}'),
        ]
        assert handle.write.mock_calls == calls


def test_ensure_report_dir_exists():
    with patch('pathlib.Path.mkdir') as mkdir_mock:
        expected_path = './reports/test-report'
        assert ensure_report_dir_exists('test-report') == expected_path
        mkdir_mock.assert_called_once_with(parents=True, exist_ok=True)


#
# datalens
#

def prepare_request_mock(base: str, id: str) -> (str, dict, MagicMock):
    """
    Подготавливает мок для реквеста, возвращает его и данные, которые надо проверить
    """
    expected_url = f'{base}/{id}'
    expected_json = dict(tested=1)
    response_mock = MagicMock()
    response_mock.json = MagicMock(return_value=expected_json)
    return expected_url, expected_json, response_mock


def test_get_report_from_datalens():
    report_id = '123456666'
    expected_url, expected_json, response_mock = prepare_request_mock(DASH_API, report_id)
    with patch('requests.get', return_value=response_mock, create=True) as get_mock:
        assert get_report_from_datalens(report_id) == expected_json
        get_mock.assert_called_once_with(expected_url, headers=HEADERS)
        response_mock.raise_for_status.assert_called_once_with()
        response_mock.json.assert_called_once_with()


def test_get_chart_from_datalens():
    chart_id = '998877'
    expected_url, expected_json, response_mock = prepare_request_mock(CHART_API, chart_id)
    with patch('requests.get', return_value=response_mock, create=True) as get_mock:
        assert get_chart_from_datalens(chart_id) == expected_json
        get_mock.assert_called_once_with(expected_url, headers=HEADERS)
        response_mock.raise_for_status.assert_called_once_with()
        response_mock.json.assert_called_once_with()


def test_save_chart_to_file():
    chart_id = '123'
    chart_key = '/chart/key'
    chart_body = dict(key=chart_key)
    report_name = 'super-report'
    path = 'billing.datalens.utils.datalens'
    with patch(f'{path}.get_chart_from_datalens', return_value=chart_body) as get_chart_mock, \
            patch(f'{path}.save_report_json') as save_mock:
        assert save_chart_to_file(chart_id, report_name, False) == chart_key
        get_chart_mock.assert_called_once_with(chart_id)
        save_mock.assert_called_once_with(report_name, chart_body, chart_id)


def test_get_report_datalens_path():
    expected = 'balance-reports/some-env/some-report'
    assert get_report_datalens_path('some-report', 'some-env') == expected


def test_get_chart_datalens_path():
    expected = 'balance-reports/some-env/some-report/chart-name'
    assert get_chart_datalens_path('some-report', 'some-env', 'chart-name') == expected
