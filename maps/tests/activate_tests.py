from unittest import mock

import pytest

import maps.pylibs.yandex_environment.environment as yenv

from maps.garden.sdk.core import Version
from maps.garden.sdk.core import optional as optional_resource
from maps.garden.sdk.resources import BuildParamsResource
from maps.garden.sdk.resources.build_params import BUILD_PARAMS_RESOURCE_NAME

from maps.garden.sdk.ecstatic import const
from maps.garden.sdk.ecstatic import resources
from maps.garden.sdk.ecstatic import tasks

from . import helpers


################################
# TEST ACTIVATING SINGLE_DATASET
################################

def _create_and_activate_dataset():
    resource = resources.DatasetResource(
        "noname",
        dataset_name_template="dataset",
        dataset_version_template="2.0"
    )
    resource.exists = True
    resource.version = Version(properties={})
    resource.save_yandex_environment()

    task = tasks.ActivateTask("stable")
    task(ecstatic_dataset=resource)


# initial state, our version (2.0) is ready, and we have one active version atop of it (3.0)
STATUS_MESSAGE_HIGHER_VERSION_ACTIVE = helpers.make_old_style_ecstatic_status([
    {
        "name": "dataset",
        "tag": "",
        "version": "1.0",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 0,
        "downloaded": 2,
        "errors": 0,
        "states": "RR"
    },
    {
        "name": "dataset",
        "tag": "",
        "version": "2.0",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 0,
        "downloaded": 2,
        "errors": 0,
        "states": "RR"
    },
    {
        "name": "dataset",
        "tag": "",
        "version": "3.0",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 2,
        "downloaded": 2,
        "errors": 0,
        "states": "AA"
    }
])
# final state, version 2.0 is active and version 3.0 is placed on hold
STATUS_MESSAGE_OUR_VERSION_ACTIVE = helpers.make_old_style_ecstatic_status([
    {
        "name": "dataset",
        "tag": "",
        "version": "1.0",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 0,
        "downloaded": 2,
        "errors": 0,
        "states": "RR"
    },
    {
        "name": "dataset",
        "tag": "",
        "version": "2.0",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 2,
        "downloaded": 2,
        "errors": 0,
        "states": "AA"
    },
    {
        "name": "dataset",
        "tag": "",
        "version": "3.0",
        "branch": const.STABLE_BRANCH,
        "clients": 2,
        "active": 0,
        "downloaded": 2,
        "errors": 0,
        "states": "RR"
    }
])


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.move_dataset_version")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_activating_single_dataset(status_mock, move_mock, sleep_mock):
    # note that initial_state is used twice to emulate delayed activation of the dataset
    status_mock.side_effect = [
        STATUS_MESSAGE_HIGHER_VERSION_ACTIVE,
        STATUS_MESSAGE_HIGHER_VERSION_ACTIVE,
        STATUS_MESSAGE_OUR_VERSION_ACTIVE
    ]

    _create_and_activate_dataset()

    assert move_mock.call_count == 2
    move_mock.assert_has_calls([
        mock.call(
            dataset="dataset",
            version="2.0",
            branch="+stable"
        ),
        mock.call(
            dataset="dataset",
            version="3.0",
            branch="+stable/hold"
        )
    ])

    assert status_mock.call_count == 3
    sleep_mock.assert_called_once()


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.move_dataset_version")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_activating_single_dataset_with_deploy_step(status_mock, move_mock, sleep_mock):
    # note that initial_state is used twice to emulate delayed activation of the dataset
    status_mock.side_effect = [
        STATUS_MESSAGE_HIGHER_VERSION_ACTIVE,
        STATUS_MESSAGE_HIGHER_VERSION_ACTIVE,
        STATUS_MESSAGE_OUR_VERSION_ACTIVE
    ]

    ecstatic_resource = resources.DatasetResource(
        "noname",
        dataset_name_template="dataset",
        dataset_version_template="2.0"
    )
    ecstatic_resource.version = Version(properties={})
    ecstatic_resource.save_yandex_environment()
    ecstatic_resource.logged_commit()

    build_params_resource = BuildParamsResource(BUILD_PARAMS_RESOURCE_NAME)
    build_params_resource.version = Version(properties={"deploy_step": "stable"})
    build_params_resource.logged_commit()

    task = tasks.ActivateTask(allowed_deploy_steps=["testing", "stable"])
    task(ecstatic_resource, build_params=build_params_resource)

    assert move_mock.call_count == 2
    move_mock.assert_has_calls([
        mock.call(
            dataset="dataset",
            version="2.0",
            branch="+stable"
        ),
        mock.call(
            dataset="dataset",
            version="3.0",
            branch="+stable/hold"
        )
    ])

    assert status_mock.call_count == 3
    sleep_mock.assert_called_once()


