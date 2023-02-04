import pytest
from checks import walle_infiniband, common
from juggler.bundles import Status, CheckResult
from .tools import mocked_timestamp, monkeypatch_hw_watcher_call


WALLE_INFINIBAND = "walle_infiniband"


def expected_result(expected_status, expected_description):
    return CheckResult([walle_infiniband.make_event(expected_status, expected_description)]).to_dict(service=WALLE_INFINIBAND)


@pytest.fixture(autouse=True)
def mock_timestamp(monkeypatch):
    monkeypatch.setattr(common, "timestamp", 1)


def get_hww_out(status, reasons=("Last check: Fri Jul 28 18:07:43 2017",)):
    return {
        "status": status,
        "timestamp": 1,
        "reason": list(reasons)
    }


@pytest.mark.usefixtures("mock_timestamp")
@pytest.mark.parametrize(["hww_status", "check_status"], [
    ("OK", Status.OK),
    ("WARNING", Status.OK),  # not actionable
    ("FAILED", Status.CRIT),  # actionable
    ("UNKNOWN", Status.WARN)  # unexpected
])
def test_status_matching(manifest, monkeypatch, hww_status, check_status):
    hw_watcher_out = get_hww_out(hww_status)
    monkeypatch.setattr(walle_infiniband, "get_hw_watcher_status", lambda a: hw_watcher_out)

    check_result = manifest.execute(WALLE_INFINIBAND)

    assert check_result == expected_result(check_status, {"result": hw_watcher_out})


@pytest.mark.usefixtures("mock_timestamp")
@pytest.mark.parametrize(["exc", "message"], [
    (IOError("random error"), "random error"),
    (common.HWWatcherModuleDisabled("infiniband"), "hwwatcher module 'infiniband' is disabled"),
    (common.HWWatcherError("won't run under root"), "won't run under root")
])
def test_returns_warn_on_exceptions(manifest, monkeypatch, exc, message):
    reason = "Can't get status from hw-watcher: {}".format(message)
    monkeypatch.setattr(common, "timestamp", mocked_timestamp)
    monkeypatch_hw_watcher_call(monkeypatch, walle_infiniband, infiniband=exc)

    check_result = manifest.execute(WALLE_INFINIBAND)

    expected_description = {"result": {"timestamp": mocked_timestamp(), "reason": [reason]}}

    assert check_result == expected_result(Status.WARN, expected_description)
