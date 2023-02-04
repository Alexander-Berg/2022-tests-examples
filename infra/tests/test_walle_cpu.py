from checks import walle_cpu

import pytest
from juggler.bundles import Status, CheckResult
from mock import Mock

from utils import make_canonization

WALLE_CPU = "walle_cpu"


def expected_result(expected_status, expected_description, boardname="FakeBrD", vendor_id="FakeCpuVndr"):
    return CheckResult([
        walle_cpu.make_event(expected_status, expected_description, boardname, vendor_id)
    ]).to_dict(service=WALLE_CPU)


FAKE_BOARDNAME_FILE_CONTENT = "{boardname}"
FAKE_CPUINFO_FILE_CONTENT = """processor       : 0
vendor_id       : {vendor_id}
cpu family      : 6
model           : 79
model name      : Intel(R) Xeon(R) CPU E5-2660 v4 @ 2.00GHz
microcode       : 0xb000021
cpu MHz         : 2399.921
cache size      : 35840 KB
physical id     : 0
siblings        : 28
core id         : 0
cpu cores       : 14
apicid          : 0
initial apicid  : 0
power management:

processor       : 1
vendor_id       : {vendor_id}
cpu family      : 6
model           : 79
model name      : Intel(R) Xeon(R) CPU E5-2660 v4 @ 2.00GHz
microcode       : 0xb000021
cpu MHz         : 2399.921
cache size      : 35840 KB
physical id     : 0
siblings        : 28
core id         : 1
cpu cores       : 14
apicid          : 2
initial apicid  : 2
power management:
"""


def mock_board_data(monkeypatch, online_cores, present_cores, offline_cores="",
                    boardname="FakeBrD", vendor_id="FakeCpuVndr"):
    monkeypatch.setattr(walle_cpu, "read_content", Mock(side_effect=[
        boardname if isinstance(boardname, Exception) else FAKE_BOARDNAME_FILE_CONTENT.format(boardname=boardname),
        vendor_id if isinstance(vendor_id, Exception) else FAKE_CPUINFO_FILE_CONTENT.format(vendor_id=vendor_id),
        online_cores,
        present_cores,
        offline_cores,
    ]))


def test_ok_when_online_cores_equals_present_cores(monkeypatch, manifest):
    mock_board_data(monkeypatch, "0-23", "0-23")

    check_result = manifest.execute(WALLE_CPU)

    return make_canonization(check_result, expected_result(Status.OK, "Ok"))


def test_crit_when_some_cores_are_offline(monkeypatch, manifest):
    mock_board_data(monkeypatch, "1-23", "0-23", "0")

    check_result = manifest.execute(WALLE_CPU)

    return make_canonization(check_result, expected_result(Status.CRIT, "offline cores: 0"))


def test_warn_when_online_cores_equals_present_cores_but_no_cores_offline(monkeypatch, manifest):
    mock_board_data(monkeypatch, "1-23", "0-23", "24-47")

    check_result = manifest.execute(WALLE_CPU)

    expected = expected_result(
        Status.WARN, "online cores (1-23) != present cores (0-23), but there are no present offline cores (24-47)")

    return make_canonization(check_result, expected)


@pytest.mark.parametrize("core_status_type", ["offline", "present", "online"])
def test_returns_warn_when_cant_read_file(monkeypatch, manifest, core_status_type):
    kwargs = {
        "online_cores": "0-23",
        "present_cores": "24-63",
        "offline_cores": "0-63",
        "{}_cores".format(core_status_type): EnvironmentError("me not exist")
    }
    mock_board_data(monkeypatch, **kwargs)

    check_result = manifest.execute(WALLE_CPU)

    expected_description = (
        "Failed to get data for walle_cpu check: can not read {} cores number: me not exist".format(
            core_status_type)
    )

    return make_canonization(check_result, expected_result(Status.WARN, expected_description))


def test_does_not_change_status_when_cant_read_board_name(monkeypatch, manifest):
    mock_board_data(monkeypatch, online_cores="0-15", present_cores="0-15",
                    boardname=EnvironmentError("no such file or et all"))

    check_result = manifest.execute(WALLE_CPU)

    expected = expected_result(Status.OK, "Ok", boardname="can not read board name: no such file or et all")

    return make_canonization(check_result, expected)


def test_does_not_change_status_when_cant_read_processor_name(monkeypatch, manifest):
    mock_board_data(monkeypatch, online_cores="0-10", present_cores="0-15", offline_cores="11-15",
                    vendor_id=EnvironmentError("no such file or et all"))

    check_result = manifest.execute(WALLE_CPU)

    expected = expected_result(Status.CRIT, "offline cores: 11-15",
                               vendor_id="can not read cpuinfo: no such file or et all")

    return make_canonization(check_result, expected)
