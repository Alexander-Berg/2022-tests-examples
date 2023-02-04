import json
import pytest

from checks import walle_link, common

from juggler.bundles import Status, Event, CheckResult
from .tools import monkeypatch_hw_watcher_call, mocked_timestamp, mock_hw_watcher_report

from utils import make_canonization


WALLE_LINK = "walle_link"
MOCK_HW_WATCHER_STATUS_REASON = ["Last check: Fri Jul 04 17:06:11 2018"]


def expected_result(expected_status, expected_metadata):
    return CheckResult([
        Event(expected_status, json.dumps(expected_metadata))
    ]).to_dict(service=WALLE_LINK)


@pytest.fixture(autouse=True)
def patch_time(monkeypatch):
    monkeypatch.setattr(common, "timestamp", mocked_timestamp)


def test_ok_when_hw_watcher_status_is_ok(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(reason=MOCK_HW_WATCHER_STATUS_REASON, status="OK", timestamp_flag=False)
    monkeypatch_hw_watcher_call(monkeypatch, walle_link, link=hw_watcher_report)

    check_result = manifest.execute(WALLE_LINK)
    return make_canonization(check_result, expected_result(Status.OK, {"result": hw_watcher_report}))


def test_crit_when_hw_watcher_status_is_failed(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(reason=MOCK_HW_WATCHER_STATUS_REASON, status="FAILED", timestamp_flag=False)
    monkeypatch_hw_watcher_call(monkeypatch, walle_link, link=hw_watcher_report)

    check_result = manifest.execute(WALLE_LINK)
    return make_canonization(check_result, expected_result(Status.CRIT, {"result": hw_watcher_report}))


def test_warn_when_hw_watcher_status_is_not_supported(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(reason=MOCK_HW_WATCHER_STATUS_REASON, status="NOT-AT-ALL", timestamp_flag=False)
    monkeypatch_hw_watcher_call(monkeypatch, walle_link, link=hw_watcher_report)

    check_result = manifest.execute(WALLE_LINK)
    return make_canonization(check_result, expected_result(Status.WARN, {"result": hw_watcher_report}))


def test_warn_when_hw_watcher_crashes(monkeypatch, manifest):
    reason = "Can't get status from hw-watcher: hw-watcher mock-failed"
    monkeypatch_hw_watcher_call(monkeypatch, walle_link, link=RuntimeError("hw-watcher mock-failed"))

    check_result = manifest.execute(WALLE_LINK)
    expected_description = {"result": {"reason": [reason], "timestamp": mocked_timestamp()}}
    return make_canonization(check_result, expected_result(Status.WARN, expected_description))
