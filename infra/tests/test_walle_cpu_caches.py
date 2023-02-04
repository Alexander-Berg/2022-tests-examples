import json
import pytest

from checks import walle_cpu_caches, common

from juggler.bundles import Status, Event, CheckResult
from .tools import monkeypatch_hw_watcher_call, mocked_timestamp, mock_hw_watcher_report as common_hw_watcher_report

from utils import make_canonization

WALLE_CPU_CACHES = "walle_cpu_caches"
MOCK_HW_WATCHER_STATUS_REASON = [
    "Status was reset",
    "Last check: Tue Feb 19 15:57:13 2019"
]


def expected_result(expected_status, expected_metadata):
    return CheckResult([
        Event(expected_status, json.dumps(expected_metadata))
    ]).to_dict(service=WALLE_CPU_CACHES)


def mock_hw_watcher_report(**kwargs):
    res = common_hw_watcher_report(**kwargs)
    res.update(raw="", socket=1)
    return res


@pytest.fixture(autouse=True)
def patch_time(monkeypatch):
    monkeypatch.setattr(common, "timestamp", mocked_timestamp)


def test_ok_when_hw_watcher_status_is_ok(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(reason=MOCK_HW_WATCHER_STATUS_REASON, status="OK", timestamp_flag=False)
    monkeypatch_hw_watcher_call(monkeypatch, walle_cpu_caches, cpu=hw_watcher_report)

    check_result = manifest.execute(WALLE_CPU_CACHES)

    return make_canonization(check_result, expected_result(Status.OK, {"result": hw_watcher_report}))


def test_crit_when_hw_watcher_status_is_failed(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(reason=MOCK_HW_WATCHER_STATUS_REASON, status="FAILED", timestamp_flag=False)
    monkeypatch_hw_watcher_call(monkeypatch, walle_cpu_caches, cpu=hw_watcher_report)

    check_result = manifest.execute(WALLE_CPU_CACHES)

    return make_canonization(check_result, expected_result(Status.CRIT, {"result": hw_watcher_report}))


def test_warn_when_hw_watcher_status_is_not_supported(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(reason=MOCK_HW_WATCHER_STATUS_REASON, status="NOT-AT-ALL", timestamp_flag=False)
    monkeypatch_hw_watcher_call(monkeypatch, walle_cpu_caches, cpu=hw_watcher_report)

    check_result = manifest.execute(WALLE_CPU_CACHES)

    return make_canonization(check_result, expected_result(Status.WARN, {"result": hw_watcher_report}))


def test_warn_when_hw_watcher_crashes(monkeypatch, manifest):
    reason = "Can't get status from hw-watcher: hw-watcher mock-failed"
    monkeypatch_hw_watcher_call(monkeypatch, walle_cpu_caches, cpu=Exception("hw-watcher mock-failed"))

    check_result = manifest.execute(WALLE_CPU_CACHES)
    expected_description = {"result": {"reason": [reason], "timestamp": mocked_timestamp()}}

    return make_canonization(check_result, expected_result(Status.WARN, expected_description))