def test_activating_single_dataset_in_different_environment(monkeypatch):
    resource = resources.DatasetResource(
        "noname",
        dataset_name_template="dataset",
        dataset_version_template="2.0"
    )
    resource.exists = True
    resource.version = Version(properties={})
    resource.save_yandex_environment()

    monkeypatch.setattr(yenv, "get_yandex_environment", lambda: "another")

    task = tasks.ActivateTask("stable")

    with pytest.raises(AssertionError):
        task(ecstatic_dataset=resource)


###################################
# TEST ACTIVATING MULTIPLE DATASETS
###################################


def _create_and_activate_multiple_datasets():
    resource1 = resources.DatasetResource(
        name="name1",
        dataset_name_template="dataset1",
        dataset_version_template="1.0"
    )
    resource1.exists = True
    resource1.version = Version(properties={})
    resource1.save_yandex_environment()

    resource2 = resources.DatasetResource(
        name="name2",
        dataset_name_template="dataset2",
        dataset_version_template="2.0"
    )
    resource2.exists = True
    resource2.version = Version(properties={})
    resource2.save_yandex_environment()

    task = tasks.ActivateTask(const.STABLE_BRANCH)

    task(resource1, resource2=resource2)


@pytest.fixture()
def make_next_status():
    STATUS_CALLS = {
        "dataset1": 0,
        "dataset2": 0
    }

    def _make_dataset1_next_status(current_calls):
        if current_calls == 0:
            return helpers.make_old_style_ecstatic_status([
                {
                    "name": "dataset1",
                    "tag": "",
                    "version": "1.0",
                    "branch": const.STABLE_BRANCH,
                    "clients": 2,
                    "active": 0,
                    "downloaded": 2,
                    "errors": 0,
                    "states": "RR"
                },
                {
                    "name": "dataset1",
                    "tag": "",
                    "version": "2.0",
                    "branch": const.STABLE_BRANCH,
                    "clients": 2,
                    "active": 2,
                    "downloaded": 2,
                    "errors": 0,
                    "states": "AA"
                }
            ])
        else:
            assert current_calls == 1
            return helpers.make_old_style_ecstatic_status([
                {
                    "name": "dataset1",
                    "tag": "",
                    "version": "1.0",
                    "branch": const.STABLE_BRANCH,
                    "clients": 2,
                    "active": 0,
                    "downloaded": 2,
                    "errors": 0,
                    "states": "AA"
                },
                {
                    "name": "dataset1",
                    "tag": "",
                    "version": "2.0",
                    "branch": const.STABLE_BRANCH,
                    "clients": 2,
                    "active": 2,
                    "downloaded": 2,
                    "errors": 0,
                    "states": "RR"
                }
            ])

    def _make_dataset2_next_status(current_calls):
        if current_calls in (0, 1):
            # emulating delayed activation by making extra
            return helpers.make_old_style_ecstatic_status([
                {
                    "name": "dataset2",
                    "tag": "",
                    "version": "1.0",
                    "branch": const.STABLE_BRANCH,
                    "clients": 2,
                    "active": 0,
                    "downloaded": 2,
                    "errors": 0,
                    "states": "RR"
                },
                {
                    "name": "dataset2",
                    "tag": "",
                    "version": "2.0",
                    "branch": const.STABLE_BRANCH,
                    "clients": 2,
                    "active": 0,
                    "downloaded": 2,
                    "errors": 0,
                    "states": "RR"
                },
                {
                    "name": "dataset2",
                    "tag": "",
                    "version": "3.0",
                    "branch": const.STABLE_BRANCH,
                    "clients": 2,
                    "active": 2,
                    "downloaded": 2,
                    "errors": 0,
                    "states": "AA"
                }
            ])
        else:
            assert current_calls == 2
            return helpers.make_old_style_ecstatic_status([
                {
                    "name": "dataset2",
                    "tag": "",
                    "version": "1.0",
                    "branch": const.STABLE_BRANCH,
                    "clients": 2,
                    "active": 0,
                    "downloaded": 2,
                    "errors": 0,
                    "states": "RR"
                },
                {
                    "name": "dataset2",
                    "tag": "",
                    "version": "2.0",
                    "branch": const.STABLE_BRANCH,
                    "clients": 2,
                    "active": 0,
                    "downloaded": 2,
                    "errors": 0,
                    "states": "AA"
                },
                {
                    "name": "dataset2",
                    "tag": "",
                    "version": "3.0",
                    "branch": const.STABLE_BRANCH,
                    "clients": 2,
                    "active": 2,
                    "downloaded": 2,
                    "errors": 0,
                    "states": "RR"
                }
            ])

    def _make_next_status(dataset, branch):
        assert branch == const.STABLE_BRANCH
        current_calls = STATUS_CALLS[dataset]
        STATUS_CALLS[dataset] += 1
        if dataset == "dataset1":
            return _make_dataset1_next_status(current_calls)
        else:
            assert dataset == "dataset2"
            return _make_dataset2_next_status(current_calls)

    return _make_next_status


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.move_dataset_version")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_activating_multiple_datasets(status_mock, move_mock, sleep_mock, make_next_status):
    status_mock.side_effect = make_next_status

    _create_and_activate_multiple_datasets()

    assert sleep_mock.call_count == 1
    assert move_mock.call_count == 4
    move_mock.assert_has_calls([
        # activating requested version of dataset1
        mock.call(
            dataset="dataset1",
            version="1.0",
            branch="+stable"
        ),
        # activating requested version of dataset2
        mock.call(
            dataset="dataset2",
            version="2.0",
            branch="+stable"
        ),
        # putting higher version of dataset1 on hold
        mock.call(
            dataset="dataset1",
            version="2.0",
            branch="+stable/hold"
        ),
        # putting higher version of dataset2 on hold
        mock.call(
            dataset="dataset2",
            version="3.0",
            branch="+stable/hold"
        )
    ])
    assert status_mock.call_count == 5
    status_mock.assert_has_calls([
        # call from hold_other_versions(resource1)
        mock.call(dataset="dataset1", branch=const.STABLE_BRANCH),
        # call from hold_other_versions(resource2)
        mock.call(dataset="dataset2", branch=const.STABLE_BRANCH),
        # call from _wait_single_dataset(resource1)
        mock.call(dataset="dataset1", branch=const.STABLE_BRANCH),
        # first call from _wait_single_dataset(resource2)
        mock.call(dataset="dataset2", branch=const.STABLE_BRANCH),
        # second call from _wait_single_dataset(resource2)
        mock.call(dataset="dataset2", branch=const.STABLE_BRANCH)
    ])


