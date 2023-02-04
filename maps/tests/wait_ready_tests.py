from unittest import mock
import pytest

from maps.garden.sdk.core import Version

from maps.garden.sdk.ecstatic import const
from maps.garden.sdk.ecstatic import resources
from maps.garden.sdk.ecstatic import tasks

from . import helpers


def _create_dataset_and_wait_until_readiness():
    resource = resources.DatasetResource(
        name="noname",
        dataset_name_template="zzz",
        dataset_version_template="1.1.1"
    )
    resource.exists = True
    resource.version = Version(properties={})
    task = tasks.WaitReadyTask(const.STABLE_BRANCH, poll_interval=1, max_waiting=2)
    task(resource)


STATUS_MESSAGE_EMPTY = helpers.make_old_style_ecstatic_status([])
STATUS_MESSAGE_READY_IN_TESTING = helpers.make_old_style_ecstatic_status([
    {
        "name": "zzz",
        "tag": "",
        "version": "1.1.1",
        "branch": const.TESTING_BRANCH,
        "clients": 2,
        "active": 2,
        "downloaded": 2,
        "errors": 0,
        "states": "RR"
    }
])
STATUS_MESSAGE_HOOK_FAILED = helpers.make_old_style_ecstatic_status([
    # previous version, active
    {
        "name": "zzz",
        "tag": "",
        "version": "1.0.0",
        "branch": const.STABLE_BRANCH,
        "clients": 1,
        "downloaded": 1,
        "active": 1,
        "errors": 0,
        "states": "[A]"
    },
    # our version, failed
    {
        "name": "zzz",
        "tag": "",
        "version": "1.1.1",
        "branch": const.STABLE_BRANCH,
        "clients": 1,
        "downloaded": 1,
        "active": 0,
        "errors": 1,
        "states": "[F]"
    }
])
STATUS_MESSAGE_NOT_DOWNLOADED = helpers.make_old_style_ecstatic_status([
    {
        "name": "zzz",
        "tag": "",
        "version": "1.1.1",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 1,
        "downloaded": 1,
        "errors": 0,
        "states": "_A"
    }
])
STATUS_MESSAGE_READY = helpers.make_old_style_ecstatic_status([
    {
        "name": "zzz",
        "tag": "",
        "version": "1.1.1",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 2,
        "downloaded": 2,
        "errors": 0,
        "states": "RA"
    }
])


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_wait_ready_if_branch_missing(status_mock, sleep_mock):
    status_mock.side_effect = [STATUS_MESSAGE_EMPTY, STATUS_MESSAGE_READY_IN_TESTING]

    _create_dataset_and_wait_until_readiness()

    sleep_mock.assert_not_called()
    assert status_mock.call_count == 2


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_wait_ready_if_dataset_missing(status_mock, sleep_mock):
    status_mock.return_value = STATUS_MESSAGE_EMPTY

    with pytest.raises(tasks.EcstaticError) as ex:
        _create_dataset_and_wait_until_readiness()

    sleep_mock.assert_not_called()
    assert status_mock.call_count == 2
    assert "Dataset 'zzz' does not have any version" in str(ex.value)


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_wait_ready_if_hook_fails(status_mock, sleep_mock):
    status_mock.return_value = STATUS_MESSAGE_HOOK_FAILED

    with pytest.raises(tasks.EcstaticError) as ex:
        _create_dataset_and_wait_until_readiness()

    assert status_mock.call_count == 1
    sleep_mock.assert_not_called()
    assert "Dataset 'zzz=1.1.1' hooks have failed" in str(ex.value)


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_wait_ready_success(status_mock, sleep_mock):
    status_mock.side_effect = [STATUS_MESSAGE_NOT_DOWNLOADED, STATUS_MESSAGE_READY]

    _create_dataset_and_wait_until_readiness()

    assert status_mock.call_count == 2
    assert sleep_mock.call_count == 1
