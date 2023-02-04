from unittest import mock
import pytest

import maps.pylibs.yandex_environment.environment as yenv

from maps.garden.sdk.core import Version

from maps.garden.sdk.ecstatic import const
from maps.garden.sdk.ecstatic import resources
from maps.garden.sdk.ecstatic import tasks


def _create_and_move_dataset(branch, hold):
    resource = resources.DatasetResource(
        "name", "aaa:{tag}", "001"
    )
    resource.exists = True
    resource.version = Version(properties={"tag": "v1"})
    resource.save_yandex_environment()

    task = tasks.MoveTask(branch=branch, direction='+', hold=hold)
    task(resource)


@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.move_dataset_version")
def test_move_task_without_hold(move_mock):
    _create_and_move_dataset(branch=const.STABLE_BRANCH, hold=False)

    target_branch = "+stable"
    move_mock.assert_called_once_with(
        dataset="aaa:v1",
        version="001",
        branch=target_branch
    )


@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.move_dataset_version")
def test_move_task_with_hold(move_mock):
    _create_and_move_dataset(branch=const.STABLE_BRANCH, hold=True)

    target_branch = "+stable/hold"
    move_mock.assert_called_once_with(
        dataset="aaa:v1",
        version="001",
        branch=target_branch
    )


def test_move_task_in_different_environment(monkeypatch):
    resource = resources.DatasetResource(
        "name", "aaa:{tag}", "001"
    )
    resource.exists = True
    resource.version = Version(properties={"tag": "v1"})
    resource.save_yandex_environment()

    monkeypatch.setattr(yenv, "get_yandex_environment", lambda: "another")

    task = tasks.MoveTask(branch=const.STABLE_BRANCH, direction='+', hold=False)

    with pytest.raises(AssertionError):
        task(resource)
