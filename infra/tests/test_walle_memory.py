# from __future__ import absolute_import

import json

import pytest

from checks import walle_memory, common

from juggler.bundles import Status, Event, CheckResult
from .tools import monkeypatch_hw_watcher_call, mocked_timestamp, mock_hw_watcher_report as common_hw_watcher_report

from utils import make_canonization

WALLE_MEMORY = "walle_memory"
MOCK_HW_WATCHER_STATUS_REASON = ["Last check: Fri Jul 04 17:06:11 2018"]


@pytest.fixture(autouse=True)
def patch_time(monkeypatch):
    monkeypatch.setattr(common, "timestamp", mocked_timestamp)


def mock_hw_watcher_report(ecc_status="OK", mem_status="OK", timestamp_flag=True, mem_args=None, ecc_args=None):
    return {
        "ecc": common_hw_watcher_report(reason=MOCK_HW_WATCHER_STATUS_REASON, status=ecc_status,
                                        timestamp_flag=timestamp_flag, **(ecc_args if ecc_args is not None else {})),
        "mem": common_hw_watcher_report(reason=MOCK_HW_WATCHER_STATUS_REASON, status=mem_status,
                                        timestamp_flag=timestamp_flag, **(mem_args if mem_args is not None else {})),
    }


def expected_result(expected_status, expected_metadata):
    return CheckResult([
        Event(expected_status, json.dumps(expected_metadata))
    ]).to_dict(service=WALLE_MEMORY)


def test_ok_when_both_ecc_and_mem_are_ok(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(timestamp_flag=False)

    monkeypatch_hw_watcher_call(monkeypatch, walle_memory, **hw_watcher_report)

    check_result = manifest.execute(WALLE_MEMORY)
    return make_canonization(check_result, expected_result(Status.OK, {"results": hw_watcher_report}))


@pytest.mark.parametrize("module_statuses", [("FAILED", "OK"), ("OK", "FAILED")])
def test_crit_when_either_ecc_or_mem_is_failed(monkeypatch, manifest, module_statuses):
    mem_args = {"comment": "some  comment", "needed_mem": 4, "real_mem": 3}
    ecc_args = {"slot": "some slot", "comment": "some comment"}
    hw_watcher_report = mock_hw_watcher_report(*module_statuses, timestamp_flag=False,
                                               mem_args=mem_args, ecc_args=ecc_args)
    monkeypatch_hw_watcher_call(monkeypatch, walle_memory, **hw_watcher_report)

    check_result = manifest.execute(WALLE_MEMORY)
    return make_canonization(check_result, expected_result(Status.CRIT, {"results": hw_watcher_report}))


@pytest.mark.parametrize("module_statuses", [("FAILED", "OK"), ("OK", "FAILED")])
def test_strips_error_message(monkeypatch, manifest, module_statuses):
    ecc_status, mem_status = module_statuses
    hw_watcher_report = {
        "mem": common_hw_watcher_report(
            reason=[
                "size: available 112 of 128 GB memory.",
                "size: available 113 of 128 GB memory.",
                "size: available 114 of 128 GB memory.",
            ],
            comment=(
                "14 modules installed: DIMM_P0_A0, DIMM_P0_A1, DIMM_P0_B0, DIMM_P0_B1, DIMM_P0_C0, DIMM_P0_C1,"
                " DIMM_P0_D0, DIMM_P0_D1, DIMM_P1_E0, DIMM_P1_E1, DIMM_P1_F0, DIMM_P1_F1, DIMM_P1_H0, DIMM_P1_H1."
            ),
            status=mem_status,
            needed_mem=4,
            real_mem=3,
            timestamp_flag=False
        ),
        "ecc": common_hw_watcher_report(
            status=ecc_status,
            reason=[
                "DDR3_P{}_F{}: mcelog: 100500 correctable errors during 0 days, 10:11:12 (threshold 1)".format(i, j)
                for i in range(16) for j in range(16)
            ],
            comment="; ".join(
                "DDR3_P{}_F{}: mcelog: 100500 correctable errors during 0 days, 10:11:12 (threshold 1)".format(i, j)
                for i in range(16) for j in range(16)
            ),
            slot="some slot",
            timestamp_flag=False
        ),
    }

    monkeypatch_hw_watcher_call(monkeypatch, walle_memory, **hw_watcher_report)
    monkeypatch.setattr(walle_memory, "DESCRIPTION_LEN_LIMIT", 1)

    expected_hw_watcher_report = {
        "mem": common_hw_watcher_report(
            reason=["size: available 112 of 128 GB memory."],
            comment="14 modules installed: DIMM_P0_A0, DIMM_P0_A1, DIMM_P0_B0, DIMM_P0_B1, DIMM_P0_C0, DIMM_P0_C1, ...",
            status=mem_status,
            needed_mem=4,
            real_mem=3,
            timestamp_flag=False
        ),
        "ecc": common_hw_watcher_report(
            status=ecc_status,
            reason=["DDR3_P0_F0: mcelog: 100500 correctable errors during 0 days, 10:11:12 (threshold 1)"],
            comment="DDR3_P0_F0: mcelog: 100500 correctable errors during 0 days, 10:11:12 (threshold 1); DDR3_P0_F...",
            slot="some slot",
            timestamp_flag=False
        ),
    }

    check_result = manifest.execute(WALLE_MEMORY)
    return make_canonization(check_result, expected_result(Status.CRIT, {"results": expected_hw_watcher_report}))


def test_crit_when_ecc_has_status_unknown(monkeypatch, manifest):
    ecc_args = {"slot": "some slot", "comment": "some comment"}
    hw_watcher_report = mock_hw_watcher_report(ecc_status="UNKNOWN", timestamp_flag=False, ecc_args=ecc_args)

    monkeypatch_hw_watcher_call(monkeypatch, walle_memory, **hw_watcher_report)

    check_result = manifest.execute(WALLE_MEMORY)
    return make_canonization(check_result, expected_result(Status.CRIT, {"results": hw_watcher_report}))


@pytest.mark.parametrize("module_statuses", [("NOT-AT-ALL", "OK"), ("OK", "NOT-AT-ALL")])
def test_warn_when_either_ecc_or_mem_has_unsupported_status(monkeypatch, manifest, module_statuses):
    hw_watcher_report = mock_hw_watcher_report(*module_statuses, timestamp_flag=False)

    monkeypatch_hw_watcher_call(monkeypatch, walle_memory, **hw_watcher_report)

    check_result = manifest.execute(WALLE_MEMORY)
    assert expected_result(Status.WARN, {"results": hw_watcher_report}) == check_result


@pytest.mark.parametrize("failed_module", ["ecc", "mem"])
def test_warn_when_either_ecc_or_mem_crashes_hw_watcher(monkeypatch, manifest, failed_module):
    kwargs = {"{}_status".format(failed_module): "Disabled"}
    hw_watcher_report = mock_hw_watcher_report(timestamp_flag=False, **kwargs)
    hw_watcher_report[failed_module]['reason'] = ["Can't get status from hw-watcher: {}".format(RuntimeError("hw-watcher mock-failed"))]

    monkeypatch_hw_watcher_call(monkeypatch, walle_memory, **hw_watcher_report)

    check_result = manifest.execute(WALLE_MEMORY)

    return make_canonization(check_result, expected_result(Status.WARN, {"results": hw_watcher_report}))
