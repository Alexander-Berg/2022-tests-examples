import pytest
from checks import walle_gpu, common
from juggler.bundles import Status, CheckResult
from .tools import time, mocked_timestamp, monkeypatch_hw_watcher_call

from utils import make_canonization

WALLE_GPU = "walle_gpu"


def expected_result(expected_status, expected_description):
    return CheckResult([walle_gpu.make_event(expected_status, expected_description)]).to_dict(service=WALLE_GPU)


@pytest.fixture(autouse=True)
def mock_timestamp(monkeypatch):
    monkeypatch.setattr(common, "timestamp", time.time)


def get_hww_out(status, reasons=("Last check: Fri Jul 28 18:07:43 2017",)):
    return {
        "status": status,
        "timestamp": 1501254463,
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
    monkeypatch.setattr(walle_gpu, "get_hw_watcher_status", lambda a: hw_watcher_out)

    check_result = manifest.execute(WALLE_GPU)

    return make_canonization(check_result, expected_result(check_status, {"result": hw_watcher_out}))


@pytest.mark.usefixtures("mock_timestamp")
@pytest.mark.parametrize(["reasons", "check_status"], [
    (["availability: less local GPUs 8 than in bot 9"], Status.CRIT),
    (["ecc: ecc errors (device memory) exceeded threshold 10"], Status.CRIT),
    (["memory: retired memory detected (single bit ecc: 16)"], Status.CRIT),
    (["driver: driver gone crazy and crashed into the wall"], Status.OK),
    ([
         "availability: less local GPUs 8 than in bot 9",
         "driver: driver gone crazy and crashed into the wall"
     ], Status.CRIT),
])
def test_returns_ok_for_non_actionable_errors(manifest, monkeypatch, reasons, check_status):
    hw_watcher_out = get_hww_out("FAILED", reasons)
    monkeypatch.setattr(walle_gpu, "get_hw_watcher_status", lambda a: hw_watcher_out)

    check_result = manifest.execute(WALLE_GPU)

    return make_canonization(check_result, expected_result(check_status, {"result": hw_watcher_out}))


@pytest.mark.usefixtures("mock_timestamp")
@pytest.mark.parametrize(["exc", "message"], [
    (IOError("random error"), "random error"),
    (common.HWWatcherModuleDisabled("gpu"), "hwwatcher module 'gpu' is disabled"),
    (common.HWWatcherError("won't run under root"), "won't run under root")
])
def test_returns_warn_on_exceptions(manifest, monkeypatch, exc, message):
    reason = "Can't get status from hw-watcher: {}".format(message)
    monkeypatch.setattr(common, "timestamp", mocked_timestamp)
    monkeypatch_hw_watcher_call(monkeypatch, walle_gpu, gpu=exc)

    check_result = manifest.execute(WALLE_GPU)

    expected_description = {"result": {"reason": [reason], "timestamp": mocked_timestamp()}}

    return make_canonization(check_result, expected_result(Status.WARN, expected_description))