###################################
# TEST ACTIVATING OPTIONAL DATASETS
###################################


def _create_and_activate_optional_datasets():
    opt_resource = optional_resource.OptionalResource(
        resources.DatasetResource(
            name="name",
            dataset_name_template="dataset",
            dataset_version_template="2.0"
        )
    )
    opt_resource.exists = True
    opt_resource.version = Version(properties={})
    opt_resource.save_yandex_environment()

    empty_resource = optional_resource.make_empty_resource(name="empty")
    empty_resource.exists = True

    task = tasks.ActivateTask(const.STABLE_BRANCH)
    task(opt_resource, empty_resource)


@mock.patch("time.sleep")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.move_dataset_version")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_deploy_status")
def test_activating_optional_datasets(status_mock, move_mock, sleep_mock):
    status_mock.side_effect = [
        STATUS_MESSAGE_HIGHER_VERSION_ACTIVE,
        STATUS_MESSAGE_OUR_VERSION_ACTIVE
    ]

    _create_and_activate_optional_datasets()

    assert move_mock.call_count == 2
    assert move_mock.has_calls([
        mock.call(
            dataset="dataset",
            version="2.0",
            branch="+stable"
        ),
        mock.call(
            dataset="dataset",
            version="3.0",
            branch="+stable/hold"
        )
    ])
    assert status_mock.call_count == 2
    sleep_mock.assert_not_called()
