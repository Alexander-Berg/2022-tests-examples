# from __future__ import absolute_import

import json

from checks import walle_disk, common

import pytest
from juggler.bundles import Status, Event, CheckResult
from .tools import monkeypatch_hw_watcher_call, mocked_timestamp, mock_hw_watcher_report as common_hw_watcher_report

from utils import make_canonization


WALLE_DISK = "walle_disk"
MOCK_HW_WATCHER_STATUS_REASON = ["Last check: Fri Jun 22 19:31:11 2018"]


def expected_result(expected_status, expected_metadata):
    return CheckResult([
        Event(expected_status, json.dumps(expected_metadata))
    ]).to_dict(service=WALLE_DISK)


@pytest.fixture
def patch_time(monkeypatch):
    monkeypatch.setattr(common, "timestamp", mocked_timestamp)


def mock_hw_watcher_report(reason, status="OK", failed_disks=None, disk2replace=None, padd=0, timestamp_flag=True):
    report = dict(
        common_hw_watcher_report(reason, status, timestamp_flag=timestamp_flag),
        padding="padd-padd-" * padd
    )

    if failed_disks:
        report["failed_disks"] = failed_disks

    if disk2replace:
        report["disk2replace"] = disk2replace

    return report


def mock_failed_disk(reason):
    return {
        "slot": 7,
        "status": "FAILED",
        "name": "sdh",
        "size_in_bytes": 8001563222016,
        "serial": "ZA16QBB5",
        "disk_type": "HDD",
        "model": "ST8000NM0055-1RM112",
        "reason": reason,
    }


@pytest.mark.usefixtures("patch_time")
@pytest.mark.parametrize("hw_watcher_status", ("OK", "RECOVERY", "WARNING"))
def test_ok_when_hw_watcher_status_is_one_of_ok_recovery_warning(monkeypatch, manifest, hw_watcher_status):
    hw_watcher_report = mock_hw_watcher_report(reason=MOCK_HW_WATCHER_STATUS_REASON, status=hw_watcher_status, timestamp_flag=False)
    monkeypatch_hw_watcher_call(monkeypatch, walle_disk, disk=hw_watcher_report)

    check_result = manifest.execute(WALLE_DISK)

    return make_canonization(check_result, expected_result(Status.OK, {"result": hw_watcher_report}))


@pytest.mark.usefixtures("patch_time")
@pytest.mark.parametrize("hw_watcher_status", ("FAILED", "UNKNOWN"))
def test_crit_when_hw_watcher_status_is_failed_or_unknown_(monkeypatch, manifest, hw_watcher_status):
    hw_watcher_report = mock_hw_watcher_report(reason=MOCK_HW_WATCHER_STATUS_REASON, status=hw_watcher_status, timestamp_flag=False)
    monkeypatch_hw_watcher_call(monkeypatch, walle_disk, disk=hw_watcher_report)

    check_result = manifest.execute(WALLE_DISK)

    return make_canonization(check_result, expected_result(Status.CRIT, {"result": hw_watcher_report}))


@pytest.mark.usefixtures("patch_time")
def test_warn_when_hw_watcher_status_is_not_known_or_supported(monkeypatch, manifest):
    hw_watcher_report = mock_hw_watcher_report(reason=MOCK_HW_WATCHER_STATUS_REASON, status="SOME_UNSUPPORTED_STATUS", timestamp_flag=False)
    monkeypatch_hw_watcher_call(monkeypatch, walle_disk, disk=hw_watcher_report)

    check_result = manifest.execute(WALLE_DISK)

    return make_canonization(check_result, expected_result(Status.WARN, {"result": hw_watcher_report}))


@pytest.mark.usefixtures("patch_time")
def test_warn_when_hw_watcher_failed(monkeypatch, manifest):
    reason = "Can't get status from hw-watcher: hw-watcher mock-failed"
    monkeypatch_hw_watcher_call(monkeypatch, walle_disk, disk=Exception("hw-watcher mock-failed"))

    check_result = manifest.execute(WALLE_DISK)
    expected_description = {"result": {"reason": [reason], "timestamp": mocked_timestamp()}}

    return make_canonization(check_result, expected_result(Status.WARN, expected_description))


def test_shrinks_metadata_to_fit_into_limit__drop_reason(monkeypatch, manifest):
    padd_size = 100
    monkeypatch_hw_watcher_call(monkeypatch, walle_disk, disk=mock_hw_watcher_report(
        reason=MOCK_HW_WATCHER_STATUS_REASON * 3,
        status="FAILED",
        disk2replace=mock_failed_disk(MOCK_HW_WATCHER_STATUS_REASON),
        padd=padd_size,
    ))

    check_result = manifest.execute(WALLE_DISK)

    expected_shrinked_metadata = mock_hw_watcher_report(
        reason=MOCK_HW_WATCHER_STATUS_REASON * 1,
        status="FAILED",
        disk2replace=mock_failed_disk(MOCK_HW_WATCHER_STATUS_REASON),
        padd=padd_size,
    )

    # do not check serialized data, it may have different order of keys
    # result metadata still does not fit, but it must keep at least one reason string
    assert {"result": expected_shrinked_metadata} == json.loads(check_result["events"][0]["description"])


def test_shrinks_metadata_to_fit_into_limit__drop_reason_from_disk_to_replace(monkeypatch, manifest):
    padd_size = 100
    monkeypatch_hw_watcher_call(monkeypatch, walle_disk, disk=mock_hw_watcher_report(
        reason=MOCK_HW_WATCHER_STATUS_REASON,
        status="FAILED",
        disk2replace=mock_failed_disk(MOCK_HW_WATCHER_STATUS_REASON * 3),
        padd=padd_size,
    ))

    check_result = manifest.execute(WALLE_DISK)

    expected_shrinked_metadata = mock_hw_watcher_report(
        reason=MOCK_HW_WATCHER_STATUS_REASON,
        status="FAILED",
        disk2replace=mock_failed_disk(MOCK_HW_WATCHER_STATUS_REASON * 1),
        padd=padd_size,
    )

    # do not check serialized data, it may have different order of keys
    # result metadata still does not fit, but it must keep at least one reason string
    assert {"result": expected_shrinked_metadata} == json.loads(check_result["events"][0]["description"])


def test_shrinks_metadata_to_fit_into_limit__drop_failed_disks(monkeypatch, manifest):
    padd_size = 100
    monkeypatch_hw_watcher_call(monkeypatch, walle_disk, disk=mock_hw_watcher_report(
        reason=MOCK_HW_WATCHER_STATUS_REASON,
        status="FAILED",
        disk2replace=mock_failed_disk(MOCK_HW_WATCHER_STATUS_REASON),
        failed_disks=[mock_failed_disk(MOCK_HW_WATCHER_STATUS_REASON)],
        padd=padd_size,
    ))

    check_result = manifest.execute(WALLE_DISK)

    expected_shrinked_metadata = mock_hw_watcher_report(
        reason=MOCK_HW_WATCHER_STATUS_REASON,
        status="FAILED",
        disk2replace=mock_failed_disk(MOCK_HW_WATCHER_STATUS_REASON),
        padd=padd_size,
    )

    # do not check serialized data, it may have different order of keys
    assert {"result": expected_shrinked_metadata} == json.loads(check_result["events"][0]["description"])
