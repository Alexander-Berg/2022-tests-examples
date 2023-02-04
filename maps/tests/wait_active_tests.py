from unittest import mock
import pytest

from maps.garden.sdk.core import Version

from maps.garden.sdk.ecstatic import const
from maps.garden.sdk.ecstatic import resources
from maps.garden.sdk.ecstatic import tasks

from . import helpers


def _create_dataset_and_wait_until_activation():
    resource = resources.DatasetResource(
        name="noname",
        dataset_name_template="zzz",
        dataset_version_template="1.1.1"
    )
    resource.exists = True
    resource.version = Version(properties={})
    task = tasks.WaitActiveTask(const.STABLE_BRANCH, poll_interval=1, max_waiting=2)
    task(resource)


STATUS_MESSAGE_EMPTY = helpers.make_old_style_ecstatic_status([])
STATUS_MESSAGE_READY_IN_TESTING = helpers.make_old_style_ecstatic_status([
    {
        "name": "zzz",
        "tag": "",
        "version": "1.1.1",
        "branch": const.TESTING_BRANCH,
        "clients": 2,
        "active": 0,
        "downloaded": 2,
        "errors": 0,
        "states": "RR"
    }
])
STATUS_MESSAGE_NOT_ACTIVE_YET = helpers.make_old_style_ecstatic_status([
    {
        "name": "zzz",
        "tag": "",
        "version": "1.0.0",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 0,
        "downloaded": 2,
        "errors": 0,
        "states": "RA"
    },
    {
        "name": "zzz",
        "tag": "",
        "version": "1.1.1",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 0,
        "downloaded": 2,
        "errors": 0,
        "states": "AR"
    }
])
STATUS_MESSAGE_NEWER_VERSION_IS_ACTIVE = helpers.make_old_style_ecstatic_status([
    {
        "name": "zzz",
        "tag": "",
        "version": "1.0.0",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 0,
        "downloaded": 2,
        "errors": 0,
        "states": "RR"
    },
    {
        "name": "zzz",
        "tag": "",
        "version": "1.1.1",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 0,
        "downloaded": 2,
        "errors": 0,
        "states": "AR"
    },
    {
        "name": "zzz",
        "tag": "",
        "version": "1.2.0",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 0,
        "downloaded": 2,
        "errors": 0,
        "states": "RA"
    }
])
STATUS_MESSAGE_SWITCH_FAILED = helpers.make_old_style_ecstatic_status([
    # previous version, active
    {
        "name": "zzz",
        "tag": "",
        "version": "1.0.0",
        "branch": const.STABLE_BRANCH,
        "clients": 1,
        "active": 1,
        "downloaded": 1,
        "errors": 0,
        "states": "A"
    },
    # our version, failed
    {
        "name": "zzz",
        "tag": "",
        "version": "1.1.1",
        "branch": const.STABLE_BRANCH,
        "clients": 1,
        "active": 0,
        "downloaded": 1,
        "errors": 1,
        "states": "F"
    }
])
STATUS_MESSAGE_ACTIVE = helpers.make_old_style_ecstatic_status([
    {
        "name": "zzz",
        "tag": "",
        "version": "1.1.1",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 2,
        "downloaded": 2,
        "errors": 0,
        "states": "AA"
    }
])


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_wait_active_if_branch_missing(status_mock, sleep_mock):
    status_mock.side_effect = [STATUS_MESSAGE_EMPTY, STATUS_MESSAGE_READY_IN_TESTING]

    _create_dataset_and_wait_until_activation()

    assert status_mock.call_count == 2
    status_mock.assert_has_calls([
        mock.call(dataset="zzz", branch=const.STABLE_BRANCH),
        mock.call(dataset="zzz", branch=None)
    ])
    sleep_mock.assert_not_called()


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_wait_active_if_dataset_missing(status_mock, sleep_mock):
    status_mock.return_value = STATUS_MESSAGE_EMPTY

    with pytest.raises(tasks.EcstaticError) as ex:
        _create_dataset_and_wait_until_activation()

    assert "Dataset 'zzz' does not have any version" in str(ex.value)
    assert status_mock.call_count == 2
    sleep_mock.assert_not_called()


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_wait_active_if_newer_version_uploaded(status_mock, sleep_mock):
    status_mock.side_effect = [STATUS_MESSAGE_NOT_ACTIVE_YET, STATUS_MESSAGE_NEWER_VERSION_IS_ACTIVE]

    with pytest.raises(tasks.EcstaticError) as ex:
        _create_dataset_and_wait_until_activation()

    assert "Version changed during waiting for activation of the dataset 'zzz=1.1.1' in branch 'stable'" in str(ex.value)
    assert status_mock.call_count == 2
    assert sleep_mock.call_count == 1


@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_wait_active_timeout(status_mock):
    status_mock.return_value = STATUS_MESSAGE_NOT_ACTIVE_YET

    with pytest.raises(tasks.EcstaticError) as ex:
        _create_dataset_and_wait_until_activation()

    assert "Dataset zzz=1.1.1 is not active on all groups after 2 seconds of waiting" in str(ex.value)


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_wait_active_if_switch_fails(status_mock, sleep_mock):
    status_mock.return_value = STATUS_MESSAGE_SWITCH_FAILED

    with pytest.raises(tasks.EcstaticError) as ex:
        _create_dataset_and_wait_until_activation()

    assert "Dataset 'zzz=1.1.1' hooks have failed" in str(ex.value)
    sleep_mock.assert_not_called()


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_wait_active_success(status_mock, sleep_mock):
    status_mock.side_effect = [STATUS_MESSAGE_NOT_ACTIVE_YET, STATUS_MESSAGE_ACTIVE]

    _create_dataset_and_wait_until_activation()

    assert status_mock.call_count == 2
    assert sleep_mock.call_count == 1
