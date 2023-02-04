from collections import OrderedDict
from checks import common, walle_link, walle_memory, walle_disk, walle_bmc

import pytest
from juggler.bundles import Status, CheckResult
from subprocess32 import CalledProcessError

from .tools import mocked_timestamp


ROOT_PROBLEM = "hw-watcher does not work under root account"


def test_get_hw_watcher_status_raises_disabled_module(monkeypatch):
    monkeypatch.setattr(common, "get_command_output", lambda _: "OK; Module bmc disabled")
    with pytest.raises(common.HWWatcherModuleDisabled) as e:
        common.get_hw_watcher_status("bmc")
    assert e.value.module == "bmc"


def test_get_hw_watcher_status_raises_run_under_root(monkeypatch):
    def raiser(args):
        raise CalledProcessError(2, args, output=ROOT_PROBLEM + "\nPlease stop")
    monkeypatch.setattr(common, "get_command_output", raiser)
    with pytest.raises(common.HWWatcherError):
        common.get_hw_watcher_status("bmc")


def test_get_hw_watcher_status_raises_on_non_json_stdout(monkeypatch):
    hww_output = "OK; VM detected by mark"
    monkeypatch.setattr(common, "get_command_output", lambda _: hww_output)
    with pytest.raises(common.HWWatcherNonJsonOutput) as e:
        common.get_hw_watcher_status("bmc")
    assert e.value.hww_output == hww_output


def expected_result(module, expected_status, expected_description):
    return CheckResult([module.make_event(expected_status, expected_description)]).to_dict(service=module.CHECK_NAME)


# memory is not tested here because of different check output format
@pytest.mark.parametrize("check_module, hww_module_name", [(walle_link, "link"),
                                                           (walle_disk, "mem"),
                                                           (walle_bmc, "bmc")])
def test_checks_return_warn_on_exceptions(manifest, monkeypatch, check_module, hww_module_name):
    monkeypatch.setattr(common, "get_command_output", lambda _: "OK; Module {} disabled".format(hww_module_name))
    monkeypatch.setattr(common, "timestamp", mocked_timestamp)

    check_result = manifest.execute(check_module.CHECK_NAME)
    expected_reason = "Can\'t get status from hw-watcher: hwwatcher module \'{}\' is disabled".format(hww_module_name)
    expected_description = {"result": {"reason": [expected_reason], "timestamp": mocked_timestamp()}}
    assert check_result == expected_result(check_module, Status.WARN, expected_description)


# memory is not tested here because of different check output format
@pytest.mark.parametrize("check_module", [walle_link, walle_disk, walle_bmc])
def test_checks_handle_hwwatcher_under_root(manifest, monkeypatch, check_module):
    exc_output = ROOT_PROBLEM + ". Please stop"

    def raiser(args):
        raise CalledProcessError(2, args, output=exc_output)
    monkeypatch.setattr(common, "get_command_output", raiser)
    monkeypatch.setattr(common, "timestamp", mocked_timestamp)

    check_result = manifest.execute(check_module.CHECK_NAME)
    expected_reason = "Can\'t get status from hw-watcher: {}".format(exc_output)
    expected_description = {"result": {"reason": [expected_reason], "timestamp": mocked_timestamp()}}
    assert check_result == expected_result(check_module, Status.WARN, expected_description)


def test_truncate_description_string_short():
    msg = "message mock"
    assert msg == common.truncate_description_string(msg)


def test_truncate_description_string_long():
    msg = "long message mock"
    result = common.truncate_description_string(msg, 10)
    assert len(result) == 10
    assert result == "lon ... ck"


def test_get_cpu_model_name(mock_xeon_e5_2660_proc_cpuinfo):
    model = common.get_cpu_model_name(mock_xeon_e5_2660_proc_cpuinfo)
    assert model == 'Intel(R) Xeon(R) CPU E5-2660 0 @ 2.20GHz'


@pytest.mark.parametrize("raw_version, expected_parts",
                         [
                             ("4.19.62-13", (4, 19, 62, "13", None)),
                             ("2.6.19", (2, 6, 19, None, None))
                         ])
def test_get_kernel_version(monkeypatch, raw_version, expected_parts):
    monkeypatch.setattr(common, "get_command_output", lambda _: raw_version)
    version = common.get_kernel_version()

    parts = ["major", "minor", "patch", "prerelease", "build"]
    expected = OrderedDict([(part, val) for part, val in zip(parts, expected_parts)])

    assert version == expected
