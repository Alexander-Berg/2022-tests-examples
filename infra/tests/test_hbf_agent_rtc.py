import unittest.mock as mock
import time


class MockResponse(object):
    def __init__(self, json_data, status_code):
        self.json_data = json_data
        self.status_code = status_code

    def json(self):
        return self.json_data


def ok_mock_requests_get(*args, **kwargs):
    last_update = time.time() - 10
    return MockResponse({"status": "OK", "last_update": last_update, "desc": ""}, 200)


def warn_mock_requests_get(*args, **kwargs):
    last_update = time.time() - 666
    return MockResponse({"status": "WARN", "last_update": last_update, "desc": "warn message"}, 200)


def crit_mock_requests_get(*args, **kwargs):
    last_update = time.time() - 666
    return MockResponse({"status": "CRIT", "last_update": last_update, "desc": "crit message"}, 200)


def disabled_mock_requests_get(*args, **kwargs):
    last_update = time.time() - 666
    return MockResponse({"status": "DISABLED", "last_update": last_update, "desc": "disabled via something"}, 200)


def unanswer_mock_requests_get(*args, **kwargs):
    return MockResponse({}, 503)


def too_old_mock_requests_get(*args, **kwargs):
    last_update = time.time() - 666
    return MockResponse({"status": "OK", "last_update": last_update, "desc": ""}, 200)


def requests_raises(*args, **kwargs):
    raise Exception('Connection timeout')


def test_ok(manifest):
    ok_data = {"events": [{"description": "OK", "service": "hbf_agent_rtc", "status": "OK"}]}
    with mock.patch('requests.get', side_effect=ok_mock_requests_get):
        result = manifest.execute('hbf_agent_rtc')
    assert result == ok_data


def test_warn(manifest):
    warn_data = {"events": [{"description": "HBF status is: WARN, warn message", "service": "hbf_agent_rtc", "status": "WARN"}]}
    with mock.patch('requests.get', side_effect=warn_mock_requests_get):
        result = manifest.execute('hbf_agent_rtc')
    assert result == warn_data


def test_crit(manifest):
    crit_data = {"events": [{"description": "HBF status is: CRIT, crit message", "service": "hbf_agent_rtc", "status": "CRIT"}]}
    with mock.patch('requests.get', side_effect=crit_mock_requests_get):
        result = manifest.execute('hbf_agent_rtc')
    assert result == crit_data


def test_disabled(manifest):
    disabled_data = {"events": [{
        "description": "HBF status is: DISABLED, disabled via something", "service": "hbf_agent_rtc", "status": "CRIT"
    }]}
    with mock.patch('requests.get', side_effect=disabled_mock_requests_get):
        result = manifest.execute('hbf_agent_rtc')
    assert result == disabled_data


def test_unanswer_w_code(manifest):
    unanswer_data = {"events": [{"description": "HBF status answers with: 503", "service": "hbf_agent_rtc", "status": "CRIT"}]}
    with mock.patch('requests.get', side_effect=unanswer_mock_requests_get):
        result = manifest.execute('hbf_agent_rtc')
    assert result == unanswer_data


def test_connection_exception_porto_not_running(manifest):
    unanswer_porto_fail_data = {"events": [{
        "description": "yandex-hbf-agent porto container is not running", "service": "hbf_agent_rtc", "status": "CRIT"
    }]}
    with mock.patch('requests.get', side_effect=requests_raises):
        with mock.patch('infra.rtc.juggler.bundle.checks.hbf_agent_rtc.JugglerHBFCheck.check_is_hbf_running', return_value=False):
            result = manifest.execute('hbf_agent_rtc')
    assert result == unanswer_porto_fail_data


def test_connection_exception_porto_is_running(manifest):
    unanswer_porto_fail_data = {"events": [{
        "description": "Could not get status from hbf: Connection timeout", "service": "hbf_agent_rtc", "status": "CRIT"
    }]}
    with mock.patch('requests.get', side_effect=requests_raises):
        with mock.patch('infra.rtc.juggler.bundle.checks.hbf_agent_rtc.JugglerHBFCheck.check_is_hbf_running', return_value=True):
            result = manifest.execute('hbf_agent_rtc')
    assert result == unanswer_porto_fail_data


def test_too_old_state(manifest):
    too_old_data = {"events": [
        {"description": "HBF last update timestamp is too old", "service": "hbf_agent_rtc", "status": "CRIT"}
    ]}
    with mock.patch('requests.get', side_effect=too_old_mock_requests_get):
        result = manifest.execute('hbf_agent_rtc')
    assert result == too_old_data
