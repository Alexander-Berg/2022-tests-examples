import json

import pytest
from checks import walle_clocksource, common

from juggler.bundles import Status, Event, CheckResult
from .tools import monkeypatch_hw_watcher_call, mock_hw_watcher_report, time, mocked_timestamp

from utils import make_canonization

WALLE_CLOCKSOURCE = "walle_clocksource"


@pytest.fixture(autouse=True)
def patch_time(monkeypatch):
    monkeypatch.setattr(common, "time", time)
    monkeypatch.setattr(time, "time", mocked_timestamp)


def expected_result(expected_status, expected_metadata):
    return CheckResult([
        Event(expected_status, json.dumps(expected_metadata))
    ]).to_dict(service=WALLE_CLOCKSOURCE)


def test_ok_when_hw_watcher_status_is_ok(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(reason=["Last check: Fri Jul 04 17:06:11 2018"], status="OK")
    monkeypatch_hw_watcher_call(monkeypatch, walle_clocksource, clock=hw_watcher_report)

    check_result = manifest.execute(WALLE_CLOCKSOURCE)
    expected_metadata = {
        "reason": "Last check: Fri Jul 04 17:06:11 2018",
        "timestamp": hw_watcher_report["timestamp"],
        "status": "OK",
    }

    return make_canonization(check_result, expected_result(Status.OK, expected_metadata))


def test_crit_when_hw_watcher_status_is_failed(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(reason=["bios: unstable hpet clocksource"], status="FAILED")
    monkeypatch_hw_watcher_call(monkeypatch, walle_clocksource, clock=hw_watcher_report)

    check_result = manifest.execute(WALLE_CLOCKSOURCE)

    expected_metadata = {
        "reason": "bios: unstable hpet clocksource",
        "timestamp": hw_watcher_report["timestamp"],
        "status": "FAILED",
    }

    return make_canonization(check_result, expected_result(Status.CRIT, expected_metadata))


def test_warn_when_hw_watcher_status_is_warn(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(reason=["bios: unstable hpet clocksource"], status="WARNING")
    monkeypatch_hw_watcher_call(monkeypatch, walle_clocksource, clock=hw_watcher_report)

    check_result = manifest.execute(WALLE_CLOCKSOURCE)
    expected_metadata = {
        "reason": "bios: unstable hpet clocksource",
        "timestamp": hw_watcher_report["timestamp"],
        "status": "WARNING",
    }

    return make_canonization(check_result, expected_result(Status.WARN, expected_metadata))


def test_warn_when_hw_watcher_status_is_not_supported(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(reason=["idk"], status="NOT-AT-ALL")
    monkeypatch_hw_watcher_call(monkeypatch, walle_clocksource, clock=hw_watcher_report)

    check_result = manifest.execute(WALLE_CLOCKSOURCE)
    expected_metadata = {
        "reason": "idk",
        "timestamp": hw_watcher_report["timestamp"],
        "status": "NOT-AT-ALL",
    }

    return make_canonization(check_result, expected_result(Status.WARN, expected_metadata))


def test_warn_when_hw_watcher_crashes(monkeypatch, manifest):
    reason = "Can't get status from hw-watcher: hw-watcher mock-failed"
    monkeypatch_hw_watcher_call(monkeypatch, walle_clocksource, clock=RuntimeError("hw-watcher mock-failed"))

    check_result = manifest.execute(WALLE_CLOCKSOURCE)
    expected_metadata = {
        "reason": reason,
        "timestamp": common.timestamp(),
    }

    return make_canonization(check_result, expected_result(Status.WARN, expected_metadata))
