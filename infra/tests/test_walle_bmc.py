import pytest
from checks import walle_bmc, common
from juggler.bundles import Status, CheckResult
from .tools import time, mocked_timestamp, monkeypatch_hw_watcher_call


from utils import make_canonization

WALLE_BMC = "walle_bmc"


def expected_result(expected_status, expected_description):
    return CheckResult([walle_bmc.make_event(expected_status, expected_description)]).to_dict(service=WALLE_BMC)


@pytest.fixture(autouse=True)
def mock_timestamp(monkeypatch):
    monkeypatch.setattr(common, "timestamp", time.time)


def get_hww_out(status):
    return {
        "status": status,
        "timestamp": 1501254463,
        "reason": [
            "Last check: Fri Jul 28 18:07:43 2017"
        ]
    }


@pytest.mark.usefixtures("mock_timestamp")
@pytest.mark.parametrize('hww_status,check_status', [('OK', Status.OK),
                                                     ('WARNING', Status.OK),
                                                     ('UNKNOWN', Status.OK),
                                                     ('FAILED', Status.CRIT),
                                                     ('WTF', Status.WARN)])  # Unknown status
def test_statuses_correnspondence(manifest, monkeypatch, hww_status, check_status):
    monkeypatch.setattr(walle_bmc, "get_hw_watcher_status", lambda a: get_hww_out(hww_status))

    check_result = manifest.execute(WALLE_BMC)

    expected_hw_out = get_hww_out(hww_status)
    expected_data = expected_result(check_status, {"result": expected_hw_out})

    return make_canonization(check_result, expected_data)


def get_raiser(exc):
    def raiser(_):
        raise exc
    return raiser


@pytest.mark.parametrize("exc", [IOError("i am not ok, please call mighty wall-e developers"),
                                 common.HWWatcherModuleDisabled("bmc"),
                                 common.HWWatcherError("hw-watcher does not work under root account")
                                 ])
def test_returns_warn_on_exceptions(manifest, monkeypatch, exc):
    reason = "Can't get status from hw-watcher: {}".format(exc)
    monkeypatch.setattr(common, "timestamp", mocked_timestamp)
    monkeypatch_hw_watcher_call(monkeypatch, walle_bmc, bmc=exc)

    check_result = manifest.execute(WALLE_BMC)

    expected_description = {"result": {"reason": [reason], "timestamp": mocked_timestamp()}}
    expected_data = expected_result(Status.WARN, expected_description)

    return make_canonization(check_result, expected_data)
