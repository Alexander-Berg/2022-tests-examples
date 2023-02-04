# coding: utf-8
from __future__ import unicode_literals

import copy
import mock
import pytest

from infra.rtc.walle_validator.lib.errors import ValidationError
from infra.rtc.walle_validator.lib.constants import RTC_TAG
from infra.rtc.walle_validator.lib.writer import upload_projects, make_diff, generate_reason
from infra.rtc.walle_validator.lib.transform import transform_project


def test_project_writer_do_nothing(all_projects, host_counts):
    mocked_client = mock.Mock()
    local_projects = {project.id: project for project in all_projects}
    remote_projects = {project.id: project for project in all_projects}
    assert not upload_projects(mocked_client, local_projects, remote_projects, host_counts, push=True, yes=True)
    assert not mocked_client.mock_calls


def test_project_delete_protection_check(all_projects, host_counts):
    mocked_client = mock.Mock()
    local_projects = {project.id: project for project in all_projects}
    remote_projects = {project.id: project for project in all_projects}

    deleted_project_id = sorted(local_projects)[0]
    host_counts = dict(host_counts)
    host_counts[deleted_project_id] = 1
    local_projects.pop(deleted_project_id)

    with pytest.raises(ValidationError):
        upload_projects(mocked_client, local_projects, remote_projects, host_counts, push=False)


def test_project_create_and_delete(all_projects, host_counts):
    local_projects = {project.id: project for project in all_projects}
    remote_projects = {project.id: project for project in all_projects}

    reference_project_id, reference_project = sorted(local_projects.items())[0]
    new_project_id = "{}-should-not-be-used".format(reference_project_id)
    assert new_project_id not in local_projects
    new_project = copy.deepcopy(reference_project)
    new_project.id = new_project_id
    new_project.name = new_project_id.replace("_", " ").replace("-", " ")
    local_projects[new_project_id] = new_project

    mocked_client = mock.Mock()
    assert upload_projects(mocked_client, local_projects, remote_projects, host_counts, push=True, yes=True)
    assert mocked_client.mock_calls
    mocked_client.add_project.assert_called_with(
        id=new_project_id,
        name=new_project.name,
        provisioner="lui",
        deploy_config="web",
        bot_project_id=100001955,
        tags=[RTC_TAG],
        reason=generate_reason()
    )
    remote_projects[new_project_id] = new_project

    mocked_client = mock.Mock()
    assert not upload_projects(mocked_client, local_projects, remote_projects, host_counts, push=True, yes=True)
    assert not mocked_client.mock_calls

    local_projects.pop(new_project_id)
    mocked_client = mock.Mock()
    assert upload_projects(mocked_client, local_projects, remote_projects, host_counts, push=True, yes=True)
    assert mocked_client.mock_calls
    mocked_client.remove_project.assert_called_with(id=new_project_id, reason=generate_reason())
    remote_projects.pop(new_project_id)

    mocked_client = mock.Mock()
    assert not upload_projects(mocked_client, local_projects, remote_projects, host_counts, push=True, yes=True)
    assert not mocked_client.mock_calls


def test_project_transform_do_nothing(project, host_counts):
    old_project_spec = project.to_dict()
    touched = transform_project(project, host_counts)
    new_project_spec = project.to_dict()
    assert not touched, "diff: {}".format(make_diff(old_project_spec, new_project_spec))
    assert old_project_spec == new_project_spec, "project changed but not touched"
